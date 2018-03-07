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
import slick.jdbc.JdbcProfile
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import services.user.ProfileService

case class SearchContract(q: String)

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class ProfileContractController @Inject() (protected val dbConfigProvider: DatabaseConfigProvider, cc: ControllerComponents)
    extends AbstractController(cc) with HasDatabaseConfigProvider[JdbcProfile] with Secured {

  val form = Form(
    mapping(
      "ct" -> text)(SearchContract.apply)(SearchContract.unapply))

  def index(month: String) = withAuth { username => implicit request =>
    val formValidationResult = form.bindFromRequest
    var strMonth = month
    var domain = ""
    try {
      if (!formValidationResult.hasErrors) {
        domain = formValidationResult.get.q.trim()
        if(month == null || month ==""){
          val now = new DateTime()
          val lastMonth = now.minusMonths(1)
          strMonth = lastMonth.toString(DateTimeFormat.forPattern("yyyy-MM"))
        }
        val response = ProfileService.get(domain,strMonth)
        println(response)
        Ok(views.html.profile.contract.index(form, response, domain+"&day="+strMonth,username,strMonth))
      } else {
        Ok(views.html.profile.contract.index(form, null, null,username,month))
      }
    } catch {
      case e: Exception => {
        e.printStackTrace()
        Ok(views.html.profile.contract.index(form, null, domain+"&day="+strMonth,username,strMonth))
        }
    }
  }
}


