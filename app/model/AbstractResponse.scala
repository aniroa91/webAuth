package model

import com.ftel.bigdata.dns.parameters.Label

case class MainDomainInfo(
    day: String,
    name: String,
    label: String,
    malware: String,
    queries: Int,
    domains: Int,
    clients: Int,
    rankFtel: Int,
    rankAlexa: Int) {
//  def getNumOfQuery(): String = {
//      DomainService.formatNumber(numOfQuery)
//    }
//  def getNumOfClient(): String = {
//      DomainService.formatNumber(numOfClient)
//    }
//  def getQueryPerClient(): String = {
//      DomainService.formatNumber(numOfQuery / numOfClient)
//    }
  def getQueryPerClient(): Int = {
      queries / clients
  }
  def this(day: String, name: String, malware: String, queries: Int, clients: Int, rankFtel: Int, rankAlexa: Int) = 
    this(day, name, Label.getLabelFrom(malware), malware, queries, 0, clients, rankFtel, rankAlexa)
    
  def this(that: MainDomainInfo, domains: Int) = 
    this(that.day, that.name, that.label, that.malware, that.queries, domains, that.clients, that.rankFtel, that.rankAlexa)
}

case class TotalInfo(
    queries: Int,
    domains: Int,
    clients: Int,
    malwares: Int,
    success: Int,
    failed: Int,
    seconds: Int)

case class MalwareInfo(malware: String, queries: Int, domains: Int, clients: Int)
    
abstract class AbstractResponse {
  
}