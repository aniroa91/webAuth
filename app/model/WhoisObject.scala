package model

import com.ftel.bigdata.utils.Parameters.TAB
import com.ftel.bigdata.utils.NumberUtil
import com.ftel.bigdata.utils.DateTimeUtil

/**
 *
 * Domain Name: live.com
 * Registry Domain ID: 2344732_DOMAIN_COM-VRSN
 * Registrar WHOIS Server: whois.corporatedomains.com
 * Registrar URL: www.cscprotectsbrands.com
 * Updated Date: 2016-05-09T11:10:46Z
 * Creation Date: 1994-12-28T05:00:00Z
 * Registrar Registration Expiration Date: 2017-12-27T05:00:00Z
 * Registrar: CSC CORPORATE DOMAINS, INC.
 * Registrar IANA ID: 299
 * Registrar Abuse Contact Email: email@cscglobal.com
 * Registrar Abuse Contact Phone: +1.8887802723
 * Domain Status: serverTransferProhibited http://www.icann.org/epp#serverTransferProhibited
 * Domain Status: serverDeleteProhibited http://www.icann.org/epp#serverDeleteProhibited
 * Domain Status: clientTransferProhibited http://www.icann.org/epp#clientTransferProhibited
 * Domain Status: clientUpdateProhibited http://www.icann.org/epp#clientUpdateProhibited
 * Domain Status: clientDeleteProhibited http://www.icann.org/epp#clientDeleteProhibited
 * Domain Status: serverUpdateProhibited http://www.icann.org/epp#serverUpdateProhibited
 * Registry Registrant ID:
 * Registrant Name: Domain Administrator
 * Registrant Organization: Microsoft Corporation
 * Registrant Street: One Microsoft Way
 * Registrant City: Redmond
 * Registrant State/Province: WA
 * Registrant Postal Code: 98052
 * Registrant Country: US
 * Registrant Phone: +1.4258828080
 * Registrant Phone Ext:
 * Registrant Fax: +1.4259367329
 * Registrant Fax Ext:
 * Registrant Email: email@microsoft.com
 * Registry Admin ID:
 * Admin Name: Domain Administrator
 * Admin Organization: Microsoft Corporation
 * Admin Street: One Microsoft Way
 * Admin City: Redmond
 * Admin State/Province: WA
 * Admin Postal Code: 98052
 * Admin Country: US
 * Admin Phone: +1.4258828080
 * Admin Phone Ext:
 * Admin Fax: +1.4259367329
 * Admin Fax Ext:
 * Admin Email: email@microsoft.com
 * Registry Tech ID:
 * Tech Name: Domain Administrator
 * Tech Organization: Microsoft Corporation
 * Tech Street: One Microsoft Way
 * Tech City: Redmond
 * Tech State/Province: WA
 * Tech Postal Code: 98052
 * Tech Country: US
 * Tech Phone: +1.4258828080
 * Tech Phone Ext:
 * Tech Fax: +1.4259367329
 * Tech Fax Ext:
 * Tech Email: email@microsoft.com
 * Name Server: ns2.msft.net
 * Name Server: ns3.msft.net
 * Name Server: ns4.msft.net
 * Name Server: ns1.msft.net
 * DNSSEC: unsigned
 */

/**
 * Summary Info:
 * Domain Name: VNEXPRESS.NET
 * Registrar: REGISTER.COM, INC.
 * Sponsoring Registrar IANA ID: 9
 * Whois Server: whois.register.com
 * Referral URL: http://www.register.com
 * Name Server: NS1.FPTONLINE.NET
 * Name Server: NS2.FPTONLINE.NET
 * Status: clientTransferProhibited https://icann.org/epp#clientTransferProhibited
 * Updated Date: 01-jul-2015
 * Creation Date: 15-aug-2000
 * Expiration Date: 15-aug-2018
 */

/*
 Domain name: String
    - Registrar: String
    - Whois server: String
    - Referral URL: String
    - Name server: Array[String]
    - Status: Enum
        + OK
        + Client transfer prohibited
        + hold
        + redemption
    - Updated date: DateTime
    - Creation date: DateTime
    - Expiration date: DateTime
    - Lable: enum
        + White     - domain in while list
        + malware    - domain malicious
    - malware: String (name of malware)
*/
case class WhoisObject(
    domainName: String,
    registrar: String,
    whoisServer: String,
    referral: String,
    nameServer: Array[String],
    status: String,
    update: String,
    create: String,
    expire: String,
    label: String,
    malware: String) {
  override def toString = List(
    domainName,
    registrar,
    whoisServer,
    referral,
    nameServer.mkString(","),
    status,
    update,
    create,
    expire,
    label,
    malware).mkString(TAB)
    
  def isValid(): Boolean = {
    if (isEmptyOrNull(domainName) ||
        isEmptyOrNull(registrar) ||
        isEmptyOrNull(whoisServer) ||
        isEmptyOrNull(referral) ||
        isEmptyOrNull(status)) false else true
  }
  
  def isEmptyOrNull(x: String): Boolean = if (x == null || x.isEmpty()) true else false
}

object WhoisObject {
  def apply(line: String): WhoisObject = {
    val array = line.split(TAB)
    var index = -1
    def increase() = {
      index = index + 1
      index
    }
    WhoisObject(
        array(increase()),
        array(increase()),
        array(increase()),
        array(increase()),
        array(increase()).split(","),
        array(increase()),
        array(increase()),
        array(increase()),
        array(increase()),
        array(increase()),
        array(increase()))
  }
  
  
  
  def main(args: Array[String]) {
    val ds = "29-jun-2016"
    println(DateTimeUtil.create(ds, "dd-MMM-yyyy"))
  }
}