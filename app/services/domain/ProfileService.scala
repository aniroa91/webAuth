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
import com.sksamuel.elastic4s.http.ElasticDsl.termsAggregation
import com.sksamuel.elastic4s.http.ElasticDsl._

import model.MainDomainInfo
import model.ProfileResponse
import utils.SearchReponseUtil
import com.sksamuel.elastic4s.http.search.SearchResponse

object ProfileService extends AbstractService {

  def get(domain: String): ProfileResponse = {
    get2(domain)
  }

  def get2(domain: String): ProfileResponse = {
    val latestDay = CommonService.getLatestDay()
    val time0 = System.currentTimeMillis()
    val multiSearchResponse = client.execute(
      multi(
        search(s"dns-second-*" / "docs") query { must(termQuery(SECOND_FIELD, domain)) } sortBy { fieldSort(DAY_FIELD) order SortOrder.DESC } limit SIZE_DAY,
//        search("dns-service-domain-*" / "answer") query { must(termQuery(SECOND_FIELD, domain)) } limit 1000,
        search(s"dns-domain-${latestDay}" / "docs") query { must(termQuery(SECOND_FIELD, domain)) },
          //aggregations (cardinalityAgg(NUM_DOMAIN_FIELD, "domain")),
        search(("dns-service-domain-whois") / "whois") query { must(termQuery(DOMAIN_FIELD, domain)) }
        )).await

    val time1 = System.currentTimeMillis()
    val secondResponse = multiSearchResponse.responses(0)
    //val answerResponse = multiSearchResponse.responses(1)
    val domainResponse = multiSearchResponse.responses(1)
    val whoisResponse = multiSearchResponse.responses(2)

    println(secondResponse.took)
    println(domainResponse.took)
    println(whoisResponse.took)
    
    if (secondResponse.totalHits > 0) {
      val time2 = System.currentTimeMillis()
      val numOfDomain = domainResponse.totalHits//SearchReponseUtil.getCardinality(domainResponse, NUM_DOMAIN_FIELD)
      val history = getMainDomainInfo(secondResponse)
      val current = new MainDomainInfo(history.head, numOfDomain)
      val time3 = System.currentTimeMillis()
      val whois = CommonService.getWhoisInfo(whoisResponse, domain, current.label, current.malware)
      val time4 = System.currentTimeMillis()
      val answers = null//answerResponse.hits.hits.map(x => x.sourceAsMap.getOrElse("answer", "").toString()).filter(x => x != "")
      val time5 = System.currentTimeMillis()
      val hourly = Array[(Int, Long)]()//getHourly(domain, current)
      val time6 = System.currentTimeMillis()
      val category = CommonService.getCategory(domain)
      CommonService.backgroupJob(CommonService.getLogo(domain, true), "Download Logo")
      val time7 = System.currentTimeMillis()
      printTime(time0,time1,time2,time3,time4,time5, time6, time7)
      ProfileResponse(whois, current, history, answers, hourly, category)
      //ProfileResponse(whois, current, history, hourly)
    } else null
  }

  @deprecated("","")
  def get1(domain: String): ProfileResponse = {
    val time0 = System.currentTimeMillis()
    val multiSearchResponse = client.execute(
      multi(
        search("dns-service-domain-*" / "second") query { must(termQuery(SECOND_FIELD, domain)) } sortBy { fieldSort(DAY_FIELD) order SortOrder.DESC } limit SIZE_DAY,
        search("dns-service-domain-*" / "answer") query { must(termQuery(SECOND_FIELD, domain)) } limit 1000,
        search("dns-service-domain-*" / "domain") query { must(termQuery(SECOND_FIELD, domain)) } aggregations (
          cardinalityAgg(NUM_DOMAIN_FIELD, "domain")),
        search((ES_INDEX + "whois") / "whois") query { must(termQuery(DOMAIN_FIELD, domain)) }
        )).await
    val time1 = System.currentTimeMillis()
    val secondResponse = multiSearchResponse.responses(0)
    val answerResponse = multiSearchResponse.responses(1)
    val domainResponse = multiSearchResponse.responses(2)
    val whoisResponse = multiSearchResponse.responses(3)
    
    if (secondResponse.totalHits > 0) {
      val time2 = System.currentTimeMillis()
      val numOfDomain = SearchReponseUtil.getCardinality(domainResponse, NUM_DOMAIN_FIELD)
      val history = getMainDomainInfo(secondResponse)
      val current = new MainDomainInfo(history.head, numOfDomain)
      val time3 = System.currentTimeMillis()
      val whois = CommonService.getWhoisInfo(whoisResponse, domain, current.label, current.malware)
      val time4 = System.currentTimeMillis()
      val answers = answerResponse.hits.hits.map(x => x.sourceAsMap.getOrElse("answer", "").toString()).filter(x => x != "")
      val time5 = System.currentTimeMillis()
      val hourly = Array[(Int, Long)]()//getHourly(domain, current)
      val time6 = System.currentTimeMillis()
      //printTime(time0,time1,time2,time3,time4,time5, time6)
      ProfileResponse(whois, current, history, answers, hourly, "N/A")
    } else null
  }
  
  private def getHourly(domain: String, current: MainDomainInfo): Array[(Int, Long)] = {
    val day = current.day
    val multiSearchResponse = client.execute(
      multi(
        search(s"dns-statslog-${day}" / "docs") query { must(termQuery(SECOND_FIELD, domain)) } aggregations (
            termsAggregation("hourly").field("hour").subagg(sumAgg("sum", "queries")) size 24// sortBy { fieldSort(DAY_FIELD) order SortOrder.DESC } 
        ))).await
    val response = multiSearchResponse.responses(0)
    getHourly(response)
  }
  
  private def getHourly(response: SearchResponse): Array[(Int, Long)] = {
    if (response.aggregations != null) {
    response.aggregations
      .getOrElse("hourly", Map[String, AnyRef]()).asInstanceOf[Map[String, AnyRef]]
      .getOrElse("buckets", List).asInstanceOf[List[AnyRef]]
      .map(x => x.asInstanceOf[Map[String, AnyRef]])
      .map(x => x.getOrElse("key", "key").asInstanceOf[Int] -> x.getOrElse("sum", Map[String, AnyRef]()).asInstanceOf[Map[String, AnyRef]])
      .map(x => x._1 -> x._2.get("value").getOrElse("0").asInstanceOf[Double])
      .map(x => x._1 -> x._2.toLong).sorted
      .toArray
    } else {
      Array[(Int, Long)]()
    }
    
  }
}