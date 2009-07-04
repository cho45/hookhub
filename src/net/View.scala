package net.lowreal.hookhub

// import net.lowreal.skirts._
import org.mozilla.javascript._
class RhinoView[T <: net.lowreal.skirts.Context] {
	def apply(name:String, context:T) = {
		context.res.header("Content-Type", "text/html; charset=utf-8")
		context.res.content(render(name, context))
	}

	def render (name:String, context:T):String = {
		val ctx = Context.enter()
		ctx.setLanguageVersion(Context.VERSION_1_7)

		val scope = ctx.initStandardObjects()

		ScriptableObject.putProperty(scope, "c", Context.javaToJS(context, scope));
		ScriptableObject.putProperty(scope, "v", Context.javaToJS(this, scope));

		val template = context.file(name + ".html")
		ScriptableObject.putProperty(scope, "template", Context.javaToJS(template, scope));


		val ejs = context.file("js/ejs.js")
		val result = ctx.evaluateString(scope, ejs, name, 1, null) 

		Context.toString(result)
	}
}

