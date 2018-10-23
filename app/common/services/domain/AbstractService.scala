package services.domain

//import com.ftel.bigdata.dns.parameters.Label
import com.sksamuel.elastic4s.http.search.SearchResponse
import services.Configure

abstract class AbstractService {

  protected val LABEL_FIELD = "label"
  protected val TOTAL_VALUE = "total"
  protected val NUM_QUERY_FIELD = "queries"
  protected val NUM_DOMAIN_FIELD = "domains"
  protected val NUM_SECOND_FIELD = "seconds"
  protected val NUM_IP_FIELD = "clients"
  protected val NUM_MALWARE_FIELD = "malwares"
  protected val DAY_FIELD = "day"
  protected val RANK_FIELD = "rank"
  protected val RANK_ALEXA_FIELD = "rankAlexa"

  protected val MALWARE_FIELD = "malware"
  protected val DOMAIN_FIELD = "domain"
  protected val SECOND_FIELD = "second"
  protected val CLIENT_FIELD = "client"
  protected val TIMESTAMP_FIELD = "timeStamp"

  val ES_INDEX = "dns-service-domain-"
  val ES_INDEX_ALL = ES_INDEX + "*"

  val SIZE_DAY = 30
  val MAX_SIZE_RETURN = 100

  
  val client = Configure.client // HttpClient(ElasticsearchClientUri(Configure.ES_HOST, Configure.ES_PORT))

  def getValueAsString(map: Map[String, Any], key: String, default: String): String = {
    val result = map.getOrElse(key, default)
    if (result != null) result.toString() else default
  }
  
  def getValueAsString(map: Map[String, Any], key: String): String = {
    getValueAsString(map, key, "")
  }
  
  def getValueAsInt(map: Map[String, Any], key: String): Int = {
    getValueAsString(map, key, "0").toInt
  }

  def getValueAsLong(map: Map[String, Any], key: String): Long = {
    getValueAsString(map, key, "0").toLong
  }

  def getValueAsDouble(map: Map[String, Any], key: String): Double = {
    getValueAsString(map, key, "0").toDouble
  }

  def printTime(times: Long*) {
    val size = times.size
    if (size > 1) {
      for (i <- 0 until size-1) {
        println(s"Time[$i] - Time[${i + 1}]: " + (times(i + 1) - times(i)))
      }
    }
  }
  
  def getTermGroupMultiSums(response: SearchResponse, nameTerm: String, nameSubTerm1: String,nameSubTerm2: String): Array[(String, Long,Long)] = {
    if (response.aggregations != null) {
    response.aggregations
      .getOrElse(nameTerm, Map[String, AnyRef]()).asInstanceOf[Map[String, AnyRef]]
      .getOrElse("buckets", List).asInstanceOf[List[AnyRef]]
      .map(x => x.asInstanceOf[Map[String, AnyRef]])
      .map(x => (x.getOrElse("key", "key").toString(),x.getOrElse(nameSubTerm1, Map[String, AnyRef]()).asInstanceOf[Map[String, AnyRef]],x.getOrElse(nameSubTerm2, Map[String, AnyRef]()).asInstanceOf[Map[String, AnyRef]]))
      .map(x => (x._1, x._2.get("value").getOrElse("0").asInstanceOf[Double],x._3.get("value").getOrElse("0").asInstanceOf[Double]))
      .map(x => (x._1,x._2.toLong,x._3.toLong)).sorted
      .toArray
    } else {
      Array[(String, Long,Long)]()
    }
  }

  def getTerm(response: SearchResponse, nameTerm: String, nameSubTerm: String): Array[(String, Long)] = {
    if (response.aggregations != null) {
      response.aggregations
        .getOrElse(nameTerm, Map[String, AnyRef]()).asInstanceOf[Map[String, AnyRef]]
        .getOrElse("buckets", List).asInstanceOf[List[AnyRef]]
        .map(x => x.asInstanceOf[Map[String, AnyRef]])
        .map(x => x.getOrElse("key", "key").toString() -> x.getOrElse(nameSubTerm, Map[String, AnyRef]()).asInstanceOf[Map[String, AnyRef]])
        .map(x => x._1 -> x._2.get("value").getOrElse("0").asInstanceOf[Double])
        .map(x => x._1 -> x._2.toLong).sorted
        .toArray
    } else {
      Array[(String, Long)]()
    }
  }

  def getTerm(response: SearchResponse, nameTerm: String, nameSubTerms: Array[String]): Array[(String, Map[String, Long])] = {
    if (response.aggregations != null) {
    response.aggregations
      .getOrElse(nameTerm, Map[String, AnyRef]()).asInstanceOf[Map[String, AnyRef]]
      .getOrElse("buckets", List).asInstanceOf[List[AnyRef]]
      .map(x => x.asInstanceOf[Map[String, AnyRef]])
      .map(x => {
        val key = x.getOrElse("key", "key").toString()
        val value = nameSubTerms
          .map(y => y -> x.getOrElse(y, Map[String, AnyRef]()).asInstanceOf[Map[String, AnyRef]])
          .map(y => y._1 -> y._2.get("value").getOrElse("0").toString().toDouble)
          .map(y => y._1 -> y._2.toLong)
          .toMap
        key -> value
        })
      .toArray
    } else {
      Array[(String, Map[String, Long])]()
    }
  }
}