package services


import scala.concurrent.{ ExecutionContext, Future }
import javax.inject.Inject

//import models.Cat
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile
import model.DomainTable
import scala.concurrent.Await
import scala.concurrent.duration.Duration

//class DAO {
//  
//}

class DAO @Inject()
  (protected val dbConfigProvider: DatabaseConfigProvider)
  (implicit executionContext: ExecutionContext) extends HasDatabaseConfigProvider[JdbcProfile] {
  import profile.api._

  private val domains = TableQuery[DomainTable]

  def all() = {
    db.run(domains.result)
    val q = domains.filter(_.name === "").map(_.name)
    val action = q.result
    val result = db.run(action)
    
  }

  def get(domain: String): Seq[String] = {
    db.run(domains.result)
    val q = domains.filter(_.name === domain).map(_.name)
    val action = q.result
    //val result = db.run(action)
    val result = Await.result(db.run(action), Duration.Inf)
    //result.toMap
    result
  }
  //def insert(cat: Cat): Future[Unit] = db.run(Cats += cat).map { _ => () }
}

object DAO {
  def get(domaim: String): Seq[String] = {
    //new DAO().get(domain)
    null
  }
}