package utils

import com.ftel.bigdata.utils.FileUtil

object BillUtil {
  private val INTERNET_BILL = FileUtil.readResource("/resources/internet-bill-08.csv")
    .distinct.map(x => x.split("\t"))
    .filter(x => x(0) != "Contract")
    .map(x => x(0) -> x(1).toLong)
    .toMap
  private val PAYTV_BILL = FileUtil.readResource("/resources/paytv-bill-08.csv")
    .distinct.map(x => x.split("\t"))
    .filter(x => x(0) != "Contract")
    .map(x => x(0) -> x(1).toLong)
    .toMap
    
  def getInternetBill(contract: String): Long = {
    INTERNET_BILL.getOrElse(contract, 0L)
  }
  
  def getPayTVBill(contract: String): Long = {
    PAYTV_BILL.getOrElse(contract, 0L)
  }
}