import android.graphics.*
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import org.apache.commons.io.IOUtils
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream

class BitmapHelper {
    companion object {

        private fun setVertical(baos: ByteArrayOutputStream, bitmap: Bitmap): Bitmap {
            val inputStream = ByteArrayInputStream(baos.toByteArray())
            val exif = ExifInterface(inputStream)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            val matrix = Matrix()
            if (orientation != 0) {
                matrix.postRotate(exifToDegrees(orientation))
            }
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, false)
        }

        fun exifToDegrees(orientation: Int): Float {
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> return 90F
                ExifInterface.ORIENTATION_ROTATE_180 -> return 180F
                ExifInterface.ORIENTATION_ROTATE_270 -> return 270F
            }
            return 0F
        }

        private fun getBitmap(baos: ByteArrayOutputStream, bm: Bitmap): Bitmap {
            val overlay = Bitmap.createBitmap(800,600, bm.config)
            val canvas = Canvas(overlay)
            canvas.drawColor(Color.WHITE)
            val bitmap = setVertical(baos, bm)
            var width = bitmap.width
            var height = bitmap.height
            var startLeft = 0F
            when {
                width > height -> {
                    if (width > 800 && height > 600) {
                        val ratio: Float = (width / 800).toFloat()
                        width = 800
                        height = ((height / ratio).toInt())
                    }
                }
                height > width -> {
                    val ratio: Float = (height / 600).toFloat()
                    height = 600
                    width = ((width / ratio).toInt())
                    startLeft = ((800 - width) / 2).toFloat()
                }
                else -> {
                    height = 600
                    width = 600
                    startLeft = ((800 - width) / 2).toFloat()
                }
            }
            canvas.drawBitmap(Bitmap.createScaledBitmap(bitmap, width, height, false), startLeft , 0F, null)
            return overlay
        }

        fun getByteArrayToSendFromFile(byteArray: ByteArray, fileName: String): ByteArray {
            val byteFile = ByteArrayOutputStream()

            val bitmap = getBitmap(getBaosFromByteArray(byteArray), BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size))
            if (fileName.endsWith(".png", true)) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 50, byteFile)
            } else {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteFile)
            }
            return byteFile.toByteArray()
        }

        private fun getBaosFromByteArray(byteArray: ByteArray): ByteArrayOutputStream {
            val boas = ByteArrayOutputStream(byteArray.size)
            val byteArrayCopy = ByteArray(byteArray.size)
            byteArray.copyInto(byteArrayCopy)
            boas.write(byteArrayCopy)
            return boas
        }
    }
}