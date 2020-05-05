import java.io.{BufferedOutputStream, FileOutputStream, IOException}
import java.nio.{ByteBuffer, ByteOrder}
import java.nio.file.{Files, Paths}

case class BMPStruct(width: Int, height: Int, bitsPerPixel: Short)

object BMPNegs{
  def convertBytesToHex(bytes: Seq[Byte]): String = {
    val sb = new StringBuilder
    for (b <- bytes) {
      sb.append(String.format("%02x", Byte.box(b)))
    }
    sb.toString
  }

  def getRowSize(bmp: BMPStruct): Int = {
    4 * math.ceil(bmp.bitsPerPixel.toFloat * bmp.width / 32).toInt
  }

  def getPadSize(bmp: BMPStruct): Int = {
    val rowSize = getRowSize(bmp)

    rowSize - math.ceil(bmp.bitsPerPixel.toFloat * bmp.width / 8).toInt
  }

  def getShortFromBytesLE(bytes: Array[Byte], offset: Int): Short = {
    ByteBuffer.wrap(bytes, offset, 2).order(ByteOrder.LITTLE_ENDIAN).getShort
  }

  def getIntFromBytesLE(bytes: Array[Byte], offset: Int): Int = {
    ByteBuffer.wrap(bytes, offset, 4).order(ByteOrder.LITTLE_ENDIAN).getInt
  }

  def getBMPOffset(bytes: Array[Byte]): Int = {
    getIntFromBytesLE(bytes, 10)
  }

  def getBMPStruct(bytes: Array[Byte]): BMPStruct = {
    BMPStruct(getIntFromBytesLE(bytes, 18),
      getIntFromBytesLE(bytes, 22),
      getShortFromBytesLE(bytes, 28))
  }

  // Main method
  def main(args: Array[String])
  {
    val bmpInput = args(0)
    val bmpOutput = if (args.length < 2) "data/out.bmp" else args(1)

    // It is expected that any image will fit in memory so
    // the whole file is loaded but in a lazy way.
    lazy val byteArray = Files.readAllBytes(Paths.get(bmpInput))

    // offset is the byte index where image data starts
    val offset = getBMPOffset(byteArray)

    // extract number of rows, columns and bits per pixel
    val bmp = getBMPStruct(byteArray)

    // row size in bytes including padding
    val rowSize = getRowSize(bmp)

    // padding which is added such that each row has byte length multiple of 4
    val pad = getPadSize(bmp)

    print(("Offset: %d, width: %d, height: %d, bits per pixel: %d, row size (in bytes): %d, " ++
      "padding: %d").format(offset, bmp.width, bmp.height, bmp.bitsPerPixel, rowSize, pad))

    val bos = new BufferedOutputStream(new FileOutputStream(bmpOutput))
    try {
      bos.write(byteArray.slice(0, offset))

      // Inverts all bits including those of padding, but this does not change the image
      for (from <- offset to offset + bmp.height*rowSize by rowSize) {
        bos.write(byteArray.slice(from, from + rowSize).map(x => (~x).toByte))
        //bos.write(byteArray.slice(from + rowSize - pad, from + rowSize))//.map(x => (~x).toByte))
      }

    } catch {
      case e: IOException => e.printStackTrace()
    } finally {
      bos.close()
    }
  }

}
