package net.lowreal.hookhub
import net.lowreal.skirts._

class Session () extends DS[Session]() {
	def updateSession ():String = {
		"......."
	}
}
object Session extends Session()


import java.util.UUID
class Hook extends DS[Hook]() {
	def randomToken ():String = {
		UUID.randomUUID().toString().replaceAll("-", "")
	}
}
object Hook extends Hook()

class Config extends DS[Config]() {
}
object Config extends Config()
