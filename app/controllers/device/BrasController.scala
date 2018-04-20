package controllers

import play.api.mvc._
import controllers.Secured
import javax.inject.Inject
import javax.inject.Singleton
import play.api.mvc.AbstractController
import play.api.mvc.ControllerComponents
import service.BrasService
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import play.api.libs.json.Json


/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
@Singleton
class BrasController @Inject()(cc: ControllerComponents) extends AbstractController(cc) with Secured{

  def index =  withAuth { username => implicit request =>
    try {
      val bras = Await.result(BrasService.listTop100Bras, Duration.Inf)
      Ok(views.html.device.bras(username, bras))
    }
    catch{
      case e: Exception => Ok(views.html.device.bras(username,null))
    }
  }

  def confirmLabel(id: String,time: String) = Action { implicit request =>
    try{
      val res =  Await.result(BrasService.confirmLabel(id,time), Duration.Inf)
      Ok(Json.toJson(res))
    }
    catch{
      case e: Exception => Ok("error")
    }
  }

  def rejectLabel(id: String,time: String) = Action { implicit request =>
    try{
      val res =  Await.result(BrasService.rejectLabel(id,time), Duration.Inf)
      Ok(Json.toJson(res))
    }
    catch{
      case e: Exception => Ok("error")
    }
  }

}



