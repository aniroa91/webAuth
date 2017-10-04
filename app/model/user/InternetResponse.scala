package model.user

case class Session(contract: String, count: Int, min: Int, max: Int, mean: Double, std: Double) {
  override def toString = List(contract, count, min, max, mean, std).mkString("\t")
}

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

case class DownUp(
    downQuad: Array[(String, Double)],
    upQuad: Array[(String, Double)],
    downDayOfWeek: Array[(String, Array[Double])],
    upDayOfWeek: Array[(String, Array[Double])],
    downDaily: Array[(String, Double)],
    upDaily: Array[(String, Double)])

case class Duration(
    hourly: Array[(String, Double)],
    dayOfWeek: Array[(String, Array[Double])],
    daily: Array[(String, Double)])

case class InternetResponse(
    contract: InternetContract,
    segment: InternetSegment,
    downUp: DownUp,
    duration: Duration,
    suyhout: Array[(String, String)],
    error: Array[(String, (Int, String, String))],
    errorModule: Array[(String, Int)],
    errorDisconnect: Array[(String, Int)],
    session: Session,
    sessiontType: (String, Int),
    bill: Double)