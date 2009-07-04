package net.lowreal.hookhub

// import net.lowreal.skirts._
import org.mozilla.javascript._
class RhinoView[T <: net.lowreal.skirts.Context] {
	def apply(name:String, context:T) = {
		val ctx = Context.enter()
		ctx.setLanguageVersion(Context.VERSION_1_7)

		val scope = ctx.initStandardObjects()
		val jsskirtcontext = Context.javaToJS(context, scope);
		ScriptableObject.putProperty(scope, "c", jsskirtcontext);

		val template = context.file(name + ".html")
		ScriptableObject.putProperty(scope, "template", Context.javaToJS(template, scope));

		val ejs = context.file("js/ejs.js")
		val result = ctx.evaluateString(scope, ejs, name, 1, null) 

		val ret = Context.toString(result)
		context.res.header("Content-Type", "text/html")
		context.res.content(ret)
	}
}

