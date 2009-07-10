package net.lowreal.hookhub

import java.util._
import java.net._
import java.io._
import java.security._
import java.lang.System
import javax.crypto.spec.SecretKeySpec
import javax.crypto.Mac 

import org.mozilla.javascript._

import org.apache.commons.codec.binary.Base64

import scala.collection.mutable.{HashMap, ArrayBuffer}
import scala.collection.jcl.Conversions._

class TimeoutError extends RuntimeException
class RestrictError(m:String) extends RuntimeException(m)

object HookRunner {
	class Proxy(stash: HashMap[String, Any], scope: ScriptableObject) {
		var maxReq = 3

		def request (obj:NativeObject):Object = withContext { ctx =>
			maxReq -= 1
			if (maxReq <= 0) throw new RestrictError("http request limit exceed")

			val ret = ctx.initStandardObjects

			if (!obj.has("url", obj)) return ret
			val method:String = (if (obj.has("method", obj)) obj.get("method", obj).asInstanceOf[String] else "GET").toUpperCase;

			val url  = new URL(obj.get("url", obj).asInstanceOf[String])
			if (url.getHost == "www.hookhub.com") throw new RestrictError("invalid host")

			val http = url.openConnection().asInstanceOf[HttpURLConnection]
			http.setConnectTimeout(3)
			http.setReadTimeout(3)
			http.setRequestMethod(method)
			http.setInstanceFollowRedirects(false)
			if (obj.has("headers", obj)) {
				val headers = obj.get("headers", obj).asInstanceOf[NativeObject]
				for (i <- headers.getIds) i match {
					case key: String => {
						http.setRequestProperty(key, headers.get(key, headers).asInstanceOf[String])
					}
				}
			}
			http.setRequestProperty("User-Agent", "Hookhub")
			if (method == "POST") {
				http.setDoOutput(true)
				val ow = new OutputStreamWriter(http.getOutputStream());
				val bw = new BufferedWriter(ow);
				bw.write(if (obj.has("data", obj)) obj.get("data", obj).asInstanceOf[String] else "");
				bw.close();
				ow.close();
			} else {
				http.connect()
			}

			try  {
				val jsheaders = ctx.initStandardObjects
				ret.put("code", ret, http.getResponseCode())
				ret.put("headers", ret, jsheaders)

				val headers = http.getHeaderFields()
				for ( (key, value) <- headers) {
					jsheaders.put(key.capitalize, jsheaders, value(0))
				}

				val reader = new BufferedReader(new InputStreamReader(http.getInputStream()))
				var line   = ""
				val sb     = new StringBuilder
				while ({ line = reader.readLine(); line != null }) {
					sb.append(line)
					sb.append("\n")
				}
				ret.put("body", ret, sb.mkString)
			} finally {
				http.disconnect()
			}
			ret
		}

		def mail (title:String, body:String) = {
			stash("mail") = title -> body
		}

		def digest_md5 (o:String):Object = {
			toHexString( MessageDigest.getInstance("MD5").digest(o.getBytes("UTF-8")) )
		}

		def digest_sha1 (o:String):Object = {
			toHexString( MessageDigest.getInstance("SHA1").digest(o.getBytes("UTF-8")) )
		}
//
//		def hmac_md5 (o:String):Object = {
//			val key = new SecretKeySpec(o.getBytes("UTF-8"), "HmacMD5")
//			val mac = Mac.getInstance(key.getAlgorithm)
//			mac.init(key)
//			toHexString( mac.doFinal("what do ya want for nothing?".getBytes ) )
//		}

		def base64_encode (o:String):Object = {
			new String(Base64.encodeBase64(o.getBytes("UTF-8")), "UTF-8")
		}

		def base64_decode (o:String):Object = {
			new String(Base64.decodeBase64(o.getBytes("UTF-8")), "UTF-8")
		}

		def toHexString (array:Array[byte]):String = {
			val sb = new StringBuffer()
			for (c <- array) {
				sb.append(Integer.toHexString((c  & 0xFF) | 0x100).substring(1, 3));
			}
			sb.toString
		}

		def withContext (block: Context => Object):Object = {
			val ctx = Context.enter()
			try {
				block(ctx)
			} finally {
				Context.exit()
			}
		}
	}

	class SandboxNativeJavaObject (scope: Scriptable, javaObject: Object, staticType: Class[_])
		extends NativeJavaObject(scope, javaObject, staticType) {

		override def get (name: String, start: Scriptable):Object = (javaObject, name) match {
			case ( _:Proxy, "request") => super.get(name, start)
			case ( _:Proxy, "mail") => super.get(name, start)
			case ( _:Proxy, "digest_md5") => super.get(name, start)
			case ( _:Proxy, "digest_sha1") => super.get(name, start)
			case ( _:Proxy, "hmac_md5") => super.get(name, start)
			case ( _:Proxy, "hmac_sha1") => super.get(name, start)
			case ( _:Proxy, "base64_encode") => super.get(name, start)
			case ( _:Proxy, "base64_decode") => super.get(name, start)
			case _ => { println(javaObject, name); null }
		}
	}

	class SandboxWrapFactory extends WrapFactory {
		override def wrapAsJavaObject (context: Context, scope: Scriptable, javaObject: Object, staticType: Class[_]):Scriptable = {
			new SandboxNativeJavaObject(scope, javaObject, staticType);
		}
	}

	class SandboxContext extends Context {
		var startTime:Long = 0;
	}

	class SandboxContextFactory extends ContextFactory {
		override def makeContext ():Context = {
			val ret = new SandboxContext()
			ret
		}

		override def observeInstructionCount (context: Context, instructionCount: Int):Unit = {
			val ctx = context.asInstanceOf[SandboxContext];
			val current = System.currentTimeMillis()
			if (current >= ctx.startTime + 5000) {
				throw new TimeoutError();
			}
		}

		override def doTopCall (callable: Callable, context: Context, scope: Scriptable, thisObject: Scriptable, args: Array[Object]):Object = {
			val ctx = context.asInstanceOf[SandboxContext]
			ctx.startTime = System.currentTimeMillis()
			super.doTopCall(callable, ctx, scope, thisObject, args)
		}
	}

	ContextFactory.initGlobal(new SandboxContextFactory());

	def run (source: String, stash: HashMap[String, Any], init:String):String = {
		if (source == null) return ""
		var ret = ""

		val ctx = Context.enter()
		try {
			def toJSObj (map:HashMap[String, Any]):ScriptableObject = {
				val ret = ctx.initStandardObjects
				for ( prop <- map ) prop match {
					case (key:String, value:String) => {
						ret.put(key, ret, value)
					}
					case (key:String, value:Long) => {
						ret.put(key, ret, value)
					}
					case (key:String, value:Boolean) => {
						ret.put(key, ret, value)
					}
					case (key:String, value:HashMap[String, Any]) =>  {
						ret.put(key, ret, toJSObj(value))
					}
				}
				ret
			}
			val jsstash = toJSObj(stash)

			ctx.setLanguageVersion(Context.VERSION_1_7)
			ctx.setInstructionObserverThreshold(1000)
			ctx.setWrapFactory(new SandboxWrapFactory())
			ctx.setOptimizationLevel(-1)
			ctx.setMaximumInterpreterStackDepth(1000)

			ctx.setClassShutter(new ClassShutter() {
				def visibleToScripts(fullClassName: String) = {
					fullClassName == classOf[Proxy].getName ||
					fullClassName == classOf[String].getName
				}
			})
		//	tx.setSecurityController(new SecurityController() {
		//	})

			val scope = ctx.initStandardObjects()

			ScriptableObject.putProperty(scope, "_proxy",  Context.javaToJS(new Proxy(stash, scope), scope));
			ScriptableObject.putProperty(scope, "stash", jsstash);
			// scope.setAttributes("http", ScriptableObject.DONTENUM);
			ctx.evaluateString(scope, init, "<init>", 1, null)

			val result = ctx.evaluateString(scope, source, "<run>", 1, null)

			jsstash.delete("config")

			// ret =  Context.toString(result)
			val uneval = scope.get("uneval", scope).asInstanceOf[Function]
			ret = uneval.call(ctx, scope, scope, Array(result)).toString
		} finally {
			Context.exit()
		}
		ret
	}

}
