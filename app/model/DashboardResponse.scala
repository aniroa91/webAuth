package model

case class DashboardResponse (
    report: ReportResponse,
    daily: Array[(String, TotalInfo)]) {
}