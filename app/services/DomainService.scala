package services

import scala.concurrent.Await
import scala.concurrent.duration.Duration

import org.elasticsearch.search.sort.SortOrder

import com.ftel.bigdata.utils.HttpUtil
import com.sksamuel.elastic4s.ElasticsearchClientUri
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.HttpClient
import com.sksamuel.elastic4s.http.search.SearchHit
import com.sksamuel.elastic4s.http.search.SearchResponse
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

import slick.driver.PostgresDriver.api._
import slick.lifted.Query
import slick.lifted.TableQuery
import utils.SearchReponseUtil
import com.ftel.bigdata.dns.model.table.MalwareTable
import com.ftel.bigdata.dns.model.table.WhoisObject
import com.ftel.bigdata.dns.model.table.LabelTable
import com.ftel.bigdata.dns.model.table.ServerNameTable
import com.ftel.bigdata.dns.model.table.DomainTable
import com.ftel.bigdata.dns.model.table.DomainServerNameTable
import com.ftel.bigdata.dns.model.table.RegistrarTable
import com.ftel.bigdata.dns.model.table.StatusTable
import model.BaicInfo
import model.Response


object DomainService {
  
  private val ES_HOST = ConfigFactory.load().getString("es.host")
  private val ES_PORT = ConfigFactory.load().getString("es.port").toInt
  
  val ES_INDEX = "dns-service-domain-"
  val ES_INDEX_ALL = ES_INDEX + "*"
  
  val SIZE_DAY = 30

  val client = HttpClient(ElasticsearchClientUri(ES_HOST, ES_PORT))
  //val db = DatabaseConfig.forConfig[JdbcProfile]("slick.dbs.default").db
  
  def close() = client.close()

  def getDomainInfo(domain: String): Response = {
    val multiSearchResponse = client.execute(
      multi(
        search(ES_INDEX_ALL / "second") query { must(termQuery("second", domain)) } sortBy { fieldSort("day") order SortOrder.DESC } limit SIZE_DAY,
        search(ES_INDEX_ALL / "answer") query { must(termQuery("second", domain)) } limit 1000,
        search(ES_INDEX_ALL / "domain") query { must(termQuery("second", domain)) } aggregations (
          cardinalityAgg("num_of_domain", "domain")),
        search((ES_INDEX + "whois") / "whois") query { must(termQuery("domain", domain)) })).await
    val secondResponse = multiSearchResponse.responses(0)
    val answerResponse = multiSearchResponse.responses(1)
    val domainResponse = multiSearchResponse.responses(2)
    val whoisResponse = multiSearchResponse.responses(3)
    if (secondResponse.totalHits > 0) {
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

      //val whois = getWhoisInfo(db, domain)
      val whois = getWhoisInfo(whoisResponse)

      //println("==============")
      val history = secondResponse.hits.hits.map(searchHit2BasicInfo)
      val current = secondResponse.hits.hits.head
      val basicInfo = searchHit2BasicInfo(current)
      val answers = answerResponse.hits.hits.map(x => x.sourceAsMap.getOrElse("answer", "").toString()).filter(x => x != "")
      Response(whois, basicInfo, answers, history, SearchReponseUtil.getCardinality(domainResponse, "num_of_domain"))
    } else Response(new WhoisObject(), null, null, null, 0)
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

  def getWhoisInfo(whoisResponse: SearchResponse): WhoisObject = {
    if (whoisResponse.totalHits > 0) {
    val map = whoisResponse.hits.hits.head.sourceAsMap
    val whois = WhoisObject(
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
    } else new WhoisObject()
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

