package net.lowreal.gae

import javax.servlet.http.{HttpServlet, HttpServletResponse, HttpServletRequest}
import com.google.appengine.api.users.{User, UserService, UserServiceFactory}

class HelloServlet extends HttpServlet {
	override def doGet (req:HttpServletRequest, resp:HttpServletResponse) = {
		val userService = UserServiceFactory.getUserService()

		val user = userService.getCurrentUser()

		if (user == null) {
			resp.sendRedirect(userService.createLoginURL(req.getRequestURI()));
		} else {
			resp.setContentType("text/html")
			resp.getWriter().println(
				<html>
					<head><title>Hello World</title></head>
				<body>
					<h1>Wellcome { user.getNickname() } ! <a href={ userService.createLogoutURL(req.getRequestURI()) }>logout</a> </h1>
				</body>
				</html>
			)
		}
	}
}

