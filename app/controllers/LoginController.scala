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
    (username == "admin@fpt.com.vn" || username == "admin1@fpt.com.vn" || username == "admin2@fpt.com.vn" && password == "admin123")
  }

  def index = Action { implicit request =>
    Ok(views.html.login.index(loginForm))
  }

  def login = Action { implicit request =>
    loginForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.login.index(formWithErrors)),
      user => Redirect(routes.DashboardController.index).withSession(Security.username -> user._1)
    )
  }

  def logout = Action {
    Redirect(routes.LoginController.index).withNewSession.flashing(
      "success" -> "You are now logged out."
    )
  }

}