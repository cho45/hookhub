package net.lowreal.hookhub
import net.lowreal.skirts._
import com.google.appengine.api.datastore._



import java.util.UUID
import java.util.Date

object Hook extends Hook()
class Hook extends DS[Hook]() {

	def randomToken ():String = {
		UUID.randomUUID().toString().replaceAll("-", "")
	}

	def user_= (o:String) = update('user, o)
	def user = apply('user, "").asInstanceOf[String]

	def title_= (o:String) = update('title, o)
	def title = apply('title, "").asInstanceOf[String]

	def result_= (o:String) = update('result, o)
	def result = apply('result, "").asInstanceOf[String]

	def token_= (o:String) = update('token, o)
	def token = apply('token, "").asInstanceOf[String]

	def code_= (str:String) = update('code, new Text(str))
	def code = apply('code, new Text("")).asInstanceOf[Text].getValue

	def created_= (o:Date) = update('created, o)
	def created = apply('created, "").asInstanceOf[Date]

	def last_hooked_= (o:Date) = update('last_hooked, o)
	def last_hooked = apply('last_hooked, "").asInstanceOf[Date]

	def updateToken () {
		this.token = randomToken
	}
}

object Config extends Config()
class Config extends DS[Config]() {
	def name_= (o:String) = update('name, o)
	def name = apply('name, "").asInstanceOf[String]

	def value_= (o:String) = update('value, o)
	def value = apply('value, "").asInstanceOf[String]
}

object UserInfo extends UserInfo()
class UserInfo extends DS[UserInfo]() {
	def randomToken ():String = {
		UUID.randomUUID().toString().replaceAll("-", "")
	}

	def nick_= (o:String) = update('nick, o)
	def nick = apply('nick, "").asInstanceOf[String]

	def config = Config.select('user -> email)
	def hooks  = Hook.select('user -> email)
	
	def token_= (o:String) = update('token, o)
	def token = apply('token, "").asInstanceOf[String]

	def updateToken () {
		this.token = randomToken
	}
}



