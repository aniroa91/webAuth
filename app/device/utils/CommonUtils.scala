package device.utils

import utils.FileUtil

object CommonUtils {

  val INDEX_LIMIT = Array("Error Per OLT Device",
    "Broken Cable Per OLT Device",
    "Alert Per Switch",
    "Alert Per OLT Device",
    "Alert Per Power Device",
    "Rate Of Suy Hao Not Pass",
    "Cycle Of OLT Error (mins)",
    "Cycle Of Switch Alert (mins)",
    "Cycle Of OLT Alert (mins)",
    "Cycle Of Power Alert (mins)",
    "LOFI Per Client",
    "SF Per Client")

  val rangeTime = Map(
    0 -> "00:00-06:59",
    1 -> "07:00-17:59",
    2 -> "18:00-23:59"
  )

  val INDEX_MAP = FileUtil.readResource("/resources/kpiIndex.csv")
    .map(x => x.split(","))
    .map(x => (x(0),x(1))-> (x(2)+x(3)+x(4)))
    .toMap

  def getRangeTime(id: Double) = {
    if(id >=0 && id<7) 0
    else if(id >=7 && id <18) 1
    else 2
  }

  def checkExistIndex(index: String) = INDEX_LIMIT.find(x=> x == getTitleIndex(index)).getOrElse("")

  def getTitleIndex(index: String) = INDEX_MAP.find(x=> x._1._1 == index.trim).getOrElse((index,index) -> "")._1._2

  def getDescriptIndex(index: String) = INDEX_MAP.find(x=> x._1._1 == index.trim).getOrElse((index,index) -> "")._2
}
