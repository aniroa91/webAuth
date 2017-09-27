package com.ftel.bigdata.utils

import com.sksamuel.elastic4s.http.HttpClient
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.get.GetResponse
import scala.util.Try
import com.sksamuel.elastic4s.ElasticsearchClientUri
import com.sksamuel.elastic4s.http.search.SearchResponse
import com.sksamuel.elastic4s.searches.queries.QueryDefinition

object ESUtil {
  
  def getClient(esHost: String, esPort: Int): HttpClient = {
    HttpClient(ElasticsearchClientUri(esHost, esPort))
  }
  
  def get(client: HttpClient, esIndex: String, esType: String, id: String): GetResponse = {
    client.execute(com.sksamuel.elastic4s.http.ElasticDsl.get(id) from s"${esIndex}/${esType}").await
  }
  
  def get(client: HttpClient, esIndex: String, esType: String, field: String, value: String): SearchResponse = {
    val searchDef = search(esIndex / esType) query {boolQuery().must(query(field + ":" + value))} size 1
//    val request = client.show(searchDef)
//    println(request)
    client.execute(searchDef).await
  }
  //val response = client.execute(search(s"dns-client-*" / "docs") query {boolQuery().must(termQuery("client", ip))} size 30).await
  
  def exist(client: HttpClient, esIndex: String, esType: String, id: String): Boolean = {
    try {
      get(client, esIndex, esType, id).exists
    } catch {
      case e: Exception => false
    }
  }
  
  def index(client: HttpClient, esIndex: String, esType: String, json: String, id: String) {
    Try(client.execute(indexInto(esIndex / esType) doc (json) id id ).await)
  }
  
  def index(client: HttpClient, esIndex: String, esType: String, fields: Map[String, Any], id: String) {
    Try(client.execute(indexInto(esIndex / esType) fields fields id id ).await)
  }
  
  def upset(client: HttpClient, esIndex: String, esType: String, field: String, value: Any, id: String) {
    Try(client.execute(update(id).in(esIndex / esType).docAsUpsert(field -> value)).await)
  }
  
  def upset(client: HttpClient, esIndex: String, esType: String, fields: Map[String, Any], id: String) {
    Try(client.execute(update(id).in(esIndex / esType).docAsUpsert(fields)).await)
  }
  
  def upset(client: HttpClient, esIndex: String, esType: String, map: Map[String, Map[String, Any]]) {
    Try(client.execute {
      bulk(
        map.map(x => update(x._1).in(esIndex / esType).docAsUpsert(x._2))
      )
    }.await)
  }

  def scroll(client: HttpClient, esIndex: String, esType: String, queryDef: QueryDefinition): Map[String, String] = {
    val MAX_SIZE_RETURN = 10000
    val KEEP_ALIVE = "1m"
    //val client = ESUtil.getClient("172.27.11.156", 9200)
    val response = client.execute(search(esIndex / esType) query {boolQuery().must(queryDef)} scroll KEEP_ALIVE size MAX_SIZE_RETURN).await
    //var size = response.hits.hits.size
    //var totalHits = response.totalHits
    var scrollId = response.scrollId.getOrElse(null)
    def getSource(acc: Map[String, String], searchResponse: SearchResponse, currentSize: Int): Map[String, String] = {
      val size = currentSize + searchResponse.hits.hits.size
      val total = searchResponse.totalHits
      //println(s"${size}/${total}")
      val hits = searchResponse.hits.hits.map(x => x.id -> x.sourceAsString).toMap
      val result = acc ++ hits
      if (size < total) {
        getSource(result, client.execute {searchScroll(scrollId, KEEP_ALIVE)}.await, size)
      } else result
    }
    
    getSource(Map(), response, 0)
//    while (size <= totalHits) {
//      val responseInner = client.execute {searchScroll(scrollId, KEEP_ALIVE)}.await
//      totalHits = responseInner.totalHits
//      size = size + responseInner.hits.hits.size
//    }

  }
  def main(args: Array[String]) {
    val esIndex = "dns-test"
    val esType = "upset"
    val client = getClient("172.27.11.156", 9200)
//    index(client, esIndex, esType, Map("a" -> 1, "b" -> 2), "a")
//    upset()
    upset(client, esIndex, esType, "c", 2, "b")
    client.close()
  }
}