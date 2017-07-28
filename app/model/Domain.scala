package model

import java.sql.Date
import java.sql.Timestamp

import com.ftel.bigdata.db.slick.SlickTrait
import com.ftel.bigdata.utils.DateTimeUtil

import slick.driver.PostgresDriver.api._
import slick.lifted.ProvenShape
import slick.lifted.ProvenShape.proveShapeOf

/**
 * - ID: Number
 * - Name: String
 * - IP Address: String
 * - IP Location: String
 * - RegistrarId: Number
 * - Status: Enum
 * + OK
 * + Client transfer prohibited
 * + hold
 * + redemption
 * - Updated date: DateTime
 * - Creation date: DateTime
 * - Expiration date: DateTime
 * - Lable: enum
 * + White     - domain in while list
 * + malware    - domain malicious
 * - MalwareId: Number
 */
case class Domain(
    id: Int,
    name: String,
    registrarId: Int,
    statusId: Int,
    create: Long,
    update: Long,
    expire: Long,
    labelId: Int,
    malwareId: Int) {
  def this(whois: WhoisObject, statusMap: Map[String, Int], labelMap: Map[String, Int], registrarMap: Map[String, Int], malwareMap: Map[String, Int]) = {
    this(
      0,
      whois.domainName,
      registrarMap.getOrElse(whois.registrar, -1),
      statusMap.getOrElse(whois.status, 0),
      DomainTable.toDateAsSeconds(whois.create),
      DomainTable.toDateAsSeconds(whois.update),
      DomainTable.toDateAsSeconds(whois.expire),
      labelMap.getOrElse(whois.label, -1), {
        val mid = malwareMap.getOrElse(whois.malware.toLowerCase().trim(), -1)
        if (mid < 0) println(whois.malware.toLowerCase().trim())
        mid
      }
      )
  }
}

class DomainTable(tag: Tag) extends com.ftel.bigdata.db.slick.TableGeneric[(String, Int, Int, Int, Int, Date, Timestamp, Timestamp)](tag, "domains") {
  // This is the primary key column:
  def id: Rep[Int] = column[Int]("domain_id", O.PrimaryKey)
  def name: Rep[String] = column[String]("domain_name")
  def registrarId: Rep[Int] = column[Int]("registrar_id", O.PrimaryKey)
  def statusId: Rep[Int] = column[Int]("status_id", O.PrimaryKey)
  def labelId: Rep[Int] = column[Int]("label_id")
  def malwareId: Rep[Int] = column[Int]("malware_id")
  def expire: Rep[Date] = column[Date]("expiration", O.SqlType("timestamp default now()"))
  def create: Rep[Timestamp] = column[Timestamp]("ctime", O.SqlType("timestamp default now()"))
  def update: Rep[Timestamp] = column[Timestamp]("utime", O.SqlType("timestamp default now()"))

  // Every table needs a * projection with the same type as the table's type parameter
  def * : ProvenShape[(String, Int, Int, Int, Int, Date, Timestamp, Timestamp)] = (name, registrarId, statusId, labelId, malwareId, expire, create, update)
}

object DomainTable {

  def toDateAsSeconds(dateString: String): Long = {
    try {
      val date = if (dateString.indexOf(",") > 0) dateString.split(",")(0) else dateString
      DateTimeUtil.create(date, "dd-MMM-yyyy").getMillis
    } catch {
      case e: Exception => {
        println("Exception Parser Data: " + e.getMessage)
        0
      }
    }
  }
  /**
   * Bulk insert new row to table, if conflict will throw exception.
   * If exist, domain will don't add to postgres
   */
    def insert(db: SlickTrait, rows: List[Domain]): Map[String, Int] = {
      val table: TableQuery[DomainTable] = TableQuery[DomainTable]
      val names = rows.map(x => x.name).toArray
      val map1 = db.getIdByNames(table, names)
      val newRows = rows.filter(x => !map1.contains(x.name))
      println(s"Add more ${newRows.size} rows to postgres")
      val map2 = db.insert[Domain, (String, Int, Int, Int, Int, Date, Timestamp, Timestamp)](
          newRows, 1000,
          table,
          (x: Domain) => (x.name, x.registrarId, x.statusId, x.labelId, x.malwareId, new Date(x.expire), new Timestamp(x.create), new Timestamp(x.update)))
      map1 ++ map2
    }

//  /**
//   * Bulk insert new row to table, if conflict will throw exception
//   */
//  def insertIfNotExists(db: Database, rows: List[Domain]): Map[String, Int] = {
//    import scala.concurrent.ExecutionContext.Implicits.global
//    val table = TableQuery[DomainTable]
//    val action = (
//      table.filter(r => {
//        r.name.inSet(rows.map(x => x.name))
//      }).result).transactionally
//    val setupFuture = db.run(action)
//    val res = Await.result(setupFuture, Duration.Inf)
//    val newRows = rows.filter(x => !res.map(r => r._2).toList.contains(x.name))
//    res.foreach(x => println(x._1))
//    //println(s"Size domain ${res.size} rows in postgres")
//    println(s"Add more ${newRows.size} rows domain to postgres")
//    insert(db, newRows)
//  }
}