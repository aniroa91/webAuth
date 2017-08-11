package model

import com.ftel.bigdata.whois.Whois
import com.google.gson.JsonObject
import com.google.gson.JsonArray
import com.google.gson.Gson

case class ProfileResponse (
    whois: Whois,
    current: MainDomainInfo,
    history: Array[MainDomainInfo],
    answers: Array[String]) extends AbstractResponse {
  def toJsonObject: JsonObject = ???
  /*
  def toJsonObject: JsonObject = if (current != null) {
    val jo = new JsonObject()
    val ja = new JsonArray()
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
  */
}