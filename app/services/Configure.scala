package services

import com.sksamuel.elastic4s.ElasticsearchClientUri
import com.sksamuel.elastic4s.http.HttpClient
import com.typesafe.config.ConfigFactory
import model.Bubble
import play.api.libs.json.Json
import com.redis.RedisClient

object Configure {
  val LOGO_PATH = ConfigFactory.load().getString("storage") + "/"
  
  val LOGO_API_URL = "https://logo.clearbit.com/"
  val CATEGORY_API_URL = "http://sitereview.bluecoat.com/sitereview.jsp#/?search="
  
  val PROXY_HOST = ConfigFactory.load().getString("proxy.host")
  val PROXY_PORT = ConfigFactory.load().getString("proxy.port").toInt
  
  val ES_HOST = ConfigFactory.load().getString("es.host")
  val ES_PORT = ConfigFactory.load().getString("es.port").toInt
  
  val REDIS_HOST = ConfigFactory.load().getString("redis.whois.host")
  val REDIS_PORT = ConfigFactory.load().getString("redis.whois.port").toInt
  
  val KIBANA_HOST = ConfigFactory.load().getString("kibana.host")
  val KIBANA_PORT = ConfigFactory.load().getString("kibana.port").toInt
  val KIBANA = KIBANA_HOST + ":" + KIBANA_PORT
  
  val LOGO_DEFAULT = "../assets/images/logo/domain.png"
  
  val redis = new RedisClient(REDIS_HOST, REDIS_PORT)
  val client = HttpClient(ElasticsearchClientUri(Configure.ES_HOST, Configure.ES_PORT))
  
  def main(args: Array[String]) {
    println(KIBANA)
  }
}