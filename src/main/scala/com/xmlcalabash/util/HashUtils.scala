package com.xmlcalabash.util

import java.security.{MessageDigest, NoSuchAlgorithmException}
import java.util.Base64
import java.util.zip.CRC32

import com.jafpl.graph.Location
import com.xmlcalabash.exceptions.XProcException
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object HashUtils {
  private val HMAC_SHA1_ALGORITHM = "HmacSHA1"

  /* From the Java 1.5 docs:
     MD2: The MD2 message digest algorithm as defined in RFC 1319.
     MD5: The MD5 message digest algorithm as defined in RFC 1321.
     SHA-1: The Secure Hash Algorithm, as defined in Secure Hash Standard, NIST FIPS 180-1.
     SHA-256, SHA-384, and SHA-512: New hash algorithms for...
   */

  def crc(bytes: Array[Byte], version: String, location: Option[Location]): String = {
    version match {
      case "32" =>
        val crc = new CRC32()
        crc.update(bytes)
        crc.getValue.toHexString
      case _ =>
        throw XProcException.xcBadCrcVersion(version, location)
    }
  }

  def md(bytes: Array[Byte], version: String, location: Option[Location]): String = {
    val mdver = "MD" + version
    try {
      val digest = MessageDigest.getInstance(mdver)
      val hash = digest.digest(bytes)
      byteString(hash)
    } catch {
      case ex: NoSuchAlgorithmException =>
        throw XProcException.xcBadMdVersion(version, location)
    }
  }

  def sha(bytes: Array[Byte], version: String, location: Option[Location]): String = {
    val shaver = "SHA-" + version
    try {
      val digest = MessageDigest.getInstance(shaver)
      val hash = digest.digest(bytes)
      byteString(hash)
    } catch {
      case ex: NoSuchAlgorithmException =>
        throw XProcException.xcBadShaVersion(version, location)
    }
  }

  /**
    * Computes RFC 2104-compliant HMAC signature.
    * Copied/modified slightly from amazon.webservices.common.Signature
    * Contributed by Henry Thompson, used with permission
    *
    * @param bytes The data to be signed.
    * @param key The signing key.
    * @return The Base64-encoded RFC 2104-compliant HMAC signature.
    */
  @throws(classOf[XProcException])
  def hmac(bytes: Array[Byte], version: String, key: String, location: Option[Location]): String = {
    var result = ""
    try {
      // get an hmac_sha1 key from the raw key bytes
      val signingKey = new SecretKeySpec(key.getBytes, HMAC_SHA1_ALGORITHM)
      // get an hmac_sha1 Mac instance and initialize with the signing key
      val mac = Mac.getInstance(HMAC_SHA1_ALGORITHM)
      mac.init(signingKey)
      // compute the hmac on input data bytes
      val rawHmac = mac.doFinal(bytes)
      // base64-encode the hmac
      val encoder = Base64.getEncoder
      result = byteString(encoder.encode(rawHmac))
    } catch {
      case e: Exception =>
        throw XProcException.xcHashFailed("Failed to generate HMAC : " + e.getMessage, location)
    }
    result
  }

  private def byteString(hash: Array[Byte]) = {
    var result = ""
    for (b <- hash) {
      var str = Integer.toHexString(b & 0xff)
      if (str.length < 2) str = "0" + str
      result = result + str
    }
    result
  }

}
