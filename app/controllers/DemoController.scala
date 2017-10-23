package controllers

import javax.inject.Inject
import javax.inject.Singleton
import play.api.mvc.AbstractController
import play.api.mvc.ControllerComponents
import services.CacheService
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.Forms.text
import services.user.DemoService
import com.ftel.bigdata.utils.DateTimeUtil

/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
@Singleton
class DemoController @Inject()(cc: ControllerComponents) extends AbstractController(cc) with Secured{

  val form = Form(
    mapping(
      "ct" -> text
    )(SearchContract.apply)(SearchContract.unapply)
  )

  def index =  withAuth { username => implicit request =>
    val formValidationResult = form.bindFromRequest
    if (!formValidationResult.hasErrors) {
      val mac = formValidationResult.get.q.trim()
      Ok(views.html.dns_v2.demo.index(form,username,mac))
    } else {
      Ok(views.html.dns_v2.demo.index(form, username, null))
    }
  }

  def message =  withAuth { username => implicit request =>
    val contract = null //DemoService.get().map(x => x._2)
    //println("Contract:" + contract.size)
    
    val msg = DateTimeUtil.now + " -> No Implement"
    //println(msg)
    Ok(views.html.dns_v2.demo.message(username, msg))
  }
  
  def message2 =  withAuth { username => implicit request =>
    val timeString = DateTimeUtil.now.toString("yyyy-MM-dd HH:mm:ss")
    val response = DemoService.get()
    response.contracts.map(x => services.Configure.CACHE_CONTRACT_FIRST.put(timeString, x._1 + "->" + x._2))
    val msg = response.contracts.map(x => x._2).map(x => s"<h3><font color='green'>Hello ${x}, Welcome You Here</font></h3>").mkString("<br/>")
    val macs = response.macs.map(x => s"<h3><font color='blue'>${x}</font></h3>").mkString("<br/>")
    val content = "<div>" +
      s"<h1>Time: ${timeString}</h1>" + 
      "<br/>" +
      s"<h2>There ${if (response.macs.size > 1) "are" else "is"} ${response.macs.size} new mac address device.</h2>" + 
       "<br/>" +
       s"${macs}" +
      s"<h2>Found ${response.contracts.size} ${if (response.contracts.size > 1) "contracts" else "contract"} in ${response.macs.size} new mac address.</h2>" +
      "<br/>" +
      s"${msg}" + "</div>"
    Ok(content)
  }
  
  def history =  withAuth { username => implicit request =>
    val content = services.Configure.CACHE_CONTRACT_FIRST.map(x => x._1 + " => " + x._2).mkString("\n")
    Ok(content)
  }
}


