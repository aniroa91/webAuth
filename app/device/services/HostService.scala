package service

import model.device._
import services.domain.AbstractService
import scala.concurrent.Future

object HostService extends AbstractService{

  def getInfHostDailyResponse(host: String,nowDay: String): Array[(String,Int,Int,Int,Int,Int,Int,Int)] = {
    InfDAO.getInfHostDailyResponse(host,nowDay)
  }

  def getNoOutlierInfByHost(host: String,nowDay: String): Int = {
    InfDAO.getNoOutlierInfByHost(host,nowDay)
  }

  def getNoOutlierInfByBras(bras: String,nowDay: String): Int = {
    InfDAO.getNoOutlierInfByBras(bras,nowDay)
  }

  def getSigLogbyModuleIndex(host: String,nowDay: String):  Array[((String,String),String)]= {
    InfDAO.getSigLogbyModuleIndex(host,nowDay)
  }

  def getErrorHostbyHourly(host: String,nowDay: String): Array[(Int,Int,Int,Int,Int,Int,Int,Int)] = {
    InfDAO.getErrorHostbyHourly(host,nowDay)
  }

  def getSuyhaobyModule(host: String,nowDay: String):Future[Seq[(String,Double,Double,Double)]] = {
    InfDAO.getSuyhaobyModule(host,nowDay)
  }

  def getSiglogByHourly(host: String,nowDay: String): SigLogByTime = {
    InfDAO.getSiglogByHourly(host,nowDay)
  }

  def getSplitterByHost(host: String,nowDay: String): Future[Seq[(String,String,Int)]] = {
    InfDAO.getSplitterByHost(host,nowDay)
  }

  def getErrorTableModuleIndex(host: String,nowDay: String): Array[(String,Int,Int)] = {
    InfDAO.getErrorTableModuleIndex(host,nowDay)
  }

  def getPortPonDown(host: String,nowDay: String): Future[Seq[(String,String,String)]] = {
    InfDAO.getPortPonDown(host,nowDay)
  }

  def getTicketOutlierByHost(host: String,nowDay: String): Future[Seq[(String,String)]] = {
    InfDAO.getTicketOutlierByHost(host,nowDay)
  }

}