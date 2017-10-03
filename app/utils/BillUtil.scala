package utils

import com.ftel.bigdata.utils.FileUtil
import play.Environment



object BillUtil {
  
//  def getLines(fileName: String): List[String] = {
////    println(fileName)
//    val in = Environment.simple().resourceAsStream("/resources/country.csv")
//    println(in)
//    val buffer = scala.io.Source.fromInputStream(in)
//    buffer.getLines().toList
//  }
  
    
//  private val INTERNET_BILL = FileUtil.readResource("/resources/internet-bill-08.csv")
//    .distinct.map(x => x.split("\t"))
//    .filter(x => x(0) != "Contract")
//    .map(x => x(0) -> x(1).toLong)
//    .toMap
//  private val PAYTV_BILL = FileUtil.readResource("/resources/paytv-bill-08.csv")
//    .distinct.map(x => x.split("\t"))
//    .filter(x => x(0) != "Contract")
//    .map(x => x(0) -> x(1).toLong)
//    .toMap
//
//  private val SESSION = FileUtil.readResource("/resources/session_T8.csv")
//    .distinct.map(x => x.split("\t"))
//    .filter(x => x(0) != "Contract")
//    .map(x => x(0) -> Session(x(0), x(1).toInt, x(2).toInt, x(3).toInt, x(4).toDouble, x(5).toDouble))
//    .toMap
//
//  def getInternetBill(contract: String): Long = INTERNET_BILL.getOrElse(contract, 0L)
//
//  def getPayTVBill(contract: String): Long = PAYTV_BILL.getOrElse(contract, 0L)
//
//  def getSession(contract: String): Session = SESSION.getOrElse(contract, null)
//
//  def main(args: Array[String]) {
//    //val contract = "NAFD01366"
//
//    //val session = BillUtil.getSession(contract)
//
//    //println(session)
//    
//    //SESSION.foreach(println)
//    //PAYTV_BILL.foreach(println)
//  }

//  def getInternetBill(contract: String): Long = INTERNET_BILL.getOrElse(contract, 0L)
////
//  def getPayTVBill(contract: String): Long = PAYTV_BILL.getOrElse(contract, 0L)
////
//  def getSession(contract: String): Session = SESSION.getOrElse(contract, null)
  
}