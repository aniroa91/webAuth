package model.device

import play.api.Play
import play.api.data.Form
import play.api.data.Forms._
import play.api.db.slick.DatabaseConfigProvider
import scala.concurrent.Future
import slick.driver.JdbcProfile
import slick.driver.PostgresDriver.api._
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

case class BrasCard(id: String, time: String,card: String, linecard: String,logoff: Int,signin: Int)

class BrasCardTableDef(tag: Tag) extends Table[BrasCard](tag, "bras_count_by_card") {

  def id = column[String]("bras_id")
  def time = column[String]("time")
  def card = column[String]("card_ol")
  def linecard = column[String]("line_ol")
  def logoff = column[Int]("logoff_total_count_by_card")
  def signin = column[Int]("signin_total_count_by_card")

  override def * =
    (id, time,card, linecard,logoff,signin) <>(BrasCard.tupled, BrasCard.unapply)
}

object BrasesCard {

  val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)

  var brases = TableQuery[BrasCardTableDef]

  def listBrasOutlier: Future[Seq[(String,String,String,String)]] = {
   // try {
      val dt = new DateTime();
      val threeHoursLater = dt.minusHours(6);
      val threeTime = threeHoursLater.toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS"))
      dbConfig.db.run(
        sql"""SELECT tbC.bras_id,tbC.time,tbC.line_ol,tbC.card_ol
                 FROM (SELECT bras_id,date_time FROM dwh_radius_bras_detail2 WHERE label = 'outlier' and date_time >= $threeTime::TIMESTAMP) tbB join
                 (SELECT * FROM bras_count_by_card WHERE time >= $threeTime::TIMESTAMP) tbC
                  on tbB.bras_id=tbC.bras_id and to_char(tbB.date_time, 'YYYY-MM-DD HH:mm:ss') = to_char(tbC.time, 'YYYY-MM-DD HH:mm:ss')
                  ORDER BY tbC.bras_id desc,tbC.line_ol desc,tbC.time desc limit 10
                  """
          .as[(String, String, String, String)])
    //}finally dbConfig.db.close
  }

  def opViewKibana(id : String,time: String,oldTime: String) : Future[Seq[(String,String,String,String)]] = {
    dbConfig.db.run(
      sql"""SELECT tbK.error_name,tbK.error_level,tbO.service_name,tbO.service_status
            FROM
                (select * from public.dwh_kibana
                 where bras_id=$id and date_time>=$oldTime::TIMESTAMP and date_time <=$time::TIMESTAMP
                 order by date_time desc
                ) tbK left join
                (select * from public.dwh_opsview
                 where bras_id=$id and date_time>=$oldTime::TIMESTAMP and date_time <=$time::TIMESTAMP
                 order by date_time desc
                ) tbO on tbO.bras_id = tbK.bras_id and tbO.date_time = tbK.date_time
            LIMIT 5
         """
        .as[(String, String,String,String)])
  }

  def getCard(id: String,time: String,sigin: String,logoff: String): Future[Seq[(String,String,String,String,Int,Int)]] = {
      dbConfig.db.run(
        sql"""SELECT bras_id,time,line_ol, card_ol,SUM(logoff_total_count_by_card),SUM(signin_total_count_by_card)
              FROM bras_count_by_card WHERE bras_id=$id AND time = $time::TIMESTAMP GROUP BY bras_id,time,line_ol, card_ol ORDER BY line_ol, card_ol"""
        .as[(String,String,String,String,Int,Int)])
  }
}
