package utils.es

import com.sksamuel.elastic4s.{ElasticsearchClientUri, TcpClient}
import com.sksamuel.elastic4s.ElasticDsl._

class EsSearch(host: String, port: Int) {

  def this(host: String) = this(host, 9300)
  val client = TcpClient.transport(ElasticsearchClientUri(host, port))

  def search1(esIndex: String, esType: String, query: String) {
    val res = client.execute {
      search(esIndex / esType).query(query)
    }.await
  }
}