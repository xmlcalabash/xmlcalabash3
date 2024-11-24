package com.xmlcalabash.util

import com.xmlcalabash.exceptions.XProcError
import net.sf.saxon.s9api.XdmValue
import net.sf.saxon.value.HexBinaryValue
import org.apache.commons.codec.digest.Blake3
import java.lang.Exception
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*
import java.util.zip.CRC32
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class HashUtils {
    companion object {
        val HMAC_SHA1_ALGORITHM = "HmacSHA1"

        /* From the Java 1.5 docs:
           MD2: The MD2 message digest algorithm as defined in RFC 1319.
           MD5: The MD5 message digest algorithm as defined in RFC 1321.
           SHA-1: The Secure Hash Algorithm, as defined in Secure Hash Standard, NIST FIPS 180-1.
           SHA-256, SHA-384, and SHA-512: New hash algorithms for...
         */
        fun crc(bytes: ByteArray, version: String): String {
            if (version == "32") {
                val crc = CRC32()
                crc.update(bytes)
                return String.format("%08x", crc.value)
            }
            throw XProcError.xcBadCrcVersion(version).exception()
        }

        fun md(bytes: ByteArray, version: String): String {
            val mdver = "MD" + version
            try {
                val digest = MessageDigest.getInstance(mdver)
                val hash = digest.digest(bytes)
                return byteString(hash)
            } catch (ex: NoSuchAlgorithmException) {
                throw XProcError.xcBadMdVersion(version).exception()
            }
        }

        fun sha(bytes: ByteArray, version: String): String {
            val shaver = "SHA-" + version
            try {
                val digest = MessageDigest.getInstance(shaver)
                val hash = digest.digest(bytes)
                return byteString(hash)
            } catch (ex: NoSuchAlgorithmException) {
                throw XProcError.xcBadShaVersion(version).exception()
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
        fun hmac(bytes: ByteArray, key: String): String {
            try {
                // get an hmac_sha1 key from the raw key bytes
                val signingKey = SecretKeySpec(key.toByteArray(), HMAC_SHA1_ALGORITHM)
                // get an hmac_sha1 Mac instance and initialize with the signing key
                val mac = Mac.getInstance(HMAC_SHA1_ALGORITHM)
                mac.init(signingKey)
                // compute the hmac on input data bytes
                val rawHmac = mac.doFinal(bytes)
                // base64-encode the hmac
                val encoder = Base64.getEncoder()
                return byteString(encoder.encode(rawHmac))
            } catch (ex: Exception) {
                throw XProcError.xcHashFailed("Failed to generate HMAC: ${ex.message}").exception()
            }
        }

        private fun byteString(hash: ByteArray): String {
            val sb = StringBuilder()
            for (b in hash) {
                sb.append(String.format("%02x", b))
            }
            return sb.toString()
        }

        fun blake3(bytes: ByteArray): String {
            try {
                val hasher = Blake3.initHash()
                hasher.update(bytes)
                val hash = ByteArray(32)
                hasher.doFinalize(hash)
                return byteString(hash)
            } catch (ex: IllegalArgumentException) {
                throw XProcError.xcHashBlake3Failed().exception(ex)
            }
        }

        fun blake3(bytes: ByteArray, key: XdmValue): String {
            try {
                val keybytes = tobytes(key)
                val hasher = Blake3.initKeyedHash(keybytes)
                hasher.update(bytes)
                val hash = ByteArray(32)
                return byteString(hash)
            } catch (ex: IllegalArgumentException) {
                throw XProcError.xcHashBlake3Failed().exception(ex)
            }
        }

        fun blake3(context: ByteArray, sharedSecret: XdmValue, senderId: XdmValue, recipientId: XdmValue): String {
            try {
                val kdf = Blake3.initKeyDerivationFunction(context)
                kdf.update(tobytes(sharedSecret))
                kdf.update(tobytes(senderId))
                kdf.update(tobytes(recipientId))
                val txKey = ByteArray(32)
                val rxKey = ByteArray(32)
                kdf.doFinalize(txKey)
                kdf.doFinalize(rxKey)
                val txHash = byteString(txKey)
                val rxHash = byteString(rxKey)
                return txHash + rxHash
            } catch (ex: IllegalArgumentException) {
                throw XProcError.xcHashBlake3Failed().exception(ex)
            }
        }

        private fun tobytes(xdmValue: XdmValue): ByteArray {
            val value = xdmValue.underlyingValue
            if (value is HexBinaryValue) {
                return value.binaryValue
            }
            return value.stringValue.toByteArray(StandardCharsets.UTF_8)
        }
    }
}