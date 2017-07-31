package services

import slick.basic.DatabaseConfig
import com.typesafe.config.ConfigFactory
import com.sksamuel.elastic4s.ElasticsearchClientUri
import slick.jdbc.JdbcProfile
import com.sksamuel.elastic4s.http.HttpClient

object Connection {
  private val ES_HOST = ConfigFactory.load().getString("es.host")
  private val ES_PORT = ConfigFactory.load().getString("es.port").toInt
  
  val ES_INDEX = "dns-service-domain-"
  val ES_INDEX_ALL = ES_INDEX + "*"
  
  val SIZE_DAY = 7

  val client = HttpClient(ElasticsearchClientUri(ES_HOST, ES_PORT))
  val db = DatabaseConfig.forConfig[JdbcProfile]("slick.dbs.default").db
  
  def close() = client.close()
}