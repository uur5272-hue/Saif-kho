package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object MediaStoreHelper {

    suspend fun saveBase64ImageToGallery(
        context: Context,
        base64Data: String,
        displayName: String = "MOTO_AI_${System.currentTimeMillis()}"
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val imageBytes = Base64.decode(base64Data, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                ?: return@withContext Result.failure(Exception("Failed to decode image bytes"))

            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "$displayName.png")
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/MOTO_AI")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: return@withContext Result.failure(Exception("Could not create MediaStore entry"))

            resolver.openOutputStream(imageUri)?.use { outStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream)
            } ?: return@withContext Result.failure(Exception("Could not open output stream"))

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(imageUri, contentValues, null, null)
            }

            Result.success(imageUri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uriToBase64(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val bytes = inputStream.readBytes()
                // Resize if huge to avoid memory overload on budget phones
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)

                var sampleSize = 1
                val maxDim = 1024
                while (options.outWidth / sampleSize > maxDim || options.outHeight / sampleSize > maxDim) {
                    sampleSize *= 2
                }

                val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                val sampledBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)
                    ?: return@withContext null

                val outStream = java.io.ByteArrayOutputStream()
                sampledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outStream)
                Base64.encodeToString(outStream.toByteArray(), Base64.NO_WRAP)
            }
        } catch (e: Exception) {
            null
        }
    }

    fun shareImage(context: Context, uri: Uri, title: String = "Share MOTO AI Creation") {
        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, title))
        } catch (e: Exception) {
            // Fallback
        }
    }
}
