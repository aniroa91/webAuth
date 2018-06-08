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

object HostService extends AbstractService{

  def getInfHostDailyResponse(host: String,nowDay: String): Future[Seq[(String,Int,Int,Int,Int,Int,Int)]] = {
    InfDAO.getInfHostDailyResponse(host,nowDay)
  }

  def getNoOutlierInfByHost(host: String,nowDay: String): Future[Seq[(Int)]] = {
    InfDAO.getNoOutlierInfByHost(host,nowDay)
  }

  def getNoOutlierInfByBras(bras: String,nowDay: String): Future[Seq[(Int)]] = {
    InfDAO.getNoOutlierInfByBras(bras,nowDay)
  }

  def getSigLogbyModuleIndex(host: String,nowDay: String):  Array[((String,String),String)]= {
    InfDAO.getSigLogbyModuleIndex(host,nowDay)
  }

  def getErrorHostbyHourly(host: String,nowDay: String): Future[Seq[(String,Int,Int,Int,Int,Int,Int)]] = {
    InfDAO.getErrorHostbyHourly(host,nowDay)
  }

  def getSuyhaobyModule(host: String,nowDay: String):Future[Seq[(String,Double,Double,Double)]] = {
    InfDAO.getSuyhaobyModule(host,nowDay)
  }

  def getSiglogByHourly(host: String,nowDay: String): SigLogByTime = {
    InfDAO.getSiglogByHourly(host,nowDay)
  }

  def getSplitterByHost(host: String,nowDay: String): Future[Seq[(String,Int)]] = {
    InfDAO.getSplitterByHost(host,nowDay)
  }

  def getErrorTableModuleIndex(host: String,nowDay: String): Future[Seq[(String,Int,Int)]] = {
    InfDAO.getErrorTableModuleIndex(host,nowDay)
  }

  def getContractwithSf(host: String,nowDay: String): Future[Seq[(String,Int,Int,Int,Int)]] = {
    InfDAO.getContractwithSf(host,nowDay)
  }

}