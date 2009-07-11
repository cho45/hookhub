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

	def user_= (o:UserInfo) = update('user, o.email)
	def user = Cache.key[UserInfo](apply('user, "")) {
		UserInfo.find('email -> apply('user, "")).getOrElse(null)
	}

	def title_= (o:String) = update('title, o)
	def title = apply[String]('title, "")

	def result_= (o:String) = update('result, o)
	def result = apply[String]('result, "")

	def token_= (o:String) = update('token, o)
	def token = apply[String]('token, "")

	def code_= (str:String) = update('code, new Text(str))
	def code = apply[Text]('code, new Text("")).getValue

	def parent_= (o:Hook) = update('parent, o.id)
	def parent = apply[Long]('parent).map { Hook.find(_).getOrElse(null) }.getOrElse[Hook](null)

	def created_= (o:Date) = update('created, o)
	def created = apply[Date]('created, new Date(0))

	def last_hooked_= (o:Date) = update('last_hooked, o)
	def last_hooked = apply[Date]('last_hooked, new Date(0))

	def updateToken () {
		this.token = randomToken
	}
}

object Config extends Config()
class Config extends DS[Config]() {
	def name_= (o:String) = update('name, o)
	def name = apply[String]('name, "")

	def value_= (o:String) = update('value, o)
	def value = apply[String]('value, "")
}

import java.util._
import java.io._
import java.security._
object UserInfo extends UserInfo()
class UserInfo extends DS[UserInfo]() {
	def nick_= (o:String) = update('nick, o)
	def nick = apply[String]('nick, "")

	def email_= (o:String) = update('email, o)
	def email = apply[String]('email, "")

	def config = Config.select('user -> email)
	def hooks  = Hook.select('user -> email)

	def icon ():String = {
		def hex (array:Array[byte]):String = {
			val sb = new StringBuffer()
			for (c <- array) {
				sb.append(Integer.toHexString((c  & 0xFF) | 0x100).substring(1, 3));
			}
			sb.toString
		}

		val md = MessageDigest.getInstance("MD5");
		"http://www.gravatar.com/avatar/" + hex(md.digest(email.getBytes("UTF-8"))) + "?s=60"
	}
}

object Comment extends Comment()
class Comment extends DS[Comment]() {
	def user_= (o:UserInfo) = update('user, o.email)
	def user = Cache.key[UserInfo](apply('user, "")) {
		UserInfo.find('email -> apply('user, "")).getOrElse(null)
	}

	def parent_= (o:Hook) = update('parent, o.id)
	def parent = apply[Long]('parent).map { Hook.find(_).getOrElse(null) }.getOrElse[Hook](null)

	def body_= (o:String) = update('body, o)
	def body = apply[String]('body, "")

	def created_= (o:Date) = update('created, o)
	def created = apply[Date]('created, new Date(0))
}


