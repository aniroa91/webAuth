package services

import com.sksamuel.elastic4s.http.search.SearchResponse

case class Bucket(key: String, count: Int, value: Int)

object ElasticUtil {
  def getBucketTerm(response: SearchResponse, nameTerm: String, nameSubTerm: String): Array[Bucket] = {
    if (response.aggregations != null) {
    response.aggregations
      .getOrElse(nameTerm, Map[String, AnyRef]()).asInstanceOf[Map[String, AnyRef]]
      .getOrElse("buckets", List).asInstanceOf[List[AnyRef]]
      .map(x => x.asInstanceOf[Map[String, AnyRef]])
      .map(x => {
        val key = x.getOrElse("key", "key").toString()
        val count = x.getOrElse("doc_count", "0").toString().toInt
        val value = x.getOrElse(nameSubTerm, Map[String, AnyRef]()).asInstanceOf[Map[String, AnyRef]].getOrElse("value", "0").toString().toDouble.toInt
        Bucket(key, count, value)
      })
      .toArray
    } else {
      Array[Bucket]()
    }
  }
}