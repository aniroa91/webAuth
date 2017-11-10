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

object BrasesCard {

  val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)

  def getHostCard(strId: String): Future[Seq[(String,String,String, String)]] = {
    val id = strId.split('/')(0)
    val time = strId.split('/')(1)
    val formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS")
    val dateTime = DateTime.parse(time, formatter)
    val oldTime  = dateTime.minusMinutes(30).toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS"))
    dbConfig.db.run(
      sql"""SELECT host,module,sum(cpe_error),sum(lostip_error) from dwh_inf_module
            WHERE bras_id=$id and date_time<=$time::TIMESTAMP and date_time>=$oldTime::TIMESTAMP
            GROUP BY host,module
                  """
        .as[(String, String,String, String)])
  }

  def listNocOutlier: Future[Seq[(String,String)]] = {
    val dt = new DateTime();
    val aHourLater = dt.minusHours(24);
    val aHourTime = aHourLater.toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS"))
    dbConfig.db.run(
      sql"""SELECT tb.province,count(distinct tb.numBras)
            FROM(
              select substring(bras_id,1, 3) as province,substring(bras_id,5, length(bras_id)) as numBras  from dwh_radius_bras_detail
              where date_time>=$aHourTime::TIMESTAMP and label ='outlier'
              order by date_time desc
              ) tb
            GROUP BY tb.province
                  """
        .as[(String,String)])
  }

  def listBrasById(id: String): Future[Seq[(String,String,String,String,String)]] = {
    val dt = new DateTime();
    val aDayLater = dt.minusMinutes(60);
    val aDayTime = aDayLater.toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS"))
    dbConfig.db.run(
      sql"""SELECT distinct tbC.bras_id,tbB.date_time,tbC.line_ol,tbC.card_ol,tbC.port_ol
             FROM (SELECT bras_id,date_time FROM dwh_radius_bras_detail WHERE date_time >= $aDayTime::TIMESTAMP and label = 'outlier' and bras_id  LIKE $id || '%') tbB join
               (SELECT * FROM bras_count_by_port WHERE bras_id LIKE  $id || '%') tbC
             on tbB.bras_id=tbC.bras_id and  date_trunc('minute', tbC.time) between date_trunc('minute', tbB.date_time) - INTERVAL '3' MINUTE and date_trunc('minute', tbB.date_time)
             ORDER BY tbC.bras_id desc,tbC.line_ol desc,tbC.port_ol desc,tbB.date_time desc
                  """
        .as[(String, String, String, String,String)])
  }

  def listBrasOutlier: Future[Seq[(String,String,String,String,String)]] = {
   // try {
      val dt = new DateTime();
      val aDayLater = dt.minusHours(1);
      val aDayTime = aDayLater.toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS"))
      dbConfig.db.run(
        sql"""SELECT tbC.bras_id,tbC.time,tbC.line_ol,tbC.card_ol,tbC.port_ol
                 FROM (SELECT bras_id,date_time FROM dwh_radius_bras_detail WHERE label = 'outlier' and date_time >= $aDayTime::TIMESTAMP) tbB join
                 (SELECT * FROM bras_count_by_port WHERE time >= $aDayTime::TIMESTAMP) tbC
                  on tbB.bras_id=tbC.bras_id and to_char(tbB.date_time, 'YYYY-MM-DD HH:mm:ss') = to_char(tbC.time, 'YYYY-MM-DD HH:mm:ss')
                  ORDER BY tbC.bras_id desc,tbC.line_ol desc,tbC.port_ol desc,tbC.time desc limit 10
                  """
          .as[(String, String, String, String,String)])
    //}finally dbConfig.db.close
  }

  def opViewKibana(id : String,time: String,oldTime: String) : Future[Seq[(String,String,String,String)]] = {
    dbConfig.db.run(
      sql"""SELECT distinct tbK.error_name,tbK.error_level,tbO.service_name,tbO.service_status
            FROM
                (select * from public.dwh_kibana
                 where bras_id=$id and date_time>=$oldTime::TIMESTAMP and date_time <=$time::TIMESTAMP
                 order by date_time desc
                ) tbK left join
                (select * from public.dwh_opsview
                 where bras_id=$id and date_time>=$oldTime::TIMESTAMP and date_time <=$time::TIMESTAMP
                 order by date_time desc
                ) tbO on tbO.bras_id = tbK.bras_id and date_trunc('minute', tbO.date_time)=date_trunc('minute', tbK.date_time)
         """
        .as[(String, String,String,String)])
  }

  def getCard(id: String,time: String,sigin: String,logoff: String): Future[Seq[(String,String,String,Int,Int)]] = {
    val formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS")
    val dateTime = DateTime.parse(time, formatter)
    val oldTime  = dateTime.minusMinutes(5).toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS"))
    val newTime  = dateTime.plusMinutes(5).toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS"))
      dbConfig.db.run(
        sql"""SELECT bras_id,line_ol, card_ol,SUM(logoff_total_count_by_card),SUM(signin_total_count_by_card)
              FROM bras_count_by_card WHERE bras_id=$id AND time>=$oldTime::TIMESTAMP AND time <= $newTime::TIMESTAMP GROUP BY bras_id,line_ol, card_ol ORDER BY line_ol, card_ol"""
        .as[(String,String,String,Int,Int)])
  }

}
