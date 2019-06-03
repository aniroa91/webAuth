package controllers

import play.api.mvc._

trait Secured {

  def username(request: RequestHeader) = request.session.get(Security.username)

  def getField(request: RequestHeader, field: String) = request.session.get(s"$field")

  def onUnauthenticated(request: RequestHeader) = Results.Redirect(routes.LoginController.index)

  def onUnauthorized(request: RequestHeader) = Results.Redirect(routes.ErrorController.index).withSession(Security.username -> username(request).getOrElse(""), "regex" -> getField(request, "regex").getOrElse(""),
    "location" -> getField(request, "location").getOrElse(""), "verifiedLocation" -> getField(request, "verifiedLocation").getOrElse(""))

  def withAuth(f: => String => Request[AnyContent] => Result) = {
    Security.Authenticated(username, onUnauthenticated) { user =>
      Security.Authenticated(hasRole, onUnauthorized) { user2 =>
        Action(request => {
          f(user2)(request)
        })
      }
    }
  }

  /**
    * This method shows how you could wrap the withAuth method to also fetch your user
    * You will need to implement UserDAO.findOneByUsername
    */
  def hasRole(request: RequestHeader): Option[String] = {
    val url = request.headers("Raw-Request-URI")
    println(url)
    val sessionRole = getField(request, "regex").get
    var isPermission = 0
    if(getField(request, "regex").get != ""){
      for(i <- 0 until sessionRole.split(",").length){
        if(url.matches(sessionRole.split(",")(i))) isPermission +=1
      }
    }
    if(isPermission > 0) Some(username(request).getOrElse("")) else None
  }
}
