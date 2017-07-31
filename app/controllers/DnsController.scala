package controllers

import javax.inject._
import play.api.mvc._
import services.DomainService
import utils.JsonUtil
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class DnsController @Inject() (protected val dbConfigProvider: DatabaseConfigProvider,
    cc: ControllerComponents) extends AbstractController(cc) with HasDatabaseConfigProvider[JdbcProfile] {

  def getDomainDetail(domain: String) = Action {
    val jsonObject = DomainService.getDomainInfo(domain)
    Ok(JsonUtil.prettyJson(jsonObject))
  }
}
