package model

import com.ftel.bigdata.db.slick.SlickTrait

import slick.driver.PostgresDriver.api._
import slick.lifted.ProvenShape
import slick.lifted.ProvenShape.proveShapeOf

/**
ServerName Table:
  - ID: Number
	- Name: String
	- IP: String
*/
case class ServerName(id: Int, name: String) {
  def this(name: String) = this(0, name)
}
class ServerNameTable(tag: Tag) extends com.ftel.bigdata.db.slick.TableGeneric[(String)](tag, "servers") {
  // This is the primary key column:
  override def id: Rep[Int] = column[Int]("server_id", O.PrimaryKey)
  override def name: Rep[String] = column[String]("server_name")

  // Every table needs a * projection with the same type as the table's type parameter
  def * : ProvenShape[(String)] = (name)
}

/**
 * companion for Malware
 */
object ServerNameTable {
  def insert(db: SlickTrait, rows: List[ServerName]): Map[String, Int] = {
    val table: TableQuery[ServerNameTable] = TableQuery[ServerNameTable]
    val names = rows.map(x => x.name).toArray
    val map1 = db.getIdByNames(table, names)
    val newRows = rows.filter(x => !map1.contains(x.name))
    val map2 = db
      .insert[ServerName, (String)](
      
      newRows, 1000,
      table,
      (x: ServerName) => (x.name))
      
    map1 ++ map2
  }
}