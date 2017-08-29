package services.domain

import org.elasticsearch.search.sort.SortOrder

import com.ftel.bigdata.dns.parameters.Label
import com.ftel.bigdata.utils.DateTimeUtil
import com.ftel.bigdata.utils.WhoisUtil
import com.ftel.bigdata.whois.Whois
import com.sksamuel.elastic4s.http.ElasticDsl.IndexHttpExecutable
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.search.SearchResponse

import model.MainDomainInfo
import scala.util.Try
import services.Configure
import com.ftel.bigdata.utils.FileUtil
import com.ftel.bigdata.utils.HttpUtil
import org.elasticsearch.search.aggregations.bucket.terms.Terms
import org.apache.http.HttpHost
import scalaj.http.Http
import play.api.libs.json.Json
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit
import org.jsoup.Jsoup
import org.jsoup.select.Elements

object CommonService extends AbstractService {

  val SIZE_DEFAULT = 20

  /**
   * Service for Get Information about day
   */
  def getLatestDay(): String = {
    val response = client.execute(
      search("dns-daily-*" / "docs") sortBy { fieldSort("day") order SortOrder.DESC } limit 1).await
    response.hits.hits.head.sourceAsMap.getOrElse("day", "").toString()
  }

  def getPreviousDay(day: String): String = {
    val prev = DateTimeUtil.create(day, DateTimeUtil.YMD)
    prev.minusDays(1).toString(DateTimeUtil.YMD)
  }
  
  def getPreviousDay(day: String, num: Int): String = {
    val prev = DateTimeUtil.create(day, DateTimeUtil.YMD)
    prev.minusDays(num).toString(DateTimeUtil.YMD)
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
      CommonService.backgroupJob(
          getWhoisFromWeb(domain, label, malware),
          "Download Whois for " + domain)
      //getWhoisFromWeb(domain, label, malware)
      new Whois()
    }
  }
  private def getWhoisFromWeb(domain: String, label: String, malware: String): Whois = {
    val esIndex = s"dns-service-domain-whois"
    val esType = "whois"
    try {
      val whois = WhoisUtil.whoisService(domain, label, malware, Configure.PROXY_HOST, Configure.PROXY_PORT)
      //println(whois)
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
   * Get Category
   */
  def getCategory(domain: String): String = {
    val getResponse = client.execute(com.sksamuel.elastic4s.http.ElasticDsl.get(domain) from "dns-category/docs").await
    println(domain -> getResponse.sourceAsMap)
    val category = getResponse.sourceAsMap.getOrElse("category", "N/A").toString()
    if (category == "N/A") {
      CommonService.backgroupJob(indexCategory(domain),"Download Category for " + domain)
    }
    category
  }
  
  def indexCategory(domain: String) {
    val category = getCategorySitereviewBluecoatCom(domain)
//    println(category)
    if (category != null) {
      client.execute( indexInto("dns-category" / "docs") fields ("category" -> category) id domain).await//(Duration.apply(10, TimeUnit.SECONDS))
    }
  }
  
  def getCategorySitereviewBluecoatCom(domain: String): String = {
    
    val req = Http("http://sitereview.bluecoat.com/rest/categorization")
                .proxy(Configure.PROXY_HOST, Configure.PROXY_PORT)
                .postForm(Seq("url" -> domain))
                
    val res = req.asString.body
    println(res)
    val json = Json.parse(res)
    val option = json.\("categorization")
    if (option.isEmpty) {
      null
    } else {
      val doc = Jsoup.parse(option.get.toString())
      val elements = doc.body().select("a")
      val seq = 0 until elements.size()
      seq.map(x => elements.get(x))
         .map(x => x.text())
         .mkString(" AND ")

//      elements.
//      for (e <- elements.toArray(Elements)) {
//        println("1" + e.text())
//      }
      
//      val endIndex = option.get.toString().lastIndexOf("</a>")
//      val beginIndex = option.get.toString().substring(0, endIndex).lastIndexOf("\\\">") + 3
//      option.get.toString().substring(beginIndex, endIndex)
      
    }
  }
  
  /**
   * Get Top
   */
//  def getTopBlackByNumOfQuery(day: String): Array[MainDomainInfo] = {
//    val response = client.execute(
//      search(s"dns-second-${day}" / "docs") query {
//        boolQuery()
//          .must(termQuery("day", day))
//          .not(termQuery("malware", "none"), termQuery("malware", "null"))
//      } sortBy {
//        fieldSort("queries") order (SortOrder.DESC)
//      } limit MAX_SIZE_RETURN).await
//    getMainDomainInfo(response)
//  }

  def getTopByNumOfQuery(day: String, label: String): Array[MainDomainInfo] = {
    val response = client.execute(
      search(s"dns-second-${day}" / "docs") query {
        boolQuery().must(termQuery("day", day), termQuery("label", label))
      } sortBy {
        fieldSort("queries") order (SortOrder.DESC)
      } limit MAX_SIZE_RETURN).await
    println(s"Time(${day} ${label}): " + response.took)
    getMainDomainInfo(response)
  }

  def getTopByNumOfQueryWithRange(fromDay: String, endDay: String): Array[MainDomainInfo] = {
    val response = client.execute(
      search(s"dns-second-*" / "docs") query {
        boolQuery()
          .must(
              rangeQuery("day").gte(fromDay).lte(endDay),
              rangeQuery("rank").gt(0).lte(MAX_SIZE_RETURN * 10)
              )
      } aggregations (
          termsAggregation("top")
            .field("second")
            .subagg(sumAgg("sum", "queries")) order(Terms.Order.aggregation("sum", false)) size MAX_SIZE_RETURN
      ) sortBy {
        fieldSort("queries") order (SortOrder.DESC)
      } limit MAX_SIZE_RETURN).await
    println(s"Time(${fromDay} ${endDay}): " + response.took)
    getMainDomainInfo2(response)
  }
  
  def getTopRank(from: Int, day: String): Array[MainDomainInfo] = {
    val response = client.execute(
      search(s"dns-second-${day}" / "docs") query {
        boolQuery().must(rangeQuery("rank").gt(from - 1).lt(MAX_SIZE_RETURN), termQuery("day", day))
      } sortBy {
        fieldSort("rank")
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
    val logo = getLogo(domain, false)
    //"<a href=\"/search?q=" + domain + "\"><img src=\"" + logo + "\" width=\"30\" height=\"30\"></a>"
    //<img id="currentPhoto" src="SomeImage.jpg" onerror="this.src='Default.jpg'" width="100" height="120">
    "<a href=\"/search?q=" + domain + "\"><img src=\"" + logo + "\" onerror=\"this.src='../assets/images/logo/default.png'\" width=\"30\" height=\"30\"></a>"
  }
  
  def getLinkTag(domain: String): String = {
    "<a href=\"/search?q=" + domain + "\">" + domain + "</a>"
  }

  /**
   * Download image
   */
//  def downloadLogo(secondDomain: String): String = {
//    val logoUrl = Configure.LOGO_API_URL + secondDomain
//    val path = Configure.LOGO_PATH + secondDomain + ".png"
//    val logo = "../extassets/" + secondDomain + ".png"
//    if (!FileUtil.isExist(path)) {
//      println("Download logo to " + path)
//      Try(HttpUtil.download(logoUrl, path, Configure.PROXY_HOST, Configure.PROXY_PORT))
//    }
//    if (FileUtil.isExist(path)) {
//      logo
//    } else Configure.LOGO_DEFAULT
//  }

  def getLogo(secondDomain: String, download: Boolean): String = {
    val logoUrl = Configure.LOGO_API_URL + secondDomain
    val path = Configure.LOGO_PATH + secondDomain + ".png"
    val logo = "../extassets/" + secondDomain + ".png"
    if (download) {
      if (!FileUtil.isExist(path)) {
        println("Download logo to " + path)
        Try(HttpUtil.download(logoUrl, path, Configure.PROXY_HOST, Configure.PROXY_PORT))
      }
    }
    if (FileUtil.isExist(path)) {
      logo
    } else {
      Configure.LOGO_DEFAULT
    }
  }

  /**
   * ********************************************************************************
   * ********************************************************************************
   * ********************************************************************************
   */

  def backgroupJob(f: => Unit, msg: String) {
    val thread = new Thread {
      override def run {
        val time0 = System.currentTimeMillis()
        println("Start " +  msg)
        //all.map(x => x.name).map(x => CommonService.getLogo(x, true))
        f
        val time1 = System.currentTimeMillis()
        println("End " +  msg + s" [${time1 -time0}]")
        
      }
    }
    thread.start()
  }
}