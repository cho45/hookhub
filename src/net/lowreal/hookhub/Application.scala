package net.lowreal.hookhub

import javax.servlet.http.{HttpServlet, HttpServletResponse, HttpServletRequest}
import scala.collection.jcl.Conversions._
import scala.collection.mutable.{HashMap, ArrayBuffer}

import net.lowreal.skirts._
import com.google.appengine.api.users.{User, UserService, UserServiceFactory}
import java.util.Date

import org.mozilla.javascript.RhinoException

class AppHttpRouter extends HttpRouter {
	val US    = UserServiceFactory.getUserService
	implicit def ctx2myctx (c:Context) = new MyContext(c)
	class MyContext (c:Context) extends Context(c.req, c.res, c.stash) with Proxy {
		def self = c

		def loginURL ():String = {
			US.createLoginURL(c.req.requestURI)
		}

		def logoutURL ():String = {
			US.createLogoutURL(c.req.requestURI)
		}

		def requireSid () = {
			c.req.sessionId == c.req.param("sid")
		}

		def requireUser () = {
			c.requireAdmin // XXX :
			val user = US.getCurrentUser
			if (user == null) {
				throw new Redirect(loginURL)
			}
		}

		def requireAdmin () = {
			if (! US.isUserAdmin) {
				throw new Redirect(loginURL)
			}
		}

		def requireUserIsAuthor () = {
			c.requireAdmin // XXX :
			if (!userIsAuthor) {
				throw new Redirect("/")
			}
		}

		def userIsAuthor ():Boolean = {
			if (c.user == null) return false
			c.user.getEmail == c.req.param("user")
		}

		def user () = {
			US.getCurrentUser
		}


		def absolute (path:String):String = {
			if (c.stash("_debug").asInstanceOf[Boolean]) {
				"http://localhost:8080" + path
			} else {
				"http://hookhub.appspot.com/" + path
			}
		}

		val rhino = new RhinoView[MyContext]
		def view (name:String) = {
			if (c.req.param.contains("user")) {
				c.stash("author") = c.req.param("user")
			}
			rhino(name, this)
		}
	}

	route("/") { c =>
		c.stash("hooks") = Hook.select('order -> ('last_hooked, 'desc)).take(10).toList
		c.view("index")
	}

//	route("/register") { c =>
//		c.req.method match {
//			case "POST" => {
//				c.res.content("post");
//			}
//			case _ => {
//				c.res.content("get");
//			}
//		}
//	}

	route("/my") { c => 
		c.requireUser
		c.res.redirect("/" + c.user.getEmail + "/")
	}

	route("/hook/:token") { c => 
		c.requireAdmin

		val token  = c.req.param("token")
		val hook   = Hook.find('token -> token).getOrElse(throw new NotFound)
		val stash  = new HashMap[String, Any]
		val config = new HashMap[String, String]
		for (c <- Config.select('user -> hook.user)) {
			config(c.name) = c.value
		}
		stash += "config" -> config
		stash += "params" -> c.req.param
		stash += "user"   -> hook.user
		stash += "id"     -> hook.key.getId
		
		try {
			val res = HookRunner.run(hook.code, stash, c.file("js/init.js"))
			hook.result = res.take(500).toString
			hook.last_hooked = new Date()
			hook.save
			c.res.code(200)
			c.res.content("ok: " + res)
		} catch {
			case e:RhinoException => {
				hook.result = e.details.take(500).toString
				hook.save
				c.res.code(500)
				c.res.content("error:" + e.details + " " + e.sourceName + ":" + e.lineNumber)
			}
			case e:TimeoutError => {
				hook.result = "timeout"
				hook.save
				c.res.code(500)
				c.res.content("timeout")
			}
		}
	}

	route("/:user") { c => 
		c.res.redirect("/" + c.req.param("user") + "/")
	}

	route("/:user/") { c => 
		if (c.req.param.contains("delete")) {
			val id = c.req.param("delete").toInt
			Hook.find(id) match {
				case None    => {}
				case Some(h) => h.delete
			}
		}

		c.stash("hooks") = Hook.select('user -> c.req.param("user"), 'order -> ('created, 'desc)).toList
		c.view("user")
	}

	route("/:user/hook/new") { c => 
		c.requireUserIsAuthor
		c.req.method match {
			case "POST" => {
				val code  = c.req.param("code")
				val title = c.req.param("title")
				val hook  = Hook.create
				hook.user    = c.user.getEmail
				hook.title   = title
				hook.result  = ""
				hook.code    = code
				hook.created = new Date
				hook.updateToken
				hook.save
				c.redirect("/" + c.user.getEmail + "/hook/" + hook.key.getId)
			}
			case _ => { }
		}

		c.stash("hook") = Hook.create
		c.view("hook.edit")
	}

	route("/:user/hook/:id") { c => 
		c.stash("hook") = Hook.find(c.req.param("id").toInt).getOrElse(throw new NotFound)
		c.view("hook")
	}

	route("/:user/hook/:id/edit") { c => 
		c.requireUserIsAuthor

		val hook = Hook.find(c.req.param("id").toInt).getOrElse(throw new NotFound)
		c.stash("hook") = hook
		(c.req.method, c.req.param.getOrElse("mode", "create")) match {
			case ("POST", "create") => {
				c.requireSid
				val title = c.req.param("title")
				val code  = c.req.param("code")
				hook.title = title
				hook.code  = code
				hook.save
				c.redirect("/" + c.user.getEmail + "/hook/" + hook.key.getId)
			}
			case ("POST", "delete") => {
				c.requireSid
				hook.delete
				c.redirect("/" + c.user.getEmail + "/")
			}
			case _ => { }
		}

		c.view("hook.edit")
	}

	route("/:user/config") { c => 
		c.requireUserIsAuthor

		(c.req.method, c.req.param.getOrElse("mode", "create")) match {
			case ("POST", "create") => {
				c.requireSid

				val name   = c.req.param("name")
				val value  = c.req.param("value")
				val config = Config.ensure('user -> c.user.getEmail, 'name -> name)
				config.value = value
				config.save
			}
			case ("POST", "delete") => {
				c.requireSid

				val config = Config.find(c.req.param("id").toInt).getOrElse(throw new NotFound)
				config.delete
				c.redirect("/" + c.user.getEmail + "/config")
			}
			case _ => { }
		}

		c.stash("configs") = Config.select('user -> c.user.getEmail, 'order -> 'name).toList
		c.view("user/config")
	}
}
