package controllers

import javax.inject.Inject
import javax.inject.Singleton
import play.api.mvc.AbstractController
import play.api.mvc.ControllerComponents
import services.CacheService
import java.text.NumberFormat
import com.ftel.bigdata.utils.NumberUtil
import dns.utils.DataSampleUtil

/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
@Singleton
class SubdomainController @Inject()(cc: ControllerComponents) extends AbstractController(cc) with Secured {

  def index =  withAuth { username => implicit request =>
    Ok(views.html.dns_v2.search.subdomain(null, null, null, null, username))
  }

}

