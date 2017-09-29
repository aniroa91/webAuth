package controllers

import javax.inject.Inject
import javax.inject.Singleton
import play.api.mvc.AbstractController
import play.api.mvc.ControllerComponents
import services.paytv.DashboardService
import services.domain.CommonService

/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
@Singleton
class PaytvClientController @Inject() (cc: ControllerComponents) extends AbstractController(cc) {

  def index() = Action {
    val response = DashboardService.get()
    Ok(views.html.dns_v2.profile.paytv.index(response))
  }
  
}
