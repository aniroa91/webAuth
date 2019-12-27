package services.domain

import org.joda.time.format.DateTimeFormat
import org.joda.time.DateTime

object CommonService {

  def getCurrentDay(): String = {
    val date = new DateTime()
    date.toString(DateTimeFormat.forPattern("yyyy-MM-dd"))
  }

}