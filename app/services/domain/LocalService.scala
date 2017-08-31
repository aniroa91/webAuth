package services.domain

import org.elasticsearch.search.sort.SortOrder

import com.sksamuel.elastic4s.http.ElasticDsl._

import model.MainDomainInfo
import model.ProfileResponse
import utils.SearchReponseUtil
import org.elasticsearch.search.aggregations.bucket.terms.Terms.Bucket
import model.ClientResponse
import services.CacheService
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.client.methods.HttpPost
import org.apache.http.HttpHost
import org.apache.http.conn.params.ConnRoutePNames
import org.apache.http.HttpStatus
import scala.tools.jline_embedded.internal.InputStreamReader
import java.io.BufferedReader
import scalaj.http.Http
import services.Configure

object LocalService extends AbstractService {

  
//  def postTest() {
//    val url = "http://sitereview.bluecoat.com/rest/categorization/google.com";
//
//    val post = new HttpPost(url)
//    
//    //post.
//    post.addHeader("User-Agent", "Mozilla/5.0")
//    //post.addHeader("query","umbrella")
//    //post.addHeader("results","10")
//    val proxy = new HttpHost("172.30.45.220",80);
////DefaultHttpClient httpclient = new DefaultHttpClient();
////httpclient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY,proxy);
//
//    val client = new DefaultHttpClient
//    //val params = client.getParams
//    //params.setParameter("foo", "bar")
//    client.getParams.setParameter(ConnRoutePNames.DEFAULT_PROXY,proxy)
//    //val nameValuePairs = new ArrayList[NameValuePair](1)
//    //nameValuePairs.add(new BasicNameValuePair("registrationid", "123456789"));
//    //nameValuePairs.add(new BasicNameValuePair("accountType", "GOOGLE"));
//    //post.setEntity(new UrlEncodedFormEntity(nameValuePairs));
//    
//    // send the post request
//    val response = client.execute(post)
//    val ips  = response.getEntity().getContent();
//        val buf = new BufferedReader(new InputStreamReader(ips,"UTF-8"));
//        println(response.getStatusLine.getStatusCode)
////        if(response.getStatusLine().getStatusCode()!=HttpStatus.SC_OK)
////        {
////            throw new Exception(response.getStatusLine().getReasonPhrase());
////        }
//        var sb = new StringBuilder();
//        var s: String = null
//        while(true )
//        {
//            s = buf.readLine();
//            if(s==null || s.length()==0) {} else {
//                
//            sb.append(s);
//            }
//
//        }
//        buf.close();
//        ips.close();
//
//        
//    println(sb.toString())
////    println("--- HEADERS ---")
////    response.getAllHeaders.foreach(arg => println(arg))
//  }
  
  def main(args: Array[String]) {
    
//    ClientService.historyBlack("210.245.24.101", 0, 10)
    println(CommonService.getCategoryFromApiXforceIbmcloud("vnexpress.net"))
//    println(CommonService.getCategorySitereviewBluecoatCom("facebook.net"))
   // val a = Configure.client.execute(search(s"dns-hourly-client-2017-08-28" / "docs") query {boolQuery().must(termQuery("name", "210.245.24.101"))})
    //val res = a.await
    
    //println(res.totalHits)
   //val info = ClientService.get("210.245.24.101", CommonService.getLatestDay())
   
   
    
//    val proxy = new HttpHost("172.30.45.220",80);
//    val req = Http("http://sitereview.bluecoat.com/rest/categorization")
//                .proxy("172.30.45.220", 80)
//                .postForm(Seq("url" -> "google.com"))
//    println(req.url)
//    val res = req.asString.body
//    println(res)
    
    //println(CommonService.getCategorySitereviewBluecoatCom("google.com"))
//    postTest()
//    println("Finish")
    //val res = DashboardService.get("2017-08-17")
    
    //res.daily.foreach(x => println(x._1 -> (x._2.queries + "|" + x._2.domains + "|" + x._2.clients)))
    //println(res)
//    val res = ClientService.get("210.245.24.101", "2017-08-27")
    //res._1.unknow.foreach(println)
//    val category = CommonService.getCategorySitereview_Bluecoat_Com("google.com")
    //println(category)
    //client.close()
//    
//    val res = CommonService.getTopByNumOfQueryWithRange("2017-08-10", "2017-08-16")
//    
//    //res.map(x => x.name -> x.queries).foreach(println)
//    //println(res)
//    //    searchProfile()
//    //    val clientIP = "103.27.237.102"
//    //    val response = ClientService.get(clientIP)
//    //    //response.history.foreach(x => {
//    //        println(response.history._1)
//    //        response.history._2.foreach(y => {
//    //          println(y.hour)
//    //          y.rows.foreach(x => println(x.domain -> x.queries -> x.rCode))
//    //        })
//    //     // })
//    //    client.close()
//    //    val json = client.show {
//    //      search(s"dns-statslog-*" / "docs") query {
//    //        boolQuery()
//    //          .must(termQuery("client", ""), termQuery("rCode", "0"))
//    //          .not(termQuery("tld", "null"))
//    //      } aggregations {
//    //        cardinalityAgg(NUM_DOMAIN_FIELD, "domain")
//    //      } sortBy (
//    //        fieldSort(DAY_FIELD) order SortOrder.DESC,
//    //        fieldSort("hour") order SortOrder.DESC) limit 1000
//    //    }
//
////    val json = client.show {
////      search(s"dns-statslog-*" / "docs") query {
////        boolQuery().must(termQuery("client", ""))
////      } aggregations {
////        cardinalityAgg(NUM_DOMAIN_FIELD, "domain")
////      } postFilter {
////          boolQuery().must(termQuery("rCode", "0")).not(termQuery("tld", "null"))
////      } sortBy (
////            fieldSort(DAY_FIELD) order SortOrder.DESC,
////            fieldSort("hour") order SortOrder.DESC
////      ) limit 1000
////    }
////
////    println(json)
//    //clientProfile()
//    
//    //val clientIP = "118.70.132.21"
//    //val response = ClientService.get(clientIP, "2017-08-16")
//    client.close()
  }

  private def clientProfile() {
    val clientIP = "103.27.237.102"
    val day = "2017-08-14"

    val time0 = System.currentTimeMillis()
    val multiSearchResponse = client.execute(
      multi(
        search(s"dns-statslog-${day}" / "docs") query { must(termQuery("client", clientIP)) } aggregations (
          termsAggregation("domain").field("domain").subagg(sumAgg("sum", "queries")) size 1000
      ))).await
    
    val time1 = System.currentTimeMillis()
    val response = multiSearchResponse.responses(0)
    
    response.aggregations.foreach(println)
//    val buckets = response.aggregations.get("hourly").getOrElse(Map[String, AnyRef]()).asInstanceOf[Map[String, AnyRef]].getOrElse("buckets", List).asInstanceOf[List[AnyRef]]
//    val res = buckets.map(x => x.asInstanceOf[Map[String, AnyRef]])
//      .map(x => x.getOrElse("key", "key").asInstanceOf[Int] -> x.getOrElse("sum", Map[String, AnyRef]()).asInstanceOf[Map[String, AnyRef]])
//      .map(x => x._1 -> x._2.get("value").getOrElse("0").asInstanceOf[Double])
    client.close()
  }

//  private def searchProfile() {
//    val clientIP = "103.27.237.102"
//    val day = "2017-08-14"
//
//    val time0 = System.currentTimeMillis()
//    val multiSearchResponse = client.execute(
//      multi(
//        search(s"dns-statslog-${day}" / "docs") query {
//          must(termQuery("client", clientIP))
//        } sortBy (
//          fieldSort(DAY_FIELD) order SortOrder.DESC,
//          fieldSort("hour") order SortOrder.DESC
//        ) limit 1000
//      )).await
//    
//    val time1 = System.currentTimeMillis()
//    val response = multiSearchResponse.responses(0)
//    
//    val a = response.hits.hits.map(x => {
//      val map = x.sourceAsMap
//      //val day = map.get("day").getOrElse("").toString()
//      val hour = map.get("hour").getOrElse("").toString()
//      val domain = map.get("domain").getOrElse("").toString()
//      val queries = map.get("queries").getOrElse("0").toString().toInt
//      hour -> (domain, queries)
////      val s = map.get("day") + "," + map.get("hour") + "," + map.get("domain") + "," + map.get("queries")
////      HistoryInfo()
////      println(s)
//    })
//    
//    val b = a.groupBy(x => x._1)
//    val history = b.map(x => {
//      val hour = x._1
//      val rows = x._2.map(x => x._2)
//      HistoryInfo(hour, rows)
//    }).toArray
//    
//    ClientResponse(Array((day -> history)))
////    val buckets = response.aggregations.get("hourly").getOrElse(Map[String, AnyRef]()).asInstanceOf[Map[String, AnyRef]].getOrElse("buckets", List).asInstanceOf[List[AnyRef]]
////    val res = buckets.map(x => x.asInstanceOf[Map[String, AnyRef]])
////      .map(x => x.getOrElse("key", "key").asInstanceOf[Int] -> x.getOrElse("sum", Map[String, AnyRef]()).asInstanceOf[Map[String, AnyRef]])
////      .map(x => x._1 -> x._2.get("value").getOrElse("0").asInstanceOf[Double])
//    client.close()
//  }
  
  private def hourly() {
    val domain = "google.com"
    val day = "2017-08-13"
    val time0 = System.currentTimeMillis()
    val multiSearchResponse = client.execute(
      multi(
        search(s"dns-statslog-${day}" / "docs") query { must(termQuery(SECOND_FIELD, domain)) } aggregations (
            termsAggregation("hourly").field("hour").subagg(sumAgg("sum", "queries")) // sortBy { fieldSort(DAY_FIELD) order SortOrder.DESC } 
        ))).await
    val time1 = System.currentTimeMillis()
    val response = multiSearchResponse.responses(0)
    
    
    //type MAP_ANY = Map[String, Any]

    //val a = response.aggregations.
    //response.aggregations.foreach(println)

    val buckets = response.aggregations.get("hourly").getOrElse(Map[String, AnyRef]()).asInstanceOf[Map[String, AnyRef]].getOrElse("buckets", List).asInstanceOf[List[AnyRef]]
    
    val res = buckets.map(x => x.asInstanceOf[Map[String, AnyRef]])
           .map(x => x.getOrElse("key", "key").asInstanceOf[Int] -> x.getOrElse("sum", Map[String, AnyRef]()).asInstanceOf[Map[String, AnyRef]])
           .map(x => x._1 -> x._2.get("value").getOrElse("0").asInstanceOf[Double])
           //.map(x => x._1 -> x._2)
    //val buckets = terms.getOrElse("buckets", List())
    //buckets.map(x => )
    //val map = terms.getAggregations.asMap()
    println(res)
    client.close()
  }
}