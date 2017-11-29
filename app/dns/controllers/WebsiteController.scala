package dns.controllers

import javax.inject.Inject
import javax.inject.Singleton
import play.api.mvc.AbstractController
import play.api.mvc.ControllerComponents
import services.CacheService
import controllers.Secured
import java.text.NumberFormat
import com.ftel.bigdata.utils.NumberUtil

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class WebsiteController @Inject()(cc: ControllerComponents) extends AbstractController(cc) with Secured {
//  def index =  withAuth { username => implicit request => 
//    Ok(dns.views.html.dashboard.index(username))
//  }
  
  def index =  Action {
    Ok(dns.views.html.website.index())
  }

  def subdomain = Action {
    val subdomain = Map(
      "r1---sn-a5mekn7k.c.drive.google.com  " -> 8,
      "www.nb0.923809482949.google.com      " -> 1,
      "r2---sn-ogueln7r.c.docs.google.com   " -> 8,
      "r3.sn-a5mekney.c.drive.google.com    " -> 4,
      "code.l.google.com                    " -> 1056,
      "mail-it0-f78.google.com              " -> 4,
      "r3---sn-oguesn7d.c.mail.google.com   " -> 9,
      "r6---sn-i3belnel.c.drive.google.com  " -> 913,
      "google-proxy-66-249-82-124.google.com" -> 30,
      "r1---sn-5hne6nsk.c.drive.google.com  " -> 1,
      "r3.sn-npoeen76.c.drive.google.com    " -> 61,
      "r13---sn-i3b7knez.c.drive.google.com " -> 21,
      "voice-search.l.google.com            " -> 4519,
      "r14---sn-a5m7ln7k.c.docs.google.com  " -> 2,
      "r1---sn-4g5e6ney.c.docs.google.com   " -> 2,
      "r2---sn-4g5e6n7k.c.drive.google.com  " -> 3,
      "r6---sn-oguesnss.c.drive.google.com  " -> 13)
    val array = subdomain.toArray.sortBy(x => x._2).reverse
    val sum = array.map(x => x._2).sum
    Ok(dns.views.html.website.subdomain(array, sum))
  }
  
  def location =  Action {
    val s = """
      #Province/City	Capital/Administrative center	Area (km²)	Population	Density (/km²)	% Urban	Region
Bắc Giang Province 	Bắc Giang 	3,827.4 	1,554,131 	406.1 	9.4 	Northeast
Bắc Kạn Province 	Bắc Kạn 	4,868.4 	293,826 	60.4 	16.1 	Northeast
Cao Bằng Province 	Cao Bằng 	6,724.6 	507,183 	75.4 	16.9 	Northeast
Hà Giang Province 	Hà Giang 	7,945.8 	724,537 	91.2 	11.6 	Northeast
Lạng Sơn Province 	Lạng Sơn 	8,331.2 	732,515 	87.9 	19.2 	Northeast
Phú Thọ Province 	Việt Trì 	3,528.4 	1,316,389 	373.1 	15.8 	Northeast
Quảng Ninh Province 	Hạ Long 	6,099.0 	1,144,988 	187.7 	51.9 	Northeast
Thái Nguyên Province 	Thái Nguyên 	3,546.6 	1,123,116 	316.7 	25.6 	Northeast
Tuyên Quang Province 	Tuyên Quang 	5,870.4 	724,821 	123.5 	13.0 	Northeast
Lào Cai Province 	Lào Cai 	6,383.9 	614,595 	96.3 	21.0 	Northeast
Yên Bái Province 	Yên Bái 	6,899.5 	740,397 	107.3 	18.8 	Northeast
Điện Biên Province 	Điện Biên Phủ 	9,562.5 	490,306 	51.3 	15.0 	Northwest
Hòa Bình Province 	Hòa Bình 	4,684.2 	785,217 	167.6 	15.0 	Northwest
Lai Châu Province 	Lai Châu 	9,112.3 	370,502 	40.7 	14.2 	Northwest
Sơn La Province 	Sơn La 	14,174.4 	1,076,055 	75.9 	13.8 	Northwest
Bắc Ninh Province 	Bắc Ninh 	823.1 	1,024,472 	1,244.7 	23.5 	Red River Delta
Hà Nam Province 	Phủ Lý 	859.7 	784,045 	912.0 	9.5 	Red River Delta
Hải Dương Province 	Hải Dương 	1,652.8 	1,705,059 	1,031.6 	19.0 	Red River Delta
Hưng Yên Province 	Hưng Yên 	923.5 	1,127,903 	1,221.3 	12.1 	Red River Delta
Nam Định Province 	Nam Định 	1,650.8 	1,828,111 	1,107.4 	17.6 	Red River Delta
Ninh Bình Province 	Ninh Bình 	1,392.4 	898,999 	645.6 	17.9 	Red River Delta
Thái Bình Province 	Thái Bình 	1,546.5 	1,781,842 	1,152.1 	9.7 	Red River Delta
Vĩnh Phúc Province 	Vĩnh Yên 	1,373.2 	999,786 	728.1 	22.4 	Red River Delta
Hà Nội City 	Hoàn Kiếm District 	3,119.0 	7,331,909 	2,068.6 	41.0 	Red River Delta
Hải Phòng City 	Hồng Bàng District 	1,520.7 	1,976,173 	1,208.1 	46.1 	Red River Delta
Hà Tĩnh Province 	Hà Tĩnh 	6,026.5 	1,227,038 	203.6 	14.9 	North Central Coast
Nghệ An Province 	Vinh 	16,498.5 	2,912,041 	176.5 	12.9 	North Central Coast
Quảng Bình Province 	Đồng Hới 	8,065.3 	844,893 	104.8 	15.0 	North Central Coast
Quảng Trị Province 	Đông Hà 	4,760.1 	598,324 	125.7 	27.4 	North Central Coast
Thanh Hóa Province 	Thanh Hóa 	11,136.3 	3,400,595 	305.4 	10.4 	North Central Coast
Thừa Thiên–Huế Province 	Huế 	5,065.3 	1,087,420 	214.7 	36.0 	North Central Coast
Đắk Lắk Province 	Buôn Ma Thuột 	13,139.2 	1,733,624 	131.9 	24.0 	Central Highlands
Đắk Nông Province 	Gia Nghĩa 	6,516.9 	489,382 	75.1 	14.7 	Central Highlands
Gia Lai Province 	Pleiku 	15,536.9 	1,274,412 	82.0 	28.6 	Central Highlands
Kon Tum Province 	Kon Tum 	9,690.5 	430,133 	44.4 	33.5 	Central Highlands
Lâm Đồng Province 	Đà Lạt 	9,776.1 	1,187,574 	121.5 	37.8 	Central Highlands
Bình Định Province 	Qui Nhơn 	6,039.6 	1,486,465 	246.1 	27.7 	South Central Coast
Bình Thuận Province 	Phan Thiết 	7,836.9 	1,167,023 	148.9 	39.3 	South Central Coast
Khánh Hòa Province 	Nha Trang 	5,217.6 	1,157,604 	221.9 	39.9 	South Central Coast
Ninh Thuận Province 	Phan Rang–Tháp Chàm 	3,363.1 	564,993 	168.0 	36.1 	South Central Coast
Phú Yên Province 	Tuy Hòa 	5,060.6 	862,231 	170.4 	21.8 	South Central Coast
Quảng Nam Province 	Tam Kỳ 	10,438.3 	1,422,319 	136.3 	18.6 	South Central Coast
Quảng Ngãi Province 	Quảng Ngãi 	5,152.7 	1,216,773 	236.1 	14.6 	South Central Coast
Đà Nẵng City 	Hải Châu District 	1,257.3 	887,435 	705.8 	86.9 	South Central Coast
Bà Rịa–Vũng Tàu Province 	Bà Rịa 	1,989.6 	996,682 	500.9 	49.9 	Southeast
Bình Dương Province 	Thủ Dầu Một 	2,696.2 	1,481,550 	549.5 	29.9 	Southeast
Bình Phước Province 	Đồng Xoài 	6,883.4 	873,598 	126.9 	16.5 	Southeast
Đồng Nai Province 	Biên Hòa 	5,903.9 	2,486,154 	421.1 	33.2 	Southeast
Tây Ninh Province 	Tây Ninh 	4,035.9 	1,066,513 	264.3 	15.6 	Southeast
Hồ Chí Minh City 	District 1 	2,095.1 	8,262,864 	3,418.9 	83.3 	Southeast
An Giang Province 	Long Xuyên 	3,536.8 	2,142,709 	605.8 	28.4 	Mekong Delta
Bạc Liêu Province 	Bạc Liêu 	2,584.1 	856,518 	331.5 	26.1 	Mekong Delta
Bến Tre Province 	Bến Tre 	2,360.2 	1,255,946 	532.1 	9.9 	Mekong Delta
Cà Mau Province 	Cà Mau 	5,331.7 	1,206,938 	226.4 	20.4 	Mekong Delta
Đồng Tháp Province 	Cao Lãnh 	3,376.4 	1,666,467 	493.9 	17.8 	Mekong Delta
Hậu Giang Province 	Vị Thanh 	1,601.1 	757,300 	473.0 	19.6 	Mekong Delta
Kiên Giang Province 	Rạch Giá 	6,348.3 	1,688,248 	265.9 	27.0 	Mekong Delta
Long An Province 	Tân An 	4,493.8 	1,436,066 	319.6 	17.4 	Mekong Delta
Sóc Trăng Province 	Sóc Trăng 	3,312.3 	1,292,853 	390.3 	19.4 	Mekong Delta
Tiền Giang Province 	Mỹ Tho 	2,484.2 	1,672,271 	673.2 	13.7 	Mekong Delta
Trà Vinh Province 	Trà Vinh 	2,295.1 	1,003,012 	437.0 	15.3 	Mekong Delta
Vĩnh Long Province 	Vĩnh Long 	1,479.1 	1,024,707 	692.8 	15.3 	Mekong Delta
Cần Thơ City 	Ninh Kiều District 	1,401.6 	1,188,435 	847.9 	65.9 	Mekong Delta
      """
    val provinces = s.split("\n")
      .map(x => x.trim())
      .filter(x => !x.startsWith("#") && x != "")
       .map(x => x.split("\t"))
    def parse(s: String): Int = NumberFormat.getNumberInstance(java.util.Locale.US).parse(s).toString().toInt
    Ok(dns.views.html.website.location(provinces.map(x => x(0) -> parse(x(3).trim())).sortBy(x => x._2).reverse))
  }
  
  def similar =  Action {
    Ok(dns.views.html.website.similar())
  }
}

