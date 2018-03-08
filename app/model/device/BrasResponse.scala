package model.device

case class BrasInfor(
                    noOutlier: Seq[(Int)],
                    siginLogoff: Seq[(Int,Int)]
                    )
case class KibanaOpviewByTime(
                       countK: Array[Int],
                       countOp: Array[Int]
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

case class BrasResponse(
                            currentsInfo: BrasInfor,
                            kibanaOpviewBytime: KibanaOpviewByTime,
                            sigLogBytime: SigLogByTime,
                            infErrorBytime: Array[(String,Int)],
                            infHostBytime: Seq[(String,(Int,Int))],
                            infModuleBytime: Seq[(String,String,Int,Int,Int)],
                            opServiceStt: Seq[(String,Int)],
                            servNameStt: ServiceNameStatus,
                            linecardhost: Seq[(String,Int,Int)]
                       )