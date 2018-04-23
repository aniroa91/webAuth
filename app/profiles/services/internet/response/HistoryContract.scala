package profile.services.internet.response


case class HistoryContractDay(
    _type: String,
    numberOfDevice: Long,
    numberOfSession: Long,
    contractHourly: Array[(Long, Long)],
    sessionHourly: Array[(Long, Long)],
    downloadHourly: Array[(Long, Long)],
    uploadHourly: Array[(Long, Long)],
    status: Array[(String, Int)],
    topSession: Array[(String, Long, Long, Long)],
    logs: Array[(String, String, Long, Long, Long, String)],
    macList: Array[(String, Long, Long)])
//    
//class HistoryContract {
//  
//}