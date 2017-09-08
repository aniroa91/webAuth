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
  
  def this(name: String, queries: Int) = this("", name, "", "", queries, 0, 0, 0, 0)

}

case class TotalInfo(
    queries: Int,
    domains: Int,
    clients: Int,
    malwares: Int,
    success: Int,
    failed: Int,
    seconds: Int) {
  def clone(numOfClients: Int) = TotalInfo(queries, domains, numOfClients, malwares, success, failed, seconds)
  def this() = this(0,0,0,0,0,0,0)
}

case class MalwareInfo(malware: String, queries: Int, domains: Int, clients: Int) {
  def this(malware: String, queries: Int) = this(malware, queries, 0, 0)
}

//private def indexLocation(client: HttpClient, domain: String) {
//    
//    val time0 = System.currentTimeMillis()
//    val getResponse = client.execute(com.sksamuel.elastic4s.http.ElasticDsl.get(domain) from "dns-location/docs").await
//    if (!getResponse.exists) {
//      val url = IP_API_URL + domain
//      //val content = HttpUtil.getContent(url, "172.30.45.220", 80)
//      val content = HttpUtil.getContent(url)
//      //println(content)
//      client.execute(indexInto("dns-location" / "docs") doc (content) id domain ).await
//      Thread.sleep(300)
//    }
//    val time1 = System.currentTimeMillis()
//    //println(time1 - time0)
//  }

case class DomainLocation(second: String, ip: String, country: String, region: String, city: String, timezone: String, org: String, lat: String, lon: String) {
  
}

case class Bubble(x: Int, y: Int, z: Int, name: String)

abstract class AbstractResponse {
  
}

