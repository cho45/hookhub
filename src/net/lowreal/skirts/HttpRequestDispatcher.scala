package net.lowreal.skirts

import javax.servlet._
import javax.servlet.http.{HttpServlet, HttpServletResponse, HttpServletRequest}

class HttpRequestDispatcher extends Filter {
	def init (filterConfig: FilterConfig) = {
		println("init")
	}

	def doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) = {
		println("doFilter")
		(request, response) match {
			case (req: HttpServletRequest, res: HttpServletResponse) =>
				println(req)
				println(res)
				res.setContentType("text/html")
				res.getWriter().println {
					<html>
						<head><title>Hello World</title></head>
						<style type='text/css'><![CDATA[
							pre {
								background: #efefef;
								border: #ccc;
								padding: 0.5em;
							}
						]]></style>
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
			case _ => chain.doFilter(request, response)
		}
	}

	def destroy () = {
		println("destroy")
	}
}

