package helper

import java.security.MessageDigest

object UserUtils {

  def secureHash(pass: String, salt: String): String = {
    def bytes2hex(bytes: Array[Byte]): String = bytes.map("%02x" format _).mkString

    def sha1(input: String): String = {
      val crypt = MessageDigest.getInstance("SHA-1")
      crypt.reset()
      crypt.update(input.getBytes("UTF-8"))
      bytes2hex(crypt.digest)
    }

    val key = "1671fd1d978e0ffac3be9f87bafa2596655e3c3e"
    var digest = key
    for (i <- 1 to 10) {
      digest = sha1(digest + "--" + salt + "--" + pass + "--" + key)
    }
    digest
  }

  def bytes2hex(bytes: Array[Byte]): String = bytes.map("%02x" format _).mkString

  def md5(s: String): String = {
    val md = MessageDigest.getInstance("MD5")
    md.update(s.getBytes("UTF-8"))
    val reGenPartnerToken = bytes2hex(md.digest())
    reGenPartnerToken
  }

  def randomAlphaNumericString(length: Int): String = {
    val chars = ('a' to 'z') ++ ('A' to 'Z') ++ ('0' to '9')
    randomStringFromCharList(length, chars)
  }

  def randomStringFromCharList(length: Int, chars: Seq[Char]): String = {
    val sb = new StringBuilder
    for (i <- 1 to length) {
      val randomNum = scala.util.Random.nextInt(chars.length)
      sb.append(chars(randomNum))
    }
    sb.toString
  }
}