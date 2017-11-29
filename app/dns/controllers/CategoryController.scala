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
class CategoryController @Inject()(cc: ControllerComponents) extends AbstractController(cc) with Secured {

  def index =  Action {
    val seq = 1 until 100
    Ok(dns.views.html.category.index(seq.map(x => "google.com").toArray))
  }
}
