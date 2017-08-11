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

import model.MainDomainInfo
import model.ProfileResponse
import utils.SearchReponseUtil

object ProfileService extends AbstractService {

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
      val whois = CommonService.getWhoisInfo(whoisResponse, domain, current.label, current.malware)
      val answers = answerResponse.hits.hits.map(x => x.sourceAsMap.getOrElse("answer", "").toString()).filter(x => x != "")
      ProfileResponse(whois, current, history, answers)
    } else null
  }

}