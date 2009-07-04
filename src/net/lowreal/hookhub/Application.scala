package net.lowreal.hookhub

import javax.servlet.http.{HttpServlet, HttpServletResponse, HttpServletRequest}
import scala.collection.jcl.Conversions._

import net.lowreal.skirts._
import com.google.appengine.api.users.{User, UserService, UserServiceFactory}

class AppHttpRouter extends HttpRouter {
	val US    = UserServiceFactory.getUserService

	class MyContext (c:Context) {
		def requireUser () = {
			val user = US.getCurrentUser
			if (user == null) {
				throw new Redirect(US.createLoginURL(c.req.requestURI))
			}
		}

		def user () = {
			val user = US.getCurrentUser
			Session.instantiate('mail -> user.getEmail)
		}
	}

	implicit def ctx2myctx (c:Context) = new MyContext(c)

	this reg ":user" -> "([A-Za-z][A-Za-z0-9_-]{2,30})"

	route("/") {
		_.res.content("hello")
	}

	route("/my") { c => 
		c.requireUser
		c.res.content("you are " + c.user.apply('mail))
	}

	route("/:user") { c => 
		c.res.redirect("/" + c.req.param("user") + "/")
	}

	route("/:user/") { c => 
		c.res.content("this is " + c.req.param("user") + "'s page")
	}
}
