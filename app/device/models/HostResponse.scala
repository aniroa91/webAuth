package model.device

case class ErrModuleIndex(
                         arrModule: Array[(String)],
                         arrIndex: Array[(Int)],
                         err: Seq[(String,Int,Int)]
                         )

case class HostResponse(
                         noOutlierModule: Seq[(Int)],
                         infHostdaily: Seq[(String,Int,Int,Int,Int,Int,Int)],
                         errorHourly: Seq[(String,Int,Int,Int,Int,Int,Int)],
                         sigLogByModule: Array[(String,String)],
                         sigLogbyModuleIndex:Array[(String,String,Int,Int)],
                         suyhaoModule: Seq[(String,Double,Double,Double)],
                         sigLogByHourly: SigLogByTime,
                         splitterByHost: Seq[(String,Int)],
                         errModuleIndex: ErrModuleIndex,
                         sfContract: Seq[(String,Int,Int,Int,Int)]
                       )