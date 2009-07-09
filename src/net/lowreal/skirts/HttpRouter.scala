package net.lowreal.skirts

import javax.servlet._
import javax.servlet.http.{HttpServlet, HttpServletResponse, HttpServletRequest}

import scala.collection.mutable.{HashMap, ArrayBuffer}
import scala.util.matching.Regex
import scala.util.matching.Regex.Match

import scala.io.Source
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
class Context (val req:Request, val res:Response, val stash:HashMap[String, Any]) {
	var debug = false

	def redispatch (path:String) {
	}

	def redirect (path:String) {
		throw new Redirect(path)
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
class NotFound  () extends RequestException(404)
class Forbidden () extends RequestException(403)
class Success   () extends RequestException(200)

trait HttpRouter {
	implicit def str2mystr (s:String) = new MyString(s)

	class Route (val source:String, val handler:(Context => Unit)) extends Throwable {
		var capture = new ArrayBuffer[String]
		val regexp  = ("^" + source.replace("""([:*])(\w*)""",
			(m:Matcher) => {
				val t = m.group(1)
				val n = m.group(2)
				capture += n
				"(" + ( regdefs.getOrElse(m.group(0), if (t == "*") ".*" else "[^/]+" ) ) + ")"
			}
		) + "$").r
	}

	val beforeFilters = new ArrayBuffer[Route]
	val afterFilters  = new ArrayBuffer[Route]

	val routing = new ArrayBuffer[Route]

	val regdefs = new HashMap[String, String]
	var debug   = false

	def route (source:String)(handler:Context => Unit) = {
		routing += new Route(source, handler)
	}

	def before (source:String)(handler:Context => Unit) = {
		beforeFilters += new Route(source, handler)
	}

	def after (source:String)(handler:Context => Unit) = {
		afterFilters  += new Route(source, handler)
	}

	def reg (o:(String, String)) = {
		regdefs += o
	}

	def dispatch (routing:Array[Route], ctx: Context):Boolean = {
		try {
			for (r <- routing) {
				val m = r.regexp.findFirstMatchIn(ctx.req.path)
				if (m.isDefined) {
					val rcaptur       = r.capture.toArray
					val capture:Match = m.get
					for ( (key, value) <- rcaptur.zip(capture.subgroups.toArray) if key.length > 0) {
						ctx.req.param(key) = value
					}
					r.handler(ctx)
					throw new Success
				}
			}
			throw new NotFound
		} catch {
			case e:Success  => true
			case e:NotFound => false
		}
	}

	def dispatch (req0: HttpServletRequest, res0: HttpServletResponse):Unit = {
		val req = new Request(req0)
		val res = new Response(res0)
		val ctx = new Context(req, res, new HashMap)
		ctx.debug = debug

		try {
			res.code(200)
			res.header("Context-Type", "text/plain")
			dispatch(beforeFilters.toArray, ctx)
			if (! dispatch(routing.toArray, ctx)) {
				throw new NotFound
			}
			dispatch(afterFilters.toArray,  ctx)
		} catch {
			case e:Redirect => {
				res.code(302)
				res.header("Location", e.url)
			}
			case e:NotFound => {
				res.code(404)
				res.content("404 Not Found")
			}
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

	private var _body:String = null
	def body ():String = _body match {
		case null => {
			_body = Source.fromInputStream(req0.getInputStream).mkString
			_body
		}
		case _ => _body
	}

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
