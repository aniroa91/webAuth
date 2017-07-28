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
import slick.driver.PostgresDriver.api.Database
import slick.driver.PostgresDriver.api.columnExtensionMethods
import slick.driver.PostgresDriver.api.intColumnType
import slick.driver.PostgresDriver.api.queryInsertActionExtensionMethods
import slick.driver.PostgresDriver.api.streamableQueryActionExtensionMethods
import slick.driver.PostgresDriver.api.stringColumnType
import slick.lifted.Query
import slick.lifted.TableQuery
import com.typesafe.config.Config

object DomainService {
  private val ES_HOST = ConfigFactory.load().getString("es.host")
  private val ES_PORT = ConfigFactory.load().getString("es.port").toInt
  
  private val ES_INDEX = "dns-service-domain-"
  private val ES_INDEX_ALL = ES_INDEX + "*"
  
  private val SIZE_DAY = 7

  private val client = HttpClient(ElasticsearchClientUri(ES_HOST, ES_PORT))
  
  def getDomainInfo(domain: String): JsonObject = {
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
    //val domain = multiSearchResponse.responses(1)
    
    val jo = new JsonObject()
    val ja = new JsonArray()
    val gson = new Gson()
    val current = secondResponse.hits.hits.head
    
    secondResponse.hits.hits.map(x => {
      //val jsonObject = new JsonObject();
      //jsonObject.add(property, value)
      
      val json = gson.fromJson(x.sourceAsString, classOf[JsonObject])
      //json.toString()
      ja.add(json)
    })
    
    val jsonObjectCurrent = gson.fromJson(current.sourceAsString, classOf[JsonObject])
    jsonObjectCurrent.addProperty("num_of_domain", SearchReponseUtil.getCardinality(domainResponse, "num_of_domain"))
    jo.add("current", jsonObjectCurrent)
    jo.add("history", ja)
    val answers = answerResponse.hits.hits.map(x => x.sourceAsMap.getOrElse("answer", "").toString()).filter(x => x != "")
    jo.addProperty("answer", answers.mkString(" "))
    
//    println(answerResponse.totalHits)
    //val second = second.hits.hits
    //domainResponse.aggregations.get(key)
//    secondResponse.hits.hits.foreach(x => println(x.sourceAsString))
    //println(SearchReponseUtil.getCardinality(domainResponse, "num_of_domain"))
    //println(domainResponse.aggregations.get("num_of_domain").get.asInstanceOf[Map[String, String]].get("value"))
    //client.close()
    //null
    
    //JsonprettyJson()
    jo
  }
  
  def close() = client.close()

  def main(args: Array[String]) {
    //getDomainInfo("google.com")
    //close()
    
//    val db: Database = Database.forConfig("postgres_dns")
//    
//    val domains = TableQuery[DomainTable]
////    val q = domains.filter(_.name === "")
//    val all = db.run(domains.result).await
//    
//    println(all)
//    //val action = q.result
//    //val result: Future[Seq[Double]] = db.run(action)
//    //val sql = action.statements.head
//    
//    db.close()
    
    DAO.
  }
}