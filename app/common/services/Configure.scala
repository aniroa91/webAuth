package common.services

import com.typesafe.config.ConfigFactory

object Configure {
  val FILE_PATH = ConfigFactory.load().getString("storage") + "/"
}
