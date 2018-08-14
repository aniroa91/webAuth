package device.utils

import com.ftel.bigdata.utils.FileUtil

object LocationUtils {
  val MAP = FileUtil.readResource("/resources/location.csv")
    .map(x => x.split(","))
    .map(x => (x(0),x(3))-> x(4))
    .toMap

  def getRegion(code: String): String = {
    //println(code)
    MAP.find(x=> x._1._2 == code.trim).get._2
  }

  def getNameProvincebyCode(code: String): String = MAP.find(x=> x._1._2 == code.trim).get._1._1
  def getCodeProvincebyName(name: String): String = MAP.find(x=> x._1._1 == name.trim).get._1._2

}
