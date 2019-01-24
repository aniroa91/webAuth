package model.device

case class ErrModuleIndex(
                         arrModule: Array[(String)],
                         arrIndex: Array[(Int)],
                         err: Seq[(String,Int,Int)]
                         )

case class HostResponse(
                         noOutlierModule:Int,
                         infHostdaily: Array[(String,Int,Int,Int,Int,Int,Int,Int)],
                         errorHourly: Array[(Int,Int,Int,Int,Int,Int,Int,Int)],
                         sigLogByModule: Array[(String,String)],
                         sigLogbyModuleIndex:Array[(String,String,Int,Int)],
                         suyhaoModule: Seq[(String,Double,Double,Double)],
                         sigLogByHourly: SigLogByTime,
                         splitterByHost: Seq[(String,String,Int)],
                         errModuleIndex: ErrModuleIndex,
                         sfContract: Seq[(String,String,String)]
                       )