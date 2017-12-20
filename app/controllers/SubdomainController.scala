package controllers

import javax.inject.Inject
import javax.inject.Singleton
import play.api.mvc.AbstractController
import play.api.mvc.ControllerComponents
import services.CacheService
import java.text.NumberFormat
import com.ftel.bigdata.utils.NumberUtil
import dns.utils.DataSampleUtil
import com.ftel.bigdata.utils.DomainUtil
import services.domain.CommonService
import com.ftel.bigdata.utils.StringUtil

/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
@Singleton
class SubdomainController @Inject()(cc: ControllerComponents) extends AbstractController(cc) with Secured {

  def index(domain: String) =  withAuth { username => implicit request =>
    if (StringUtil.isNullOrEmpty(domain)) {
      Ok(views.html.dns_v2.search.subdomain(null, null, null, username))
    } else {
      val second = DomainUtil.extract(domain).second
      val logo = CommonService.getLogo(second, false)
      val response = CacheService.getDomain(second)
      Ok(views.html.dns_v2.search.subdomain(domain, response._1, logo, username))
    }
    
  }
}

