package model

import com.ftel.bigdata.db.slick.SlickTrait

import slick.jdbc.PostgresProfile.api._
import slick.lifted.ProvenShape
import slick.lifted.ProvenShape.proveShapeOf

/**
Status Table:
  - ID: Number
	- Name: String
*/
case class Status(id: Int, name: String)
class StatusTable(tag: Tag) extends com.ftel.bigdata.db.slick.TableGeneric[(Int, String)](tag, "status") {
  // This is the primary key column:
  def id: Rep[Int] = column[Int]("status_id", O.PrimaryKey)
  def name: Rep[String] = column[String]("name")

  // Every table needs a * projection with the same type as the table's type parameter
  def * : ProvenShape[(Int, String)] = (id, name)
}

/**
 * companion for Malware
 */
object StatusTable {
  def insert(db: SlickTrait, rows: List[Status]): Map[String, Int] = {
    val table: TableQuery[StatusTable] = TableQuery[StatusTable]
    db.insert[Status, (Int, String)](
        
        rows,
        1000,
        table,
        (x: Status) => (x.id, x.name))
  }
  def getNameAndId(db: SlickTrait): Map[String, Int] = {
    val table: TableQuery[StatusTable] = TableQuery[StatusTable]
    db.getNameAndId(table)
  }
}