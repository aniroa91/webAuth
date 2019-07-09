package device.utils

import utils.FileUtil

object LocationUtils {
  val MAP = FileUtil.readResource("/resources/location.csv")
    .map(x => x.split(","))
    .map(x => (x(0),x(1))-> x(2))
    .toMap

  def getRegion(code: String): String = MAP.find(x=> x._1._2 == code.trim).getOrElse(("","") -> "")._2

  def getNameProvincebyCode(code: String): String = MAP.find(x=> x._1._2 == code.trim).getOrElse(("","") -> "")._1._1

  def getCodeProvincebyName(name: String): String = MAP.map(x=> (x._1._1, x._1._2, x._2)).filter(x=> x._1 == name.trim).map(x=> x._2).mkString(",")

}
