package net.lowreal.hookhub

import java.lang.System
import org.mozilla.javascript._

object Rhino {
	ContextFactory.initGlobal(new SandboxContextFactory());

	def js (source: String):String = {
		if (source == null) return ""
		var ret = ""

		val ctx = Context.enter()
		ctx.setLanguageVersion(Context.VERSION_1_7)

		// Java のクラスを一切エクスポートしない
		ctx.setClassShutter(new ClassShutter() {
			def visibleToScripts(fullClassName: String) = false;
		})
	//	ctx.setSecurityController(new SecurityController() {
	//	})

		try {
			val scope = ctx.initStandardObjects()
			ctx.evaluateString(
				scope,
				"""
				Global = (function () { return this })();
				""",
				"<init>",
				1,
				null
			)

			// JSON に変換してセットがいい
			println("running")
			val result = ctx.evaluateString(
				scope,
				source,
				"<run>",
				1,
				null
			)


			ret =  Context.toString(result)
		} catch {
			case e: EcmaError => println(e)
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
		ret.setWrapFactory(new SandboxWrapFactory())
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
