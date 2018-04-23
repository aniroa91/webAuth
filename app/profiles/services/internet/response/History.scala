package profile.services.internet.response

import com.ftel.bigdata.utils.DateTimeUtil
import profile.services.internet.Common


case class Hourly(
    contract: Array[(Long, Long)],
    session: Array[(Int, Long)],
    download: Array[(Int, Long)],
    upload: Array[(Int, Long)],
    duration: Array[(Int, Long)]) {
  
    def this(hourly: Array[(String, Long, Long, Long, Long, Long)]) = this(
        hourly.map(x => x._1.toLong -> x._2).sortBy(x => x._1).toArray,
        Common.reduceByKeyInt(hourly.map(x => DateTimeUtil.create(x._1.toLong / 1000).getHourOfDay -> x._3)),
        Common.reduceByKeyInt(hourly.map(x => DateTimeUtil.create(x._1.toLong / 1000).getHourOfDay -> x._4)),
        Common.reduceByKeyInt(hourly.map(x => DateTimeUtil.create(x._1.toLong / 1000).getHourOfDay -> x._5)),
        Common.reduceByKeyInt(hourly.map(x => DateTimeUtil.create(x._1.toLong / 1000).getHourOfDay -> x._6))
      )
}
    
case class DayOfWeek(
    contract: Array[(Int, Long)],
    session: Array[(Int, Long)],
    download: Array[(Int, Long)],
    upload: Array[(Int, Long)],
    duration: Array[(Int, Long)]) {
  def this(weekly: Array[(String, Long, Long, Long, Long, Long)]) = this(
        Common.reduceByKeyInt(weekly.map(x => DateTimeUtil.create(x._1.toLong / 1000).getDayOfWeek -> x._2)),
        Common.reduceByKeyInt(weekly.map(x => DateTimeUtil.create(x._1.toLong / 1000).getDayOfWeek -> x._3)),
        Common.reduceByKeyInt(weekly.map(x => DateTimeUtil.create(x._1.toLong / 1000).getDayOfWeek -> x._4)),
        Common.reduceByKeyInt(weekly.map(x => DateTimeUtil.create(x._1.toLong / 1000).getDayOfWeek -> x._5)),
        Common.reduceByKeyInt(weekly.map(x => DateTimeUtil.create(x._1.toLong / 1000).getDayOfWeek -> x._6))
      )
}
//
case class Daily(
    contract: Array[(Long, Long)],
    session: Array[(Long, Long)],
    download: Array[(Long, Long)],
    upload: Array[(Long, Long)],
    duration: Array[(Long, Long)]) {
  def this(daily: Array[(String, Long, Long, Long, Long, Long)]) = this(
        daily.map(x => x._1.toLong -> x._2).sortBy(x => x._1).toArray,
        daily.map(x => x._1.toLong -> x._3).sortBy(x => x._1).toArray,
        daily.map(x => x._1.toLong -> x._4).sortBy(x => x._1).toArray,
        daily.map(x => x._1.toLong -> x._5).sortBy(x => x._1).toArray,
        daily.map(x => x._1.toLong -> x._6).sortBy(x => x._1).toArray
        //Common.reduceByKeyInt(daily.map(x => DateTimeUtil.create(x._1.toLong / 1000).getDayOfMonth -> x._3)),
        //Common.reduceByKeyInt(daily.map(x => DateTimeUtil.create(x._1.toLong / 1000).getDayOfMonth -> x._4)),
        //Common.reduceByKeyInt(daily.map(x => DateTimeUtil.create(x._1.toLong / 1000).getDayOfMonth -> x._5)),
        //Common.reduceByKeyInt(daily.map(x => DateTimeUtil.create(x._1.toLong / 1000).getDayOfMonth -> x._6))
      )
}

case class History(
    _type: String,
    numberOfContract: Long,
    numberOfSession: Long,
    hourly: Hourly,
    dayOfWeek: DayOfWeek,
    daily: Daily,
    status: Array[(String, Int)],
    contracts: Array[(String, Long, Long, Long, Long, Long)],
    provinces: Array[(String, Long, Long, Long, Long, Long)],
    regions: Array[(String, Long, Long, Long, Long, Long)],
    sessions: Array[(String, Long, Long, Long, Long, Long)])
    
case class HistoryContract(
    numberOfDevice: Long,
    numberOfSession: Long,
    hourly: Hourly,
    dayOfWeek: DayOfWeek,
    daily: Daily,
    status: Array[(String, Int)],
    sessions: Array[(String, Long, Long, Long)],
    macs: Array[(String, Long, Long)],
    logs: Array[(String, String, Long, Long, Long, String)])
