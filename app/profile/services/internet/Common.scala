package profile.services.internet

object Common {
  
  def humanReadableByteCount(bytes: Long): String = {
    humanReadableByteCount(bytes, true)
  }
  
  def humanReadableByteCount(bytes: Long, si: Boolean): String = {
    val unit = if (si) 1000 else 1024
    if (bytes < unit) {
      bytes + " B";
    } else {
      val exp = (Math.log(bytes) / Math.log(unit)).toInt
      val pre = (if (si) "kMGTPE" else "KMGTPE").charAt(exp - 1) + (if (si) "" else "i")
      f"${bytes / Math.pow(unit, exp)}%.1f " + pre + "B"
    }

    //String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");
    //return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
  }
  
  
  def main(args: Array[String]) {
    println(humanReadableByteCount(1855425871872L , true))
    println(humanReadableByteCount(9223372036854775807L , true))
    
  }
}