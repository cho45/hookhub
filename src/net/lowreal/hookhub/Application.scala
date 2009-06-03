package net.lowreal.hookhub

import net.lowreal.skirts._
import javax.servlet.http.{HttpServlet, HttpServletResponse, HttpServletRequest}

trait Req {
	def method = ""

	def unapply (req: HttpServletRequest) = {
		if ( method == "" || req.getMethod == method ) {
			Some(req.getRequestURI)
		} else {
			Some()
		}
	}
}

object GET  extends Req { override def method = "GET" }
object POST extends Req { override def method = "POST" }
object PUT  extends Req { override def method = "PUT" }
object HEAD extends Req { override def method = "HEAD" }

class AppHttpRouter extends HttpRouter {

	override def dispatch (req: HttpServletRequest, res: HttpServletResponse):Unit = req match {
		case GET("/") => {
			println("get")
		}

		case POST("/") => {
			println("post")
		}

		case _ => {
			println("404")
		}
	}
}
