package common.auth

import com.unboundid.ldap.sdk._
import services.Configure
import controllers.Account

object LDAP {

  def authenticate(email: String, password: String): Option[Account] = {
    if(usernameValidation(email) && passwordValidation(password)){
      try {
        val ldapConn = new LDAPConnection()
        ldapConn.connect(Configure.LDAP_HOST, Configure.LDAP_PORT)
        val DN = s"uid=$email,${Configure.LDAP_DN}"
        val bindRequest = new SimpleBindRequest(DN, password)
        val bindResult = ldapConn.bind(bindRequest)
        if (bindResult.getResultCode == ResultCode.SUCCESS) {
          val account = new Account(email, password, "(?!.*noc.*).*", "Ha Noi", "1")
          return toOption(account)
        }
      }
      catch{
        case e: Exception => {
          return None
        }
      }
    }
    return None
  }

  def usernameValidation(username: String):Boolean = {
    return username.matches("^(?!.*__.*)(?!.*\\.\\..*)[a-z0-9_.]+$")
  }

  def passwordValidation(password: String):Boolean = {
    return true
  }

  def toOption[T](value: T): Option[T] = if (value == null) None else Some(value)

}
