package model.device

case class InfResponse(
                        userDown: Seq[(String,String,String,Int)],
                        infDown: Seq[(String,String,String,Int)],
                        spliter: Seq[(String,String,String,Int)],
                        sfLofi:  Seq[(String,String,String,Int,Int,Int,Int,Int)],
                        indexRouge: Seq[(String,String,String,String,Int)]
                       )