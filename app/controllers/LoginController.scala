package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import views._
import javax.inject.Inject
import javax.inject.Singleton
import play.api.mvc.AbstractController
import play.api.mvc.ControllerComponents


@Singleton
class LoginController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

  val loginForm = Form(
    tuple(
      "email" -> text,
      "password" -> text
    ) verifying ("Invalid email or password", result => result match {
      case (email, password) => check(email, password)
    })
  )

  def check(username: String, password: String) = {
    (username == "btgd@ftel" && password == "da@171020") || (username == "demo" && password == "demo")
    
  }

  def index = Action { implicit request =>
    val ssId = request.session.get("username").toString
    if(ssId != "None") {
      //Redirect(routes.DashboardController.index)
      //Redirect(_root_.profile.controllers.internet.HistoryController.index)
      Redirect(_root_.profile.controllers.internet.routes.HistoryController.index(""))
      //Ok(views.html.login.index(loginForm))
    }
    else
       Ok(views.html.login.index(loginForm))
  }

  def login = Action { implicit request =>
    loginForm.bindFromRequest.fold(
      formWithErrors => Redirect(routes.LoginController.index),
      user => Redirect(routes.DashboardController.index).withSession(Security.username -> user._1)
    )
  }

  def logout = Action {
    Redirect(routes.LoginController.index).withNewSession.flashing(
      "success" -> "You are now logged out."
    )
  }

}