package net.lowreal.hookhub

import net.lowreal.skirts._
import javax.servlet.http.{HttpServlet, HttpServletResponse, HttpServletRequest}

class AppHttpRouter extends HttpRouter {

	override def dispatch (req: HttpServletRequest, res: HttpServletResponse):Unit = req match {
		case GET("/") => {
			println("get")
			val source = req.getParameter("source")
			val result = Rhino.js(source)

			res.setContentType("text/html")
			res.getWriter.println { """<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">""" }
			res.getWriter.println {
				<html>
					<head><title>404</title></head>

					<link rel="stylesheet" type="text/css" href="/css/base.css" media="screen,tv"/>
					<!-- script type="text/javascript" src="/js/site-script.js"></script -->
				<body>
					<h1>Request</h1>
					<pre>result { result }</pre>
				</body>
				</html>
			}
		}

		case POST("/") => {
			println("post")
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

