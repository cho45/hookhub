package net.lowreal.hookhub

import java.lang.System
import org.mozilla.javascript._
import scala.collection.mutable.{HashMap, ArrayBuffer}

object HookRunner {
	def run (source: String, stash: HashMap[String, Any], init:String):String = {
		if (source == null) return ""
		var ret = ""

		val ctx = Context.enter()
		try {
			ctx.setWrapFactory(new SandboxWrapFactory())
			ctx.setLanguageVersion(Context.VERSION_1_7)

			ctx.setClassShutter(new ClassShutter() {
				def visibleToScripts(fullClassName: String) = fullClassName match {
					case "scala.collection.mutable.HashMap" => true
					case _ => false
				}
			})
		//	tx.setSecurityController(new SecurityController() {
		//	})

			val scope = ctx.initStandardObjects()

			ScriptableObject.putProperty(scope, "stash", Context.javaToJS(stash, scope));
			ctx.evaluateString(scope, init, "<init>", 1, null)

			val result = ctx.evaluateString(scope, source, "<run>", 1, null)

			ret =  Context.toString(result)
			println(ret)
		} finally {
			Context.exit()
		}
		ret
	}
}

class SandboxNativeJavaObject (scope: Scriptable, javaObject: Object, staticType: Class[_])
	extends NativeJavaObject(scope, javaObject, staticType) {

	override def get (name: String, start: Scriptable):Object = null
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
		ret.setInstructionObserverThreshold(1000)
		ret
	}

	override def observeInstructionCount (context: Context, instructionCount: Int):Unit = {
		val ctx = context.asInstanceOf[SandboxContext];
		val current = System.currentTimeMillis()
		if (current >= ctx.startTime + 1000) {
			throw new Error();
		}
	}

	override def doTopCall (callable: Callable, context: Context, scope: Scriptable, thisObject: Scriptable, args: Array[Object]):Object = {
		val ctx = context.asInstanceOf[SandboxContext]
		ctx.startTime = System.currentTimeMillis()
		super.doTopCall(callable, ctx, scope, thisObject, args)
	}
}
