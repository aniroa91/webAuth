package model

//case class LabelResponse(label: String, queries: Int, domains: Int, clients: Int, malwares: Int, success: Int, failed: Int, seconds: Int)
//
//case class MalwareResponse(malware: String, queries: Int, domains: Int, clients: Int)
//case class DomainResponse(domain: String, malware: String, queries: Int, clients: Int)
//case class DailyResponse(day: String, queries: Int, domains: Int, clients: Int)
//case class SecondResponse(second: String, label: String, malware: String, queries: Int, domains: Int, clients: Int)
//case class ReportResponse (
//    day: String,
//    total: LabelResponse,
//    totalPrev: LabelResponse,
//    labels: Array[LabelResponse],
//    malwares: Array[MalwareResponse],
//    domainBlacks: Array[DomainResponse]) {
//}

case class DayHourly(hour: Int, queries: Int, clients: Int, seconds: Int)

case class ReportResponse (
    day: String,
    current: TotalInfo,
    previous: TotalInfo,
    labels: Array[(String, TotalInfo)],
    malwares: Array[MalwareInfo],
    blacks: Array[MainDomainInfo],
    seconds: Array[MainDomainInfo],
    org: Array[(String, Int)],
    country: Array[(String, Int)],
    hourly: Array[DayHourly]) {
}