package model

import com.ftel.bigdata.db.slick.SlickTrait

import slick.jdbc.PostgresProfile.api._
import slick.lifted.ProvenShape
import slick.lifted.ProvenShape.proveShapeOf

//import com.ftel.bigdata.db.PostgresSlick

/**
Registrar Table:
    - ID: Number
    - Name: String
    - Whois server: String
    - Referral URL: String
*/
case class Registrar(id: Int, name: String, server: String, url: String) {
  def this(whois: WhoisObject) = this(0, whois.registrar, whois.whoisServer, whois.referral)
}

class RegistrarTable(tag: Tag) extends com.ftel.bigdata.db.slick.TableGeneric[(String, String, String)](tag, "registrar") {
  // This is the primary key column:
  def id: Rep[Int] = column[Int]("registrar_id", O.PrimaryKey)
  def name: Rep[String] = column[String]("registrar_name")
  def server: Rep[String] = column[String]("whois_server")
  def url: Rep[String] = column[String]("referral_url")

  // Every table needs a * projection with the same type as the table's type parameter
  def * : ProvenShape[(String, String, String)] = (name, server, url)
}

/**
 * companion for Malware
 */
object RegistrarTable {
  def insert(db: SlickTrait, rows: List[Registrar]): Map[String, Int] = {
    val table: TableQuery[RegistrarTable] = TableQuery[RegistrarTable]
    val names = rows.map(x => x.name).toArray
    val map1 = db.getIdByNames(table, names)
    val newRows = rows.filter(x => !map1.contains(x.name))
    val map2 = db.insert[Registrar, (String, String, String)](
      newRows, 1000,
      table,
      (x: Registrar) => (x.name, x.server, x.url))
      
    map1 ++ map2
  }
}