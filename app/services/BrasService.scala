package service

import model.device.{Bras, BrasList,BrasesCard,BrasDAO}
import scala.concurrent.Future

object BrasService {

  // for page Search Bras
  def getSigLogResponse(bras: String,nowDay: String):Future[Seq[(Int,Int)]] = {
    BrasDAO.getSigLogResponse(bras,nowDay)
  }

  def getNoOutlierResponse(bras: String,nowDay: String):Future[Seq[(Int)]] = {
    BrasDAO.getNoOutlierResponse(bras,nowDay)
  }

  def getOpviewBytimeResponse(bras: String,nowDay: String,hourly: Int):Future[Seq[(Int,Int)]] = {
    BrasDAO.getOpviewBytimeResponse(bras,nowDay,hourly)
  }

  def getKibanaBytimeResponse(bras: String,nowDay: String,hourly: Int):Future[Seq[(Int,Int)]] = {
    BrasDAO.getKibanaBytimeResponse(bras,nowDay,hourly)
  }

  def getSigLogBytimeResponse(bras: String,nowDay: String,hourly: Int):Future[Seq[(Int,Int,Int)]] = {
    BrasDAO.getSigLogBytimeResponse(bras,nowDay,hourly)
  }

  def getInfErrorBytimeResponse(bras: String,nowDay: String,hourly: Int):Future[Seq[(Int,Int)]] = {
    BrasDAO.getInfErrorBytimeResponse(bras,nowDay,hourly)
  }

  def getInfhostResponse(bras: String,nowDay: String):Future[Seq[(String,Int,Int)]] = {
    BrasDAO.getInfhostResponse(bras,nowDay)
  }

  def getInfModuleResponse(bras: String,nowDay: String):Future[Seq[(String,String,Int,Int,Int)]] = {
    BrasDAO.getInfModuleResponse(bras,nowDay)
  }

  def getOpsviewServiceSttResponse(bras: String,nowDay: String):Future[Seq[(String,Int)]] = {
    BrasDAO.getOpsviewServiceSttResponse(bras,nowDay)
  }

  def getOpServByStatusResponse(bras: String,nowDay: String):Future[Seq[(String,String,Int)]] = {
    BrasDAO.getOpServByStatusResponse(bras,nowDay)
  }

  def getLinecardhostResponse(bras: String,nowDay: String):Future[Seq[(String,Int,Int)]] = {
    BrasDAO.getLinecardhostResponse(bras,nowDay)
  }

  // end page Search Bras

  def listTop100Bras: Future[Seq[(String,String,String,String,String, String, String, String,String,Option[String])]] = {
    BrasList.top100
  }

  def listNocOutlier: Future[Seq[(String,String)]] = {
    BrasesCard.listNocOutlier
  }

  def listBrasOutlier: Future[Seq[(String,String,String,String,String)]] = {
    BrasesCard.listBrasOutlier
  }

  def listBrasById(id: String): Future[Seq[(String,String,String,String,String,Int,Int)]] = {
    BrasesCard.listBrasById(id)
  }

  def getHostBras(id: String): Future[Seq[(String,String,String, String)]] = {
    BrasesCard.getHostCard(id)
  }

  def confirmLabel(id: String,time: String) = {
    BrasList.confirmLabel(id,time)
  }

  def rejectLabel(id: String,time: String) = {
    BrasList.rejectLabel(id,time)
  }

  def opViewKibana(id : String,time: String,oldTime: String): Future[Seq[(String,String,String,String)]] = {
    BrasesCard.opViewKibana(id,time,oldTime)
  }

  def getNumLogSiginById(id : String,time: String): Future[Seq[(Int,Int)]] = {
    BrasesCard.getNumLogSiginById(id,time)
  }

  def getBrasTime(id : String,time: String) : Future[Seq[Bras]] = {
    BrasList.getTime(id,time)
  }

  def getBrasChart(id : String,time: String) : Future[Seq[Bras]] = {
    BrasList.getChart(id,time)
  }

  def getJsonBrasChart(id : String,time: String) : Future[Seq[(String,Int, Int,Int)]] = {
    BrasList.getJsonChart(id,time)
  }

  def getBrasCard(id : String,time: String,sigin: String,logoff: String) : Future[Seq[(String,String,String,Int,Int)]] = {
    BrasesCard.getCard(id,time,sigin,logoff)
  }

}