package model.device

case class RegionOverview(
                         time: TimePicker,
                         opsviews: Array[(String,String,String,Int,String)],
                         kibana: Array[(String,String,String,Int,String)],
                         suyhao: Array[(String,String,String,Int,String)],
                         sigLogRegion: SigLogRegion,
                         nocCount : NocCount,
                         contracts: Array[(String,String,String,String,Int,Int,Int)],
                         opsviewType: Array[(String,String)],
                         infTypeError: InfTypeError,
                         totalInf: Array[(String,String,Double)]
                         )
case class TimePicker(
                     startMonth: String,
                     endMonth: String,
                     fromDate: String,
                     toDate: String,
                     rangeMonth: Array[(String)]
                     )
case class SigLogRegion(
                               signin: Array[(String,Array[Int])],
                               logoff: Array[(String,Array[Int])]
                             )
case class NocCount(
                    alertCount: Array[(String,String,String,Int)],
                    critCount: Array[(String,String,String,Int)],
                    warningCount: Array[(String,String,String,Int)],
                    noticeCount: Array[(String,String,String,Int)],
                    errCount: Array[(String,String,String,Int)],
                    emergCount: Array[(String,String,String,Int)]
                  )

case class InfTypeError(
                         infDown: Array[(String,String,String,Int)],
                         userDown: Array[(String,String,String,Int)],
                         rougeError: Array[(String,String,String,Int)],
                         lostSignal: Array[(String,String,String,Int)]
                       )
case class BrasInfor(
                    noOutlierByhost: Int,
                    noOutlier: Int,
                    siginLogoff:(Int,Int)
                    )

case class KibanaOpviewByTime(
                       countK: Array[(Int,Int)],
                       countOp: Array[(Int,Int)]
                       )

case class SigLogByHost(
                         arrCates: Array[String],
                         sumSig: Array[Int],
                         sumLog: Array[Int]
                       )

case class SigLogByTime(
                         sumSig: Array[Long],
                         sumLog: Array[Long]
                       )

case class ServiceNameStatus(
                         arrName: Seq[String],
                         arrStatus: Seq[String],
                         arrService: Seq[(String,String,Int)]
                       )

case class KibanaOverview(
                              arrSeverity: Array[(String,Long)],
                              arrErrorType: Array[(String,Long)],
                              arrFacility: Array[(String,Long)],
                              arrDdos: Array[(String,Long)],
                              arrSevValue:  Array[((String,String),Long)]
                            )
case class BrasOutlier(
                      time: String,
                      brasId: String,
                      signin: Int,
                      logOff: Int
                      )

case class BrasResponse(
                            currentsInfo: BrasInfor,
                            kibanaOpviewBytime: KibanaOpviewByTime,
                            sigLogBytime: SigLogByTime,
                            infErrorBytime: Array[(Int,Int)],
                            infHostBytime: Array[(String,Long,Long)],
                            infModuleBytime: Array[(String,String,Long,Long,Long)],
                            opServiceStt: Seq[(String,Int)],
                            servNameStt: ServiceNameStatus,
                            linecardhost: Array[(String,String)],
                            kibanaOverview: KibanaOverview,
                            siglogByhost: SigLogByHost
                       )