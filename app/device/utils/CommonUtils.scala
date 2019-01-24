package device.utils

object CommonUtils {

  val rangeTime = Map(
    0 -> "00:00-06:59",
    1 -> "07:00-17:59",
    2 -> "18:00-23:59"
  )
  def getRangeTime(id: Double) = {
    if(id >=0 && id<7) 0
    else if(id >=7 && id <18) 1
    else 2
  }
}
