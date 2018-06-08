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
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval
import org.joda.time.DateTimeZone
import com.ftel.bigdata.utils.DateTimeUtil

object BrasService extends AbstractService{

  /*def getBrasOutlierCurrent(day: String):Array[BrasOutlier]  ={
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
  }*/
  def getBrasOutlierCurrent(day: String): Future[Seq[(String,String,Int,Int,String)]] ={
    BrasDAO.getBrasOutlierCurrent(day)
  }

  def getProvinceOpsview(fromMonth: String,toMonth: String): Future[Seq[(String,String,String,Int)]]   ={
    BrasDAO.getProvinceOpsview(fromMonth,toMonth)
  }

  def getProvinceContract(fromMonth: String,toMonth: String): Future[Seq[(String,String,String,Int,Int,Int)]]   ={
    BrasDAO.getProvinceContract(fromMonth,toMonth)
  }

  def getProvinceKibana(fromMonth: String,toMonth: String): Future[Seq[(String,String,String,Int)]]   ={
    BrasDAO.getProvinceKibana(fromMonth,toMonth)
  }

  def getProvinceSuyhao(fromMonth: String,toMonth: String): Future[Seq[(String,String,String,Int)]]   ={
    BrasDAO.getProvinceSuyhao(fromMonth,toMonth)
  }

  def getProvinceCount(month: String): Future[Seq[(String,String,Int,Int,Int,Int,Int,Int)]]   ={
    BrasDAO.getProvinceCount(month)
  }

  def getProvinceOpsviewType(month: String): Future[Seq[(String,Int,Int,Int,Int)]]   ={
    BrasDAO.getProvinceOpsviewType(month)
  }

  def getBrasOpsviewType(month: String,id: String): Future[Seq[(String,Int,Int,Int,Int)]]   ={
    BrasDAO.getBrasOpsviewType(month,id)
  }

  def getProvinceInfDownError(month: String): Future[Seq[(String,String,Int,Int,Int,Int)]]   ={
    BrasDAO.getProvinceInfDownError(month)
  }

  def getProvinceTotalInf(fromMonth: String,toMonth: String): Future[Seq[(String,String,Double)]]   ={
    BrasDAO.getProvinceTotalInf(fromMonth,toMonth)
  }

  def getProvinceSigLogoff(): Future[Seq[(String,String,Double,Double)]]   ={
    BrasDAO.getProvinceSigLogoff()
  }

  def getTotalInfbyProvince(month: String,id: String,lstBrasId: Array[(String)]): Future[Seq[(String,String,Double)]]   ={
    BrasDAO.getTotalInfbyProvince(month,id,lstBrasId)
  }

  def getSigLogconnbyProvince(id: String): Future[Seq[(String,String,Double,Double)]]   ={
    BrasDAO.getSigLogconnbyProvince(id)
  }

  def getTotalInfbyBras(month: String,id: String,lstHost: Array[(String)]): Future[Seq[(String,String,Double)]]   ={
    BrasDAO.getTotalInfbyBras(month,id,lstHost)
  }

  def getSigLogconnbyBras(id: String): Future[Seq[(String,String,Double,Double)]]   ={
    BrasDAO.getSigLogconnbyBras(id)
  }

  def getTopSignin(month: String): Future[Seq[(String,String,Int)]]   ={
    BrasDAO.getTopSignin(month)
  }

  def getTopLogoff(month: String): Future[Seq[(String,String,Int)]]   ={
    BrasDAO.getTopLogoff(month)
  }

  def getTopKibana(month: String,_typeError: String): Future[Seq[(String,Int)]]   ={
    BrasDAO.getTopKibana(month,_typeError)
  }

  def getSigLogByRegion(month: String): Future[Seq[(String,Int,Int)]]   ={
    BrasDAO.getSigLogByRegion(month)
  }

  def getSigLogByProvince(month: String, provinceCode: String): Future[Seq[(String,Int,Int)]]   ={
    BrasDAO.getSigLogByProvince(month,provinceCode)
  }

  def getDistinctBrasbyProvince(month: String, provinceCode: String): Future[Seq[(String)]]   ={
    BrasDAO.getDistinctBrasbyProvince(month,provinceCode)
  }

  def getDistinctHostbyBras(month: String, bras: String): Future[Seq[(String)]]   ={
    BrasDAO.getDistinctHostbyBras(month,bras)
  }

  def getTop10HostId(month: String,lstHost: Array[(String)]): Future[Seq[(String)]]   ={
    BrasDAO.getTop10HostId(month,lstHost)
  }

  def getSigLogByBras(month: String, bras: String): Future[Seq[(String,Int,Int)]]   ={
    BrasDAO.getSigLogByBras(month,bras)
  }

  def getTopOpsview(month: String,_typeService: String): Future[Seq[(String,Int)]]   ={
    BrasDAO.getTopOpsview(month,_typeService)
  }

  def getTopInf(month: String,_typeInferr: String): Future[Seq[(String,String,Int)]]   ={
    BrasDAO.getTopInf(month,_typeInferr)
  }

  def getTopnotSuyhao(month: String): Future[Seq[(String,Int)]]   ={
    BrasDAO.getTopnotSuyhao(month)
  }

  def getTopPoorconn(month: String,_typeOLTpoor: String): Future[Seq[(String,Int)]]   ={
    BrasDAO.getTopPoorconn(month,_typeOLTpoor)
  }

  def rejectLabelInf(host: String,module: String,time: String,bras: String) ={
    BrasDAO.rejectLabelInf(host,module,time,bras)
  }

  def confirmLabelInf(host: String,module: String,time: String,bras: String)={
    BrasDAO.confirmLabelInf(host,module,time,bras)
  }

  def getUserDownMudule(day: String): Future[Seq[(String,String,String,Int)]]   ={
    BrasDAO.getUserDownMudule(day)
  }

  def getSpliterMudule(day: String): Future[Seq[(String,String,String,Int)]]   ={
    BrasDAO.getSpliterMudule(day)
  }

  def getSflofiMudule(queries: String): Future[Seq[(String,String,String,Int,Int,Boolean,String)]]   ={
    BrasDAO.getSflofiMudule(queries)
  }

  def getIndexRougeMudule(day: String): Future[Seq[(String,String,String,String,Int)]]   ={
    BrasDAO.getIndexRougeMudule(day)
  }

  def getInfDownMudule(day: String): Future[Seq[(String,String,String,Int)]]   ={
    BrasDAO.getInfDownMudule(day)
  }

  /*def getBrasOutlierJson(day: String): Array[(String,String,Int,Int)] ={
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
  }*/

  def getUserLogOff(bras: String,time: String,typeLog: String): Array[(String,String,String)] = {
    val formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")
    val dateTime = DateTime.parse(time, formatter)
    val nextMinute  = dateTime.plusMinutes(1).toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss"))
    val response = client.execute(
      search(s"radius-streaming-*" / "docs")
        query { must(termQuery("type", "con"), termQuery("nasName", bras.toLowerCase),not(termQuery("card.olt", "")),not(termQuery("card.olt", "N/A")),not(termQuery("card.indexId", -1)),not(termQuery("card.ontId", -1)),termQuery("typeLog", typeLog),rangeQuery("timestamp").gte(CommonService.formatStringToUTC(time)).lt(CommonService.formatStringToUTC(nextMinute)))} size 100
        sortBy { fieldSort("timestamp") order SortOrder.DESC }
    ).await
    val jsonRs = response.hits.hits.map(x=> x.sourceAsMap)
      .map(x=>(
        getValueAsString(x,"name"),
        formatUTC(getValueAsString(x,"timestamp")),
        getValueAsString(x,"card.olt")+"_"+getValueAsInt(x,"cable.ontId").toString+"_"+getValueAsInt(x,"cable.indexId").toString
      ))
    jsonRs
  }

  def getJsonBrasCard(bras: String,time: String,_type: String): Array[((String,String),Long)] = {
    val formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")
    val dateTime = DateTime.parse(time, formatter)
    val oldHalfHour  = dateTime.minusMinutes(1).toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss"))
    val afterHalfHour  = dateTime.plusMinutes(1).toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss"))
    val response = client.execute(
        search(s"radius-streaming-*" / "docs")
          query { must(termQuery("type", "con"), termQuery("nasName", bras.toLowerCase),termQuery("typeLog", _type),rangeQuery("timestamp").gte(CommonService.formatStringToUTC(time)).lt(CommonService.formatStringToUTC(afterHalfHour)))}
          aggregations (
          termsAggregation("linecard")
            .field("card.lineId")
            .subAggregations(
              termsAggregation("card")
                .field("card.id")
            )
          ) size 0
    ).await
    val mapHeat = CommonService.getSecondAggregations(response.aggregations.get("linecard"),"card")
    mapHeat.flatMap(x => x._2.map(y => x._1 -> y))
      .map(x => (x._1 -> x._2._1) -> x._2._2)/*.filter(x=> x._1._1 != "-1").filter(x=> x._1._2 != "-1")*/
  }

  def getSigLogInfjson(id: String): (Array[(String, Int)], Array[(String, Int)]) = {
    //module: cable.ontId
    // host: card.olt
    val time = id.split("/")(0)
    val host = id.split("/")(1)
    val module = id.split("/")(2)
    val formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS")
    val dateTime = DateTime.parse(time, formatter)
    val oldHalfHour  = dateTime.minusMinutes(30).toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss"))
    val afterHalfHour  = dateTime.plusMinutes(30).toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss"))
    val mulRes = client.execute(
      multi(
        search(s"radius-streaming-*" / "docs")
          query { must(termQuery("type","con"),termQuery("card.olt",host),termQuery("typeLog","SignIn"),termQuery("cable.ontId",module),rangeQuery("timestamp").gte(CommonService.formatStringToUTC(oldHalfHour)).lte(CommonService.formatStringToUTC(afterHalfHour))) } size 100
          aggregations(
          dateHistogramAggregation("minutes")
            .field("timestamp")
            .interval(new DateHistogramInterval("5m"))
            .timeZone(DateTimeZone.forID(DateTimeUtil.TIMEZONE_HCM))
          )
          sortBy { fieldSort("timestamp") order SortOrder.DESC },
        search(s"radius-streaming-*" / "docs")
          query { must(termQuery("type","con"),termQuery("card.olt",host),termQuery("typeLog","LogOff"),termQuery("cable.ontId",module),rangeQuery("timestamp").gte(CommonService.formatStringToUTC(oldHalfHour)).lte(CommonService.formatStringToUTC(afterHalfHour))) } size 100
          aggregations(
          dateHistogramAggregation("minutes")
            .field("timestamp")
            .interval(new DateHistogramInterval("5m"))
            .timeZone(DateTimeZone.forID(DateTimeUtil.TIMEZONE_HCM))
          )
          sortBy { fieldSort("timestamp") order SortOrder.DESC }
      )
    ).await
    val arrSigin = CommonService.getAggregationsKeyString(mulRes.responses(0).aggregations.get("minutes"))
    val arrLogoff = CommonService.getAggregationsKeyString(mulRes.responses(1).aggregations.get("minutes"))
    val timesKey = (arrSigin++arrLogoff).groupBy(_._1).map(x=> x._1).toArray.sorted
    val rsSigin = timesKey.map(x=> x->CommonService.getLongValueByKey(arrSigin,x)).map(x=>CommonService.formatUTC(x._1)->x._2)
    val rsLog = timesKey.map(x=> x->CommonService.getLongValueByKey(arrLogoff,x)).map(x=>CommonService.formatUTC(x._1)->x._2)
    (rsSigin,rsLog)
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
  def getKibanaBytimeES(bras: String,day: String): Array[(Int,Int)] ={
    BrasDAO.getKibanaBytimeES(bras,day)
  }

  def getSigLogBytimeResponse(bras: String,nowDay: String,hourly: Int):Future[Seq[(Int,Int,Int)]] = {
    BrasDAO.getSigLogBytimeResponse(bras,nowDay,hourly)
  }
  def getSigLogBytimeCurrent(bras: String,nowDay: String): SigLogByTime = {
    BrasDAO.getSigLogBytimeCurrent(bras,nowDay)
  }

  def getInfErrorBytimeResponse(bras: String,nowDay: String,hourly: Int): Array[(Int,Int)] = {
    BrasDAO.getInfErrorBytimeResponse(bras,nowDay,hourly)
  }

  def getInfhostResponse(bras: String,nowDay: String):Array[(String,Long,Long)] = {
    BrasDAO.getInfhostResponse(bras,nowDay)
  }

  def getInfModuleResponse(bras: String,nowDay: String): Array[(String,String,Long,Long,Long)] = {
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
  def getErrorSeverityES(bras: String,nowDay: String): Array[(String,Long)] = {
    BrasDAO.getErrorSeverityES(bras,nowDay)
  }

  def getErrorTypeResponse(bras: String,nowDay: String):Future[Seq[(String,Int)]] = {
    BrasDAO.getErrorTypeResponse(bras,nowDay)
  }
  def getErrorTypeES(bras: String,nowDay: String): Array[(String,Long)] = {
    BrasDAO.getErrorTypeES(bras,nowDay)
  }

  def getFacilityResponse(bras: String,nowDay: String):Future[Seq[(String,Int)]] = {
    BrasDAO.getFacilityResponse(bras,nowDay)
  }
  def getFacilityES(bras: String,nowDay: String): Array[(String,Long)] = {
    BrasDAO.getFacilityES(bras,nowDay)
  }

  def getDdosResponse(bras: String,nowDay: String):Future[Seq[(String,Int)]] = {
    BrasDAO.getDdosResponse(bras,nowDay)
  }
  def getDdosES(bras: String,nowDay: String): Array[(String,Long)] = {
    BrasDAO.getDdosES(bras,nowDay)
  }

  def getSeveValueResponse(bras: String,nowDay: String):Future[Seq[(String,String,Int)]] = {
    BrasDAO.getSeveValueResponse(bras,nowDay)
  }
  def getSeveValueES(bras: String,nowDay: String):  Array[((String,String),Long)] = {
    BrasDAO.getSeveValueES(bras,nowDay)
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

  def getHostBras(id: String): Future[Seq[(String,String,Int, Int,Int, Int,Int)]] = {
    BrasesCard.getHostCard(id)
  }

  def confirmLabel(id: String,time: String) = {
    BrasList.confirmLabel(id,time)
  }

  def rejectLabel(id: String,time: String) = {
    BrasList.rejectLabel(id,time)
  }

  def getOpsview(id : String,time: String,oldTime: String): Future[Seq[(String,String)]] = {
    BrasesCard.getOpsview(id,time,oldTime)
  }

  def getKibana(id : String,time: String,oldTime: String): Future[Seq[(String,String)]] = {
    BrasesCard.getKibana(id,time,oldTime)
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