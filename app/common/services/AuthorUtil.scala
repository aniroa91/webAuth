package common.services

import play.api.Logger
import scalaj.http.{Http, HttpOptions}
import play.api.libs.json._

object AuthorUtil {
  val logger = Logger(this.getClass())

  def hasAuthor(username: String, regex: String, url: String): Option[String] =  {
    val sessionRole = regex
    var isPermission = 0
    if(regex != ""){
      for(i <- 0 until sessionRole.split(",").length){
        if(url.matches(sessionRole.split(",")(i))) isPermission +=1
      }
    }
    if(isPermission > 0) Some(username) else None
  }

}