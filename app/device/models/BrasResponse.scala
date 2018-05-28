package model.device

case class RegionOverview(
                         opsviews: Array[(String,String,String,Int,String)],
                         kibana: Array[(String,String,String,Int,String)],
                         suyhao: Array[(String,String,String,Int,String)],
                         sigLogRegion: SigLogRegion,
                         nocCount : NocCount,
                         contracts: Array[(String,String,String,String,Int,Int,Int)],
                         opsviewType: Array[(String,String)]
                         )
case class SigLogRegion(
                               signin: Array[(String,Int)],
                               logoff: Array[(String,Int)]
                             )
case class NocCount(
                    alertCount: Array[(String,String,String,Int)],
                    critCount: Array[(String,String,String,Int)],
                    warningCount: Array[(String,String,String,Int)],
                    noticeCount: Array[(String,String,String,Int)],
                    errCount: Array[(String,String,String,Int)],
                    emergCount: Array[(String,String,String,Int)]
                  )

case class BrasInfor(
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
                            infHostBytime: Seq[(String,(Int,Int))],
                            infModuleBytime: Seq[(String,String,Int,Int,Int)],
                            opServiceStt: Seq[(String,Int)],
                            servNameStt: ServiceNameStatus,
                            linecardhost: Array[(String,String)],
                            kibanaOverview: KibanaOverview,
                            siglogByhost: SigLogByHost
                       )