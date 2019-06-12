package common.services

import play.api.Logger
import scalaj.http.{Http, HttpOptions}
import play.api.libs.json._
import services.Configure

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

  def getRole(username: String, col: String): String = {
    val env_host = Configure.SVC_USER_HOST
    val env_port = Configure.SVC_USER_PORT
    logger.info(s"host:$env_host, port:$env_port")
    System.setProperty("http.nonProxyHosts", "127.0.0.1|10.0.0.0/8|172.16.0.0/12|192.168.0.0/16|*.cluster.local|*.local|172.27.11.151|"+env_host);
    logger.info("http://"+env_host+":"+env_port+"/profile/getRolebyUser?username="+username+"&webType=infra")

    val req = Http("http://"+env_host+":"+env_port+"/profile/getRolebyUser?username="+username+"&webType=infra")
      .option(HttpOptions.followRedirects(true)).asString
    logger.info("response:"+req.body)
    val userResponse = Json.parse(req.body)
    val rs = userResponse.\("data").get.\(s"$col").get.toString.replace("\"","").trim

    return rs
  }

}