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
case class InternetContract(date: String,ct: String)

@Singleton
class InternetHistoryController @Inject()(cc: ControllerComponents) extends AbstractController(cc) with Secured{

  val form = Form(
    mapping(
      "date" -> text,
      "ct" -> text
    )(InternetContract.apply)(InternetContract.unapply))

  def index =  withAuth { username => implicit request =>
    val formValidationResult = form.bindFromRequest
    try {
      if (!formValidationResult.hasErrors) {
        val ct = formValidationResult.get.ct.trim()
        Ok(views.html.profile.internet.history(form, username,ct))
      } else {
        Ok(views.html.profile.internet.history(form, username,null))
      }
    } catch {
      case e: Exception => Ok(views.html.profile.internet.history(form, username,null))
    }

  }

  def compareDate =  withAuth { username => implicit request =>
    val formValidationResult = form.bindFromRequest
    try {
      if (!formValidationResult.hasErrors) {
        val ct = formValidationResult.get.ct.trim()
        Ok(views.html.profile.internet.compareDate(form, username,ct))
      } else {
        Ok(views.html.profile.internet.compareDate(form, username,null))
      }
    } catch {
      case e: Exception => Ok(views.html.profile.internet.compareDate(form, username,null))
    }
  }

  def compareContract =  withAuth { username => implicit request =>
    val formValidationResult = form.bindFromRequest
    try {
      if (!formValidationResult.hasErrors) {
        val ct = formValidationResult.get.ct.trim()
        Ok(views.html.profile.internet.compareContract(form, username,ct))
      } else {
        Ok(views.html.profile.internet.compareContract(form, username,null))
      }
    } catch {
      case e: Exception => Ok(views.html.profile.internet.compareContract(form, username,null))
    }
  }

}

