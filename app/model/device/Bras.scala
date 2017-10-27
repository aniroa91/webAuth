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

case class Bras(id: String, time: String,logoff: Int, signin: Int,lostip_error: Int, active_user: Int,crit_kibana: Int, crit_opsview: Int,cpe_error: Int, label: String)

class BrasTableDef(tag: Tag) extends Table[Bras](tag, "dwh_radius_bras_detail2") {

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

  override def * =
    (id, time,logoff, signin,lostip_error, active_user,crit_kibana, crit_opsview,cpe_error, label) <>(Bras.tupled, Bras.unapply)
}

object BrasList {

  val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)

  var brases = TableQuery[BrasTableDef]

  def top100: Future[Seq[Bras]] = {
    val dt = new DateTime();
    val oneHoursLater = dt.minusHours(24);
    val oldHour  = oneHoursLater.toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS"))
    dbConfig.db.run(brases.sortBy(_.time.desc).filter(_.time >= oldHour).filter(_.label ==="outlier").take(100).result)
  }

/*  def get(id: String,time: String): Future[Option[Bras]] = {
    dbConfig.db.run(brases.filter(_.id === id).filter(_.time === time).result.headOption)
  }*/

  def getTime(id: String,time: String): Future[Seq[Bras]] = {
    val formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS")
    val dateTime = DateTime.parse(time, formatter)
    val oldTime  = dateTime.minusHours(1).toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS"))

    dbConfig.db.run(brases.filter(_.id === id).filter(_.time >= oldTime).filter(_.time <= time).sortBy(_.time.desc).take(5).result)
  }

  def getChart(id: String,time: String): Future[Seq[Bras]] = {
    val formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS")
    val dateTime = DateTime.parse(time, formatter)
    val oldHalfHour  = dateTime.minusMinutes(30).toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS"))
    val addHalfHour  = dateTime.plusMinutes(30).toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss"))

    dbConfig.db.run(brases.filter(_.id === id).filter(_.time >= oldHalfHour).filter(_.time <= addHalfHour).sortBy(_.time.asc).result)
  }

}