package services.domain

import com.ftel.bigdata.dns.parameters.Label
import com.sksamuel.elastic4s.http.search.SearchResponse

import model.MainDomainInfo
import model.MalwareInfo
import model.TotalInfo
import services.Configure

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
  
  val client = Configure.client // HttpClient(ElasticsearchClientUri(Configure.ES_HOST, Configure.ES_PORT))

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