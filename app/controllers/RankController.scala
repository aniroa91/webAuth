package controllers

//import com.ftel.bigdata.dns.parameters.Label

import javax.inject.Inject
import javax.inject.Singleton
import play.api.mvc.AbstractController
import play.api.mvc.ControllerComponents
import services.domain.CommonService
import services.CacheService

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class RankController @Inject() (cc: ControllerComponents) extends AbstractController(cc) with Secured{

  def index = withAuth { username => implicit request =>
      Ok(views.html.dns_v2.rank.index(CacheService.getRank()._1,username))
  }
}
