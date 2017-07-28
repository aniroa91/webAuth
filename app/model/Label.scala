package model

import com.ftel.bigdata.db.slick.SlickTrait

import slick.driver.PostgresDriver.api._
import slick.lifted.ProvenShape
import slick.lifted.ProvenShape.proveShapeOf

//import 

//import com.ftel.bigdata.db.PostgresSlick

/**
Label Table:
  - ID: Number
	- Name: String
*/
case class Label(id: Int, name: String)
class LabelTable(tag: Tag) extends com.ftel.bigdata.db.slick.TableGeneric[(Int, String)](tag, "labels") {
  // This is the primary key column:
  def id: Rep[Int] = column[Int]("label_id", O.PrimaryKey)
  def name: Rep[String] = column[String]("label_name")

  // Every table needs a * projection with the same type as the table's type parameter
  def * : ProvenShape[(Int, String)] = (id, name)
}

/**
 * companion for Malware
 */
object LabelTable {
  def insert(db: SlickTrait, rows: List[Label]): Map[String, Int] = {
    val table: TableQuery[LabelTable] = TableQuery[LabelTable]
    db.insert[Label, (Int, String)](
        
        rows,
        1000,
        table,
        (x: Label) => (x.id, x.name))
  }
  def getNameAndId(db: SlickTrait): Map[String, Int] = {
    val table: TableQuery[LabelTable] = TableQuery[LabelTable]
    db.getNameAndId(table)
  }
}