package device.models

case class KpiResponse (
                             weekly: Seq[(String, String)],
                             location: Seq[(String, String)],
                             kpi: (Seq[(String, String, String, Double)], Seq[(String, String, String, Double)], Seq[(String, String, String, Double)], Seq[(String, String, String, Double)])
                           )
