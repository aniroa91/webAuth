package service

import model.device._
import services.domain.{AbstractService, CommonService}
import com.sksamuel.elastic4s.http.ElasticDsl._

import scala.concurrent.{Await, Future}
import org.elasticsearch.search.sort.SortOrder
import services.domain.CommonService.formatUTC
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval
import org.joda.time.DateTimeZone
import utils.DateTimeUtil
import device.utils.LocationUtils

import scala.concurrent.duration.Duration

object BrasService extends AbstractService{

  def getSuggestions(prefix: String, _type: String) = {
    val index = if(_type.equals("1")) "olt-name" else "bras-name"
    val field = if(_type.equals("1")) "olt_id" else "bras_id"
    val response = client.execute(
      search(s"$index") query
        matchPhrasePrefixQuery(s"$field", prefix.toUpperCase) limit(10)
    ).await
    response.hits.hits.map(x => x.sourceAsMap)
      .map(x => x.getOrElse(s"$field", "??").asInstanceOf[String])
  }

  def getSigLogByMonth(month: String) = {
    BrasDAO.getSigLogByMonth(month)
  }

  def getTopBrasOutMonthly() = {
    BrasDAO.getTopBrasOutMonthly()
  }

  def getTopConnectMonthly() = {
    BrasDAO.getTopConnectMonthly()
  }

  def getTopOltMonthly(province: String) = {
    BrasDAO.getTopOltMonthly(province)
  }

  def getTopServBrasErrMonthly() = {
    BrasDAO.getTopServBrasErrMonthly()
  }

  def getTopServOpsviewMonthly() = {
    BrasDAO.getTopServOpsviewMonthly()
  }

  def getTopServInfErrMonthly(province: String) = {
    BrasDAO.getTopServInfErrMonthly(province)
  }

  def getTopOverviewNocMonthly(col: String) = {
    BrasDAO.getTopOverviewNocMonthly(col)
  }

  def getTopInfErrMonthly(province: String) = {
    BrasDAO.getTopInfErrMonthly(province)
  }

  def getSuyhaoByMonth(month: String, province: String) = {
    BrasDAO.getSuyhaoByMonth(month, province)
  }

  def getBrasOutlierByMonth(month: String) = {
    BrasDAO.getBrasOutlierByMonth(month)
  }

  def getInfOutlierByMonth(month: String,province: String) = {
    BrasDAO.getInfOutlierByMonth(month,province)
  }

  def getDeviceByMonth(month: String,province: String) = {
    BrasDAO.getDeviceByMonth(month,province)
  }

  def getKibaOpsByMonth(month: String) = {
    BrasDAO.getKibaOpsByMonth(month)
  }

  def getInfErrorByMonth(month: String,province: String) = {
    BrasDAO.getInfErrorByMonth(month,province)
  }

  def getErrorMetric(bras: String, time: String) = {
    BrasDAO.getErrorMetric(bras, time)
  }

  def getSigninUnique(bras: String, time: String) = {
    BrasDAO.getSigninUnique(bras, time)
  }

  def getTrackingUser(bras: String, time: String) = {
    BrasDAO.getTrackingUser(bras, time)
  }

  def getMinMaxMonth(): Future[Seq[(String,String)]] = {
    BrasDAO.getMinMaxMonth()
  }

  def get3MonthLastest(): Future[Seq[(String)]] = {
    BrasDAO.get3MonthLastest()
  }

  def getHostMonitor(id: String): Future[Seq[(String,String,String)]] ={
    BrasDAO.getHostMonitor(id)
  }

  def getBrasOutlierCurrent(day: String): Future[Seq[(String,String,Int,Int,String)]] ={
    BrasDAO.getBrasOutlierCurrent(day)
  }

  def getProvinceOpsview(fromMonth: String,toMonth: String): Future[Seq[(String,String,String,Int)]]   ={
    BrasDAO.getProvinceOpsview(fromMonth,toMonth)
  }

  def getOutlierMonthly(fromMonth: String,toMonth: String,db: String, province: String) ={
    BrasDAO.getOutlierMonthly(fromMonth,toMonth,db, province)
  }

  def getProvinceContract(fromMonth: String,toMonth: String, province: String): Future[Seq[(String,String,String,Int,Int,Int)]]   ={
    BrasDAO.getProvinceContract(fromMonth,toMonth,province)
  }

  def getProvinceKibana(fromMonth: String,toMonth: String): Future[Seq[(String,String,String,Int)]]   ={
    BrasDAO.getProvinceKibana(fromMonth,toMonth)
  }

  def getProvinceSuyhao(fromMonth: String,toMonth: String, province: String): Future[Seq[(String,String,String,Int,Int)]]   ={
    BrasDAO.getProvinceSuyhao(fromMonth,toMonth, province)
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

  def getProvinceInfDownError(month: String,province: String): Future[Seq[(String,String,Int,Int,Int,Int)]]   ={
    BrasDAO.getProvinceInfDownError(month,province)
  }

  def getProvinceTotalInf(fromMonth: String,toMonth: String, province: String): Future[Seq[(String,String,Double)]]   ={
    BrasDAO.getProvinceTotalInf(fromMonth,toMonth, province)
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

  def getTopSignin(month: String): Future[Seq[(String,String,Int,Int)]]   ={
    BrasDAO.getTopSignin(month)
  }

  def getTopLogoff(month: String): Future[Seq[(String,String,Int,Int)]]   ={
    BrasDAO.getTopLogoff(month)
  }

  def topInfOut(month: String, province: String): Future[Seq[(String,String,Int)]]   ={
    BrasDAO.topInfOut(month,province)
  }

  def topBrasOut(month: String): Future[Seq[(String,String,Int)]]   ={
    BrasDAO.topBrasOut(month)
  }

  def getTopKibana(month: String,_typeError: String): Future[Seq[(String,String,Int)]]   ={
    BrasDAO.getTopKibana(month,_typeError)
  }

  def getSigLogByRegion(month: String): Future[Seq[(String,Int,Int,Int,Int)]]   ={
    BrasDAO.getSigLogByRegion(month)
  }

  def getSigLogByProvince(month: String, provinceCode: String, lastMonth: String): Future[Seq[(String,Int,Int,Int,Int)]]   ={
    BrasDAO.getSigLogByProvince(month,provinceCode,lastMonth)
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

  def getSigLogByBras(month: String, bras: String, lastMonth: String): Future[Seq[(String,Int,Int,Int,Int)]]   ={
    BrasDAO.getSigLogByBras(month,bras,lastMonth)
  }

  def getTopOpsview(month: String,_typeService: String): Future[Seq[(String,String,Int)]]   ={
    BrasDAO.getTopOpsview(month,_typeService)
  }

  def getTopInf(month: String,_typeInferr: String, province: String): Future[Seq[(String, String, String, Int)]]   ={
    BrasDAO.getTopInf(month,_typeInferr, province)
  }

  def getTopnotSuyhao(month: String, province: String): Future[Seq[(String,String,Int,Int)]]   ={
    BrasDAO.getTopnotSuyhao(month, province)
  }

  def getTopPoorconn(month: String,_typeOLTpoor: String,province: String): Future[Seq[(String,String,Int)]]   ={
    BrasDAO.getTopPoorconn(month,_typeOLTpoor,province)
  }

  def rejectLabelInf(host: String,module: String,time: String) ={
    BrasDAO.rejectLabelInf(host,module,time)
  }

  def confirmLabelInf(host: String,module: String,time: String)={
    BrasDAO.confirmLabelInf(host,module,time)
  }

  def getUserDownMudule(province: String): Future[Seq[(String,String,String,Int)]]   ={
    BrasDAO.getUserDownMudule(province)
  }

  def getSpliterMudule(province: String): Future[Seq[(String,String,String,Int)]]   ={
    BrasDAO.getSpliterMudule(province)
  }

  def getSflofiMudule(queries: String, province: String): Future[Seq[(String,String,String,Int,Int,Int,Int,Int,Int,String)]]   ={
    BrasDAO.getSflofiMudule(queries, province)
  }

  def getTotalOutlier(province: String): Future[Seq[(Int)]] = {
    BrasDAO.getTotalOutlier(province)
  }

  def getIndexRougeMudule(day: String): Array[(String,String,String,String,Int)]   ={
    BrasDAO.getIndexRougeMudule(day)
  }

  def getInfDownMudule(province: String): Future[Seq[(String,String,String,Int)]]   ={
    BrasDAO.getInfDownMudule(province)
  }

  def getSigLogByDaily(bras: String, day: String): SigLogClientsDaily = {
    try {
      val brasId = if(bras.equals("*")) bras else bras.toLowerCase
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")
      val nowDay = DateTime.parse(day +" 00:00:00", formatter).toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss"))
      val nextDay = DateTime.parse(CommonService.getNextDay(day) +" 00:00:00", formatter).toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss"))

      val mulRes = if(brasId.equals("*")) client.execute(
        multi(
          search(s"radius-streaming-*" / "docs")
            query {must(termQuery("type", "con"), termQuery("typeLog", "SignIn"), rangeQuery("timestamp").gte(CommonService.formatStringToUTC(nowDay)).lt(CommonService.formatStringToUTC(nextDay)))}
            aggregations (
            dateHistogramAggregation("hourly")
              .field("timestamp")
              .interval(DateHistogramInterval.HOUR)
              .timeZone(DateTimeZone.forID(DateTimeUtil.TIMEZONE_HCM))
              subAggregations(
                cardinalityAgg("name","name")
              )
            ),
          search(s"radius-streaming-*" / "docs")
            query {must(termQuery("type", "con"), termQuery("typeLog", "LogOff"), rangeQuery("timestamp").gte(CommonService.formatStringToUTC(nowDay)).lt(CommonService.formatStringToUTC(nextDay)))}
            aggregations (
            dateHistogramAggregation("hourly")
              .field("timestamp")
              .interval(DateHistogramInterval.HOUR)
              .timeZone(DateTimeZone.forID(DateTimeUtil.TIMEZONE_HCM))
              subAggregations(
                cardinalityAgg("name","name")
              )
            )
        )
      ).await
      else
        client.execute(
          multi(
            search(s"radius-streaming-*" / "docs")
              query {must(termQuery("type", "con"), termQuery("typeLog", "SignIn"), termQuery("nasName", brasId), rangeQuery("timestamp").gte(CommonService.formatStringToUTC(nowDay)).lt(CommonService.formatStringToUTC(nextDay)))}
              aggregations (
              dateHistogramAggregation("hourly")
                .field("timestamp")
                .interval(DateHistogramInterval.HOUR)
                .timeZone(DateTimeZone.forID(DateTimeUtil.TIMEZONE_HCM))
                subAggregations(
                cardinalityAgg("name","name")
                )
              ),
            search(s"radius-streaming-*" / "docs")
              query {must(termQuery("type", "con"), termQuery("typeLog", "LogOff"), termQuery("nasName", brasId), rangeQuery("timestamp").gte(CommonService.formatStringToUTC(nowDay)).lt(CommonService.formatStringToUTC(nextDay)))}
              aggregations (
              dateHistogramAggregation("hourly")
                .field("timestamp")
                .interval(DateHistogramInterval.HOUR)
                .timeZone(DateTimeZone.forID(DateTimeUtil.TIMEZONE_HCM))
                subAggregations(
                cardinalityAgg("name","name")
                )
              )
          )
        ).await
      val arrSigin = CommonService.getAggregationsAndCountDistinct(mulRes.responses(0).aggregations.get("hourly")).map(x => (CommonService.getHoursFromMiliseconds(x._1.toLong), x._2, x._3))
      val arrLogoff = CommonService.getAggregationsAndCountDistinct(mulRes.responses(1).aggregations.get("hourly")).map(x => (CommonService.getHoursFromMiliseconds(x._1.toLong), x._2, x._3))
      SigLogClientsDaily(arrSigin, arrLogoff)
    }
    catch{
      case e: Exception => SigLogClientsDaily(Array[(Int, Long, Long)](), Array[(Int, Long, Long)]())
    }
  }

  def getSiglogContract(host: String): Array[(String,String,String)] = {
    val dt = new DateTime()
    val currentDate = dt.toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss"))
    val formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")
    val nowDay = DateTime.parse(CommonService.getCurrentDay()+" 00:00:00", formatter).toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss"))
    val response = client.execute(
      search(s"radius-streaming-*" / "docs")
        query { must(termQuery("type", "con"), termQuery("card.olt", host),rangeQuery("timestamp").gte(CommonService.formatStringToUTC(nowDay)).lte(CommonService.formatStringToUTC(currentDate)))}
        sortBy{ fieldSort("timestamp") order SortOrder.DESC} size 1000
    ).await
    response.hits.hits.map(x=> x.sourceAsMap).map(x=> (getValueAsString(x,"name"),getValueAsString(x,"typeLog"),getValueAsString(x,"timestamp")))

  }

  /*def getKibanaDailyES(bras: String, day: String) = {
    try {
      val rs = client_kibana.execute(
        search(s"infra_dwh_kibana_2018-07-31" / "docs") query (s"bras_id.keyword:$bras")
          aggregations (
          dateHistogramAggregation("hourly")
            .field("date_time")
            .interval(DateHistogramInterval.HOUR)
            .timeZone(DateTimeZone.forID(DateTimeUtil.TIMEZONE_HCM))
          )
      ).await
      CommonService.getAggregationsSiglog(rs.aggregations.get("hourly")).map(x => (CommonService.getHoursFromMiliseconds(x._1.toLong) -> x._2.toInt))

    }
    catch {
      case e: Exception => Array[(Int, Int)]()
    }
  }

  def getOpviewDailyES(bras: String, day: String) = {
    try {
      val res = client_kibana.execute(
        search(s"infra_dwh_opsview_2018-07-31" / "docs") query (s"bras_id.keyword:$bras")
          aggregations (
          dateHistogramAggregation("hourly")
            .field("date_time")
            .interval(DateHistogramInterval.HOUR)
            .timeZone(DateTimeZone.forID(DateTimeUtil.TIMEZONE_HCM))
          ) size 1000
      ).await
      CommonService.getAggregationsKeyString(res.aggregations.get("hourly")).map(x => (CommonService.getHourFromES5(x._1), x._2))
        .groupBy(_._1).mapValues(_.map(x => x._2.toInt).sum).toArray.sorted
    }
    catch {
      case e: Exception => Array[(Int, Int)]()
    }
  }*/

  //for daily inf errors
  def getInfErrorsDaily(day: String, _type: String, province: String) = {
    val rs = Await.result(BrasDAO.getInfErrorsDaily(day, province), Duration.Inf).filter(x=> x._1 != "" && x._1.split("-").length == 4 && x._1.split("-")(0).length == 3)
    val mapErrs = _type match {
      case _type if(_type.equals("*")) => {
        rs.map(x=> (x._1, x._2,(x._3+x._4+x._5+x._6+x._7+x._8))).map(x=> (LocationUtils.getRegion(x._1.substring(0, 3)), LocationUtils.getNameProvincebyCode(x._1.substring(0, 3)), x._1, x._2, x._3)).toArray.sorted
      }
      case _type if(_type.equals("user_down")) => {
        rs.map(x=> (x._1, x._2, x._3)).map(x=> (LocationUtils.getRegion(x._1.substring(0, 3)), LocationUtils.getNameProvincebyCode(x._1.substring(0, 3)), x._1, x._2, x._3)).toArray.sorted
      }
      case _type if(_type.equals("inf_down")) => {
        rs.map(x=> (x._1, x._2, x._4)).map(x=> (LocationUtils.getRegion(x._1.substring(0, 3)), LocationUtils.getNameProvincebyCode(x._1.substring(0, 3)), x._1, x._2, x._3)).toArray.sorted
      }
      case _type if(_type.equals("sf_error")) => {
        rs.map(x=> (x._1, x._2, x._5)).map(x=> (LocationUtils.getRegion(x._1.substring(0, 3)), LocationUtils.getNameProvincebyCode(x._1.substring(0, 3)), x._1, x._2, x._3)).toArray.sorted
      }
      case _type if(_type.equals("lofi_error")) => {
        rs.map(x=> (x._1, x._2, x._6)).map(x=> (LocationUtils.getRegion(x._1.substring(0, 3)), LocationUtils.getNameProvincebyCode(x._1.substring(0, 3)), x._1, x._2, x._3)).toArray.sorted
      }
      case _type if(_type.equals("rouge_error")) => {
        rs.map(x=> (x._1, x._2, x._7)).map(x=> (LocationUtils.getRegion(x._1.substring(0, 3)), LocationUtils.getNameProvincebyCode(x._1.substring(0, 3)), x._1, x._2, x._3)).toArray.sorted
      }
      case _type if(_type.equals("lost_signal")) => {
        rs.map(x=> (x._1, x._2, x._8)).map(x=> (LocationUtils.getRegion(x._1.substring(0, 3)), LocationUtils.getNameProvincebyCode(x._1.substring(0, 3)), x._1, x._2, x._3)).toArray.sorted
      }
    }
    mapErrs
  }

  //for daily notice service
  def getServiceNoticeRegionDaily(day: String, _type: String) ={
    val rs = Await.result(BrasDAO.getServiceNoticeDaily(day), Duration.Inf).filter(x=> x._1 != "" && x._1.split("-").length == 4 && x._1.split("-")(0).length == 3)
    val mapRs = _type match {
      case _type if(_type.equals("*")) => {
         rs.map(x=> x._1 -> (x._2+x._3+x._4+x._5)).map(x=> (LocationUtils.getRegion(x._1.substring(0, 3)), LocationUtils.getNameProvincebyCode(x._1.substring(0, 3)), x._1, x._2)).toArray.sorted
      }
      case _type if(_type.equals("warn")) => {
        rs.map(x=> x._1 -> x._2).map(x=> (LocationUtils.getRegion(x._1.substring(0, 3)), LocationUtils.getNameProvincebyCode(x._1.substring(0, 3)), x._1, x._2)).toArray.sorted
      }
      case _type if(_type.equals("unknown")) => {
        rs.map(x=> x._1 -> x._3).map(x=> (LocationUtils.getRegion(x._1.substring(0, 3)), LocationUtils.getNameProvincebyCode(x._1.substring(0, 3)), x._1, x._2)).toArray.sorted
      }
      case _type if(_type.equals("ok")) => {
        rs.map(x=> x._1 -> x._4).map(x=> (LocationUtils.getRegion(x._1.substring(0, 3)), LocationUtils.getNameProvincebyCode(x._1.substring(0, 3)), x._1, x._2)).toArray.sorted
      }
      case _type if(_type.equals("crit")) => {
        rs.map(x=> x._1 -> x._5).map(x=> (LocationUtils.getRegion(x._1.substring(0, 3)), LocationUtils.getNameProvincebyCode(x._1.substring(0, 3)), x._1, x._2)).toArray.sorted
      }
    }
    mapRs
  }

  def getInfAccessOutlierDaily(day: String, province: String) = {
    BrasDAO.getInfAccessOutlierDaily(day, province)
  }

  def getTicketIssue(day: String, province: String) = {
    BrasDAO.getTicketIssue(day, province)
  }

  def getBrasOutlierDaily(day: String) = {
    BrasDAO.getBrasOutlierDaily(day)
  }

  def getErrorHostdaily(id: String,day: String, province: String) = {
    BrasDAO.getErrorHostdaily(id, day, province)
  }

  // for daily device errors
  def getDeviceErrorsRegionDaily(day: String): Array[(String, Double, Double,Double, Double,Double,Double)] ={
    val rs = Await.result(BrasDAO.getDeviceErrorsDaily(day), Duration.Inf).filter(x=> x._1 != "" && x._1.split("-").length == 4 && x._1.split("-")(0).length == 3)
    val mapRegion = rs.map(x=> (LocationUtils.getRegion(x._1.substring(0,3)), x._2, x._3, x._4, x._5, x._6, x._7))
      .groupBy(x=> x._1).map(x=> (x._1, x._2.map(x=>x._2).sum, x._2.map(x=>x._3).sum, x._2.map(x=>x._4).sum, x._2.map(x=>x._5).sum, x._2.map(x=>x._6).sum, x._2.map(x=>x._7).sum)).toArray.sorted
    mapRegion
  }

  def getDeviceErrorsProvinceDaily(day: String, regionCode: String): Array[(String, Double, Double,Double, Double,Double,Double)] ={
    val rs = Await.result(BrasDAO.getDeviceErrorsDaily(day), Duration.Inf).filter(x=> x._1 != "" && x._1.split("-").length == 4 && x._1.split("-")(0).length == 3)
    val mapProvince = rs.map(x=> (LocationUtils.getRegion(x._1.substring(0,3)),x._1, x._2, x._3, x._4, x._5, x._6, x._7)).filter(x=> x._1 == regionCode)
        .map(x=> (LocationUtils.getNameProvincebyCode(x._2.substring(0,3)), x._3, x._4, x._5, x._6, x._7,x._8))
      .groupBy(x=> x._1).map(x=> (x._1, x._2.map(x=>x._2).sum, x._2.map(x=>x._3).sum, x._2.map(x=>x._4).sum, x._2.map(x=>x._5).sum, x._2.map(x=>x._6).sum, x._2.map(x=>x._7).sum)).toArray.sorted
    mapProvince
  }

  def getDeviceErrorsBrasDaily(day: String, provinceCode: String): Array[(String, Double, Double,Double, Double,Double,Double)] ={
    val rs = Await.result(BrasDAO.getDeviceErrorsDaily(day), Duration.Inf).filter(x=> x._1 != "" && x._1.split("-").length == 4 && x._1.split("-")(0).length == 3)
    val mapBras = rs.map(x=> (LocationUtils.getNameProvincebyCode(x._1.substring(0,3)),x._1,x._2, x._3, x._4, x._5, x._6, x._7)).filter(x=> x._1 == provinceCode)
        .map(x=> (x._2,x._3,x._4,x._5,x._6,x._7,x._8)).toArray
      .groupBy(x=> x._1).map(x=> (x._1, x._2.map(x=>x._2).sum, x._2.map(x=>x._3).sum, x._2.map(x=>x._4).sum, x._2.map(x=>x._5).sum, x._2.map(x=>x._6).sum, x._2.map(x=>x._7).sum)).toArray.sorted
    mapBras
  }

  // for daily siglog
  def getSigLogdaily(day: String, queries: String) = {
    try {
      val aggField = if (queries.equals("")) "nasName" else "card.olt"
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")
      val nowDay = DateTime.parse(day +" 00:00:00", formatter).toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss"))
      val nextDay = DateTime.parse(CommonService.getNextDay(day) +" 00:00:00", formatter).toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss"))

      val multiRs = if(queries.equals("")) client.execute(
        multi(
          search(s"radius-streaming-*" / "docs")
            query {must(termQuery("type", "con"), termQuery("typeLog", "LogOff"), rangeQuery("timestamp").gte(CommonService.formatStringToUTC(nowDay)).lt(CommonService.formatStringToUTC(nextDay)))}
            aggregations (
            termsAggregation("bras")
              .field(s"$aggField") size 1000
              subAggregations (
              cardinalityAgg("name", "name")
              )
            ),
          search(s"radius-streaming-*" / "docs")
            query {must(termQuery("type", "con"), termQuery("typeLog", "SignIn"), rangeQuery("timestamp").gte(CommonService.formatStringToUTC(nowDay)).lt(CommonService.formatStringToUTC(nextDay)))}
            aggregations (
            termsAggregation("bras")
              .field(s"$aggField") size 1000
              subAggregations (
              cardinalityAgg("name", "name")
              )
            )
        )
      ).await
      else
        client.execute(
          multi(
            search(s"radius-streaming-*" / "docs")
              query {must(termQuery("type", "con"), termQuery("typeLog", "LogOff"), termQuery("nasName", queries), rangeQuery("timestamp").gte(CommonService.formatStringToUTC(nowDay)).lt(CommonService.formatStringToUTC(nextDay)))}
              aggregations (
              termsAggregation("bras")
                .field(s"$aggField") size 1000
                subAggregations (
                cardinalityAgg("name", "name")
                )
              ),
            search(s"radius-streaming-*" / "docs")
              query {must(termQuery("type", "con"), termQuery("typeLog", "SignIn"), termQuery("nasName", queries), rangeQuery("timestamp").gte(CommonService.formatStringToUTC(nowDay)).lt(CommonService.formatStringToUTC(nextDay)))}
              aggregations (
              termsAggregation("bras")
                .field(s"$aggField") size 1000
                subAggregations (
                cardinalityAgg("name", "name")
                )
              )
          )
        ).await
      val rsLogoff = CommonService.getAggregationsAndCountDistinct(multiRs.responses(0).aggregations.get("bras"))
      val rsSignin = CommonService.getAggregationsAndCountDistinct(multiRs.responses(1).aggregations.get("bras"))
      (rsLogoff, rsSignin)
    }
    catch {
      case e: Exception => (Array[(String,Long, Long)](), Array[(String,Long, Long)]())
    }
  }

  def getSigLogRegionDaily(day: String, queries: String) = {
    val rs = getSigLogdaily(day, queries)
    val logoff = rs._1.filter(x=> x._1.indexOf("-")>=0).filter(x=> x._1.split("-").length == 4 && x._1.split("-")(0).length == 3).map(x=> (LocationUtils.getRegion(x._1.substring(0,3).toUpperCase) , x._2, x._3))
      .groupBy(x=> x._1).map(x=> (x._1, x._2.map(x=> x._2).sum, x._2.map(x=> x._3).sum)).toArray.sorted
    val signin = rs._2.filter(x=> x._1.indexOf("-")>=0).filter(x=> x._1.split("-").length == 4 && x._1.split("-")(0).length == 3).map(x=> (LocationUtils.getRegion(x._1.substring(0,3).toUpperCase) , x._2, x._3))
      .groupBy(x=> x._1).map(x=> (x._1, x._2.map(x=> x._2).sum, x._2.map(x=> x._3).sum)).map(x=> (x._1, -x._2, x._3)).toArray.sorted
    (logoff, signin)
  }

  def getSigLogProvinceDaily(day: String, regionCode: String) = {
    val rs = getSigLogdaily(day, "")
    val logoff = rs._1.filter(x=> x._1.indexOf("-")>=0).filter(x=> x._1.split("-").length == 4 && x._1.split("-")(0).length == 3).map(x=> (LocationUtils.getRegion(x._1.substring(0,3).toUpperCase), x._1 , x._2, x._3)).filter(x=> x._1 == regionCode ).map(x=> (LocationUtils.getNameProvincebyCode(x._2.substring(0,3).toUpperCase), x._3, x._4))
        .groupBy(x=> x._1).map(x=> (x._1, x._2.map(x=> x._2).sum, x._2.map(x=> x._3).sum)).toArray.sorted
    val signin = rs._2.filter(x=> x._1.indexOf("-")>=0).filter(x=> x._1.split("-").length == 4 && x._1.split("-")(0).length == 3).map(x=> (LocationUtils.getRegion(x._1.substring(0,3).toUpperCase) ,x._1, x._2, x._3)).filter(x=> x._1 == regionCode ).map(x=> (LocationUtils.getNameProvincebyCode(x._2.substring(0,3).toUpperCase), x._3, x._4))
        .groupBy(x=> x._1).map(x=> (x._1, x._2.map(x=> x._2).sum, x._2.map(x=> x._3).sum)).map(x=> (x._1, -x._2, x._3)).toArray.sorted
    (logoff, signin)
  }

  def getSigLogBrasDaily(day: String, provinceCode: String) = {
    val rs = getSigLogdaily(day, "")
    val logoff = rs._1.filter(x=> x._1.indexOf("-")>=0).filter(x=> x._1.split("-").length == 4 && x._1.split("-")(0).length == 3).map(x=> ( x._1 , x._2, x._3)).filter(x=> x._1.substring(0,3) == provinceCode.toLowerCase() ).map(x=> (x._1, x._2, x._3))
      .groupBy(x=> x._1).map(x=> (x._1, x._2.map(x=> x._2).sum, x._2.map(x=> x._3).sum)).toArray.sorted
    val signin = rs._2.filter(x=> x._1.indexOf("-")>=0).filter(x=> x._1.split("-").length == 4 && x._1.split("-")(0).length == 3).map(x=> ( x._1, x._2, x._3)).filter(x=> x._1.substring(0,3) == provinceCode.toLowerCase() ).map(x=> (x._1, x._2, x._3))
      .groupBy(x=> x._1).map(x=> (x._1, x._2.map(x=> x._2).sum, x._2.map(x=> x._3).sum)).map(x=> (x._1, -x._2, x._3)).toArray.sorted
    (logoff, signin)
  }

  def getSigLogHostDaily(day: String, queries: String) = {
    val rs = getSigLogdaily(day, queries)
    val logoff = rs._1.map(x=> (x._1, x._2, x._3)).groupBy(x=> x._1).map(x=> (x._1, x._2.map(x=> x._2).sum, x._2.map(x=> x._3).sum))
    val signin = rs._2.map(x=> (x._1 , x._2, x._3)).groupBy(x=> x._1).map(x=> (x._1, x._2.map(x=> x._2).sum, x._2.map(x=> x._3).sum))
    val top = signin.toArray.sortWith((x, y) => x._2> y._2).filter(x=> x._1 != "").filter(x=> x._1 != "N/A").map(x=> x._1)
    val topHost = logoff.filter(x=> top.indexOf(x._1)>=0).map(x=> x._1).slice(0,10)

    //topHost.foreach(println)
    val topLogoff = topHost.map(x=> (x, logoff.filter(y=> y._1 == x).map(y=> (y._2, y._3)).toArray))
      .flatMap(x=> x._2.map(y=> x._1 -> y)).map(x=> (x._1, x._2._1, x._2._2))
    val topSignin = topHost.map(x=> (x, signin.filter(y=> y._1 == x).map(y=> (-y._2, y._3)).toArray))
      .flatMap(x=> x._2.map(y=> x._1 -> y)).map(x=> (x._1, x._2._1, x._2._2))
    (topLogoff, topSignin)
  }

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
          ) size 100
    ).await
    val mapHeat = CommonService.getSecondAggregations(response.aggregations.get("linecard"),"card")
    mapHeat.flatMap(x => x._2.map(y => x._1 -> y))
      .map(x => (x._1 -> x._2._1) -> x._2._2)/*.filter(x=> x._1._1 != "-1").filter(x=> x._1._2 != "-1")*/
  }

  def getErrorHistory(id: String, province: String): Future[Seq[(String,Int)]] = {
    BrasDAO.getErrorHistory(id, province)
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

  def getSiglogClients(bras: String,nowDay: String): (Int,Int) = {
    BrasDAO.getSiglogClients(bras,nowDay)
  }

  def getNoOutlierResponse(bras: String,nowDay: String):Future[Seq[(Int)]] = {
    BrasDAO.getNoOutlierResponse(bras,nowDay)
  }
  def getNoOutlierCurrent(bras: String,nowDay: String): Int = {
    BrasDAO.getNoOutlierCurrent(bras,nowDay)
  }

  def getOpviewBytimeResponse(bras: String,nowDay: String,hourly: Int): Array[(Int,Int)] = {
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

  def getInfErrorBytimeResponse(bras: String,nowDay: String,hourly: Int): Array[(Int,Int, Int,Int, Int,Int,Int,Int)] = {
    BrasDAO.getInfErrorBytimeResponse(bras,nowDay,hourly)
  }

  def getSankeyService(bras: String,nowDay: String) = {
    BrasDAO.getSankeyService(bras,nowDay)
  }

  def getDeviceServStatus(bras: String, day: String) = {
    BrasDAO.getDeviceServStatus(bras,day)
  }

  def getServiceNameStt(bras: String,nowDay: String) = {
    BrasDAO.getServiceNameStt(bras,nowDay)
  }

  def getInfModuleResponse(bras: String,nowDay: String): Array[(String,String,Long,Long,Long)] = {
    BrasDAO.getInfModuleResponse(bras,nowDay)
  }

  def getTicketOutlierByBrasId(bras: String,nowDay: String): Future[Seq[(String,String)]] = {
    BrasDAO.getTicketOutlierByBrasId(bras,nowDay)
  }

  def getOpsviewServiceSttResponse(bras: String,nowDay: String):Future[Seq[(String,Int)]] = {
    BrasDAO.getOpsviewServiceSttResponse(bras,nowDay)
  }

  def getOpServByStatusResponse(bras: String,nowDay: String): Array[(String,String,Int)] = {
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
  def getSeveValueES(bras: String,nowDay: String):  Array[(String,String,String,Long)] = {
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

  def getHostBras(id: String): Future[Seq[(String,String,Int, Int,Int, Int,Int, Int,Int, Int,Int)]] = {
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

  def getDeviceSwitch(id : String,time: String,oldTime: String) = {
    BrasesCard.getDeviceSwitch(id,time,oldTime)
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