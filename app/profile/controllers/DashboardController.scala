package profile.controllers

import javax.inject.Inject
import javax.inject.Singleton
import play.api.mvc.AbstractController
import play.api.mvc.ControllerComponents
import controllers.Secured


/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
@Singleton
class DashboardController @Inject() (cc: ControllerComponents) extends AbstractController(cc) with Secured {

  def index() = withAuth { username => implicit request =>
    //val response = DashboardService.get()
    Ok(profile.views.html.index())
  }
}