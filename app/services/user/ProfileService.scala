package services.user

import com.sksamuel.elastic4s.http.ElasticDsl._
import services.Configure
import com.ftel.bigdata.utils.ESUtil
import com.sksamuel.elastic4s.searches.SearchDefinition
import services.ElasticUtil
import services.domain.AbstractService
//import model.paytv.InternetContract
import com.ftel.bigdata.utils.DateTimeUtil
//import model.paytv.PayTVContract
//import model.user.Response
import model.user.PayTVSegment
import model.user.PayTVVector
import model.user.InternetSegment
import services.Bucket
//import utils.Session
//import model.user.Bill
import services.BucketDouble
import services.Bucket2
import model.user.ProfileResponse
import model.user.PayTVContract
import model.user.PayTVResponse
import model.user.PayTVBox
import model.user.InternetResponse
import model.user.InternetContract
import model.user.Session
import model.user.DownUp
import model.user.Duration
import org.joda.time.DateTime
import model.user.Device

case class DeviceRow(day: String,
    contract: String,
    mac: String,
    name: String,
    falseField: Double,
    trueField: Double,
    signal_mean: Double,
    mac_device: String,
    vendor: String,
    mobile: Boolean) {
  override def toString = List(day, contract, mac, name, falseField, trueField, signal_mean, mac_device, vendor, mobile).mkString("\t")
}

object ProfileService extends AbstractService {
  //val client = Configure.client
  val SIZE_DEFAULT = 100

  private def getHourly(boxId: String,month:String): SearchDefinition = {
    search(s"paytv-weekly-hourly-$month-*" / "docs") query { must(termQuery("customer", boxId)) } aggregations (
      termsAggregation("top")
      .field("hour")
      .subaggs(
        sumAgg("sum", "value")) size SIZE_DEFAULT)
  }

  private def getHourlyInMonth(boxId: String,month:String): SearchDefinition = {
    search(s"paytv-weekly-hourly-$month-*" / "docs") query { must(termQuery("customer", boxId)) } aggregations (
      termsAggregation("top")
      .field("hour")
      .subaggs(
        sumAgg("sum", "value")) size SIZE_DEFAULT)
  }

  private def getApp(boxId: String,month:String): SearchDefinition = {
    search(s"user-paytv-weekly-daily-$month-*" / "docs") query { must(termQuery("customer", boxId)) } aggregations (
      termsAggregation("top")
      .field("app.keyword")
      .subaggs(
        sumAgg("sum", "value")) size SIZE_DEFAULT)
  }

  private def getDayOfWeek(boxId: String,month:String): SearchDefinition = {
    search(s"paytv-weekly-daily-$month-*" / "docs") query { must(termQuery("customer", boxId)) } aggregations (
      termsAggregation("top")
      .field("dayOfWeek")
      .subaggs(
        sumAgg("sum", "value")) size SIZE_DEFAULT)
  }

  private def getIPTV(boxId: String,month:String): SearchDefinition = {
    search(s"paytv-weekly-iptv-$month-*" / "docs") query { must(termQuery("customer", boxId)) } aggregations (
      termsAggregation("top")
      .field("cate")
      .subaggs(
        sumAgg("sum", "value")) size SIZE_DEFAULT)
  }

  private def getAppHourly(boxId: String,month:String): SearchDefinition = {
    search(s"paytv-weekly-hourly-$month-*" / "docs") query { must(termQuery("customer", boxId)) } aggregations (
      termsAggregation("top").field("app")
      .subaggs(termsAggregation("sub").field("hour")
        .subaggs(sumAgg("sum", "value")) size SIZE_DEFAULT) size SIZE_DEFAULT) limit SIZE_DEFAULT
  }

  private def getAppDayOfWeek(boxId: String,month:String): SearchDefinition = {
    search(s"paytv-weekly-daily-$month-*" / "docs") query { must(termQuery("customer", boxId)) } aggregations (
      termsAggregation("top")
      .field("app")
      .subaggs(
        termsAggregation("sub").field("dayOfWeek").subaggs(
          sumAgg("sum", "value")) size SIZE_DEFAULT) size SIZE_DEFAULT) limit (SIZE_DEFAULT * 10)

  }
  
  private def getVod(boxId: String,month: String): SearchDefinition = {
    search(s"user-paytv-weekly-vod-$month-*" / "docs") query { must(termQuery("customer_id", boxId)) }
  }
  
  private def getVodGiaitri(boxId: String,month: String): SearchDefinition = {
    search(s"user-paytv-weekly-vod-giaitri-$month-*" / "docs") query { must(termQuery("customer_id", boxId)) }
  }
  
  private def getVodThieuNhi(boxId: String,month: String): SearchDefinition = {
    search(s"user-paytv-weekly-vod-thieunhi-$month-*" / "docs") query { must(termQuery("customer_id", boxId)) }
  }
  
  private def getDevice(mac: String): SearchDefinition = {
    search(s"user-device-*" / "docs") query { must(termQuery("mac", mac)) } limit (SIZE_DEFAULT * 10)
  }
  
  private def getDevice2(contract: String): SearchDefinition = {
    search(s"user-cpe-*" / "docs") query { must(termQuery("contract.keyword", contract)) } limit (10000)
  }
  
  private def getDevice3(contract: String): SearchDefinition = {
    search(s"user-cpe-2017-09" / "docs") query { must(termQuery("contract.keyword", contract)) } aggregations (
        cardinalityAgg("devices", "mac_device.keyword"),
        cardinalityAgg("vendors", "vendor.keyword"),
        cardinalityAgg("deviceType", "name.keyword"),
        termsAggregation("top")
          .field("mac_device")
          .subaggs(
              termsAggregation("day")
                .field("day")
                .subaggs(
                    sumAgg("sum", "signal_mean")
                ) size SIZE_DEFAULT
           ) size SIZE_DEFAULT
        ) limit (SIZE_DEFAULT * 10)
  }
//
//    private def getVOD(to: String, boxId: String): SearchDefinition = {
//      search(s"vod_cate" / "docs") query { must(termQuery("customer", boxId)) } aggregations (
//        termsAggregation("top")
//        .field("cate")
//        .subaggs(
//          sumAgg("sum", "value")) size SIZE_DEFAULT)
//    }
//    
//    private def getVODthieunhi(to: String, boxId: String): SearchDefinition = {
//      search(s"vod_thieu" / "docs") query { must(termQuery("customer", boxId)) } aggregations (
//        termsAggregation("top")
//        .field("cate")
//        .subaggs(
//          sumAgg("sum", "value")) size SIZE_DEFAULT)
//    }
//    
//    private def getVODgiaitri(to: String, boxId: String): SearchDefinition = {
//      search(s"vod_giaitri" / "docs") query { must(termQuery("customer", boxId)) } aggregations (
//        termsAggregation("top")
//        .field("cate")
//        .subaggs(
//          sumAgg("sum", "value")) size SIZE_DEFAULT)
//    }

  private def getDaily(boxId: String): SearchDefinition = {
    search(s"paytv-weekly-daily-*" / "docs") query { must(termQuery("customer", boxId)) } aggregations (
      termsAggregation("top")
      .field("day")
      .subaggs(
        sumAgg("sum", "value")) size SIZE_DEFAULT) size SIZE_DEFAULT
  }

  private def getDurationDoW(contract: String): Array[(String, Array[Double])] = {
     val response = client.execute(search(s"user-duration-day-of-week-2017-09" / "docs") query { must(termQuery("Contract.keyword", contract)) }).await
     val result= response.hits.hits.map(x => x.sourceAsMap)
       .map(x => getValueAsString(x, "wod") -> Array(
           getValueAsDouble(x, "Mon"),
           getValueAsDouble(x, "Tue"),
           getValueAsDouble(x, "Wed"),
           getValueAsDouble(x, "Thu"),
           getValueAsDouble(x, "Fri"),
           getValueAsDouble(x, "Sat"),
           getValueAsDouble(x, "Sun")))
     result
  }
  
  private def getDownloadDoW(contract: String): (Array[(String, Array[Double])], Array[(String, Array[Double])]) = {
     val response = client.execute(search(s"user-downup-day-of-week-2017-09" / "docs") query { must(termQuery("Contract.keyword", contract)) }).await
     val download= response.hits.hits.map(x => x.sourceAsMap)
       .map(x => getValueAsString(x, "wod") -> Array(
           getValueAsDouble(x, "Mon_Download"),
           getValueAsDouble(x, "Tue_Download"),
           getValueAsDouble(x, "Wed_Download"),
           getValueAsDouble(x, "Thu_Download"),
           getValueAsDouble(x, "Fri_Download"),
           getValueAsDouble(x, "Sat_Download"),
           getValueAsDouble(x, "Sun_Download")))
     val upload= response.hits.hits.map(x => x.sourceAsMap)
       .map(x => getValueAsString(x, "wod") -> Array(
           getValueAsDouble(x, "Mon_Upload"),
           getValueAsDouble(x, "Tue_Upload"),
           getValueAsDouble(x, "Wed_Upload"),
           getValueAsDouble(x, "Thu_Upload"),
           getValueAsDouble(x, "Fri_Upload"),
           getValueAsDouble(x, "Sat_Upload"),
           getValueAsDouble(x, "Sun_Upload")))
     (download, upload)
  }
  
  def get(contract: String,month: String): ProfileResponse = {
    ProfileResponse(getInternetResponse(contract,month), getPayTVResponse(contract,month))
  }

  private def getInternetResponse(contract: String,month: String): InternetResponse = {
    val internetRes = ESUtil.get(client, "user-contract-internet", "docs", contract)
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
      DateTimeUtil.create(getValueAsLong(internetSource, "start_date") / 1000),
      DateTimeUtil.create(getValueAsLong(internetSource, "active_date") / 1000),
      DateTimeUtil.create(getValueAsLong(internetSource, "change_date") / 1000),
      getValueAsString(internetSource, "location"),
      getValueAsString(internetSource, "region"),
      getValueAsString(internetSource, "point_set"),
      getValueAsString(internetSource, "host"),
      getValueAsInt(internetSource, "port"),
      getValueAsInt(internetSource, "slot"),
      getValueAsInt(internetSource, "onu"),
      getValueAsString(internetSource, "cable_type"),
      getValueAsInt(internetSource, "life_time"))
    val internetSegmentRes = ESUtil.get(client, "user-segment-internet-"+month, "docs", contract)
    val internetSegmentSource = internetSegmentRes.source
    val segment = InternetSegment(
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

    
    val downupQuadRes = ESUtil.get(client, "user-downup-quad-"+month, "docs", contract)
    val downupQuadSource = downupQuadRes.source
    //    downupSource.keySet.filter(x => x.contains("Download")).foreach(println)
    val downloadQuad = downupQuadSource.keySet
      .filter(x => x.contains("Quad_"))
      .filter(x => x.contains("Download"))
      .map(x => x.substring(5).replace("_Download", "") -> getValueAsString(downupQuadSource, x))
      .map(x => x._1.toInt.toString -> (if (x._2.toDouble < 0) 0 else x._2.toDouble))
      .toArray
      //.map(x => x._1 -> BigDecimal(x._2).setScale(3, BigDecimal.RoundingMode.HALF_UP).toDouble)
      .sortBy(x => x._1)
    val uploadQuad = downupQuadSource.keySet
      .filter(x => x.contains("Quad_"))
      .filter(x => x.contains("Upload"))
      .map(x => x.substring(5).replace("_Upload", "") -> getValueAsString(downupQuadSource, x))
      .map(x => x._1.toInt.toString -> (if (x._2.toDouble < 0) 0 else x._2.toDouble))
      .toArray
      //.map(x => x._1 -> BigDecimal(x._2).setScale(3, BigDecimal.RoundingMode.HALF_UP).toDouble)
      .sortBy(x => x._1)
    val downloadDaily = downupQuadSource.keySet
      .filter(x => x.contains("Size"))
      .filter(x => x.contains("Download"))
      .map(x => x.substring(4).replace("Download", "") -> getValueAsString(downupQuadSource, x))
      .map(x => x._1.toInt.toString -> (if (x._2.toDouble < 0) 0 else x._2.toDouble))
      .toArray
      //.map(x => x._1 -> BigDecimal(x._2).setScale(3, BigDecimal.RoundingMode.HALF_UP).toDouble)
      .sortBy(x => x._1)
    val uploadDaily = downupQuadSource.keySet
      .filter(x => x.contains("Size"))
      .filter(x => x.contains("Upload"))
      .map(x => x.substring(4).replace("Upload", "") -> getValueAsString(downupQuadSource, x))
      .map(x => x._1.toInt.toString -> (if (x._2.toDouble < 0) 0 else x._2.toDouble))
      .toArray
      //.map(x => x._1 -> BigDecimal(x._2).setScale(3, BigDecimal.RoundingMode.HALF_UP).toDouble)
      .sortBy(x => x._1)
    val downupDow = getDownloadDoW(contract)
    val downup = DownUp(downloadQuad, uploadQuad, downupDow._1, downupDow._2, formatArray(downloadDaily), formatArray(uploadDaily))
    
    val durationDailyRes = ESUtil.get(client, "user-duration-daily-"+month, "docs", contract)
    val durationDailySource = durationDailyRes.source
    val durationDaily = durationDailySource.keySet
      .filter(x => x.contains("sum(Size"))
      .map(x => x.substring(8).replace("Duration)", "") -> getValueAsString(durationDailySource, x))
      .map(x => x._1.toInt.toString -> x._2.toDouble)
      .toArray
      .map(x => x._1 -> BigDecimal(x._2).setScale(3, BigDecimal.RoundingMode.HALF_UP).toDouble)
      .sortBy(x => x._1)
      //.take(28)

    val durationHourlyRes = ESUtil.get(client, "user-duration-hourly-"+month, "docs", contract)
    val durationHourlySource = durationHourlyRes.source
    //println(durationHourlySource)
    val durationHourly = durationHourlySource.keySet
      .filter(x => !x.contains("Contract") && !x.contains("Name") && !x.contains("Date"))
      .map(x => {
        //println(x -> getValueAsString(durationHourlySource, x))
        x -> getValueAsDouble(durationHourlySource, x)
        })
      .map(x => x._1.toInt.toString -> x._2.toDouble)
      .toArray
      //.map(x => x._1 -> BigDecimal(x._2).setScale(3, BigDecimal.RoundingMode.HALF_UP).toDouble)
      .sortBy(x => x._1.toInt)
      //.take(28)

    val durationDow = getDurationDoW(contract)
    val duration = Duration(durationHourly, durationDow, formatArray(durationDaily))
    val pon = client.execute(search(s"user-inf-pon-$month" / "docs") query { must(termQuery("contract.keyword", contract)) } limit 1000).await
    val suyhoutSource = if (pon.totalHits <= 0) {
      client.execute(search(s"user-inf-adsl-$month" / "docs") query { must(termQuery("contract.keyword", contract)) } limit 1000).await
    } else pon

    val suyhout = suyhoutSource.hits.hits.map(x => x.sourceAsMap)
      .map(x => (getValueAsLong(x, "date") / 1000) -> getValueAsString(x, "passed"))
      .map(x => DateTimeUtil.create(x._1).toString(DateTimeUtil.YMD) -> x._2)
      .sortBy(x => x._1)
      //.toMap.toArray
    //println(client.show(search(s"user-inf-error-2017-09" / "docs") query { must(termQuery("contract.keyword", contract)) } limit 1000))
    val errorRes = client.execute(search(s"user-inf-error-$month" / "docs") query { must(termQuery("contract.keyword", contract)) } limit 1000).await
    val error = errorRes.hits.hits.map(x => {
        //println(x.sourceAsMap)
        x.sourceAsMap
      })
      .map(x => (getValueAsLong(x, "date") / 1000) -> (getValueAsInt(x, "time"), getValueAsString(x, "error"), getValueAsString(x, "n_error")))
      .map(x => DateTimeUtil.create(x._1).toString(DateTimeUtil.YMD) -> x._2)
      //.toMap.toArray
    val module = error.filter(x => x._2._2 == "module/cpe error")
      .map(x => x._1 -> x._2._3.toInt).groupBy(x => x._1).map(x => x._1 -> x._2.map(y => y._2).sum)
      .toArray
      .sortBy(x => x._1)
    val disconnet = error.filter(x => x._2._2 == "disconnect/lost IP")
      .map(x => x._1 -> x._2._3.toInt).groupBy(x => x._1).map(x => x._1 -> x._2.map(y => y._2).sum)
      .toArray
      .sortBy(x => x._1)

    val sessionRes = ESUtil.get(client, s"user-session-${month}", "docs", contract)
    val session: Session = if (sessionRes.exists) {
      val map = sessionRes.source
      Session(contract,
        getValueAsInt(map, "Session_Count"),
        getValueAsInt(map, "ssOnline_Min"),
        getValueAsInt(map, "ssOnline_Max"),
        getValueAsDouble(map, "ssOnline_Mean"),
        getValueAsDouble(map, "ssOnline_Std"))
    } else null
    val checkListRes = ESUtil.get(client, "user-ticket-"+month, "docs", contract)
    val checkList = if (checkListRes.exists) {
      val map = checkListRes.source
      getValueAsString(map, "Nhom_CheckList") -> getValueAsInt(map, "So_checklist")
    } else null
    
    val billRes = ESUtil.get(client, "user-bill-internet-"+month, "docs", contract)
    val bill = if (billRes.exists) getValueAsDouble(billRes.source, "BillFee") else 0

//    val mac = internetInfo.macAddress.replace(":", "")
//    val deviceRes = client.execute(getDevice(mac)).await
//    
//    val numberOfDevice = deviceRes.totalHits
//    val venders = deviceRes.hits.hits.map(x => x.sourceAsMap).map(x => getValueAsString(x, "vendor")).groupBy(x => x).mapValues(x => x.length).toArray
//    val deviceTypes = deviceRes.hits.hits.map(x => x.sourceAsMap).map(x => getValueAsString(x, "name")).groupBy(x => x).mapValues(x => x.length).toArray
//    
//    val numberOfVender = venders.size
//    val numberOfDeviceType = deviceTypes.size
//    val numberOfMobile = deviceRes.hits.hits.map(x => x.sourceAsMap).map(x => getValueAsString(x, "members").toBoolean).filter(x => x).length
//    val numberOfPermanent = numberOfDevice - numberOfMobile
//    val device = Device(numberOfDevice, numberOfVender, numberOfDeviceType, numberOfMobile, numberOfPermanent, venders, deviceTypes)
    
    val device2Res = client.execute(getDevice2(contract)).await
    
    val rows = device2Res.hits.hits.map(x => x.sourceAsMap)
      .map(x => DeviceRow(
          getValueAsString(x, "day"),
          getValueAsString(x, "contract"),
          getValueAsString(x, "mac"),
          getValueAsString(x, "name"),
          getValueAsDouble(x, "falseField"),
          getValueAsDouble(x, "trueField"),
          getValueAsDouble(x, "signal_mean"),
          getValueAsString(x, "mac_device"),
          getValueAsString(x, "vendor"),
          getValueAsString(x, "mobile").toBoolean
          ))
    val numberOfDevice = rows.map(x => x.mac_device).distinct.length
    val numberOfVender = rows.map(x => x.vendor).distinct.length
    val numberOfDeviceType = rows.map(x => x.name).distinct.length
    val numberOfMobile = rows.filter(x => x.mobile).map(x => x.mac_device).distinct.length
    val numberOfPermanent = rows.filter(x => !x.mobile).map(x => x.name).distinct.length
    
    val venders = rows.map(x => x.vendor).groupBy(x => x).mapValues(x => x.length).toArray
    val deviceTypes = rows.map(x => x.name).groupBy(x => x).mapValues(x => x.length).toArray
    
    //rows.foreach(println)
    val deviceCharts= rows.map(x => Array(x.mac_device, x.name, x.vendor).mkString(",") -> (DateTimeUtil.create(x.day, "yyyy-MM-dd HH:mm:SS.S").dayOfMonth().get.toString, x.signal_mean))
        .groupBy(x => x._1)
        .mapValues(x => formatArray(x.map(y => y._2)).map(y => y._2))
    //a.map(x => x._1 + ": " + x._2.mkString(" ")).foreach(println)
    val device = Device(numberOfDevice, numberOfVender, numberOfDeviceType, numberOfMobile, numberOfPermanent, venders, deviceTypes, deviceCharts)
    //rows.map(x => x.mac_device -> (x.day, x.signal_mean)).foreach(println)
    InternetResponse(internetInfo, segment, downup, duration, suyhout, error, formatArray2(module), formatArray2(disconnet), session, checkList, bill, device)
  }

  private def getPayTVResponse(contract: String,month: String): PayTVResponse = {
    val payTVRes = ESUtil.get(client, "user-contract-paytv", "docs", contract)
    if (payTVRes.exists) {
      // payTV contract
      val paytvSource = payTVRes.source
      val payTVContract = PayTVContract(
        getValueAsString(paytvSource, "contract"),
        getValueAsInt(paytvSource, "box_count"),
        getValueAsString(paytvSource, "status"),
        DateTimeUtil.create(getValueAsLong(paytvSource, "start_date") / 1000),
        DateTimeUtil.create(getValueAsLong(paytvSource, "active_date") / 1000),
        DateTimeUtil.create(getValueAsLong(paytvSource, "change_date") / 1000))

      // Box
      val boxRes = client.execute(search(s"user-contract-box" / "docs") query { must(termQuery("contract.keyword", contract)) } limit 10).await
      val boxs = boxRes.hits.hits.map(x => x.sourceAsMap)
        .map(x => PayTVBox(getValueAsString(x, "customer_id"),
          getValueAsString(x, "contract"),
          getValueAsString(x, "status"),
          DateTimeUtil.create(getValueAsLong(paytvSource, "change_date") / 1000),
          getValueAsString(x, "mac_address")))

      // Get Segment
      println("Month:"+month)
      val segments = boxs.map(x => x -> ESUtil.get(client, "user-segment-paytv-"+month, "docs", x.id))
        .map(x => {
            //println(x._2.source)
            x._1 -> x._2.source
          })
        .map(x => x._1.id -> PayTVSegment(
          getValueAsString(x._2, "cluster_app"),
          getValueAsString(x._2, "cluster_hourly"),
          getValueAsString(x._2, "cluster_daily"),
          getValueAsString(x._2, "cluster_lifetoend"),
          getValueAsString(x._2, "cluster_sum"),
          getValueAsString(x._2, "cluster_iptv"),
          getValueAsString(x._2, "cluster_vod"),
          getValueAsString(x._2, "cluster_vod_giaitri"),
          getValueAsString(x._2, "cluster_vod_thieunhi"),
          x._1.status)).toMap
      // Vector
      val vectors = boxs.map(x => x.id).map(x => {
        val hourly = getHourly(x,month)
        val hourlyInMonth = getHourlyInMonth(x,month)
        val app = getApp(x,month)
        println(client.show(app))
        val dayOfWeek = getDayOfWeek(x,month)
        val iptv = getIPTV(x,month)
        val appHourly = getAppHourly(x,month)
        //println(client.show(appHourly))
        val appDaily = getAppDayOfWeek(x,month)
        val daily = getDaily(x)
        
        val vodRequest = getVod(x,month)
        //println(client.show(vodRequest))
        val vodgiaitriRequest = getVodGiaitri(x,month)
        val vodthieunhiRequest = getVodThieuNhi(x,month)
        val multiSearchResponse = client.execute(multi(hourly, app, dayOfWeek, iptv, appHourly, appDaily, daily, hourlyInMonth, vodRequest, vodgiaitriRequest, vodthieunhiRequest)).await

        //multiSearchResponse.responses(4).aggregations.foreach(println)

        val hourlyBucket = ElasticUtil.getBucketDoubleTerm(multiSearchResponse.responses(0), "top", "sum")
        val appBucket = ElasticUtil.getBucketDoubleTerm(multiSearchResponse.responses(1), "top", "sum")

        val dayOfWeekBucket = ElasticUtil.getBucketDoubleTerm(multiSearchResponse.responses(2), "top", "sum")
          .sortBy(x => x.key.toInt)
          .map(x => BucketDouble(dayOfWeekNumberToLabel(x.key.toInt), x.count, x.value))
        val iptvBucket = ElasticUtil.getBucketDoubleTerm(multiSearchResponse.responses(3), "top", "sum")
        val appHourlyBucket = ElasticUtil.getBucketTerm2(multiSearchResponse.responses(4), "top", "sum")
        val appDailyBucket = ElasticUtil.getBucketTerm2(multiSearchResponse.responses(5), "top", "sum")
        //multiSearchResponse.responses(6).aggregations.foreach(println)
        val dailyBucket = ElasticUtil.getBucketDoubleTerm(multiSearchResponse.responses(6), "top", "sum").sortBy(x => x.key.toInt)
        val hourlyInMonthBucket = ElasticUtil.getBucketDoubleTerm(multiSearchResponse.responses(7), "top", "sum")

        val vodRes = multiSearchResponse.responses(8)
        val vodgiaitriRes = multiSearchResponse.responses(9)//ESUtil.get(client, "vod_giaitri", "docs", x)
        val vodthieunhiRes = multiSearchResponse.responses(10)//ESUtil.get(client, "vod_thieu", "docs", x)
        //println(vodRes.totalHits)
        //vodRes.hits.hits.foreach(println)
        val vodCate = Array("action", "animation", "comedy", "documentary", "horror", "romance", "drama", "family", "crime", "adventure", "costume")

        val vod = if (!vodRes.hits.hits.isEmpty) {
          val array = vodRes.hits.hits.map(x => {
            val source = x.sourceAsMap
            source.keySet.filter(x => x != "ds" && x != "contract" && x != "customer_id" && x != "vec_type")
                               .map(x => x -> getValueAsInt(source, x))
                               .toMap
          })
          mergeArrayMap(array).toArray.map(x => Bucket(x._1, 0, x._2)).filter(x => vodCate.contains(x.key.toLowerCase()))
          
          //map.sum(x => map1 ++ map2.map{ case (k,v) => k -> (v + map1.getOrElse(k,0)) })
          //null
        } else null
        val vodgiaitri = if (!vodgiaitriRes.hits.hits.isEmpty) {
          val array = vodgiaitriRes.hits.hits.map(x => {
            val source = x.sourceAsMap
            source.keySet.filter(x => x != "ds" && x != "contract" && x != "customer_id" && x != "vec_type")
                               .map(x => x -> getValueAsInt(source, x))
                               .toMap
          })
          mergeArrayMap(array).toArray.map(x => Bucket(x._1, 0, x._2))
          //map.sum(x => map1 ++ map2.map{ case (k,v) => k -> (v + map1.getOrElse(k,0)) })
          //null
        } else null
        val vodthieunhi = if (!vodthieunhiRes.hits.hits.isEmpty) {
          val array = vodthieunhiRes.hits.hits.map(x => {
            val source = x.sourceAsMap
            source.keySet.filter(x => x != "ds" && x != "contract" && x != "customer_id" && x != "vec_type")
                               .map(x => x -> getValueAsInt(source, x))
                               .toMap
          })
          mergeArrayMap(array).toArray.map(x => Bucket(x._1, 0, x._2))
          //map.sum(x => map1 ++ map2.map{ case (k,v) => k -> (v + map1.getOrElse(k,0)) })
          //null
        } else null
        //val vodgiaitri = null
//        val vodthieu = if (vodthieuRes.exists) {
//          val source = vodthieuRes.source
//          source.keySet.filter(x => x != "ds" && x != "contract" && x != "customer_id" && x != "vec_type").map(x => Bucket(x, 0, getValueAsInt(source, x))).toArray
//        } else null
//
//        val vodgiaitri = if (vodgiaitriRes.exists) {
//          val source = vodgiaitriRes.source
//          source.keySet.filter(x => x != "ds" && x != "contract" && x != "customer_id" && x != "vec_type").map(x => Bucket(x, 0, getValueAsInt(source, x))).toArray
//        } else null

        def group(array: Array[Bucket2], key: String): Array[(String, Double)] = {
          array.filter(a => a.key == key).map(a => a.term -> a.value)
            .groupBy(a => a._1).map(a => a._1 -> a._2.map(b => b._2).sum)
            .toArray
            .sortBy(a => a._1.toInt)
            .map(x => x._1 -> BigDecimal(x._2).setScale(3, BigDecimal.RoundingMode.HALF_UP).toDouble)
        }
        def group2(array: Array[Bucket2], key: String): Array[(String, Double)] = {
          array.filter(a => a.key == key).map(a => a.term -> a.value)
            .groupBy(a => a._1).map(a => a._1 -> a._2.map(b => b._2).sum)
            .toArray
            .sortBy(a => a._1.toInt)
            .map(x => dayOfWeekNumberToLabel(x._1.toInt) -> BigDecimal(x._2).setScale(3, BigDecimal.RoundingMode.HALF_UP).toDouble)
        }
        def groupHourAndDayOfWeek(array: Array[Bucket2]): Array[(String, Array[Double])] = {
          array.groupBy(x => x.key)
            .map(x => x._1 -> x._2.map(y => y.term -> y.value).sortBy(y => y._1.toInt).map(y => y._2))
            .toArray
        }

        //val b = a.groupBy(x => x._1).map(x => x._1 -> x._2.map(y => y._2).sum).toArray
        x -> PayTVVector(hourlyBucket, hourlyInMonthBucket, appBucket, dayOfWeekBucket, iptvBucket, appHourlyBucket, appDailyBucket,
          groupHourAndDayOfWeek(appHourlyBucket),
          groupHourAndDayOfWeek(appDailyBucket),
          group(appHourlyBucket, "IPTV"),
          group(appHourlyBucket, "VOD"),
          group2(appDailyBucket, "IPTV"),
          group2(appDailyBucket, "VOD"),
          vod, vodthieunhi, vodgiaitri, formatArray(dailyBucket.map(x => x.key -> x.value)))
      }).toMap

      val billRes = ESUtil.get(client, s"user-bill-paytv-$month", "docs", contract)
      val bill = if (billRes.exists) getValueAsDouble(billRes.source, "BillFee") else 0

      PayTVResponse(payTVContract, boxs, segments, vectors, bill)
    } else null
  }

  def dayOfWeekNumberToLabel = (i: Int) => {
    i match {
      case 0 => "Mon"
      case 1 => "Tue"
      case 2 => "Wed"
      case 3 => "Thu"
      case 4 => "Fri"
      case 5 => "Sat"
      case 6 => "Sun"
      case _ => "???"
    }
  }

  private def formatArray(array: Array[(String, Double)]): Array[(String, Double)] = {
//    array.foreach(println)
    val seq = 1 until (DateTimeUtil.create("2017-09-01", DateTimeUtil.YMD).dayOfMonth().getMaximumValue + 1)
    val map = array.toMap
//    println("TEST: " + map.getOrElse("2017-09-11", 0))
    val result = seq.toArray.map(x => {
      //println(x.toString)
      x.toString -> map.getOrElse(x.toString, 0.0)
      })
    //result.foreach(println)
    result
  }
  
  private def formatArray2(array: Array[(String, Int)]): Array[(String, Int)] = {
    val seq = 1 until (DateTimeUtil.create("2017-09-01", DateTimeUtil.YMD).dayOfMonth().getMaximumValue + 1)
    val map = array.toMap
    seq.map(x => "2017-09-" + f"$x%02d").toArray.map(x => x.toString -> map.getOrElse(x.toString, 0))
  }

  private def mergeArrayMap(array: Array[Map[String, Int]]): Map[String, Int] = {
    array.reduce((map1, map2) => map1 ++ map2.map{ case (k,v) => k -> (v + map1.getOrElse(k,0)) })
  }
  
  def main(args: Array[String]) {
    val time0 = System.currentTimeMillis()
    
    //val map1 = Map("a" -> 1, "b" -> 2)
    //val map2 = Map("a" -> 2, "b" -> 4)
    //val array = Array(map1, map2)
    
    //val map3 = mergeArrayMap(array)
    //map3.foreach(println)
    //val response = ProfileService.get("HUFD08955","")
    val response = ProfileService.get("SGH210510", "2017-10")
    //val a = response.paytv.vectors.get("445814").get
//    a.app.foreach(println)
//    response.internet.errorDisconnect.foreach(println)
//    response.internet.errorModule.foreach(println)
//    response.internet.errorDisconnect.foreach(println)
//    response.internet.errorModule.map(x => x._1).foreach(println)
    //println(response.internet.errorDisconnect.size -> response.internet.errorModule.size)
    //response.internet.duration.hourly.foreach(println)
    //response.paytv.vectors.get("90012").get.daily.foreach(println)
    //response.paytv.vectors.get("356724").get.hourlyInMonths.foreach(println)
    //a.vodgiaitri.foreach(println)
    //response.internet.device.venders.foreach(println)
    //response.internet.device.deviceTypes.foreach(println)
    //println(response.internet.device.numberOfDevice)
    //println(response.internet.device.numberOfMobile)
//    println(response.internet.segment)
//    response.internet.device.deviceChars.foreach(x => {
//      println(x._1)
//      x._2.foreach(println)
//    })
    
    response.paytv.vectors.get("624956").get.app.foreach(println)
    println("BILL: " + response.internet.bill)
    val time1 = System.currentTimeMillis()
    println(time1 - time0)
    client.close()
  }
  
}