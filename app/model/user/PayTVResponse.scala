package model.user

import services.Bucket2
import services.BucketDouble
import services.Bucket

case class PayTVSegment(
    app: String,
    hourly: String,
    daily: String,
    lifetoend: String,
    sum: String,
    iptv: String,
    vod: String,
    vod_giaitri: String,
    vod_thieunhi: String,
    status: String)

case class PayTVVector(
     hourly: Array[BucketDouble],
     hourlyInMonths: Array[BucketDouble],
     app: Array[BucketDouble],
     dayOfWeek: Array[BucketDouble],
     iptv: Array[BucketDouble],
     appHourly: Array[Bucket2],
     appDayOfWeek: Array[Bucket2],
     appHourly2: Array[(String,Array[Double])],
     appDayOfWeek2: Array[(String,Array[Double])],
     iptvHourly: Array[(String, Double)],
     vodHourly: Array[(String, Double)],
     iptvDayOfWeek: Array[(String, Double)],
     vodDayOfWeek: Array[(String, Double)],
     vod: Array[Bucket],
     vodthieunhi: Array[Bucket],
     vodgiaitri: Array[Bucket],
     daily: Array[(String, Double)])


case class PayTVResponse (
  contract: PayTVContract,
  boxs: Array[PayTVBox],
  segments: Map[String, PayTVSegment],
  vectors: Map[String, PayTVVector],
  bill: Double
)