package model.user

import services.Bucket
import services.BucketDouble

case class DashboardResponse(app: Array[Bucket],
    uniqueCustomer: Int,
    uniqueContract: Int,
    dayOfWeek: Array[Bucket],
    region: Array[BucketDouble],
    province: Array[BucketDouble],
    hourly: Array[Bucket])