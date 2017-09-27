package services

import com.sksamuel.elastic4s.http.search.SearchResponse

case class Bucket(key: String, count: Int, value: Int)
case class BucketDouble(key: String, count: Double, value: Double)
case class Bucket2(key: String, term: String, count: Double, value: Double)

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
  
  def getBucketDoubleTerm(response: SearchResponse, nameTerm: String, nameSubTerm: String): Array[BucketDouble] = {
    if (response.aggregations != null) {
    response.aggregations
      .getOrElse(nameTerm, Map[String, AnyRef]()).asInstanceOf[Map[String, AnyRef]]
      .getOrElse("buckets", List).asInstanceOf[List[AnyRef]]
      .map(x => x.asInstanceOf[Map[String, AnyRef]])
      .map(x => {
        val key = x.getOrElse("key", "key").toString()
        val count = x.getOrElse("doc_count", "0").toString().toDouble
        val value = x.getOrElse(nameSubTerm, Map[String, AnyRef]()).asInstanceOf[Map[String, AnyRef]].getOrElse("value", "0").toString().toDouble
        BucketDouble(key, count, value)
      })
      .toArray
    } else {
      Array[BucketDouble]()
    }
  }
  
  def getBucketTerm2(response: SearchResponse, nameTerm: String, nameSubTerm: String): Array[Bucket2] = {
    if (response.aggregations != null) {
    response.aggregations
      .getOrElse(nameTerm, Map[String, AnyRef]()).asInstanceOf[Map[String, AnyRef]]
      .getOrElse("buckets", List).asInstanceOf[List[AnyRef]]
      .map(x => x.asInstanceOf[Map[String, AnyRef]])
      .flatMap(x => {
        val key = x.getOrElse("key", "key").toString()
        val sub = x.getOrElse("sub", Map[String, AnyRef]()).asInstanceOf[Map[String, AnyRef]]
                   .getOrElse("buckets", List).asInstanceOf[List[AnyRef]]
                   .map(y => y.asInstanceOf[Map[String, AnyRef]])
                   .map(y => {
                      Bucket2(
                          key,
                          y.getOrElse("key", "key").toString(),
                          y.getOrElse("doc_count", "0").toString().toDouble,
                          y.getOrElse(nameSubTerm, Map[String, AnyRef]()).asInstanceOf[Map[String, AnyRef]].getOrElse("value", "0").toString().toDouble)
                   })
        //val count = x.getOrElse("doc_count", "0").toString().toDouble
        //val value = x.getOrElse(nameSubTerm, Map[String, AnyRef]()).asInstanceOf[Map[String, AnyRef]].getOrElse("value", "0").toString().toDouble
       sub
      }).toArray
    } else {
      Array[Bucket2]()
    }
  }
}