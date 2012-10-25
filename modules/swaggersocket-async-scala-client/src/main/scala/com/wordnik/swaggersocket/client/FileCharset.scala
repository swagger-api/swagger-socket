package com.wordnik.swaggersocket.client

import java.io.{FileInputStream, File}
import org.mozilla.universalchardet.UniversalDetector
import java.nio.charset.Charset
import scala.io.Codec

object FileCharset {

  def apply(file: File) = {
    val buf = Array.ofDim[Byte](4096)
    val detector = new UniversalDetector(null)
    try {
      using(new FileInputStream(file)) { fis =>
        var idx = fis.read(buf)
        while(idx > 0 && !detector.isDone) {
          detector.handleData(buf, 0, idx)
          idx = fis.read(buf)
        }
        detector.dataEnd()
      }
      val cs = detector.getDetectedCharset
      if (cs == null || cs.trim().isEmpty)
        Codec.fileEncodingCodec.charSet
      else
        Charset.forName(cs)
    } finally {
      detector.reset()
    }
  }

  /**
   * Executes a block with a closeable resource, and closes it after the block runs
   *
   * @tparam A the return type of the block
   * @tparam B the closeable resource type
   * @param closeable the closeable resource
   * @param f the block
   */
  def using[A, B <: { def close(): Unit }](closeable: B)(f: B => A) {
    try {
      f(closeable)
    }
    finally {
      if (closeable != null)
        closeable.close()
    }
  }
}
