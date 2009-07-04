package net.lowreal.hookhub

import javax.servlet.http.{HttpServlet, HttpServletResponse, HttpServletRequest}
import scala.collection.jcl.Conversions._

import net.lowreal.skirts._
import com.google.appengine.api.users.{User, UserService, UserServiceFactory}

class AppHttpRouter extends HttpRouter {
	val US    = UserServiceFactory.getUserService
	implicit def ctx2myctx (c:Context) = new MyContext(c)
	class MyContext (c:Context) extends Context(c.req, c.res) with Proxy {
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
			if (c.user.getEmail != c.req.param("user")) {
				throw new Redirect("/")
			}
		}

		def user () = {
			US.getCurrentUser
		}
		
		val rhino = new RhinoView[MyContext]
		def view (name:String) = {
			rhino(name, this)
		}
	}

	route("/") { c =>
		c.view("index")
	}

	route("/register") { c =>
		c.req.method match {
			case "POST" => {
				c.res.content("post");
			}
			case _ => {
				c.res.content("get");
			}
		}
	}

	route("/my") { c => 
		c.requireUser
		c.res.content("you are " + c.user.getNickname)
	}


	route("/:user") { c => 
		c.res.redirect("/" + c.req.param("user") + "/")
	}

	route("/:user/") { c => 
		c.res.content("this is " + c.req.param("user") + "'s page")
	}

	route("/:user/hooks") { c => 
	}

	route("/:user/config") { c => 
		c.requireUserIsAuthor
	}
}
