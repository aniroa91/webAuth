package services.domain

import org.elasticsearch.search.sort.SortOrder

import com.ftel.bigdata.dns.parameters.Label
import com.ftel.bigdata.utils.DateTimeUtil
import com.ftel.bigdata.utils.WhoisUtil
import com.ftel.bigdata.whois.Whois
import com.sksamuel.elastic4s.http.ElasticDsl.IndexHttpExecutable
import com.sksamuel.elastic4s.http.ElasticDsl.RichFuture
import com.sksamuel.elastic4s.http.ElasticDsl.RichString
import com.sksamuel.elastic4s.http.ElasticDsl.SearchHttpExecutable
import com.sksamuel.elastic4s.http.ElasticDsl.boolQuery
import com.sksamuel.elastic4s.http.ElasticDsl.fieldSort
import com.sksamuel.elastic4s.http.ElasticDsl.indexInto
import com.sksamuel.elastic4s.http.ElasticDsl.rangeQuery
import com.sksamuel.elastic4s.http.ElasticDsl.search
import com.sksamuel.elastic4s.http.ElasticDsl.termQuery
import com.sksamuel.elastic4s.http.search.SearchResponse

import model.MainDomainInfo
import scala.util.Try
import services.Configure
import com.ftel.bigdata.utils.FileUtil
import com.ftel.bigdata.utils.HttpUtil

object CommonService extends AbstractService {

  /**
   * Service for Get Information about day
   */
  def getLatestDay(): String = {
    val response = client.execute(
      search(ES_INDEX_ALL / "second") sortBy { fieldSort("day") order SortOrder.DESC } limit 1).await
    response.hits.hits.head.sourceAsMap.getOrElse("day", "").toString()
  }

  def getPreviousDay(day: String): String = {
    val prev = DateTimeUtil.create(day, DateTimeUtil.YMD)
    prev.minusDays(1).toString(DateTimeUtil.YMD)
  }
  
  def isDayValid(day: String): Boolean = {
    Try(DateTimeUtil.create(day, DateTimeUtil.YMD)).isSuccess
  }

  /**
   * Get Whois From Web
   */
  def getWhoisInfo(whoisResponse: SearchResponse, domain: String, label: String, malware: String): Whois = {
    if (whoisResponse.totalHits > 0) {
      val map = whoisResponse.hits.hits.head.sourceAsMap
      val whois = Whois(
        map.getOrElse("domain", "").toString(),
        map.getOrElse("registrar", "").toString(),
        map.getOrElse("whoisServer", "").toString(),
        map.getOrElse("referral", "").toString(),
        map.getOrElse("servername", "").toString().split(" "),
        map.getOrElse("status", "").toString(),
        map.getOrElse("create", "").toString(),
        map.getOrElse("update", "").toString(),
        map.getOrElse("expire", "").toString(),
        map.getOrElse("label", "").toString(),
        map.getOrElse("malware", "").toString())
      whois
    } else {
      getWhoisFromWeb(domain, label, malware)
    }
  }
  private def getWhoisFromWeb(domain: String, label: String, malware: String): Whois = {
    val esIndex = s"dns-service-domain-whois"
    val esType = "whois"
    try {
      val whois = WhoisUtil.whoisService(domain, label, malware, Configure.PROXY_HOST, Configure.PROXY_PORT)
      if (whois.isValid()) {
        indexWhois(esIndex, esType, whois)
        whois
      } else new Whois()
    } catch {
      case e: Exception => e.printStackTrace(); new Whois()
    }
  }
  private def indexWhois(esIndex: String, esType: String, whois: Whois) {
    client.execute(
      indexInto(esIndex / esType) fields (
        "domain" -> whois.domainName,
        "registrar" -> whois.registrar,
        "whoisServer" -> whois.whoisServer,
        "referral" -> whois.referral,
        "servername" -> whois.nameServer.mkString(" "),
        "status" -> whois.status,
        "create" -> whois.create.substring(0, 10),
        "update" -> whois.update.substring(0, 10),
        "expire" -> (if (whois.expire.isEmpty()) "2999-12-31" else whois.expire.substring(0, 10)),
        "label" -> whois.label,
        "malware" -> whois.malware)
        id whois.domainName).await
  }

  /**
   * Get Top
   */
  def getTopBlackByNumOfQuery(day: String): Array[MainDomainInfo] = {
    val response = client.execute(
      search(ES_INDEX_ALL / "second") query {
        boolQuery()
          .must(termQuery("day", day))
          .not(termQuery("malware", "none"), termQuery("malware", "null"))
      } sortBy {
        fieldSort("number_of_record") order (SortOrder.DESC)
      } limit MAX_SIZE_RETURN).await
    getMainDomainInfo(response)
  }

  def getTopByNumOfQuery(day: String, label: String): Array[MainDomainInfo] = {
    val malware = if (label == Label.White) Label.None else if (label == Label.Unknow) "null" else ???
    val response = client.execute(
      search(ES_INDEX_ALL / "second") query {
        boolQuery().must(termQuery("day", day), termQuery("malware", malware))
      } sortBy {
        fieldSort("number_of_record") order (SortOrder.DESC)
      } limit MAX_SIZE_RETURN).await
    getMainDomainInfo(response)
  }

  def getTopRank(from: Int, day: String): Array[MainDomainInfo] = {
    val response = client.execute(
      search(ES_INDEX_ALL / "second") query {
        boolQuery().must(rangeQuery("rank_ftel").gt(from - 1).lt(MAX_SIZE_RETURN), termQuery("day", day))
      } sortBy {
        fieldSort("rank_ftel")
      } limit MAX_SIZE_RETURN).await
    getMainDomainInfo(response)
  }

  /**
   * Utils
   */
  def formatNumber(number: Int): String = {
    val formatter = java.text.NumberFormat.getIntegerInstance
    formatter.format(number)
  }
  
  def percent(number: Int, prev: Int): Double = {
    val value = ((number - prev) / (prev * 1.0)) / 100.0
    BigDecimal(value).setScale(3, BigDecimal.RoundingMode.HALF_UP).toDouble
  }
  
  /**
   * Create html tag
   */
  def getImageTag(domain: String): String = {
    val logo = downloadLogo(domain)
    "<a href=\"/search?q=" + domain + "\"><img src=\"" + logo + "\" width=\"30\" height=\"30\"></a>"
  }
  
  def getLinkTag(domain: String): String = {
    "<a href=\"/search?q=" + domain + "\">" + domain + "</a>"
  }

  /**
   * Download image
   */
  private def downloadLogo(secondDomain: String): String = {
    val logoUrl = Configure.LOGO_API_URL + secondDomain
    val path = Configure.LOGO_PATH + secondDomain + ".png"
    val logo = "../extassets/" + secondDomain + ".png"
    if (!FileUtil.isExist(path)) {
      println("Download logo to " + path)
      Try(HttpUtil.download(logoUrl, path, Configure.PROXY_HOST, Configure.PROXY_PORT))
    }
    if (FileUtil.isExist(path)) {
      logo
    } else Configure.LOGO_DEFAULT
  }
  
  /**
   * ********************************************************************************
   * ********************************************************************************
   * ********************************************************************************
   */
  
  
}