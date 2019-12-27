package controllers

import java.io.File
import java.nio.file.Paths

import common.services.Configure
import javax.inject.Inject
import javax.inject.Singleton
import play.api.Logger
import play.api.mvc.AbstractController
import play.api.mvc.ControllerComponents
import services.domain.CommonService

case class DayPicker( csrfToken: String, day: String)

/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
@Singleton
class DailyController @Inject()(cc: ControllerComponents) extends AbstractController(cc) with Secured{
  val logger: Logger = Logger(this.getClass())

  def index =  Action {implicit request =>
    val success = request.flash.get("success").getOrElse("not")
    Ok(home.views.html.index(CommonService.getCurrentDay(), success, controllers.routes.DailyController.index()))
  }

  def importFile() = Action(parse.multipartFormData) { implicit request =>
    try {
      request.body.file("fileUpload").map { picture =>
        picture.ref.moveTo(Paths.get(Configure.FILE_PATH + "/" + picture.filename).toFile, replace = true)
      }
      Redirect(controllers.routes.DailyController.index).flashing("success" -> "ok")
    }
    catch {
      case e: Exception => Redirect(controllers.routes.DailyController.index).flashing("success" -> "not")
    }
  }

}