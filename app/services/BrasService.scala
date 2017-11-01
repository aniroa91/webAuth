package service

import model.device.{Bras,BrasCard, BrasList,BrasesCard}
import scala.concurrent.Future

object BrasService {
  def listTop100Bras: Future[Seq[Bras]] = {
    BrasList.top100
  }

  def listBrasOutlier: Future[Seq[(String,String,String,String)]] = {
    BrasesCard.listBrasOutlier
  }

  def opViewKibana(id : String,time: String,oldTime: String): Future[Seq[(String,String,String,String)]] = {
    BrasesCard.opViewKibana(id,time,oldTime)
  }

  def getBrasTime(id : String,time: String) : Future[Seq[Bras]] = {
    BrasList.getTime(id,time)
  }

  def getBrasChart(id : String,time: String) : Future[Seq[Bras]] = {
    BrasList.getChart(id,time)
  }

  def getBrasCard(id : String,time: String,sigin: String,logoff: String) : Future[Seq[(String,String,String,String,Int,Int)]] = {
    BrasesCard.getCard(id,time,sigin,logoff)
  }

}