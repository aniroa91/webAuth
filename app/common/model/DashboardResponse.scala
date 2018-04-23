package model

import play.api.libs.json.Json

case class DashboardResponse (
    report: ReportResponse,
    daily: Array[(String, TotalInfo)],
    black: Array[MainDomainInfo],
    white: Array[MainDomainInfo],
    unknow: Array[MainDomainInfo]) {

  private def toBubble(arr: Array[MainDomainInfo]) = {
    implicit val bubbleWrites = Json.writes[Bubble]
    Json.toJson(arr.map(x => Bubble(x.queries, x.clients, x.queries/x.clients, x.name)))
  }
  
  def toBubbleBlack = toBubble(black)
  
  def toBubbleWhite = toBubble(white)
  
  def toBubbleUnknow = toBubble(unknow)
}