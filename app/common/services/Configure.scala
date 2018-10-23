package services

import com.sksamuel.elastic4s.ElasticsearchClientUri
import com.sksamuel.elastic4s.http.HttpClient
import com.typesafe.config.ConfigFactory

object Configure {
  val LOGO_PATH = ConfigFactory.load().getString("storage") + "/"
  
  val LOGO_API_URL = "https://logo.clearbit.com/"
  val CATEGORY_API_URL = "http://sitereview.bluecoat.com/sitereview.jsp#/?search="
  
  val PROXY_HOST = ConfigFactory.load().getString("proxy.host")
  val PROXY_PORT = ConfigFactory.load().getString("proxy.port").toInt
  
  val ES_HOST = ConfigFactory.load().getString("es.dns.host")
  val ES_PORT = ConfigFactory.load().getString("es.dns.port").toInt

  val LOGO_DEFAULT = "../assets/images/logo/domain.png"

  val client = HttpClient(ElasticsearchClientUri(Configure.ES_HOST, Configure.ES_PORT))

  def main(args: Array[String]) {
    println("")
  }
  
  val CACHE_CONTRACT_FIRST = scala.collection.mutable.Map[String, String]()
}