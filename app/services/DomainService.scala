package services

import com.sksamuel.elastic4s.ElasticsearchClientUri
import com.sksamuel.elastic4s.http.HttpClient
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.search.SearchResponse
import com.sksamuel.elastic4s.http.search.MultiSearchResponse
import com.typesafe.config.ConfigFactory
import org.elasticsearch.search.sort.SortOrder
import utils.SearchReponseUtil
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonElement
import com.google.gson.Gson
import model.DomainTable
import slick.lifted.TableQuery
import com.ftel.bigdata.db.slick.PostgresSlick

// DB
//slick.driver.PostgresDriver
//import slick.jdbc.PostgresProfile.api._
//import slick.driver.PostgresDriver
import slick.driver.PostgresDriver.api._
//import slick.driver.PostgresDriver.api.columnExtensionMethods
//import slick.driver.PostgresDriver.api.intColumnType
//import slick.driver.PostgresDriver.api.queryInsertActionExtensionMethods
//import slick.driver.PostgresDriver.api.streamableQueryActionExtensionMethods
//import slick.driver.PostgresDriver.api.stringColumnType
import slick.lifted.Query
import slick.lifted.TableQuery
import com.typesafe.config.Config
import slick.basic.DatabaseConfig
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfig
import slick.jdbc.JdbcProfile
import slick.jdbc.PostgresProfile
import javax.inject.Inject
import play.db.NamedDatabase
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import model.MalwareTable
import model.LabelTable
import model.ServerNameTable
import model.DomainServerNameTable
import model.RegistrarTable
import model.StatusTable
import model.WhoisObject
import services.Connection._
import model.Response
//import model.HistoryInfo
import model.BaicInfo
import com.sksamuel.elastic4s.http.search.SearchHit
import com.ftel.bigdata.utils.HttpUtil


object DomainService {
  
  private val ES_HOST = ConfigFactory.load().getString("es.host")
  private val ES_PORT = ConfigFactory.load().getString("es.port").toInt
  
  val ES_INDEX = "dns-service-domain-"
  val ES_INDEX_ALL = ES_INDEX + "*"
  
  val SIZE_DAY = 7

  val client = HttpClient(ElasticsearchClientUri(ES_HOST, ES_PORT))
  val db = DatabaseConfig.forConfig[JdbcProfile]("slick.dbs.default").db
  
  def close() = client.close()
  
  def getDomainInfo(domain: String): Response = {
    val multiSearchResponse = client.execute(
      multi( 
        search(ES_INDEX_ALL / "second") query {must(termQuery("second", domain))} sortBy {fieldSort("day") order SortOrder.DESC} limit SIZE_DAY,
        search(ES_INDEX_ALL / "answer") query {must(termQuery("second", domain))} limit 1000,
        search(ES_INDEX_ALL / "domain") query {must(termQuery("second", domain))}  aggregations (
          cardinalityAgg("num_of_domain", "domain")
        )
    )).await
    val secondResponse = multiSearchResponse.responses(0)
    val answerResponse = multiSearchResponse.responses(1)
    val domainResponse = multiSearchResponse.responses(2)
    
    def searchHit2BasicInfo = (x: SearchHit) => {
      val map = x.sourceAsMap
      val label = map.getOrElse("label", "").toString()
      val numOfClient = map.getOrElse("number_of_ip", "0").toString().toInt
      val second = map.getOrElse("second", "").toString()
      val rankAlexa = map.getOrElse("rank_alexa", "0").toString().toInt
      val rankFtel = map.getOrElse("rank_ftel", "0").toString().toInt
      val numOfQuery = map.getOrElse("number_of_record", "0").toString().toInt
      val malware = map.getOrElse("malware", "").toString()
      val day = map.getOrElse("day", "").toString()
      BaicInfo(day, numOfQuery, numOfClient, label, malware, rankFtel, rankAlexa)
    }
    val whois = getWhoisInfo(db, domain)
    
    //println("==============")
    val history = secondResponse.hits.hits.map(searchHit2BasicInfo)
    val current = secondResponse.hits.hits.head
    val basicInfo = searchHit2BasicInfo(current)
    val answers = answerResponse.hits.hits.map(x => x.sourceAsMap.getOrElse("answer", "").toString()).filter(x => x != "")
    Response(whois, basicInfo, answers, history, SearchReponseUtil.getCardinality(domainResponse, "num_of_domain"))
    //null
//    Response(whois,)
//    
//    println(whoisInfo)
//    val jo = new JsonObject()
//    val ja = new JsonArray()
//    val gson = new Gson()
//    val current = secondResponse.hits.hits.head
//
//    val jsonObjectCurrent = gson.fromJson(current.sourceAsString, classOf[JsonObject])
//    jsonObjectCurrent.addProperty("num_of_domain", SearchReponseUtil.getCardinality(domainResponse, "num_of_domain"))
//    
//    // Convert History to JsonArray
//    secondResponse.hits.hits.map(x => {
//      //val jsonObject = new JsonObject();
//      //jsonObject.add(property, value)
//      
//      val json = gson.fromJson(x.sourceAsString, classOf[JsonObject])
//      //json.toString()
//      ja.add(json)
//    })
//
//    
////    registrar: String,
////    whoisServer: String,
////    referral: String,
////    nameServer: Array[String],
////    status: String,
////    update: String,
////    create: String,
////    expire: String,
//    
//    // Add whois Info
//    jsonObjectCurrent.addProperty("registrar", whoisInfo.registrar)
//    jsonObjectCurrent.addProperty("whoisServer", whoisInfo.whoisServer)
//    jsonObjectCurrent.addProperty("referral", whoisInfo.referral)
//    jsonObjectCurrent.addProperty("nameServer", whoisInfo.nameServer.mkString(" "))
//    jsonObjectCurrent.addProperty("status", whoisInfo.status)
//    jsonObjectCurrent.addProperty("create", whoisInfo.create)
//    jsonObjectCurrent.addProperty("update", whoisInfo.update)
//    jsonObjectCurrent.addProperty("expire", whoisInfo.expire)
//    
//    
//    jo.add("current", jsonObjectCurrent)
//    jo.add("history", ja)
//    val answers = answerResponse.hits.hits.map(x => x.sourceAsMap.getOrElse("answer", "").toString()).filter(x => x != "")
//    jo.addProperty("answer", answers.mkString(" "))
//
//    jo
    
    /*
     *     

     */
  }
  
  

  def getWhoisInfo(db: Database, domain: String): WhoisObject = {
    val domains = TableQuery[DomainTable]
    val domainServerName = TableQuery[DomainServerNameTable]
    val label = TableQuery[LabelTable]
    val malware = TableQuery[MalwareTable]
    val registrar = TableQuery[RegistrarTable]
    val serverName = TableQuery[ServerNameTable]
    val status = TableQuery[StatusTable]
    
    val q = for {
      d <- domains if d.name === domain
      ds <- domainServerName if d.id === ds.domainId
      l <- label if d.labelId === l.id
      m <- malware if d.malwareId === m.id
      r <- registrar if d.registrarId === r.id
      s <- serverName if ds.serverId === s.id
      st <- status if d.statusId === st.id
    } yield (d.name, r.name, r.server, r.url, s.name, st.name, d.create, d.update, d.expire, l.name, m.name)
    val result = Await.result(db.run(q.result), Duration.Inf)
    val servers = result.map(x => x._5).toArray
    val head = result.head
    val whois = WhoisObject(head._1, head._2, head._3, head._4, servers, head._6, head._8.toString().split(" ")(0), head._7.toString().split(" ")(0), head._9.toString(), head._10, head._11)
    whois
  }

  def formatNumber(number: Int): String = {
    val formatter = java.text.NumberFormat.getIntegerInstance
    formatter.format(number)
  }
  def main(args: Array[String]) {
    //DomainService.getDomainInfo("google.com")
    //val db = DatabaseConfig.forConfig[JdbcProfile]("slick.dbs.default").db
    //val whoisInfo = getWhoisInfo(db, "google.com")
    
    //println(whoisInfo)
    HttpUtil.download("https://logo.clearbit.com/" + "google.com", "public/images/" + "google.com" + ".png", "172.30.45.220", 80)
  }
}

