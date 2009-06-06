package net.lowreal.hookhub

import javax.servlet.http.{HttpServlet, HttpServletResponse, HttpServletRequest}
import scala.collection.jcl.Conversions._

import net.lowreal.skirts._
import com.google.appengine.api.users.{User, UserService, UserServiceFactory}

class AppHttpRouter extends HttpRouter {
	val top   = "/".r
	val login = "/login".r
	val user  = "/([A-Za-z][A-Za-z0-9_-]{1,30})/".r
	val user0 = "/([A-Za-z][A-Za-z0-9_-]{1,30})".r

	val US    = UserServiceFactory.getUserService

	class MyContext (c:Context) {
		def requireUser () = {
			val user = US.getCurrentUser
			if (user == null) {
				throw new RedirectException(US.createLoginURL())
			}
		}
	}

	implicit def ctx2myctx (c:Context) = new MyContext(c)

	override def route (c:Context):Unit = (c.req.method, c.req.requestURI) match {
		case ("GET", top()) => {
			println("top")
			c.res.code(200)
			c.res.header("Content-Type", "text/plain")
			c.res.content("Test")

			c.requireUser
		}

//		case ("GET", top()) => {
//			println("get")
//			val source = req.getParameter("source")
//			val result = Rhino.js(source)
//
//			res.setContentType("text/html")
//			res.getWriter.println { """<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">""" }
//			res.getWriter.println {
//				<html>
//					<head><title>eval</title></head>
//
//					<link rel="stylesheet" type="text/css" href="/css/base.css" media="screen,tv"/>
//					<!-- script type="text/javascript" src="/js/site-script.js"></script -->
//				<body>
//					<h1>Request</h1>
//					<pre>result { result }</pre>
//					<form action="" method="GET">
//						<p>
//							<textarea name="source" cols="200" rows="20">{ source }</textarea>
//						</p>
//						<p>
//							<input type="submit" value="eval" />
//						</p>
//					</form>
//				</body>
//				</html>
//			}
//		}
//
//		case ("POST", top()) => {
//			println("post")
//		}
//
//		case ("GET", login()) => {
//		//	val user = requireUser
//		}
//
//		case (_, user0(name)) => {
//			res.sendRedirect("/" + name + "/")
//		}
//
//		case (_, user(name)) => {
//			println("user page:" + name)
//
////			val query = new Query("User").addFilter("name", Query.FilterOperator.EQUAL, name)
////			if (query.first == null) {
////				// 404
////			} else {
////				//...
////			}
//		}
//
//		case _ => {
//			println("404")
//			res.setContentType("text/html")
//			res.getWriter.println { """<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">""" }
//			res.getWriter.println {
//				<html>
//					<head><title>404</title></head>
//
//					<link rel="stylesheet" type="text/css" href="/css/base.css" media="screen,tv"/>
//					<!-- script type="text/javascript" src="/js/site-script.js"></script -->
//				<body>
//					<h1>Request</h1>
//					<pre>Method: { req.getMethod }</pre>
//					<pre>RequestURI: { req.getRequestURI }</pre>
//					<pre>QueryString: { req.getQueryString }</pre>
//					<h1>Raw</h1>
//					<pre>{ req }</pre>
//				</body>
//				</html>
//			}
	}

}
