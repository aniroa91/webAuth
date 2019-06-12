package controllers

import play.mvc._
import javax.inject.Inject
import javax.inject.Singleton

import play.api.mvc.AbstractController
import play.api.mvc.ControllerComponents

@Singleton
class LoginController @Inject()(cc: ControllerComponents) extends AbstractController(cc){

  def index = Action { implicit request =>
    val ssId = request.session.get("username").toString
    if(ssId != "None") {
      Redirect("/daily")
    }
    else
      Ok(views.html.login(request.flash.get("login").getOrElse("None")))
  }
  def logout = Action {
    Redirect(routes.LoginController.index).withNewSession.flashing(
      "logout" -> "You are now logged out."
    )
  }


}
