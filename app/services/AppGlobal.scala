package services
import com.ftel.bigdata.utils.HttpUtil
import com.sksamuel.elastic4s.ElasticsearchClientUri
import com.sksamuel.elastic4s.http.HttpClient
import com.sksamuel.elastic4s.http.search.SearchHit
import com.sksamuel.elastic4s.http.search.SearchResponse
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.ftel.bigdata.utils.FileUtil
import scala.util.Try

object AppGlobal {
  val client = HttpClient(ElasticsearchClientUri(Configure.ES_HOST, Configure.ES_PORT))
  
  private val URL_DOMAIN_DEFAULT = "../assets/images/logo/domain.png"
  def getLogoPath(secondDomain: String): String = {
    val logoUrl = Configure.LOGO_API_URL + secondDomain
    val path = Configure.LOGO_PATH + secondDomain + ".png"
    val logo = "../extassets/" + secondDomain + ".png"
    if (!FileUtil.isExist(path)) {
      println("Don't exist " + path)
      Try(HttpUtil.download(logoUrl, path, "172.30.45.220", 80))
    }
    if (FileUtil.isExist(path)) {
      logo
    } else URL_DOMAIN_DEFAULT
  }
  
   def getImgTag(domain: String): String = {
    val logo = getLogoPath(domain)
    "<a href=\"/search?value=" + domain + "\"><img src=\"" + logo + "\" width=\"30\" height=\"30\"></a>"
  }
  
  def getLinkTag(domain: String): String = {
    "<a href=\"/search?value=" + domain + "\">" + domain + "</a>"
  }
  
     
  
  def formatNumber(number: Int): String = {
    val formatter = java.text.NumberFormat.getIntegerInstance
    formatter.format(number)
  }
  
  def percent(number: Int, prev: Int): Double = {
    val value = ((number - prev) / (prev * 1.0)) / 100.0
    BigDecimal(value).setScale(3, BigDecimal.RoundingMode.HALF_UP).toDouble
  }
}