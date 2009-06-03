package net.lowreal.skirts

import javax.servlet._
import javax.servlet.http.{HttpServlet, HttpServletResponse, HttpServletRequest}

class HttpRouter {
	def dispatch (req: HttpServletRequest, res: HttpServletResponse):Unit = {
		print("router!")

		res.setContentType("text/html")
		res.getWriter.println { """<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">""" }
		res.getWriter.println {
			<html>
				<head><title>Hello World</title></head>

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

class HttpRequestDispatcher extends Filter {
	var static = """^/(?:css|js|images|static).*""".r
	var router:HttpRouter = null

	def init (filterConfig: FilterConfig) = {
		println("initializing...")
		val applicationName = filterConfig.getInitParameter("application");
		val klass  = Class.forName(applicationName)

		static = filterConfig.getInitParameter("static").r
		router = klass.newInstance.asInstanceOf[HttpRouter];

		println("initialized: " + router);
	}

	def doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) = {
		println("doFilter")
		(request, response) match {
			case (req: HttpServletRequest, res: HttpServletResponse) =>
				val path    = req.getRequestURI
				val statics = static
				path match {
					case statics() =>
						println("static")
						chain.doFilter(request, response)
					case _ =>
						router.dispatch(req, res)
				}
			case _ => chain.doFilter(request, response)
		}
	}

	def destroy () = {
		println("destroy")
	}
}

