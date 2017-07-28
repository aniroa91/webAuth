package controllers

import javax.inject._
import play.api.mvc._
import services.DomainService
import utils.JsonUtil

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class DnsController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

  def getDomainDetail(domain: String) = Action {
    val jsonObject = DomainService.getDomainInfo(domain)
    Ok(JsonUtil.prettyJson(jsonObject))
  }

}
