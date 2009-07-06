package net.lowreal.hookhub

import java.net._
import java.io._
import java.lang.System
import org.mozilla.javascript._
import scala.collection.mutable.{HashMap, ArrayBuffer}
import scala.collection.jcl.Conversions._

class TimeoutError extends RuntimeException

object HookRunner {
	class Http {
		def request (obj:NativeObject):Unit = {
			if (!obj.has("url", obj)) return
			val method:String = if (obj.has("method", obj)) obj.get("method", obj).asInstanceOf[String] else "GET";

			val url  = new URL(obj.get("url", obj).asInstanceOf[String])
			val http = url.openConnection().asInstanceOf[HttpURLConnection]
			http.setConnectTimeout(3)
			http.setReadTimeout(3)
			http.setRequestMethod(method)
			http.setInstanceFollowRedirects(false)
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
				val headers = http.getHeaderFields()
				println( http.getResponseCode() )
				headers.foreach( println(_) )

				val reader = new BufferedReader(new InputStreamReader(http.getInputStream()))
				var line   = ""
				while ({ line = reader.readLine(); line != null }) {
					println(line)
				}
			} finally {
				http.disconnect()
			}
		}
	}

	class SandboxNativeJavaObject (scope: Scriptable, javaObject: Object, staticType: Class[_])
		extends NativeJavaObject(scope, javaObject, staticType) {

		override def get (name: String, start: Scriptable):Object = (javaObject, name) match {
			case ( _:Http, "request") => super.get(name, start)
			case ( _:HashMap[String, String], "apply") => super.get(name, start)
			case ( _:HashMap[String, String], "keys") => super.get(name, start)
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
			if (current >= ctx.startTime + 3000) {
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

			ctx.setClassShutter(new ClassShutter() {
				def visibleToScripts(fullClassName: String) = fullClassName match {
					case "net.lowreal.hookhub.HookRunner$Http" => true
					case _ => false
				}
			})
		//	tx.setSecurityController(new SecurityController() {
		//	})

			val scope = ctx.initStandardObjects()

			// ScriptableObject.putProperty(scope, "_args", Context.javaToJS(stash, scope));
			ScriptableObject.putProperty(scope, "http",  Context.javaToJS(new Http, scope));
			ScriptableObject.putProperty(scope, "stash", jsstash);
			scope.setAttributes("http", ScriptableObject.DONTENUM | ScriptableObject.PERMANENT | ScriptableObject.READONLY);
			ctx.evaluateString(scope, init, "<init>", 1, null)

			val result = ctx.evaluateString(scope, source, "<run>", 1, null)

			ret =  Context.toString(result)
		} finally {
			Context.exit()
		}
		ret
	}

}
