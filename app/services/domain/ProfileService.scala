package services.domain

import model.DashboardResponse

import org.elasticsearch.search.sort.SortOrder
import com.ftel.bigdata.utils.HttpUtil
import com.sksamuel.elastic4s.ElasticsearchClientUri
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.HttpClient
import com.sksamuel.elastic4s.http.search.SearchHit
import com.sksamuel.elastic4s.http.search.SearchResponse
//import model.MalwareResponse
//import model.DailyResponse
//import model.DomainResponse
//import model.LabelResponse
//import model.SecondResponse
import services.CacheService
//import model.Response
//import model.BasicInfo
import utils.SearchReponseUtil
import model.ProfileResponse
import model.MainDomainInfo


class ProfileService extends AbstractService {

  def get(domain: String): ProfileResponse = {
    val multiSearchResponse = client.execute(
      multi(
        search(ES_INDEX_ALL / "second") query { must(termQuery(SECOND_FIELD, domain)) } sortBy { fieldSort(DAY_FIELD) order SortOrder.DESC } limit SIZE_DAY,
        search(ES_INDEX_ALL / "answer") query { must(termQuery(SECOND_FIELD, domain)) } limit 1000,
        search(ES_INDEX_ALL / "domain") query { must(termQuery(SECOND_FIELD, domain)) } aggregations (
          cardinalityAgg(NUM_DOMAIN_FIELD, "domain")),
        search((ES_INDEX + "whois") / "whois") query { must(termQuery(DOMAIN_FIELD, domain)) })).await
    val secondResponse = multiSearchResponse.responses(0)
    val answerResponse = multiSearchResponse.responses(1)
    val domainResponse = multiSearchResponse.responses(2)
    val whoisResponse = multiSearchResponse.responses(3)
    
    if (secondResponse.totalHits > 0) {
      val numOfDomain = SearchReponseUtil.getCardinality(domainResponse, NUM_DOMAIN_FIELD)
      val history = getMainDomainInfo(secondResponse)
      val current = new MainDomainInfo(history.head, numOfDomain)
      val whois = getWhoisInfo(whoisResponse, domain, current.label, current.malware)
      val answers = answerResponse.hits.hits.map(x => x.sourceAsMap.getOrElse("answer", "").toString()).filter(x => x != "")
      ProfileResponse(whois, current, history, answers)
    } else null
  }

}

object ProfileService {
  def get(domain: String): ProfileResponse = {
    val service = new ProfileService();
    service.get(domain)
  }
}