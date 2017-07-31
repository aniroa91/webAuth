package model

import com.google.gson.JsonObject
import com.google.gson.JsonArray
import com.google.gson.Gson
import services.DomainService

case class BaicInfo(day: String, numOfQuery: Int, numOfClient: Int, /*numOfDomain: String,*/ label: String, malware: String, rankFtel: Int, rankAlexa: Int) {
  def getNumOfQuery(): String = {
      DomainService.formatNumber(numOfQuery)
    }
  def getNumOfClient(): String = {
      DomainService.formatNumber(numOfClient)
    }
}
//case class HistoryInfo(day: String, baicInfos: Array[String])
case class Response(whois: WhoisObject, basicInfo: BaicInfo, answers: Array[String], history: Array[BaicInfo], numOfDomain: Int) {
  def toJsonObject: JsonObject = {
    val jo = new JsonObject()
    val ja = new JsonArray()
    val gson = new Gson()
    val jsonObjectCurrent = new JsonObject()
    // Convert History to JsonArray
    history.map(x => {
      val json = new JsonObject()
      json.addProperty("day", x.day)
      json.addProperty("label", x.label)
      json.addProperty("malware", x.malware)
      json.addProperty("numOfQuery", x.numOfQuery)
      json.addProperty("numOfClient", x.numOfClient)
      json.addProperty("rankFtel", x.rankFtel)
      json.addProperty("rankAlexa", x.rankAlexa)
      ja.add(json)
    })
    
  
//
//    
////    registrar: String,
////    whoisServer: String,
////    referral: String,
////    nameServer: Array[String],
////    status: String,
////    update: String,
////    create: String,
////    expire: String,
//    
    // Add whois Info
    jsonObjectCurrent.addProperty("registrar", whois.registrar)
    jsonObjectCurrent.addProperty("whoisServer", whois.whoisServer)
    jsonObjectCurrent.addProperty("referral", whois.referral)
    jsonObjectCurrent.addProperty("nameServer", whois.nameServer.mkString(" "))
    jsonObjectCurrent.addProperty("status", whois.status)
    jsonObjectCurrent.addProperty("create", whois.create)
    jsonObjectCurrent.addProperty("update", whois.update)
    jsonObjectCurrent.addProperty("expire", whois.expire)
    jsonObjectCurrent.addProperty("numOfDomain", numOfDomain)
    
//    
//    
    jo.add("current", jsonObjectCurrent)
    jo.add("history", ja)
//    val answers = answerResponse.hits.hits.map(x => x.sourceAsMap.getOrElse("answer", "").toString()).filter(x => x != "")
    jo.addProperty("answer", answers.mkString(" "))
//
    jo
  }
  
  def getNumOfDomain(): String = {
      DomainService.formatNumber(numOfDomain)
    }
}