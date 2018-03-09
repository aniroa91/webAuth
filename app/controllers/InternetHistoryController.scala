package controllers

import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.Forms.text
import javax.inject.Inject
import javax.inject.Singleton
import play.api.mvc.AbstractController
import play.api.mvc.ControllerComponents
import services.CacheService

/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
@Singleton
class InternetHistoryController @Inject()(cc: ControllerComponents) extends AbstractController(cc) with Secured{

  val form = Form(
    mapping(
      "ct" -> text)(SearchContract.apply)(SearchContract.unapply))

  def index =  withAuth { username => implicit request =>
    val formValidationResult = form.bindFromRequest
    try {
      if (!formValidationResult.hasErrors) {
        Ok(views.html.profile.internet.history(form, username))
      } else {
        Ok(views.html.profile.internet.history(form, username))
      }
    } catch {
      case e: Exception => Ok(views.html.profile.internet.history(form, username))
    }

  }
}

