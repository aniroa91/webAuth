package profile.services.internet

import com.ftel.bigdata.utils.DateTimeUtil
import com.ftel.bigdata.utils.StringUtil
import org.joda.time.Days
import org.joda.time.DateTimeConstants
import services.Configure
import com.sksamuel.elastic4s.http.ElasticDsl._

object Common {
  
  val client = Configure.client
  
  def humanReadableByteCount(bytes: Long): String = {
    humanReadableByteCount(bytes, true)
  }
  
  def humanReadableByteCount(bytes: Long, si: Boolean): String = {
    val unit = if (si) 1000 else 1024
    if (bytes < unit) {
      bytes + " B";
    } else {
      val exp = (Math.log(bytes) / Math.log(unit)).toInt
      val pre = (if (si) "kMGTPE" else "KMGTPE").charAt(exp - 1) + (if (si) "" else "i")
      f"${bytes / Math.pow(unit, exp)}%.1f " + pre + "B"
    }
    //String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");
    //return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
  }
  
  def bytesToGigabytes(bytes: Long): Double = {
    bytesTo(bytes, true, 3)
  }
  
  def bytesToTerabytes(bytes: Long): Double = {
    bytesTo(bytes, true, 4)
  }
  
  def bytesToPetabytes(bytes: Long): Double = {
    bytesTo(bytes, true, 5)
  }
  
  def bytesTo(bytes: Long, si: Boolean, exp: Int): Double = {
    val unit = if (si) 1000 else 1024
    val number = bytes / Math.pow(unit, exp)
    BigDecimal(number).setScale(3, BigDecimal.RoundingMode.HALF_UP).toDouble
  }
  
  def getIndexString(indexParttern: String, date: String): String = {
    if (StringUtil.isNullOrEmpty(date)) {
      s"${indexParttern}-2018-02-01"
    } else {
    val arr = date.split("/")
    val days = (0 until Days.daysBetween(DateTimeUtil.create(arr(0), DateTimeUtil.YMD), DateTimeUtil.create(arr(1), DateTimeUtil.YMD)).getDays + 1)
      .map(x => DateTimeUtil.create(arr(0), DateTimeUtil.YMD).plusDays(x).toString(DateTimeUtil.YMD))
      days.map(x => s"${indexParttern}-${x}").filter(x => client.execute(indexExists(x)).await.isExists).mkString(",")
    }
  }
  
  def getRangeDateForWeek(day: String): String = {
    DateTimeUtil.create(day, DateTimeUtil.YMD).withDayOfWeek(DateTimeConstants.MONDAY).toString(DateTimeUtil.YMD) + 
    "/" +
    DateTimeUtil.create(day, DateTimeUtil.YMD).withDayOfWeek(DateTimeConstants.SUNDAY).toString(DateTimeUtil.YMD)
  }
  
  def getRangeDateForMonth(day: String): String = {
    DateTimeUtil.create(day, DateTimeUtil.YMD).dayOfMonth().withMinimumValue().toString(DateTimeUtil.YMD) + 
    "/" +
    DateTimeUtil.create(day, DateTimeUtil.YMD).dayOfMonth().withMaximumValue().toString(DateTimeUtil.YMD)
  }
  
  def reduceByKeyString(array: Array[(String, Long)]): Array[(String, Long)] = {
    array.groupBy(x => x._1)
         .map(x => x._1 -> x._2.map(y => y._2).sum)
         .toArray
         .sortBy(x => x._1)
  }
  
  def reduceByKeyInt(array: Array[(Int, Long)]): Array[(Int, Long)] = {
    array.groupBy(x => x._1)
         .map(x => x._1 -> x._2.map(y => y._2).sum)
         .toArray
         .sortBy(x => x._1)
  }
  
  
  def main(args: Array[String]) {
    println(humanReadableByteCount(1855425871872L , true))
    println(humanReadableByteCount(9223372036854775807L , true))
  }
}