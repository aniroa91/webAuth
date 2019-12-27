package handles

import play.api.http.HttpErrorHandler
import play.api.mvc._
import play.api.mvc.Results._
import scala.concurrent._
import javax.inject.Singleton

@Singleton
class ErrorHandle extends HttpErrorHandler {

  def onClientError(request: RequestHeader, statusCode: Int, message: String) = {
    Future.successful(
      Redirect("/home")
    )
  }

  def onServerError(request: RequestHeader, exception: Throwable) = {
    Future.successful(
      Redirect("/home")
    )
  }
}

