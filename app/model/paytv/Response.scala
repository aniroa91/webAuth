package model.paytv

import services.BucketDouble
import services.Bucket2

/**
 * cluster_sum:USE_125 cluster_iptv:PRO_VTV1 cluster_vod:DRAMA_ROMANCE cluster_vod_giaitri:TALK_SHOW cluster_vod_thieunhi:MOVIE_ONLY _id:943110 
 */
case class PayTVSegment(
    app: String,
    hourly: String,
    daily: String,
    lifetoend: String,
    sum: String,
    iptv: String,
    vod: String,
    vod_giaitri: String,
    vod_thieunhi: String)

case class PayTVVector(
     hourly: Array[BucketDouble],
     app: Array[BucketDouble],
     dayOfWeek: Array[BucketDouble],
     iptv: Array[BucketDouble],
     appHourly: Array[Bucket2],
     appDayOfWeek: Array[Bucket2],
     daily: Array[BucketDouble])

case class InternetSegment(
    contract: String,
    province: String,
    region: String,
    internetLifeToEnd: String,
    session_Count: String, 
    ssOnline_Mean: String,
    downUpload: String,
    attendNew: String,
    internetAvgFee: String,
    loaiKH: String,
    nhom_CheckList: String,
    so_checklist: String,
    lifeToEndFactor: String,
    nhom_Tuoi: String,
    avgFeeFactor: String,
    nhom_Cuoc: String,
    ketnoiFactor: String, 
    nhom_Ket_Noi: String,
    NCSDFactor:String,
    Nhom_Nhu_Cau:String,
    So_Lan_Loi_Ha_Tang: String, 
    So_Ngay_Loi_Ha_Tang: String)

case class Response(
    internet: InternetContract,
    paytv: PayTVContract,
    segments: Map[String, PayTVSegment],
    vectors: Map[String, PayTVVector],
    internetSegment: InternetSegment,
    download: Array[(Int, Double)],
    upload: Array[(Int, Double)])