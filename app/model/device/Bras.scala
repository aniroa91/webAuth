package model.device

import play.api.Play
import play.api.data.Form
import play.api.data.Forms._
import play.api.db.slick.DatabaseConfigProvider
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormat
import scala.concurrent.Future
import slick.driver.JdbcProfile
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global

case class Bras(id: String, time: String,logoff: Int, signin: Int,lostip_error: Int, active_user: Int,crit_kibana: Int, crit_opsview: Int,cpe_error: Int, label: String,verified: Option[String])

class BrasTableDef(tag: Tag) extends Table[Bras](tag, "dwh_radius_bras_detail") {

  def id = column[String]("bras_id")
  def time = column[String]("date_time")
  def logoff = column[Int]("logoff_total_count")
  def signin = column[Int]("signin_total_count")
  def lostip_error = column[Int]("lostip_error")
  def active_user = column[Int]("active_user")
  def crit_kibana = column[Int]("crit_kibana")
  def crit_opsview = column[Int]("crit_opsview")
  def cpe_error = column[Int]("cpe_error")
  def label = column[String]("label")
  def verified = column[Option[String]]("verified")

  override def * =
    (id, time,logoff, signin,lostip_error, active_user,crit_kibana, crit_opsview,cpe_error, label,verified) <>(Bras.tupled, Bras.unapply)
}

object BrasList {

  val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)

  var brases = TableQuery[BrasTableDef]

  def top100: Future[Seq[(String,String,String,String,String, String, String, String,String,Option[String])]] = {
    val dt = new DateTime();
    val oneDayLater = dt.minusHours(48);
    val oldDay  = oneDayLater.toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS"))
   // dbConfig.db.run(brases.sortBy(_.time.desc).filter(_.time >= oldDay).filter(_.label ==="outlier").take(100).result)
    dbConfig.db.run(
      sql"""select tbD.bras_id,tbD.date_time,tbD.active_user,tbD.signin_total_count,tbD.logoff_total_count,tbD.lostip_error,tbD.cpe_error,tbD.crit_kibana,tbD.crit_opsview,tbD.verified
            from
            (select bras_id,max(date_time) as time from dwh_radius_bras_detail
            where label='outlier'
            group by bras_id) tbB
            join dwh_radius_bras_detail tbD
            on tbD.bras_id = tbB.bras_id and tbD.date_time=tbB.time and tbD.date_time>=$oldDay::TIMESTAMP
            order by tbD.date_time desc
                  """
        .as[(String, String, String, String,String,String, String, String, String,Option[String])])
  }

 /*  def get(id: String,time: String): Future[Option[Bras]] = {
    dbConfig.db.run(brases.filter(_.id === id).filter(_.time === time).result.headOption)
  }*/

  def getTime(id: String,time: String): Future[Seq[Bras]] = {
    val strTime = time.substring(0,time.indexOf(".")+3)
    val formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS")
    val dateTime = DateTime.parse(strTime, formatter)
    val oldTime  = dateTime.minusHours(1).toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS"))

    dbConfig.db.run(brases.filter(_.id === id).filter(_.time >= oldTime).filter(_.time <= time).filter(_.label ==="outlier").sortBy(_.time.desc).take(5).result)
  }

  def getChart(id: String,time: String): Future[Seq[Bras]] = {
    val strTime = time.substring(0,time.indexOf(".")+3)
    val formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS")
    val dateTime = DateTime.parse(strTime, formatter)
    val oldHalfHour  = dateTime.minusMinutes(30).toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS"))
    val addHalfHour  = dateTime.plusMinutes(30).toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss"))

    dbConfig.db.run(brases.filter(_.id === id).filter(_.time >= oldHalfHour).filter(_.time <= addHalfHour).sortBy(_.time.asc).result)
  }

  def getJsonChart(id: String,time: String): Future[Seq[(String,Int, Int,Int)]] = {
    val formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS")
    val dateTime = DateTime.parse(time, formatter)
    val oldHalfHour  = dateTime.minusMinutes(30).toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS"))

    dbConfig.db.run(
      sql"""SELECT date_time,logoff_total_count,signin_total_count,active_user FROM dwh_radius_bras_detail
            WHERE bras_id =$id and date_time >=$oldHalfHour::TIMESTAMP and date_time >=$time::TIMESTAMP
            ORDER BY date_time desc
                  """
        .as[(String, Int, Int,Int)])
    //dbConfig.db.run(brases.filter(_.id === id).filter(_.time >= oldHalfHour).filter(_.time <= addHalfHour).sortBy(_.time.asc).result)
  }

  def confirmLabel(id: String,time: String) = {
    dbConfig.db.run(
      sqlu"""UPDATE dwh_radius_bras_detail SET verified =1
            where bras_id =$id and date_time>=$time::TIMESTAMP
                  """
    )
  }

  def rejectLabel(id: String,time: String) = {
    dbConfig.db.run(
      sqlu"""UPDATE dwh_radius_bras_detail SET verified =0
            where bras_id =$id and date_time>=$time::TIMESTAMP
                  """
    )
  }

}