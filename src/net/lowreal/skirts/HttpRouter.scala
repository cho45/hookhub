package net.lowreal.skirts

import javax.servlet._
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

abstract class HttpRouter {
	def dispatch (req: HttpServletRequest, res: HttpServletResponse):Unit
}

