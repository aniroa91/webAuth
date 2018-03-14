package profile.services.internet

import profile.services.internet.response.History
import profile.services.internet.response.HistoryContract

object CompareService {
  def getDay(days: Array[String]): Array[(String, History)] = {
    val a = days.map(x => x-> HistoryService.getAll("day", s"$x/$x"))
    a
  }
  
  def getWeek(days: Array[String]): Array[(String, History)] = {
    days.map(x => x-> HistoryService.getAll("week", s"$x"))
  }
  
  def getMonth(days: Array[String]): Array[(String, History)] = {
    days.map(x => x-> HistoryService.getAll("month", s"$x"))
  }
  
  
  def getContractDay(contract: String, days: Array[String]): Array[(String, HistoryContract)] = {
    days.map(x => x-> HistoryService.getContract("day", s"$x/$x", contract))
  }
  
  def getContractWeek(contract: String, days: Array[String]): Array[(String, HistoryContract)] = {
    days.map(x => x-> HistoryService.getContract("week", s"$x", contract))
  }
  
  def getContractMonth(contract: String, days: Array[String]): Array[(String, HistoryContract)] = {
    days.map(x => x-> HistoryService.getContract("month", s"$x", contract))
  }
}