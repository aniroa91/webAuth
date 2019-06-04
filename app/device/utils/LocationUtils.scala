package device.utils

import utils.FileUtil

object LocationUtils {
  val MAP = FileUtil.readResource("/resources/location.csv")
    .map(x => x.split(","))
    .map(x => (x(0),x(3))-> x(4))
    .toMap

  val MAP_WORLD = FileUtil.readResource("/resources/locationWorld.csv")
    .map(x => x.split(","))
    .map(x => (x(0),x(1)) -> x(2))
    .toMap

  def getRegion(code: String): String = MAP.find(x=> x._1._2 == code.trim).get._2

  def getNameProvincebyCode(code: String): String = MAP.find(x=> x._1._2 == code.trim).getOrElse(("","") -> "")._1._1

  def getCodeProvincebyName(name: String): String = MAP.find(x=> x._1._1 == name.trim).getOrElse(("","") -> "")._1._2

  //for problem page

  def getRegionByProvWorld(province: String): String = MAP_WORLD.find(x=> x._1._2 == province.trim).getOrElse(("","") -> "")._2

  def getNameProvWorld(code: String): String = MAP_WORLD.find(x=> x._1._2 == code.trim).getOrElse(("","") -> "")._1._1

  def getCodeProvWorld(name: String): String = MAP_WORLD.find(x=> x._1._1 == name.trim).getOrElse(("",name) -> "")._1._2

}
