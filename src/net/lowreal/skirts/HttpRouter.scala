package net.lowreal.skirts

import javax.servlet._
import javax.servlet.http.{HttpServlet, HttpServletResponse, HttpServletRequest}

import scala.collection.mutable.HashMap

abstract class HttpRouter {
	def dispatch (req0: HttpServletRequest, res0: HttpServletResponse):Unit = {
		val req = new Request(req0)
		val res = new Response(res0)
		val ctx = new Context(req, res)
		route(ctx)
	}

	def route (c:Context):Unit
}

class Context (val req:Request, val res:Response) {
}

class Request (req0:HttpServletRequest)  {
	def method () = req0.getMethod
	def cookie () = req0.getCookies
	def header () = {
		val ret = new HashMap[String, String]
		val e   = req0.getHeaderNames
		while (e.hasMoreElements) {
			val name = e.nextElement.asInstanceOf[String]
			ret += (name -> req0.getHeader(name))
		}
		ret
	}
	def requestURI () = req0.getRequestURI
	def requestURL () = req0.getRequestURL

	def session (create:Boolean) = req0.getSession(create)
	def session ()               = req0.getSession(true)
	def sessionId ()             = req0.getRequestedSessionId
}

class Response (res0:HttpServletResponse){
	res0.setCharacterEncoding("UTF-8")

	def header (name:String, value:String) = res0.setHeader(name, value)
	def code  (status:Int)                = res0.setStatus(status)
	def redirect (location:String)        = res0.sendRedirect(location)
	def content (body:String)             = res0.getWriter.println(body)
}
