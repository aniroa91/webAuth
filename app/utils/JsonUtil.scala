package utils

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject

object JsonUtil {
  def prettyJson(jsonObject: JsonObject): String = {
    val gson = new GsonBuilder().setPrettyPrinting().create()
    gson.toJson(jsonObject)
  }
}