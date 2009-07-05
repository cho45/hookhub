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

		def requireUser () = {
			val user = US.getCurrentUser
			if (user == null) {
				throw new Redirect(loginURL)
			}
		}

		def requireUserIsAuthor () = {
			if (!userIsAuthor) {
				throw new Redirect("/")
			}
		}

		def userIsAuthor () = {
			c.user.getEmail == c.req.param("user")
		}

		def user () = {
			US.getCurrentUser
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
		val token  = c.req.param("token")
		val hook   = Hook.find('token -> token).getOrElse(throw new NotFound)
		val stash  = new HashMap[String, Any]
		val config = new HashMap[String, String]
		for (c <- Config.select('user -> hook('user))) {
			config(c('key).asInstanceOf[String]) = c('value).asInstanceOf[String]
		}
		stash += "config" -> config
		stash += "params" -> c.req.param
		
		try {
			val res = HookRunner.run(hook('code).asInstanceOf[String], stash, c.file("js/init.js"))
			hook.param('result -> res)
			hook.save
			c.res.code(200)
			c.res.content("ok")
		} catch {
			case e:RhinoException => {
				hook.param('result -> e.details)
				hook.save
				c.res.code(500)
				c.res.content("error:" + e.details + " " + e.sourceName + ":" + e.lineNumber)
			}
			case e:TimeoutError => {
				hook.param('result -> "TimeoutError")
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
		c.req.method match {
			case "POST" => {
				val code  = c.req.param("code")
				val title = c.req.param("title")
				val hook  = Hook.create(
					'user      -> c.user.getEmail,
					'title     -> title,
					'code      -> code,
					'result    -> "",
					'token     -> Hook.randomToken,
					'created   -> new Date()
				)
				hook.save
				c.redirect("/" + c.user.getEmail + "/hook/" + hook.key.getId)
			}
			case _ => { }
		}

		c.stash("hook") = new Hook;
		c.view("hook.edit")
	}

	route("/:user/hook/:id") { c => 
		c.stash("hook") = Hook.find(c.req.param("id").toInt).getOrElse(throw new NotFound)
		c.view("hook")
	}

	route("/:user/hook/:id/edit") { c => 
		val hook = Hook.find(c.req.param("id").toInt).getOrElse(throw new NotFound)
		c.stash("hook") = hook
		c.req.method match {
			case "POST" => {
				val title = c.req.param("title")
				val code  = c.req.param("code")
				hook.param('title -> title, 'code -> code)
				hook.save
				c.redirect("/" + c.user.getEmail + "/hook/" + hook.key.getId)
			}
			case _ => { }
		}

		c.view("hook.edit")
	}

	route("/:user/config") { c => 
		c.requireUserIsAuthor

		c.req.method match {
			case "POST" => {
				val key    = c.req.param("key")
				val value  = c.req.param("value")
				val config = Config.ensure('user -> c.user.getEmail, 'key -> key)
				config.param('value -> value)
				config.save
			}
			case _ => { }
		}

		c.stash("configs") = Config.select('user -> c.user.getEmail, 'order -> 'key).toList
		c.view("user/config")
	}
}
