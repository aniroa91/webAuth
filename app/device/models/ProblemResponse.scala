package device.models

case class ProblemResponse (
                             weekly: Seq[(String, String)],
                             location: Seq[(String, String)],
                             deviceType: Array[(String, Long)],
                             probConnectivity: Seq[(String, Long, Long)],
                             probError: Seq[(String, Long)],
                             probWarn: Seq[(String, Long)],
                             critAlert: Seq[(String, Long)],
                             warnAlert: Seq[(String, Long)],
                             suyhao: Seq[(String, Long, Long, Double)],
                             broken: Seq[(String, Long, Long)],
                             olts: Seq[(String, String, String, String, String, String, String, String)]
                           )
