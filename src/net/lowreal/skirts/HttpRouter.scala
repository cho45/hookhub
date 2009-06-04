package net.lowreal.skirts

import javax.servlet._
import javax.servlet.http.{HttpServlet, HttpServletResponse, HttpServletRequest}

abstract class HttpRouter {
	def dispatch (req: HttpServletRequest, res: HttpServletResponse):Unit
}
