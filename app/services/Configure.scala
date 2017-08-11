package services

import com.typesafe.config.ConfigFactory

object Configure {
  val LOGO_PATH = ConfigFactory.load().getString("storage") + "/"
  
  val LOGO_API_URL = "https://logo.clearbit.com/"
  
  val PROXY_HOST = ConfigFactory.load().getString("proxy.host")
  val PROXY_PORT = ConfigFactory.load().getString("proxy.port")
  
  val ES_HOST = ConfigFactory.load().getString("es.host")
  val ES_PORT = ConfigFactory.load().getString("es.port").toInt
}