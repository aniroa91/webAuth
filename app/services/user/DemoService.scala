package services.user

import com.sksamuel.elastic4s.http.ElasticDsl._
import services.Configure
import com.ftel.bigdata.utils.ESUtil
import com.sksamuel.elastic4s.searches.SearchDefinition
import services.domain.AbstractService
import services.ElasticUtil
import model.user.DashboardResponse
import services.Bucket

object DemoService extends AbstractService {

  private val ES_INDEX_TEST = "user-iot-hello"
  private val ES_TYPE_TEST = "docs"
  private val FIELD = "first"

  def getSearchFromMac(mac: String): SearchDefinition = {
    search("user-cpe-*" / "docs") query { must(termQuery("mac_device.keyword", mac)) } limit 1
  }

  def get(): Array[(String, String)] = {
    //println(client.show(search(ES_INDEX_TEST / ES_TYPE_TEST) query { must(termQuery(FIELD, "true")) } limit (10000)))
    val responseMacDevice = client.execute(search(ES_INDEX_TEST / ES_TYPE_TEST) query { must(termQuery(FIELD, "true")) } limit (10000)).await
    val macs = responseMacDevice.hits.hits.map(x => x.id)
    //macs.foreach(println)
    macs.foreach(x => {
      ESUtil.upset(client, ES_INDEX_TEST, ES_TYPE_TEST, FIELD, "false", x)
    })
//    println(client.show(multi(
//          macs.map(x => getSearchFromMac(x.toLowerCase()))
//        )))
    val response = client.execute(multi(
          macs.map(x => getSearchFromMac(x.toLowerCase()))
        )).await
    //println(response.responses.isEmpty)
    val contracts =
      if (response.responses.isEmpty) {
        Array[(String, String)]()
        } else {
          //println("--------")
        //println(response.responses.filter(x => !x.isEmpty).size)
        
        val a = response.responses.filter(x => !x.isEmpty).map(x => x.hits.hits.head.sourceAsMap)
//        
          a.map(x => getValueAsString(x, "mac_device") -> getValueAsString(x, "contract"))
          .map(x => x._1 -> x._2)
          .toArray
//        Array[String]()
      }
    contracts
  }
  
  def main(args: Array[String]) {
    //ESUtil.index(client, "user-cpe-test", "docs", Map("contract" -> "IOT-REALTIME-01", "mac_device" -> "ac:af:b9:13:16:d4"), "iot-realtime-01")
    val contracts = get()
    contracts.foreach(println)
    
    client.close()
  }
}