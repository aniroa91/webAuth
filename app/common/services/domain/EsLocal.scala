package services.domain

import com.sksamuel.elastic4s.http.ElasticDsl._

import services.Configure
import org.elasticsearch.index.mapper.LegacyIntegerFieldMapper.IntegerFieldType


object EsLocal {
  def main(args: Array[String]) {
    //Configure.client.execute( indexInto("dns-category" / "docs") fields ("category" -> category) id domain).await
    
//    Configure.client.execute {
//      createIndex("dns-category").mappings(
//        mapping("docs") as (
//            keywordField("category")
////            textField("category")//,
////            longField("")
//        )
//      )
//    }.await

//    Configure.client.execute {
//////      createIndex("dns-category").mappings(
//////        mapping("docs") as (
//////            keywordField("category")
////////            textField("category")//,
////////            longField("")
//////        )
//////      )
//////    }.await
////      
    val resp = Configure.client.execute {
      update(5).in("dns-test/update").doc(
        "field1" -> "a",
        "field2" -> "b")
    }.await
    
//    println(Configure.client.show( indexInto("dns-category" / "docs") fields ("category" -> "a") id "a"))//.await
//    println(a)
    
    //val category = CommonService.getCategorySitereviewBluecoatCom("google.com")
    //println(category)
    //Configure.client.execute( indexInto("dns-category" / "docs") fields ("category" -> category) id "google.com").await
  }
}