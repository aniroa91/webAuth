package dns.utils

object DataSampleUtil {
  private val domains = Array(
    ("r1---sn-a5mekn7k.c.drive.google.com  ", "necurs", 0.4, 8),
    ("www.nb0.923809482949.google.com      ", "necurs", 0.4, 1),
    ("r2---sn-ogueln7r.c.docs.google.com   ", "virut", 0.4, 8),
    ("r3.sn-a5mekney.c.drive.google.com    ", "necurs", 0.4, 4),
    ("code.l.google.com                    ", "virut", 0.4, 1056),
    ("mail-it0-f78.google.com              ", "ransomeware", 0.4, 4),
    ("r3---sn-oguesn7d.c.mail.google.com   ", "ransomeware", 0.4, 9),
    ("r6---sn-i3belnel.c.drive.google.com  ", "ransomeware", 0.4, 913),
    ("google-proxy-66-249-82-124.google.com", "locky", 0.4, 30),
    ("r1---sn-5hne6nsk.c.drive.google.com  ", "locky", 0.4, 1),
    ("r3.sn-npoeen76.c.drive.google.com    ", "locky", 0.4, 61),
    ("r13---sn-i3b7knez.c.drive.google.com ", "locky", 0.4, 21),
    ("voice-search.l.google.com            ", "locky", 0.4, 4519),
    ("r14---sn-a5m7ln7k.c.docs.google.com  ", "karen", 0.4, 2),
    ("r1---sn-4g5e6ney.c.docs.google.com   ", "karen", 0.4, 2),
    ("r2---sn-4g5e6n7k.c.drive.google.com  ", "karen", 0.4, 3),
    ("r6---sn-oguesnss.c.drive.google.com  ", "karen", 0.4, 12))

  def getSubDomainSample(): Array[(String, Int)] = {
    domains.map(x => x._1 -> x._4)
  }
  
  def getDomainBlackSample(): Array[(String, String, Double, Int)] = {
    domains
  }
}