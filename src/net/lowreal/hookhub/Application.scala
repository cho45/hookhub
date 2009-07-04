package net.lowreal.hookhub

import javax.servlet.http.{HttpServlet, HttpServletResponse, HttpServletRequest}
import scala.collection.jcl.Conversions._

import net.lowreal.skirts._
import com.google.appengine.api.users.{User, UserService, UserServiceFactory}

class AppHttpRouter extends HttpRouter {
	val US    = UserServiceFactory.getUserService

	implicit def ctx2myctx (c:Context) = new MyContext(c)
	class MyContext (c:Context) {
		def requireUser () = {
			val user = US.getCurrentUser
			if (user == null) {
				throw new Redirect(US.createLoginURL(c.req.requestURI))
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
	}

	route("/") {
		_.res.content("hello")
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
