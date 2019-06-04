package utils

import com.sksamuel.elastic4s.http.search.SearchResponse
import org.elasticsearch.search.aggregations.metrics.cardinality.InternalCardinality

object SearchReponseUtil {
  def getCardinality(searchResponse: SearchResponse, name: String): Int = {
    searchResponse.aggregations.getOrElse(name, Map()).asInstanceOf[Map[String, Int]].getOrElse("value", -1)
  }
}