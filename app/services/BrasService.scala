package service

import model.device._
import services.domain.{AbstractService, CommonService}
import com.sksamuel.elastic4s.http.ElasticDsl._
import scala.concurrent.Future
import org.elasticsearch.search.sort.SortOrder
import services.domain.CommonService.formatUTC
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormat

object BrasService extends AbstractService{

  def getBrasOutlierCurrent(day: String):Array[BrasOutlier]  ={
    val response = client.execute(
      search(s"monitor-radius-$day" / "docs")
        query { must(termQuery("label","outlier")) }
        sortBy { fieldSort("date_time") order SortOrder.DESC } size 100
    ).await
    val brasOutlier = response.hits.hits.map(x => x.sourceAsMap)
      .map(x => BrasOutlier(
        getValueAsString(x, "date_time"),
        getValueAsString(x, "bras_id"),
        getValueAsInt(x,"signIn"),
        getValueAsInt(x,"logOff")
       )
      )
    brasOutlier.asInstanceOf[Array[BrasOutlier]]
  }

  def getBrasOutlierJson(day: String): Array[(String,String,Int,Int)] ={
    val response = client.execute(
      search(s"monitor-radius-$day" / "docs")
        query { must(termQuery("label","outlier")) }
        sortBy { fieldSort("date_time") order SortOrder.DESC } size 100
    ).await
    val brasOutlier = response.hits.hits.map(x => x.sourceAsMap)
      .map(x =>
        ( getValueAsString(x, "bras_id"),
          formatUTC(getValueAsString(x, "date_time")),
          getValueAsInt(x,"signIn"),
        getValueAsInt(x,"logOff"))
      )
    brasOutlier
  }

  def getJsonBrasCard(bras: String,time: String,_type: String): Array[((String,String),Long)] = {
    val formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")
    val dateTime = DateTime.parse(time, formatter)
    val oldHalfHour  = dateTime.minusHours(14).toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss"))
    val afterHalfHour  = dateTime.plusHours(4).toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss"))
    val response = client.execute(
        search(s"radius-streaming-*" / "con")
          query { must(termQuery("nasName", bras.toLowerCase),termQuery("typeLog", _type),rangeQuery("timestamp").gte(CommonService.formatStringToUTC(oldHalfHour)).lte(CommonService.formatStringToUTC(afterHalfHour)))}
          aggregations (
          termsAggregation("linecard")
            .field("card.lineId")
            .subAggregations(
              termsAggregation("card")
                .field("card.id")
            )
          )
    ).await
     /*println(client.show(search(s"radius-streaming-*" / "con")
       query { must(termQuery("nasName", bras.toLowerCase),termQuery("typeLog", _type),rangeQuery("timestamp").gte(CommonService.formatStringToUTC(oldHalfHour)).lte(CommonService.formatStringToUTC(afterHalfHour)))}
       aggregations (
       termsAggregation("linecard")
         .field("card.lineId")
         .subAggregations(
           termsAggregation("card")
             .field("card.id")
         )
       )))*/
    val mapHeat = CommonService.getSecondAggregations(response.aggregations.get("linecard"))
    mapHeat.flatMap(x => x._2.map(y => x._1 -> y))
      .map(x => (x._1 -> x._2._1) -> x._2._2).filter(x=> x._1._1 != "-1").filter(x=> x._1._2 != "-1")
  }

  def getJsonESBrasChart(bras: String,time: String):Array[(String,Int,Int,Int)] = {
    val formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")
    val dateTime = DateTime.parse(time, formatter)
    val oldHalfHour  = dateTime.minusMinutes(30).toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss"))
    val afterHalfHour  = dateTime.plusMinutes(30).toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss"))
    val response = client.execute(
      search(s"monitor-radius-*" / "docs")
        query { must(termQuery("bras_id",bras),rangeQuery("date_time").gte(CommonService.formatStringToUTC(oldHalfHour)).lte(CommonService.formatStringToUTC(afterHalfHour))) } size 100
        sortBy { fieldSort("date_time") order SortOrder.DESC }
    ).await
    val jsonRs = response.hits.hits.map(x=> x.sourceAsMap)
      .map(x=>(
        getValueAsString(x,"date_time"),
        getValueAsInt(x,"logOff"),
        getValueAsInt(x,"signIn"),
        getValueAsInt(x,"active_users")
      ))
    jsonRs
  }

  def getSigLogByHost(bras: String,day: String): SigLogByHost ={
    BrasDAO.getSigLogByHost(bras,day)
  }
  // for page Search Bras
  def getSigLogResponse(bras: String,fromDay: String,nextDay: String):Future[Seq[(Int,Int)]] = {
    BrasDAO.getSigLogResponse(bras,fromDay,nextDay)
  }
  def getSigLogCurrent(bras: String,nowDay: String):(Int,Int) = {
    BrasDAO.getSigLogCurrent(bras,nowDay)
  }

  def getNoOutlierResponse(bras: String,nowDay: String):Future[Seq[(Int)]] = {
    BrasDAO.getNoOutlierResponse(bras,nowDay)
  }
  def getNoOutlierCurrent(bras: String,nowDay: String): Int = {
    BrasDAO.getNoOutlierCurrent(bras,nowDay)
  }

  def getOpviewBytimeResponse(bras: String,nowDay: String,hourly: Int):Future[Seq[(Int,Int)]] = {
    BrasDAO.getOpviewBytimeResponse(bras,nowDay,hourly)
  }

  def getKibanaBytimeResponse(bras: String,nowDay: String,hourly: Int):Future[Seq[(Int,Int)]] = {
    BrasDAO.getKibanaBytimeResponse(bras,nowDay,hourly)
  }

  def getSigLogBytimeResponse(bras: String,nowDay: String,hourly: Int):Future[Seq[(Int,Int,Int)]] = {
    BrasDAO.getSigLogBytimeResponse(bras,nowDay,hourly)
  }
  def getSigLogBytimeCurrent(bras: String,nowDay: String): SigLogByTime = {
    BrasDAO.getSigLogBytimeCurrent(bras,nowDay)
  }

  def getInfErrorBytimeResponse(bras: String,nowDay: String,hourly: Int):Future[Seq[(Int,Int)]] = {
    BrasDAO.getInfErrorBytimeResponse(bras,nowDay,hourly)
  }

  def getInfhostResponse(bras: String,nowDay: String):Future[Seq[(String,Int,Int)]] = {
    BrasDAO.getInfhostResponse(bras,nowDay)
  }

  def getInfModuleResponse(bras: String,nowDay: String):Future[Seq[(String,String,Int,Int,Int)]] = {
    BrasDAO.getInfModuleResponse(bras,nowDay)
  }

  def getOpsviewServiceSttResponse(bras: String,nowDay: String):Future[Seq[(String,Int)]] = {
    BrasDAO.getOpsviewServiceSttResponse(bras,nowDay)
  }

  def getOpServByStatusResponse(bras: String,nowDay: String):Future[Seq[(String,String,Int)]] = {
    BrasDAO.getOpServByStatusResponse(bras,nowDay)
  }

  def getLinecardhostResponse(bras: String,nowDay: String):Future[Seq[(String,Int,Int)]] = {
    BrasDAO.getLinecardhostResponse(bras,nowDay)
  }
  def getLinecardhostCurrent(bras: String,nowDay: String): Array[(String,String)]= {
    BrasDAO.getLinecardhostCurrent(bras,nowDay)
  }

  def getErrorSeverityResponse(bras: String,nowDay: String):Future[Seq[(String,Int)]] = {
    BrasDAO.getErrorSeverityResponse(bras,nowDay)
  }

  def getErrorTypeResponse(bras: String,nowDay: String):Future[Seq[(String,Int)]] = {
    BrasDAO.getErrorTypeResponse(bras,nowDay)
  }

  def getFacilityResponse(bras: String,nowDay: String):Future[Seq[(String,Int)]] = {
    BrasDAO.getFacilityResponse(bras,nowDay)
  }

  def getDdosResponse(bras: String,nowDay: String):Future[Seq[(String,Int)]] = {
    BrasDAO.getDdosResponse(bras,nowDay)
  }

  def getSeveValueResponse(bras: String,nowDay: String):Future[Seq[(String,Int)]] = {
    BrasDAO.getSeveValueResponse(bras,nowDay)
  }

  // end page Search Bras

  def listTop100Bras: Future[Seq[(String,String,String,String,String, String, String, String,String,Option[String])]] = {
    BrasList.top100
  }

  def listNocOutlier: Future[Seq[(String,String)]] = {
    BrasesCard.listNocOutlier
  }

  def listBrasOutlier: Future[Seq[(String,String,String,String,String)]] = {
    BrasesCard.listBrasOutlier
  }

  def listBrasById(id: String): Future[Seq[(String,String,String,String,String,Int,Int)]] = {
    BrasesCard.listBrasById(id)
  }

  def getHostBras(id: String): Future[Seq[(String,String,String, String)]] = {
    BrasesCard.getHostCard(id)
  }

  def confirmLabel(id: String,time: String) = {
    BrasList.confirmLabel(id,time)
  }

  def rejectLabel(id: String,time: String) = {
    BrasList.rejectLabel(id,time)
  }

  def opViewKibana(id : String,time: String,oldTime: String): Future[Seq[(String,String,String,String)]] = {
    BrasesCard.opViewKibana(id,time,oldTime)
  }

  def getNumLogSiginById(id : String,time: String): Future[Seq[(Int,Int)]] = {
    BrasesCard.getNumLogSiginById(id,time)
  }

  def getBrasTime(id : String,time: String) : Future[Seq[Bras]] = {
    BrasList.getTime(id,time)
  }

  def getBrasChart(id : String,time: String) : Future[Seq[Bras]] = {
    BrasList.getChart(id,time)
  }

  def getJsonBrasChart(id : String,time: String) : Future[Seq[(String,Int, Int,Int)]] = {
    BrasList.getJsonChart(id,time)
  }

  def getBrasCard(id : String,time: String,sigin: String,logoff: String) : Future[Seq[(String,String,String,Int,Int)]] = {
    BrasesCard.getCard(id,time,sigin,logoff)
  }

}