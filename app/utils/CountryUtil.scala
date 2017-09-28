package utils

import com.ftel.bigdata.utils.FileUtil

object CountryUtil {
  val COUNTRY_MAP = FileUtil.readResource("/resources/country.csv").distinct.map(x => x.split("\t")).map(x => x(0) -> x(1)).toMap
  
  def main(args: Array[String]) {
    COUNTRY_MAP.foreach(println)
    //FileUtil.readResource("/resources/paytv-bill-08.csv").foreach(println)
  }
}