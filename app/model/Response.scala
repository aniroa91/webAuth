package model

import com.google.gson.JsonObject
import com.google.gson.JsonArray
import com.google.gson.Gson
import services.DomainService
import com.ftel.bigdata.dns.parameters.Label

//import com.ftel.bigdata.dns.model.table.WhoisObject

case class BasicInfo(day: String, numOfQuery: Int, numOfClient: Int, /*numOfDomain: String,*/ label: String, malware: String, rankFtel: Int, rankAlexa: Int) {
  def getNumOfQuery(): String = {
      DomainService.formatNumber(numOfQuery)
    }
  def getNumOfClient(): String = {
      DomainService.formatNumber(numOfClient)
    }
  def getQueryPerClient(): String = {
      DomainService.formatNumber(numOfQuery / numOfClient)
    }
  def this(day: String, numOfQuery: Int, numOfClient: Int, malware: String, rankFtel: Int, rankAlexa: Int) = 
    this(day, numOfQuery, numOfClient, Label.getLabelFrom(malware), malware, rankFtel, rankAlexa)
}

//case class HistoryInfo(day: String, baicInfos: Array[String])
case class Response(
    whois: com.ftel.bigdata.dns.model.table.WhoisObject,
    basicInfo: BasicInfo,
    answers: Array[String],
    history: Array[BasicInfo],
    numOfDomain: Int) {
  def toJsonObject: JsonObject = if (basicInfo != null) {
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

    jsonObjectCurrent.addProperty("day", basicInfo.day)
    jsonObjectCurrent.addProperty("label", basicInfo.label)
    jsonObjectCurrent.addProperty("malware", basicInfo.malware)
    jsonObjectCurrent.addProperty("numOfQuery", basicInfo.numOfQuery)
    jsonObjectCurrent.addProperty("numOfClient", basicInfo.numOfClient)
    jsonObjectCurrent.addProperty("rankFtel", basicInfo.rankFtel)
    jsonObjectCurrent.addProperty("rankAlexa", basicInfo.rankAlexa)

    jo.add("current", jsonObjectCurrent)
    jo.add("history", ja)
    jo.addProperty("answer", answers.mkString(" "))
    jo
  } else new JsonObject()
  
  def getNumOfDomain(): String = {
      DomainService.formatNumber(numOfDomain)
    }
}