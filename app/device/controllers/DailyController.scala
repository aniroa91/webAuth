package controllers

import javax.inject.Inject
import javax.inject.Singleton

import play.api.Logger
import play.api.mvc.AbstractController
import play.api.mvc.ControllerComponents
import services.domain.CommonService

case class DayPicker( csrfToken: String, day: String)

/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
@Singleton
class DailyController @Inject()(cc: ControllerComponents) extends AbstractController(cc) with Secured{
  val logger: Logger = Logger(this.getClass())

  def getDaily =  Action {implicit request =>
    Ok(device.views.html.daily(CommonService.getCurrentDay()))
  }

}