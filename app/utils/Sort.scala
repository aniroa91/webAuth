package utils

import scala.reflect.ClassTag

object Sort {
  def apply[T: ClassTag](array: Array[T], f: T => Int, asc: Boolean): Array[T] = {
    if (asc) array.sortBy(x => f(x)) else array.sortBy(x => -f(x))
  }
  
  def apply(array: Array[(String, Int)], asc: Boolean): Array[(String, Int)] = {
    if (asc) array.sortBy(x => x._2) else array.sortBy(x => -x._2)
  }
  
  def main(args: Array[String]) {
    val array = Array(1,2,3,4,5,6,7,8)
    
    val sorted = Sort[Int](array, (x: Int) => x, true)
    
    sorted.foreach(println)
  }
}