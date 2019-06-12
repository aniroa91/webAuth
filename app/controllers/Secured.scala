package controllers

import play.api.mvc._

import common.services.AuthorUtil

trait Secured {

  def username(request: RequestHeader) = {
    request.session.get(Security.username)
  }
  def role(request: RequestHeader) = {
    request.session.get("role")
  }

  def hasRole(request: RequestHeader) = {
    AuthorUtil.hasAuthor(
      username(request).getOrElse(""),
      role(request).getOrElse(""),
      request.headers("Raw-Request-URI"))
  }

  def onUnauthenticated(request: RequestHeader) = Results.Redirect(routes.LoginController.index)

  def onUnauthorized(request: RequestHeader) = Results.Redirect(controllers.routes.ErrorController.index)

  def withAuth(f: => String => Request[AnyContent] => Result) = {
    Security.Authenticated(username, onUnauthenticated) { user =>
      Security.Authenticated(hasRole, onUnauthorized) { user2 =>
        Action(request => {
          f(user2)(request)
        })
      }
    }
  }
}
