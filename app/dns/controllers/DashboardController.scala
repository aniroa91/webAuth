package dns.controllers

import javax.inject.Inject
import javax.inject.Singleton
import play.api.mvc.AbstractController
import play.api.mvc.ControllerComponents
import services.CacheService
import controllers.Secured

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class DashboardController @Inject()(cc: ControllerComponents) extends AbstractController(cc) with Secured {
//  def index =  withAuth { username => implicit request => 
//    Ok(dns.views.html.dashboard.index(username))
//  }
  
  def index =  Action {
    Ok(dns.views.html.dashboard.index())
  }
  
}

