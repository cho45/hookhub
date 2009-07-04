package net.lowreal.skirts

import javax.servlet._
import javax.servlet.http.{HttpServlet, HttpServletResponse, HttpServletRequest}

import scala.collection.mutable.{HashMap, ArrayBuffer}
import scala.util.matching.Regex
import scala.util.matching.Regex.Match

import scala.collection.jcl._

import java.util.regex.Matcher
class MyString (s:String) {
	def replace (source:String, replace:(Matcher => String)) = {
		val m = source.r.pattern.matcher(s)
		val sb = new StringBuffer(32)
		while (m.find) {
			m.appendReplacement(sb, replace(m))
		}
		m.appendTail(sb)
		sb.toString
	}
}


import java.io._
class Context (val req:Request, val res:Response, val stash:HashMap[String, Any] ) {
	def redispatch (path:String) {
	}

	def file (path:String) = {
		val file = new File(path)
		val fis  = new FileInputStream(file)
		val isr  = new InputStreamReader(fis, "UTF-8")
		val br   = new BufferedReader(isr)
		val sb   = new StringBuffer(32)
		while (br.ready) {
			sb.append(br.readLine)
			sb.append("\n")
		}
		sb.toString
	}
}

class RequestException (val code:Int) extends Exception("RequestException " + code)
class Redirect (val url:String) extends RequestException(302)
class NotFound () extends RequestException(404)
class Success  () extends RequestException(200)

trait HttpRouter {
	class Route (val regexp:Regex, val source:String, val capture:Array[String], val handler:(Context => Unit)) extends Throwable

	val routing = new ArrayBuffer[Route]
	val regdefs = new HashMap[String, String]

	implicit def str2mystr (s:String) = new MyString(s)

	def route (source:String)(handler:Context => Unit) = {
		val capture = new ArrayBuffer[String]
		val regexp  = ("^" + source.replace("""([:*])(\w+)""",
			(m:Matcher) => {
				val t = m.group(1)
				val n = m.group(2)
				capture += n
				"(" + ( regdefs.getOrElse(m.group(0), if (t == "*") ".*" else "[^/]+" ) ) + ")"
			}
		) + "$").r

		routing += new Route(regexp, source, capture.toArray, handler);
	}

	def reg (o:(String, String)) = {
		regdefs += o
	}

	def dispatch (req0: HttpServletRequest, res0: HttpServletResponse):Unit = {
		val req = new Request(req0)
		val res = new Response(res0)
		val ctx = new Context(req, res, new HashMap)

		try {
			for (r <- routing) {
				val m = r.regexp.findFirstMatchIn(req.path)
				if (m.isDefined) {
					val capture:Match = m.get
					for ( (key, value) <- r.capture.zip(capture.subgroups.toArray)) {
						req.param(key) = value
					}
					res.code(200)
					res.header("Context-Type", "text/plain")
					r.handler(ctx)
					throw new Success
				}
			}
			throw new NotFound();
		} catch {
			case e:Redirect => {
				res.code(302)
				res.header("Location", e.url)
			}
			case r:Success => {}
		}
	}
}

class Request (req0:HttpServletRequest)  {
	req0.setCharacterEncoding("UTF-8")

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
	def path = requestURI

	def query () = req0.getQueryString

	def session (create:Boolean) = req0.getSession(create)
	def session ()               = req0.getSession(true)
	def sessionId ()             = req0.getRequestedSessionId

	val param = new HashMap[String, String]
	for ( (key, value) <- Map(req0.getParameterMap.asInstanceOf[java.util.Map[String, Array[String]]])) {
		param(key) = value.first
	}
}

class Response (res0:HttpServletResponse){
	res0.setCharacterEncoding("UTF-8")

	def header (name:String, value:String) = res0.setHeader(name, value)
	def code  (status:Int)                = res0.setStatus(status)
	def redirect (location:String)        = res0.sendRedirect(location)
	def content (body:String)             = res0.getWriter.println(body)
}
