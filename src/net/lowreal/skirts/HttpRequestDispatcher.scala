package net.lowreal.skirts

import javax.servlet._
import javax.servlet.http.{HttpServlet, HttpServletResponse, HttpServletRequest}

class HttpRequestDispatcher extends Filter {
	var static = """^/(?:css|js|images|static).*""".r
	var router:HttpRouter = null

	def init (filterConfig: FilterConfig) = {
		println("initializing...")
		val routerName = filterConfig.getInitParameter("router");
		val klass  = Class.forName(routerName)

		static = filterConfig.getInitParameter("static").r
		router = klass.newInstance.asInstanceOf[HttpRouter];

		println("initialized: " + router);
		println("static path: " + static);
	}

	def doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) = {
		(request, response) match {
			case (req: HttpServletRequest, res: HttpServletResponse) =>
				val path    = req.getRequestURI
				val statics = static
				path match {
					case statics() =>
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

