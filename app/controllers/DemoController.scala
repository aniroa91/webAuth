package controllers

import javax.inject.Inject
import javax.inject.Singleton
import play.api.mvc.AbstractController
import play.api.mvc.ControllerComponents
import services.CacheService
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.Forms.text

/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
@Singleton
class DemoController @Inject()(cc: ControllerComponents) extends AbstractController(cc) with Secured{

  val form = Form(
    mapping(
      "ct" -> text
    )(SearchContract.apply)(SearchContract.unapply)
  )

  def index =  withAuth { username => implicit request =>
    val formValidationResult = form.bindFromRequest
    if (!formValidationResult.hasErrors) {
      val mac = formValidationResult.get.q.trim()
      Ok(views.html.dns_v2.demo.index(form,username,mac))
    } else {
      Ok(views.html.dns_v2.demo.index(form, username, null))
    }
  }
}


