package services.user

import com.sksamuel.elastic4s.http.ElasticDsl._
import services.Configure
import com.ftel.bigdata.utils.ESUtil
import com.sksamuel.elastic4s.searches.SearchDefinition
import services.domain.AbstractService
import services.ElasticUtil
import model.user.DashboardResponse
import services.Bucket
import com.redis.RedisClient
import com.redis.PubSubMessage
import com.redis.{ RedisClient, PubSubMessage, S, U, M }

case class DemoResponse(macs: Array[String], contracts: Map[String, String])

object DemoService extends AbstractService {

  private val ES_INDEX_TEST = "user-iot-hello"
  private val ES_TYPE_TEST = "docs"
  private val FIELD = "first"
  private val REDIS_CLIENT = new RedisClient("172.27.11.141", 6372)
  private val REDIS_SUB = new RedisClient("172.27.11.141", 6371)

//  def getSearchFromMac(mac: String): SearchDefinition = {
//    search("user-cpe-*" / "docs") query { must(termQuery("mac_device.keyword", mac)) } limit 1
//  }

  def get(): DemoResponse = {

    //println("Run Demo find new device")

    val keys = REDIS_SUB.keys("newmac-*").getOrElse(List()).filter(x => !x.isEmpty).map(x => x.get)
    val macs = keys.map(x => x.substring("newmac-".length())).toArray

    val contracts = macs.map(x => x.toLowerCase()).map(x => x -> REDIS_CLIENT.get(x)).filter(x => !x._2.isEmpty).map(x => x._1 -> x._2.get).toMap

    // Delete Keys
    keys.foreach(x => REDIS_SUB.del(x))

    DemoResponse(macs, contracts)
  }

  def main(args: Array[String]) {
    //ESUtil.index(client, "user-cpe-test", "docs", Map("contract" -> "IOT-REALTIME-01", "mac_device" -> "ac:af:b9:13:16:d4"), "iot-realtime-01")
    val contracts = get()
    //contracts.foreach(println)
    contracts.macs.foreach(println)
    contracts.contracts.foreach(println)
    client.close()
  }
}