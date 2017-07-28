package model

import scala.concurrent.Await
import scala.concurrent.duration.Duration

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
case class DomainServerName(
  domainId: Int,
  serverId: Int)

class DomainServerNameTable(tag: Tag) extends Table[(Int, Int)](tag, "domain_server") {
  // This is the primary key column:
  def domainId: Rep[Int] = column[Int]("domain_id")
  def serverId: Rep[Int] = column[Int]("server_id")

  // Every table needs a * projection with the same type as the table's type parameter
  def * : ProvenShape[(Int, Int)] = (domainId, serverId)
}

object DomainServerNameTable {
  def insertIfNotExists(db: Database, rows: List[DomainServerName]) {
    val table = TableQuery[DomainServerNameTable]
    val action = (
      table.filter(r => {
        r.domainId.inSet(rows.map(x => x.domainId)) && r.serverId.inSet(rows.map(x => x.serverId))
      }).result).transactionally
    val setupFuture = db.run(action)
    val res = Await.result(setupFuture, Duration.Inf)
    val newRows = (rows.map(x => x.domainId -> x.serverId) diff res.map(x => x._1 -> x._2))
      .map(x => DomainServerName(x._1, x._2))
    insert(db, newRows)
  }

  private def insert(db: Database, rows: List[DomainServerName]) {
    val table = TableQuery[DomainServerNameTable]
    rows.grouped(1000).foreach(r => {
      val action = DBIO.seq(table ++= r.map(x => (x.domainId, x.serverId)))
      val setupFuture = db.run(action)
      Await.result(setupFuture, Duration.Inf)
    })
  }
}