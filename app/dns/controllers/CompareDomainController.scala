package dns.controllers

import javax.inject.Inject
import javax.inject.Singleton
import play.api.mvc.AbstractController
import play.api.mvc.ControllerComponents
import services.CacheService
import controllers.Secured
import java.text.NumberFormat
import com.ftel.bigdata.utils.NumberUtil
import dns.utils.DataSampleUtil

/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
@Singleton
class CompareDomainController @Inject()(cc: ControllerComponents) extends AbstractController(cc) with Secured {

  def index =  Action {
    Ok(dns.views.html.compare.index())
  }

}

