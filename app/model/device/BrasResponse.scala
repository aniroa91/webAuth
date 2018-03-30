package model.device

case class BrasInfor(
                    noOutlier: Int,
                    siginLogoff:(Int,Int)
                    )

case class KibanaOpviewByTime(
                       countK: Array[(Int,Int)],
                       countOp: Array[(Int,Int)]
                       )

case class SigLogByTime(
                         sumSig: Array[Int],
                         sumLog: Array[Int]
                       )

case class ServiceNameStatus(
                         arrName: Seq[String],
                         arrStatus: Seq[String],
                         arrService: Seq[(String,String,Int)]
                       )

case class KibanaOverview(
                              arrSeverity: Seq[(String,Int)],
                              arrErrorType: Seq[(String,Int)],
                              arrFacility: Seq[(String,Int)],
                              arrDdos: Seq[(String,Int)],
                              arrSevValue: Seq[(String,Int)]
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
                            kibanaOverview: KibanaOverview
                       )