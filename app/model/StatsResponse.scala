package model

case class LabelResponse(label: String, queries: Int, domains: Int, clients: Int, malwares: Int, success: Int, failed: Int, seconds: Int)

case class MalwareResponse(malware: String, queries: Int, domains: Int, clients: Int)
case class DomainResponse(domain: String, malware: String, queries: Int, clients: Int)
case class DailyResponse(day: String, queries: Int, domains: Int, clients: Int)
//case class SecondResponse(domain: String, label: String, malware: String, queries: Int, domains: Int, client: Int)
case class StatsResponse (
    day: String,
    total: LabelResponse,
    totalPrev: LabelResponse,
    labels: Array[LabelResponse],
    malwares: Array[MalwareResponse],
    domainBlacks: Array[DomainResponse],
    daily: Array[DailyResponse]) {
}