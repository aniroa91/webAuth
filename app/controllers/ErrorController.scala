package controllers

import javax.inject.Inject
import javax.inject.Singleton

import play.api.mvc.AbstractController

import play.api.mvc.ControllerComponents

@Singleton
class ErrorController @Inject()(cc: ControllerComponents) extends AbstractController(cc){

  def index = Action { implicit request =>
    Ok(views.html.permission_error(request.session.get("username").get, routes.ErrorController.index))
  }
}

