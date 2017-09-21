package model
//import slick.driver.PostgresDriver.api.Database
//import com.ftel.bigdata.db.PostgresSlick
//import com.ftel.bigdata.conf.PostgresFactory

object Label {
  val UNKOWN = "unknow" -> 0
  val WHITE = "white" -> 1
  val BLACK = "black" -> 2
  
  val White = WHITE._1
  val Black = BLACK._1
  val Unknow = UNKOWN._1
  val None = "none"

  def getId(label: String): Int = label match {
    case WHITE._1 => WHITE._2
    case BLACK._1 => BLACK._2
    case _ => UNKOWN._2
  }

  def getLabelFrom(malware: String): String = {
     if (malware == null || malware == "null") {
       Unknow
     } else if (malware == None) {
       White
     } else {
       Black
     }
  }

//  def main(args: Array[String]) {
//    val db = PostgresFactory()
//    val rows: List[com.ftel.bigdata.dns.model.table.Label] = List(
//        com.ftel.bigdata.dns.model.table.Label(UNKOWN._2, UNKOWN._1),
//        com.ftel.bigdata.dns.model.table.Label(WHITE._2, WHITE._1),
//        com.ftel.bigdata.dns.model.table.Label(BLACK._2, BLACK._1))
//
//    val result = com.ftel.bigdata.dns.model.table.LabelTable.insert(db, rows)
//    db.close()
//    println(result)
//  }
}