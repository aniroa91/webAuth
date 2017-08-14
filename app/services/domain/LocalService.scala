package services.domain

import org.elasticsearch.search.sort.SortOrder

import com.sksamuel.elastic4s.http.ElasticDsl.MultiSearchHttpExecutable
import com.sksamuel.elastic4s.http.ElasticDsl.RichFuture
import com.sksamuel.elastic4s.http.ElasticDsl.RichString
import com.sksamuel.elastic4s.http.ElasticDsl.cardinalityAgg
import com.sksamuel.elastic4s.http.ElasticDsl.fieldSort
import com.sksamuel.elastic4s.http.ElasticDsl.multi
import com.sksamuel.elastic4s.http.ElasticDsl.must
import com.sksamuel.elastic4s.http.ElasticDsl.search
import com.sksamuel.elastic4s.http.ElasticDsl.termQuery
import com.sksamuel.elastic4s.http.ElasticDsl.termsAgg
import com.sksamuel.elastic4s.http.ElasticDsl.termsAggregation
import com.sksamuel.elastic4s.http.ElasticDsl.sumAgg

import model.MainDomainInfo
import model.ProfileResponse
import utils.SearchReponseUtil
import org.elasticsearch.search.aggregations.bucket.terms.Terms.Bucket

object LocalService extends AbstractService {

  
  
  def main(args: Array[String]) {
    val domain = "google.com"
    val day = "2017-08-13"
    val time0 = System.currentTimeMillis()
    val multiSearchResponse = client.execute(
      multi(
        search(s"dns-statslog-${day}" / "docs") query { must(termQuery(SECOND_FIELD, domain)) } aggregations (
            termsAggregation("hourly").field("hour").subagg(sumAgg("sum", "queries")) // sortBy { fieldSort(DAY_FIELD) order SortOrder.DESC } 
        ))).await
    val time1 = System.currentTimeMillis()
    val response = multiSearchResponse.responses(0)
    
    
    //type MAP_ANY = Map[String, Any]

    //val a = response.aggregations.
    //response.aggregations.foreach(println)

    val buckets = response.aggregations.get("hourly").getOrElse(Map[String, AnyRef]()).asInstanceOf[Map[String, AnyRef]].getOrElse("buckets", List).asInstanceOf[List[AnyRef]]
    
    val res = buckets.map(x => x.asInstanceOf[Map[String, AnyRef]])
           .map(x => x.getOrElse("key", "key").asInstanceOf[Int] -> x.getOrElse("sum", Map[String, AnyRef]()).asInstanceOf[Map[String, AnyRef]])
           .map(x => x._1 -> x._2.get("value").getOrElse("0").asInstanceOf[Double])
           //.map(x => x._1 -> x._2)
    //val buckets = terms.getOrElse("buckets", List())
    //buckets.map(x => )
    //val map = terms.getAggregations.asMap()
    println(res)
    client.close()
  }

}