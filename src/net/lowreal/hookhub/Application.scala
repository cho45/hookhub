package net.lowreal.hookhub

import net.lowreal.skirts._
import javax.servlet.http.{HttpServlet, HttpServletResponse, HttpServletRequest}

import com.google.appengine.api.users.{User, UserService, UserServiceFactory}
import com.google.appengine.api.datastore._

class AppHttpRouter extends HttpRouter {
	val top   = "/".r
	val login = "/login".r
	val user  = "/([A-Za-z][A-Za-z0-9_-]{1,30})/".r
	val user0 = "/([A-Za-z][A-Za-z0-9_-]{1,30})".r

	override def dispatch (req: HttpServletRequest, res: HttpServletResponse):Unit = (req.getMethod, req.getRequestURI) match {
		case ("GET", top()) => {
			println("get")
			val source = req.getParameter("source")
			val result = Rhino.js(source)

			res.setContentType("text/html")
			res.getWriter.println { """<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">""" }
			res.getWriter.println {
				<html>
					<head><title>eval</title></head>

					<link rel="stylesheet" type="text/css" href="/css/base.css" media="screen,tv"/>
					<!-- script type="text/javascript" src="/js/site-script.js"></script -->
				<body>
					<h1>Request</h1>
					<pre>result { result }</pre>
					<form action="" method="GET">
						<p>
							<textarea name="source" cols="200" rows="20">{ source }</textarea>
						</p>
						<p>
							<input type="submit" value="eval" />
						</p>
					</form>
				</body>
				</html>
			}
		}

		case ("POST", top()) => {
			println("post")
		}

		case ("GET", login()) => {
			println("login")
		}

		case (_, user0(name)) => {
			res.sendRedirect("/" + name + "/")
		}

		case (_, user(name)) => {
			println("user page:" + name)
		}

		case _ => {
			println("404")
			res.setContentType("text/html")
			res.getWriter.println { """<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">""" }
			res.getWriter.println {
				<html>
					<head><title>404</title></head>

					<link rel="stylesheet" type="text/css" href="/css/base.css" media="screen,tv"/>
					<!-- script type="text/javascript" src="/js/site-script.js"></script -->
				<body>
					<h1>Request</h1>
					<pre>Method: { req.getMethod }</pre>
					<pre>RequestURI: { req.getRequestURI }</pre>
					<pre>QueryString: { req.getQueryString }</pre>
					<h1>Raw</h1>
					<pre>{ req }</pre>
				</body>
				</html>
			}
		}
	}

}

