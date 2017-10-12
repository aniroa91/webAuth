package services.user

import com.sksamuel.elastic4s.http.ElasticDsl._
import services.Configure
import com.ftel.bigdata.utils.ESUtil
import com.sksamuel.elastic4s.searches.SearchDefinition
import services.domain.AbstractService
import services.ElasticUtil
import model.user.DashboardResponse
import services.Bucket

object DashboardService extends AbstractService {

  private val TO = "2017-09-25"
  val SIZE_DEFAULT = 100

  def get(): DashboardResponse = {
    val multiSearchResponse = client.execute(multi(
          term(TO),
          termHourly(TO)
        )).await
    //println(client.show(term(TO)))
    val responses = multiSearchResponse.responses
    //val appByCount = responses(0)
    val app = ElasticUtil.getBucketTerm(responses(0), "topApp", "unique")
    val uniqueCustomer = responses(0).aggregations.get("uniqueCustomer").get.asInstanceOf[Map[String, Int]].getOrElse("value", 0)
    val uniqueContract = responses(0).aggregations.get("uniqueContract").get.asInstanceOf[Map[String, Int]].getOrElse("value", 0)
    val dayOfWeek = ElasticUtil.getBucketTerm(responses(0), "topDayOfWeek", "avg")
    val region = ElasticUtil.getBucketDoubleTerm(responses(0), "topRegion", "sum")
    val province = ElasticUtil.getBucketDoubleTerm(responses(0), "topProvince", "sum")

    val hourly = ElasticUtil.getBucketTerm(responses(1), "top", "avg")
    //    //appByCount.aggregations.foreach(println)
    //    appByCustomer.foreach(x => println(x.key -> x.count -> x.value))
    //    println(uniqueCustomer -> uniqueContract)
    //    responses(1).aggregations.foreach(println)
    //responses(1).aggregations.foreach(println)
//    dayOfWeek.foreach(x => println(x.key -> x.value))
    //province.foreach(x => println(x.key -> x.value))
    
    DashboardResponse(app, uniqueCustomer, uniqueContract, dayOfWeek.map(x => Bucket(ProfileService.dayOfWeekNumberToLabel(x.key.toInt), x.count, x.value)), region, province, hourly)
  }

  private def term(to: String): SearchDefinition = {
    search(s"paytv-weekly-daily-${to}" / "docs") aggregations (
        termsAggregation("topApp").field("app").subaggs(cardinalityAgg("unique", "customer")) size SIZE_DEFAULT,
        cardinalityAggregation("uniqueCustomer").field("customer"),
        cardinalityAggregation("uniqueContract").field("contract"),
        termsAggregation("topDayOfWeek").field("dayOfWeek").subaggs(avgAgg("avg", "value")) size 7,
        termsAggregation("topRegion").field("region").subaggs(sumAgg("sum", "value")) size SIZE_DEFAULT,
        termsAggregation("topProvince").field("province").subaggs(sumAgg("sum", "value")) size 60
        )
  }

  private def termHourly(to: String): SearchDefinition = {
    search(s"paytv-weekly-hourly-${to}" / "docs") aggregations (
        termsAggregation("top").field("hour").subaggs(avgAgg("avg", "value")) size 24)
  }
  
//  private def uniqueContract(to: String): SearchDefinition = {
//    search(s"paytv-weekly-daily-${to}" / "docs") aggregations (cardinalityAggregation("unique").field("contract"))
//  }

  def main(args: Array[String]) {
    DashboardService.get()
    client.close()
  }
}