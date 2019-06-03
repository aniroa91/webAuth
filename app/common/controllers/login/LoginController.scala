package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import views._
import javax.inject.Inject
import javax.inject.Singleton

import common.auth
import common.auth.LDAP
import play.api.mvc.AbstractController
import play.api.mvc.ControllerComponents

case class Account(user: String, password: String, regex: String, location: String, verifiedLocation: String)

@Singleton
class LoginController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

  /*val loginForm = Form(
    tuple(
      "email" -> text,
      "password" -> text
    ) verifying ("Invalid email or password", result => result match {
      case (email, password) => check(email, password)
    })
  )*/

  val loginForm = Form {
    mapping("user" -> text, "password" -> text)(LDAP.authenticate)(_.map(u => (u.user, "")))
      .verifying("Invalid email or password", result => result.isDefined)
  }

  /*def check(username: String, password: String) = {
    ((username == "inf" && password == "inf123") || (username == "noc" && password == "noc123") || (username == "admin" && password == "admin123"))
  }*/

  def index = Action { implicit request =>
    val ssId = request.session.get("username").toString
    if(ssId != "None") {
      Redirect("/daily")
    }
    else
       Ok(common.views.html.login.index(loginForm))
  }

  /*def login = Action { implicit request =>
    loginForm.bindFromRequest.fold(
      formWithErrors => Redirect(routes.LoginController.index),
      user =>{
        user._1 match {
          case "inf" =>
            Redirect("/daily").withSession(Security.username -> user._1)
          case "noc" =>
            Redirect("/daily").withSession(Security.username -> user._1)
          case "admin" =>
            Redirect("/daily").withSession(Security.username -> user._1)
        }
      }
    )
  }*/

  def login = Action { implicit request =>
    loginForm.bindFromRequest.fold(
      formWithErrors => Redirect(routes.LoginController.index),
      user => Redirect("/daily").withSession(Security.username -> user.get.user, "regex" -> user.get.regex, "location" -> user.get.location,
      "verifiedLocation" -> user.get.verifiedLocation)
    )
  }

  def logout = Action {
    Redirect(routes.LoginController.index).withNewSession.flashing(
      "success" -> "You are now logged out."
    )
  }

}