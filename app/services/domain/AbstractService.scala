package services.domain

import org.elasticsearch.search.sort.SortOrder

import com.ftel.bigdata.utils.DateTimeUtil
import com.sksamuel.elastic4s.ElasticsearchClientUri
import com.sksamuel.elastic4s.http.ElasticDsl.RichFuture
import com.sksamuel.elastic4s.http.ElasticDsl.RichString
import com.sksamuel.elastic4s.http.ElasticDsl.SearchHttpExecutable
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.HttpClient

import services.Configure
import com.ftel.bigdata.dns.parameters.Label
//import model.BasicInfo
import com.sksamuel.elastic4s.http.search.SearchResponse
import com.ftel.bigdata.whois.Whois
import com.ftel.bigdata.utils.WhoisUtil
//import services.DomainService
import services.AppGlobal
import model.TotalInfo
import model.MalwareInfo
import model.MainDomainInfo
import com.ftel.bigdata.utils.FileUtil
import scala.util.Try
import com.ftel.bigdata.utils.HttpUtil

abstract class AbstractService {
  
  protected val LABEL_FIELD = "label"
  protected val BLACK_VALUE = Label.Black
  protected val TOTAL_VALUE = "total"
  protected val NUM_QUERY_FIELD = "number_of_record"
  protected val NUM_DOMAIN_FIELD = "number_of_domain"
  protected val NUM_SECOND_FIELD = "number_of_second_domain"
  protected val NUM_IP_FIELD = "number_of_ip"
  protected val NUM_MALWARE_FIELD = "number_of_malware"
  protected val DAY_FIELD = "day"
  
  protected val MALWARE_FIELD = "malware"
  protected val DOMAIN_FIELD = "domain"
  protected val SECOND_FIELD = "second"
  

  val ES_INDEX = "dns-service-domain-"
  val ES_INDEX_ALL = ES_INDEX + "*"
  
  val SIZE_DAY = 30
  val MAX_SIZE_RETURN = 100
  
  val client = AppGlobal.client // HttpClient(ElasticsearchClientUri(Configure.ES_HOST, Configure.ES_PORT))
  
  def getLatestDay(): String = {
    val response = client.execute(
        search(ES_INDEX_ALL / "second") sortBy { fieldSort("day") order SortOrder.DESC } limit 1
    ).await
    response.hits.hits.head.sourceAsMap.getOrElse("day", "").toString()
  }

  def getPreviousDay(day: String): String = {
    val prev = DateTimeUtil.create(day, DateTimeUtil.YMD)
    prev.minusDays(1).toString(DateTimeUtil.YMD)
  }
  
  def getTopBlackByNumOfQuery(day: String): Array[MainDomainInfo] = {
    val response = client.execute (
        search(ES_INDEX_ALL / "second") query {
          boolQuery()
            .must(termQuery("day", day))
            .not(termQuery("malware", "none"),termQuery("malware", "null"))
        }  sortBy {
          fieldSort("number_of_record") order(SortOrder.DESC)
        } limit MAX_SIZE_RETURN).await
    getMainDomainInfo(response)
  }
  
  def getTopByNumOfQuery(day: String, label: String): Array[MainDomainInfo] = {
    val malware = if (label == Label.White) Label.None else if (label == Label.Unknow) "null" else ???
    val response = client.execute (
        search(ES_INDEX_ALL / "second") query {
          boolQuery().must(termQuery("day", day), termQuery("malware", malware))
        }  sortBy {
          fieldSort("number_of_record") order(SortOrder.DESC)
        } limit MAX_SIZE_RETURN).await
    getMainDomainInfo(response)
  }
  
//  private def convert(response: SearchResponse): Array[MainDomainInfo] = {
//    response.hits.hits.map(x => {
//      val map = x.sourceAsMap
//      val day: String = map.getOrElse("day", "").toString()
//      val numOfQuery: Int = map.getOrElse("number_of_record", "0").toString().toInt
//      val numOfClient: Int = map.getOrElse("number_of_ip", "0").toString().toInt
//      val malware: String = map.getOrElse("malware", "null").toString()
//      val label: String = Label.getLabelFrom(malware)
//      val rankFtel: Int = map.getOrElse("rank_ftel", "0").toString().toInt
//      val rankAlexa: Int = map.getOrElse("rank_alexa", "0").toString().toInt
//      val domain = map.getOrElse("second", "").toString()
//      domain -> new BasicInfo(day, numOfQuery, numOfClient, malware, rankFtel, rankAlexa)
//    })
//  }
  
  

  def getTopRank(from: Int, day: String): Array[MainDomainInfo] = {
    val response = client.execute(
        search(ES_INDEX_ALL / "second") query {
          boolQuery().must(rangeQuery("rank_ftel").gt(from-1).lt(MAX_SIZE_RETURN),termQuery("day", day))
          } sortBy { fieldSort("rank_ftel") 
        } limit MAX_SIZE_RETURN
    ).await
    getMainDomainInfo(response)
  }
  
//  def getTotalFrom(response: SearchResponse): LabelResponse = {
//    val arrayLabelResponse = response.hits.hits.map(x => {
//      val map = x.sourceAsMap
//      val label = map.getOrElse("label", "").toString()
//      val malwares = if (label == BLACK_VALUE) map.getOrElse("number_of_malware", "0").toString().toInt else 0
//      LabelResponse(
//          label,
//          map.getOrElse("number_of_record", "0").toString().toInt,
//          map.getOrElse("number_of_domain", "0").toString().toInt,
//          map.getOrElse("number_of_ip", "0").toString().toInt,
//          malwares,
//          map.getOrElse("success", "0").toString().toInt,
//          map.getOrElse("failed", "0").toString().toInt,
//          map.getOrElse("number_of_second_domain", "0").toString().toInt)
//    })
//    arrayLabelResponse.reduce((x,y) => LabelResponse("total",
//        x.queries + y.queries,
//        x.domains + y.domains,
//        x.clients + y.clients,
//        x.malwares + y.malwares,
//        x.success + y.success,
//        x.failed + y.failed,
//        x.seconds + y.seconds))
//    
//  }
  
  
  def getWhoisInfo(whoisResponse: SearchResponse, domain: String, label: String, malware: String): Whois = {
    //println(whoisResponse.totalHits)
    if (whoisResponse.totalHits > 0) {
      
      val map = whoisResponse.hits.hits.head.sourceAsMap
      //println(map)
      val whois = Whois(
        map.getOrElse("domain", "").toString(),
        map.getOrElse("registrar", "").toString(),
        map.getOrElse("whoisServer", "").toString(),
        map.getOrElse("referral", "").toString(),
        map.getOrElse("servername", "").toString().split(" "),
        map.getOrElse("status", "").toString(),
        map.getOrElse("update", "").toString(),
        map.getOrElse("create", "").toString(),
        map.getOrElse("expire", "").toString(),
        map.getOrElse("label", "").toString(),
        map.getOrElse("malware", "").toString())
      whois
    } else {
      
      getWhoisFromWeb(domain, label, malware)
    }
  }
  
   def getWhoisFromWeb(domain: String, label: String, malware: String): Whois = {
    val esIndex = s"dns-service-domain-whois"
    val esType = "whois"
    val whois = WhoisUtil.whoisService(domain, label, malware, "172.30.45.220", 80)
    if (whois.isValid()) {
      indexWhois(esIndex, esType, whois)
      whois
    } else new Whois()
  }
   
   def indexWhois(esIndex: String, esType: String, whois: Whois) {
    // indexInto("bands" / "artists") doc Artist("Coldplay") refresh(RefreshPolicy.IMMEDIATE)
    // domain:bmwsociety.com servername:ns18.worldnic.com ns17.worldnic.com label:white create:2002-01-11 referral:networksolutions.com registrar:network solutions, llc. expire:2020-01-11 update:2014-11-12 malware:none _id:bmwsociety.com 
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
//
//  private val URL_DOMAIN_DEFAULT = "../assets/images/logo/domain.png"
////  val STORAGE_PATH = ConfigFactory.load().getString("storage") + "/"
////  private val LOGO_URL = "https://logo.clearbit.com/"
//  def getLogoPath(secondDomain: String): String = {
//    val logoUrl = Configure.LOGO_API_URL + secondDomain
//    val path = Configure.LOGO_PATH + secondDomain + ".png"
//    val logo = "../extassets/" + secondDomain + ".png"
//    if (!FileUtil.isExist(path)) {
//      println("Don't exist " + path)
//      Try(HttpUtil.download(logoUrl, path, "172.30.45.220", 80))
//    }
//    if (FileUtil.isExist(path)) {
//      logo
//    } else URL_DOMAIN_DEFAULT
//  }
//   
   /*******************************
    * 
    * REFACTOR
    * 
    * ****************************/
  def getTotalInfo(searchResponse: SearchResponse): Array[(String, TotalInfo)] = {
    val result = searchResponse.hits.hits.map(x => {
      val sourceAsMap = x.sourceAsMap
      val label = sourceAsMap.getOrElse("label", "").toString()
      val malwares = if (label == BLACK_VALUE) sourceAsMap.getOrElse(NUM_MALWARE_FIELD, "0").toString().toInt else 0
      label -> TotalInfo(
         getValueAsInt(sourceAsMap, NUM_QUERY_FIELD),
         getValueAsInt(sourceAsMap, NUM_DOMAIN_FIELD),
         getValueAsInt(sourceAsMap, NUM_IP_FIELD),
         malwares,
         getValueAsInt(sourceAsMap, "success"),
         getValueAsInt(sourceAsMap, "failed"),
         getValueAsInt(sourceAsMap, NUM_SECOND_FIELD))
     })
    result
  }

  def getTotalInfoDaily(searchResponse: SearchResponse): Array[(String, TotalInfo)] = {
    searchResponse.hits.hits.reverse.map(x => {
      val sourceAsMap = x.sourceAsMap
      getValueAsString(sourceAsMap, DAY_FIELD) -> TotalInfo(
        getValueAsInt(sourceAsMap, NUM_QUERY_FIELD),
        getValueAsInt(sourceAsMap, NUM_DOMAIN_FIELD),
        getValueAsInt(sourceAsMap, NUM_IP_FIELD),
        0, 0, 0, 0)
    })
  }
  
  def getMalwareInfo(searchResponse: SearchResponse): Array[MalwareInfo] = {
    searchResponse.hits.hits.map(x => {
      val sourceAsMap = x.sourceAsMap
      MalwareInfo(
          getValueAsString(sourceAsMap, MALWARE_FIELD),
          getValueAsInt(sourceAsMap, NUM_QUERY_FIELD),
          getValueAsInt(sourceAsMap, NUM_DOMAIN_FIELD),
          getValueAsInt(sourceAsMap, NUM_IP_FIELD))
    })
  }

  def getMainDomainInfo(searchResponse: SearchResponse): Array[MainDomainInfo] = {
    searchResponse.hits.hits.map(x => {
      val sourceAsMap = x.sourceAsMap
      new MainDomainInfo(
        getValueAsString(sourceAsMap, DAY_FIELD),
        getValueAsString(sourceAsMap, SECOND_FIELD),
        //getValueAsString(sourceAsMap, LABEL_FIELD),
        getValueAsString(sourceAsMap, MALWARE_FIELD),
        getValueAsInt(sourceAsMap, NUM_QUERY_FIELD),
        //getValueAsInt(sourceAsMap, NUM_DOMAIN_FIELD),
        getValueAsInt(sourceAsMap, NUM_IP_FIELD),
        getValueAsInt(sourceAsMap, "rank_ftel"),
        getValueAsInt(sourceAsMap, "rank_alexa"))
    })
  }
  
//  def getMainDomainInfo(searchResponse: SearchResponse, numOfDomain: Int): Array[MainDomainInfo] = {
//    searchResponse.hits.hits.map(x => {
//      val sourceAsMap = x.sourceAsMap
//      new MainDomainInfo(
//        getValueAsString(sourceAsMap, DAY_FIELD),
//        getValueAsString(sourceAsMap, SECOND_FIELD),
//        //getValueAsString(sourceAsMap, LABEL_FIELD),
//        getValueAsString(sourceAsMap, MALWARE_FIELD),
//        getValueAsInt(sourceAsMap, NUM_QUERY_FIELD),
//        numOfDomain,
//        getValueAsInt(sourceAsMap, NUM_IP_FIELD),
//        getValueAsInt(sourceAsMap, "rank_ftel"),
//        getValueAsInt(sourceAsMap, "rank_alexa"))
//    })
//  }

  private def getValueAsString(map: Map[String, Any], key: String): String = {
    map.getOrElse(key, "").toString()
  }
  
  private def getValueAsInt(map: Map[String, Any], key: String): Int = {
    map.getOrElse(key, "0").toString().toInt
  }
  
  def sumTotalInfo(totalInfoArrs: Array[TotalInfo]): TotalInfo = {
    totalInfoArrs.reduce((x, y) => {
      TotalInfo(
          x.queries  + y.queries,
          x.domains  + y.domains,
          x.clients  + y.clients,
          x.malwares + y.malwares,
          x.success  + y.success,
          x.failed   + y.failed,
          x.seconds  + y.seconds)
    })
  }
}