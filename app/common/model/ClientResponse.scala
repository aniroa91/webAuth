package model

import com.google.gson.JsonObject

case class HistoryRow(domain: String, second: String, label: String, queries: Int, rCode: String)
case class HistoryHour(hour: String, rows: Array[HistoryRow])

case class HistoryDay(day: String, hourly: Array[HistoryHour]) {
  def group(): HistoryDay = {
    HistoryDay(this.day, this.hourly
        .groupBy(x => x.hour)
        .map(x => {
          val hour = x._1
          val rows = x._2.map(y => y.rows).flatten.sortBy(x => x.queries).reverse
          HistoryHour(hour, rows)
        }).toArray.sortBy(x => x.hour).reverse
    )
  }
}
case class HistoryInfo(daily: Array[HistoryDay]) {
  def group(): HistoryInfo = {
    HistoryInfo(daily
        .groupBy(x => x.day)
        .map(x => {
          val day = x._1
          val hourly = x._2.map(y => y.hourly).flatten
          HistoryDay(day, hourly).group()
        }).toArray.sortBy(x => x.day)
    )
  }
}

case class ClientInfo(day: String, client: String, queries: Int, seconds: Int, domains: Int, success: Int, failed: Int, malwares: Int, valid: Int, rank: Int) {
  def updateMalware(num: Int): ClientInfo = {
    println(malwares + "-" + num)
    println(client + ": " + queries)
    ClientInfo(day, client, queries, seconds, domains, success, failed, num, valid, rank)
  }
}

case class ClientResponse (
    current: ClientInfo,
    prev: ClientInfo,
    topDomain: Array[MainDomainInfo],
    topSecond: Array[MainDomainInfo],
    topMalware: Array[MalwareInfo],
    hourly: Array[(Int, Long)],
    daily: Array[ClientInfo],
    history: HistoryInfo,
    historyBlack: Array[Array[String]]) extends AbstractResponse {
  def toJsonObject: JsonObject = ???
}