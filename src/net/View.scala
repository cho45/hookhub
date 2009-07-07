package net.lowreal.hookhub

import scala.io.Source
import java.util._
import java.io._
import java.security._

// import net.lowreal.skirts._
import org.mozilla.javascript._
class RhinoView[T <: net.lowreal.skirts.Context] {
	def apply(name:String, context:T) = {
		context.res.header("Content-Type", "text/html; charset=utf-8")
		context.res.content(render(name, context))
	}

	def file (name:String):String = {
		Source.fromFile(name + ".html").mkString
	}

	def log (obj:Any) {
		println(obj)
	}

	def icon (name:String):String = {
		def hex (array:Array[byte]):String = {        
			val sb = new StringBuffer();        
			for (c <- array) {            
				sb.append(Integer.toHexString((c  & 0xFF) | 0x100).substring(1, 3));        
			}        
			sb.toString
		}

		val md = MessageDigest.getInstance("MD5");
		"http://www.gravatar.com/avatar/" + hex(md.digest(name.getBytes("CP1252"))) + "?s=60"
	}

	def name (name:String):String = {
		name.replaceAll("""@.+""", "") 
	}

	def render (name:String, context:T):String = {
		val ctx = Context.enter()
		try {
			ctx.setLanguageVersion(Context.VERSION_1_7)

			val scope = ctx.initStandardObjects()

			ScriptableObject.putProperty(scope, "c", Context.javaToJS(context, scope));
			ScriptableObject.putProperty(scope, "v", Context.javaToJS(this, scope));

			val template = file(name)
			ScriptableObject.putProperty(scope, "template", Context.javaToJS(template, scope));

			val ejs = context.file("js/ejs.js")
			val result = ctx.evaluateString(scope, ejs, name, 1, null) 

			Context.toString(result)
		} finally {
			Context.exit()
		}
	}
}

