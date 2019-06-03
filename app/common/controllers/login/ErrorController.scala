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
class ErrorController @Inject()(cc: ControllerComponents) extends AbstractController(cc){

  def index = Action { implicit request =>
    Ok(common.views.html.login.permission(request.session.get("username").get, routes.ErrorController.index))
  }
}

