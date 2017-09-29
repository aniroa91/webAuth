package services.paytv

import com.sksamuel.elastic4s.http.ElasticDsl._
import services.Configure
import com.ftel.bigdata.utils.ESUtil
import com.sksamuel.elastic4s.searches.SearchDefinition
import services.ElasticUtil
import services.domain.AbstractService
import model.paytv.InternetContract
import com.ftel.bigdata.utils.DateTimeUtil
import model.paytv.PayTVContract
import model.paytv.Response
import model.paytv.PayTVSegment
import model.paytv.PayTVVector
import model.paytv.InternetSegment
import services.Bucket
import utils.Session
import model.paytv.Bill

object ProfileService extends AbstractService {
  //val client = Configure.client
  val SIZE_DEFAULT = 100

  private def getHourly(to: String, boxId: String): SearchDefinition = {
    search(s"paytv-weekly-hourly-${to}" / "docs") query { must(termQuery("customer", boxId)) } aggregations (
      termsAggregation("top")
      .field("hour")
      .subaggs(
        sumAgg("sum", "value")) size SIZE_DEFAULT)
  }

  private def getApp(to: String, boxId: String): SearchDefinition = {
    search(s"paytv-weekly-daily-${to}" / "docs") query { must(termQuery("customer", boxId)) } aggregations (
      termsAggregation("top")
      .field("app")
      .subaggs(
        sumAgg("sum", "value")) size SIZE_DEFAULT)
  }

  private def getDayOfWeek(to: String, boxId: String): SearchDefinition = {
    search(s"paytv-weekly-daily-${to}" / "docs") query { must(termQuery("customer", boxId)) } aggregations (
      termsAggregation("top")
      .field("dayOfWeek")
      .subaggs(
        sumAgg("sum", "value")) size SIZE_DEFAULT)
  }

  private def getIPTV(to: String, boxId: String): SearchDefinition = {
    search(s"paytv-weekly-iptv-${to}" / "docs") query { must(termQuery("customer", boxId)) } aggregations (
      termsAggregation("top")
      .field("cate")
      .subaggs(
        sumAgg("sum", "value")) size SIZE_DEFAULT)
  }

  private def getAppHourly(to: String, boxId: String): SearchDefinition = {
    search(s"paytv-weekly-hourly-${to}" / "docs") query { must(termQuery("customer", boxId)) } aggregations (
      termsAggregation("top").field("app")
        .subaggs(termsAggregation("sub").field("hour")
            .subaggs(sumAgg("sum", "value"))) size SIZE_DEFAULT)
  }

  private def getAppDayOfWeek(to: String, boxId: String): SearchDefinition = {
    search(s"paytv-weekly-daily-${to}" / "docs") query { must(termQuery("customer", boxId)) } aggregations (
      termsAggregation("top")
      .field("app")
      .subaggs(
        termsAggregation("sub").field("dayOfWeek").subaggs(
          sumAgg("sum", "value"))) size SIZE_DEFAULT)

  }

//  private def getVOD(to: String, boxId: String): SearchDefinition = {
//    search(s"vod_cate" / "docs") query { must(termQuery("customer", boxId)) } aggregations (
//      termsAggregation("top")
//      .field("cate")
//      .subaggs(
//        sumAgg("sum", "value")) size SIZE_DEFAULT)
//  }
//  
//  private def getVODthieunhi(to: String, boxId: String): SearchDefinition = {
//    search(s"vod_thieu" / "docs") query { must(termQuery("customer", boxId)) } aggregations (
//      termsAggregation("top")
//      .field("cate")
//      .subaggs(
//        sumAgg("sum", "value")) size SIZE_DEFAULT)
//  }
//  
//  private def getVODgiaitri(to: String, boxId: String): SearchDefinition = {
//    search(s"vod_giaitri" / "docs") query { must(termQuery("customer", boxId)) } aggregations (
//      termsAggregation("top")
//      .field("cate")
//      .subaggs(
//        sumAgg("sum", "value")) size SIZE_DEFAULT)
//  }
  
  private def getDaily(month: Int, boxId: String): SearchDefinition = {
    search(s"paytv-weekly-daily-*" / "docs") query { must(termQuery("customer", boxId), termQuery("month", month)) } aggregations (
      termsAggregation("top")
      .field("day")
      .subaggs(
        sumAgg("sum", "value")) size SIZE_DEFAULT)
  }

  def get(contract: String): Response = {
    val internetRes = ESUtil.get(client, "internet-contract", "docs", contract)
    val payTVRes = ESUtil.get(client, "paytv-contract", "docs", contract)
    val segmentsVectorInfo = if (payTVRes.exists) {
      val size = payTVRes.source.getOrElse("box_count", "0").toString().toInt
      val boxRes = client.execute(search(s"paytv-box" / "docs") query { must(termQuery("contract.keyword", contract)) } limit 10).await
      //println(client.show(search(s"paytv-box" / "docs") limit 10))
      //println(boxRes.size)
      //boxRes.hits.hits.map(x => x.sourceAsMap).foreach(println)
      val boxids = boxRes.hits.hits.map(x => x.id)
      val segments = boxids.map(x => ESUtil.get(client, "segment-paytv", "docs", x))
        .map(x => x.id -> x.source)
        .map(x => x._1 -> PayTVSegment(
            getValueAsString(x._2, "cluster_app"),
            getValueAsString(x._2, "cluster_hourly"),
            getValueAsString(x._2, "cluster_daily"),
            getValueAsString(x._2, "cluster_lifeoemd"),
            getValueAsString(x._2, "cluster_sum"),
            getValueAsString(x._2, "cluster_iptv"),
            getValueAsString(x._2, "cluster_vod"),
            getValueAsString(x._2, "cluster_vod_giaitri"),
            getValueAsString(x._2, "cluster_vod_thieunhi")
            )).toMap
//      println(segments.size)
      val to = "2017-09-25"
      val vectors = boxids.map(x => {
        
        val hourly = getHourly(to, x)
        val app = getApp(to, x)
        val dayOfWeek = getDayOfWeek(to, x)
        val iptv = getIPTV(to, x)
        val appHourly = getAppHourly(to, x)
        val appDaily = getAppDayOfWeek(to, x)
        val daily = getDaily(8, x)
        val multiSearchResponse = client.execute(multi(hourly, app, dayOfWeek, iptv, appHourly, appDaily, daily)).await
        
        val hourlyBucket = ElasticUtil.getBucketDoubleTerm(multiSearchResponse.responses(0), "top", "sum")
        val appBucket = ElasticUtil.getBucketDoubleTerm(multiSearchResponse.responses(1), "top", "sum")
        val dayOfWeekBucket = ElasticUtil.getBucketDoubleTerm(multiSearchResponse.responses(2), "top", "sum")
        val iptvBucket = ElasticUtil.getBucketDoubleTerm(multiSearchResponse.responses(3), "top", "sum")
        val appHourlyBucket = ElasticUtil.getBucketTerm2(multiSearchResponse.responses(4), "top", "sum")
        val appDailyBucket = ElasticUtil.getBucketTerm2(multiSearchResponse.responses(5), "top", "sum")
        val dailyBucket = ElasticUtil.getBucketDoubleTerm(multiSearchResponse.responses(6), "top", "sum")
        
        val vodRes = ESUtil.get(client, "vod_cate", "docs", x)
        val vodthieuRes = ESUtil.get(client, "vod_thieu", "docs", x)
        val vodgiaitriRes = ESUtil.get(client, "vod_giaitri", "docs", x)
        
        val vod = if (vodRes.exists) {
          val source = vodRes.source
          source.keySet.filter(x => x != "ds" && x != "contract" && x != "customer_id" && x != "vec_type").map(x => Bucket(x, 0, getValueAsInt(source, x))).toArray
          } else null
        
        val vodthieu = if (vodthieuRes.exists) {
          val source = vodthieuRes.source
          source.keySet.filter(x => x != "ds" && x != "contract" && x != "customer_id" && x != "vec_type").map(x => Bucket(x, 0, getValueAsInt(source, x))).toArray
          } else null
        
        val vodgiaitri = if (vodgiaitriRes.exists) {
          val source = vodgiaitriRes.source
          source.keySet.filter(x => x != "ds" && x != "contract" && x != "customer_id" && x != "vec_type").map(x => Bucket(x, 0, getValueAsInt(source, x))).toArray
          } else null
        
        x -> PayTVVector(hourlyBucket, appBucket, dayOfWeekBucket, iptvBucket, appHourlyBucket, appDailyBucket, vod, vodthieu, vodgiaitri, dailyBucket)

      }).toMap
      //boxids.foreach(println)
      val paytvSource = payTVRes.source
      val payTVContract = PayTVContract(
          getValueAsString(paytvSource, "contract"),
          getValueAsInt(paytvSource, "box_count"),
          getValueAsString(paytvSource, "status"),
          DateTimeUtil.create(getValueAsLong(paytvSource, "start_date")/1000),
          DateTimeUtil.create(getValueAsLong(paytvSource, "active_date")/1000),
          DateTimeUtil.create(getValueAsLong(paytvSource, "change_date")/1000))
          
      (segments, vectors, payTVContract)
    } else (null,null,null)

    val internetSource = internetRes.source
    val internetInfo = InternetContract(
        getValueAsString(internetSource, "contract"),
        getValueAsString(internetSource, "object_id"),
        getValueAsString(internetSource, "name"),
        getValueAsString(internetSource, "profile"),
        getValueAsString(internetSource, "profile_type"),
        getValueAsInt(internetSource, "upload_lim").toLong,
        getValueAsInt(internetSource, "download_lim").toLong,
        getValueAsString(internetSource, "status"),
        getValueAsString(internetSource, "mac_address"),
        DateTimeUtil.create(getValueAsLong(internetSource, "start_date")/1000),
        DateTimeUtil.create(getValueAsLong(internetSource, "active_date")/1000),
        DateTimeUtil.create(getValueAsLong(internetSource, "change_date")/1000),
        getValueAsString(internetSource, "location"),
        getValueAsString(internetSource, "region"),
        getValueAsString(internetSource, "point_set"),
        getValueAsString(internetSource, "host"),
        getValueAsInt(internetSource, "port"),
        getValueAsInt(internetSource, "slot"),
        getValueAsString(internetSource, "cable_type"),
        getValueAsInt(internetSource, "life_time"))
    val internetSegmentRes = ESUtil.get(client, "segment-internet", "docs", contract)
    val internetSegmentSource = internetSegmentRes.source
   
    val internetSegment = InternetSegment(
        getValueAsString(internetSegmentSource, "Contract"),
        getValueAsString(internetSegmentSource, "City"),
        getValueAsString(internetSegmentSource, "Region"),
        getValueAsString(internetSegmentSource, "InternetLifeToEnd"),
        getValueAsString(internetSegmentSource, "Session_Count"),
        getValueAsString(internetSegmentSource, "ssOnline_Mean"),
        getValueAsString(internetSegmentSource, "DownUpload"),
        getValueAsString(internetSegmentSource, "AttendNew"),
        getValueAsString(internetSegmentSource, "InternetAvgFee"),
        getValueAsString(internetSegmentSource, "LoaiKH"),
        getValueAsString(internetSegmentSource, "Nhom_CheckList"),
        getValueAsString(internetSegmentSource, "So_checklist"),
        getValueAsString(internetSegmentSource, "LifeToEndFactor"),
        getValueAsString(internetSegmentSource, "Nhom_Tuoi"),
        getValueAsString(internetSegmentSource, "AvgFeeFactor"),
        getValueAsString(internetSegmentSource, "Nhom_Cuoc"),
        getValueAsString(internetSegmentSource, "KetnoiFactor"),
        getValueAsString(internetSegmentSource, "Nhom_Ket_Noi"),
        getValueAsString(internetSegmentSource, "NCSDFactor"),
        getValueAsString(internetSegmentSource, "Nhom_Nhu_Cau"),
        getValueAsString(internetSegmentSource, "So_Lan_Loi_Ha_Tang"),
        getValueAsString(internetSegmentSource, "So_Ngay_Loi_Ha_Tang"))
    val downupRes = ESUtil.get(client, "downup", "docs", contract)
    val downupSource = downupRes.source
//    downupSource.keySet.filter(x => x.contains("Download")).foreach(println)
    val download = downupSource.keySet.filter(x => x.contains("Download"))
      .map(x => x.substring(4).replace("Download", "") -> getValueAsString(downupSource, x))
      .map(x => x._1.toInt -> x._2.toDouble).toArray
    val upload = downupSource.keySet.filter(x => x.contains("Upload"))
      .map(x => x.substring(4).replace("Upload", "") -> getValueAsString(downupSource, x))
      .map(x => x._1.toInt -> x._2.toDouble).toArray
    
    val pon = client.execute(search(s"pon" / "docs") query { must(termQuery("contract.keyword", contract)) } limit 1000).await
    val suyhoutSource = if (pon.totalHits <= 0) {
      client.execute(search(s"adsl" / "docs") query { must(termQuery("contract.keyword", contract)) } limit 1000).await
    } else pon
    
    //println(suyhout.totalHits)
    val suyhout = suyhoutSource.hits.hits.map(x => x.sourceAsMap)
      .map(x => (getValueAsLong(x, "date")/1000) -> getValueAsString(x, "passed"))
      .map(x => DateTimeUtil.create(x._1).toString(DateTimeUtil.YMD) -> x._2)

    val errorRes = client.execute(search(s"inf" / "docs") query { must(termQuery("contract.keyword", contract)) } limit 1000).await
    val error = errorRes.hits.hits.map(x => x.sourceAsMap)
      .map(x => (getValueAsLong(x, "date")/1000) -> (getValueAsInt(x, "time"), getValueAsString(x, "error") , getValueAsString(x, "n_error")))
      .map(x => DateTimeUtil.create(x._1).toString(DateTimeUtil.YMD) -> x._2)
    val module = error.filter(x => x._2._2 == "module/cpe error").map(x => x._1 -> x._2._3.toInt).groupBy(x => x._1).map(x => x._1 -> x._2.map(y => y._2).sum).toArray
    val disconnet = error.filter(x => x._2._2 == "disconnect/lost IP").map(x => x._1 -> x._2._3.toInt).groupBy(x => x._1).map(x => x._1 -> x._2.map(y => y._2).sum).toArray
    
    val internetBillRes = ESUtil.get(client, "bill-internet", "docs", contract)
    val payTVBillRes = ESUtil.get(client, "bill-paytv", "docs", contract)
    val sessionRes = ESUtil.get(client, "session", "docs", contract)
    
    val internetBill = if (internetBillRes.exists) getValueAsInt(internetBillRes.source, "SoTienDaThanhToan") else 0
    val payTVBill = if (payTVBillRes.exists) getValueAsInt(payTVBillRes.source, "BillFee") else 0
    
    val session: Session = if (sessionRes.exists) {
      val map = sessionRes.source
      Session(contract,
          getValueAsInt(map, "Session_Count"),
          getValueAsInt(map, "ssOnline_Min"),
          getValueAsInt(map, "ssOnline_Max"),
          getValueAsDouble(map, "ssOnline_Mean"),
          getValueAsDouble(map, "ssOnline_Std"))
    } else null
    
    val checkListRes = ESUtil.get(client, "check-list", "docs", contract)
    val checkList = if (checkListRes.exists) {
      val map = checkListRes.source
      getValueAsString(map, "Nhom_CheckList") -> getValueAsInt(map, "So_checklist")
    } else null
//    val payTVBillRes = ESUtil.get(client, "bill-paytv", "docs", contract)
    
    Response(internetInfo, segmentsVectorInfo._3, segmentsVectorInfo._1, segmentsVectorInfo._2, internetSegment, download, upload, suyhout, error, module, disconnet,
        Bill(internetBill, payTVBill),
        session,
        checkList)
    
  }

  def main(args: Array[String]) {
    val time0 = System.currentTimeMillis()
//    ProfileService.get("LDD018356")
    
//    val response = ProfileService.get("DNFD21708")
    
    val response = ProfileService.get("HNFD94642")
    //val response = ProfileService.get("NAFD01366")
    
    //val response = ProfileService.get("LDD018356")
    //val response = ProfileService.get("CBFD01425")
    //response.segments.foreach(x => println(x._1 -> x._2.app))
    //println("IPTV")
//    response.vectors.foreach(x => x._1 -> x._2.iptv.foreach(println))
    //println("Vod")
    //response.vectors.foreach(x => x._1 -> x._2.vod.foreach(println))
    //response.download.foreach(println)
    //response.suyhout.foreach(println)
    //response.error.foreach(println)
//    response.vectors.head
    //println(response.paytv.box_count)//
    //println(response.bill.internet -> response.bill.paytv)
    //println(response.session)
    //response.upload.foreach(println)//
    response.errorModule.foreach(println)
//    response.errorDisconnect.foreach(println)
    val time1 = System.currentTimeMillis()
    println(time1 - time0)
    client.close()
  }
}