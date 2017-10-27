package controllers

import com.ftel.bigdata.utils.DomainUtil
import javax.inject.Inject
import javax.inject.Singleton

import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.Forms.text
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import play.api.i18n.I18nSupport
import play.api.mvc.AbstractController
import play.api.mvc.AnyContent
import play.api.mvc.ControllerComponents
import play.api.mvc.Request
import service.BrasService
import services.CacheService
import services.domain.CommonService
import slick.jdbc.JdbcProfile

import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
case class SearchBras(id: String,time: String,sigin: String, logoff: String)

@Singleton
class BrasDetailController @Inject() (protected val dbConfigProvider: DatabaseConfigProvider, cc: ControllerComponents)
  extends AbstractController(cc) with HasDatabaseConfigProvider[JdbcProfile] with Secured {

  val form = Form(
    mapping(
      "id" -> text,
      "time" -> text,
      "sigin" -> text,
      "logoff" -> text
    )(SearchBras.apply)(SearchBras.unapply)
  )

  def index = withAuth { username => implicit request =>
    val formValidationResult = form.bindFromRequest
    try {
      if (!formValidationResult.hasErrors) {
        val id = formValidationResult.get.id.trim()
        val time = formValidationResult.get.time.trim()
        val sigin = formValidationResult.get.sigin.trim()
        val logoff = formValidationResult.get.logoff.trim()

        val brasChart = Await.result(BrasService.getBrasChart(id,time),Duration.Inf)
        val brasTime = Await.result(BrasService.getBrasTime(id,time),Duration.Inf)
        val brasCard = Await.result(BrasService.getBrasCard(id,time,sigin,logoff),Duration.Inf)

        Ok(views.html.device.brasDetail(form, username,brasTime,brasCard,brasChart))
      } else {
        Ok(views.html.device.brasDetail(form, username,null,null,null))
      }
    }
    catch{
      case e: Exception => Ok(views.html.device.brasDetail(form, username,null,null,null))
    }
  }
}



