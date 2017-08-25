package controllers

import com.ftel.bigdata.dns.parameters.Label

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
class RankController @Inject() (cc: ControllerComponents) extends AbstractController(cc) {

  def index = Action {
      Ok(views.html.ace.rank(CacheService.getRank()._1))
  }
}
