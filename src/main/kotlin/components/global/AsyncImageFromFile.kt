package components.global

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.useResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mvvm.ShareViewModels.bitmaps
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Image
import org.jetbrains.skia.SamplingMode
import java.io.File
import javax.imageio.ImageIO

@Composable
fun AsyncImageFromFile(file: File, isThumbnail: Boolean = true) {
    val bitmapState = remember(file.path) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(file.path) {
        withContext(Dispatchers.IO) {
            if (isThumbnail && bitmaps.containsKey(file.path)) {
                bitmapState.value = bitmaps[file.path]
                return@withContext
            }

            try {
                Image.makeFromEncoded(file.readBytes()).use {
                    if(isThumbnail) {
                        val targetWidth = 500
                        val targetHeight = (it.height * (targetWidth.toFloat() / it.width)).toInt()

                        val dstBitmap = Bitmap().apply {
                            allocN32Pixels(targetWidth, targetHeight)
                        }

                        it.scalePixels(
                            dstBitmap.peekPixels()!!, SamplingMode.LINEAR,
                            cache = true,
                        )

                        val thumbnail = dstBitmap.asImageBitmap()

                        bitmaps[file.path] = thumbnail
                        bitmapState.value = thumbnail
                    } else {
                        bitmapState.value = it.toComposeImageBitmap()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    bitmapState.value?.let { bmp ->
        Image(
            bitmap = bmp,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
fun AsyncImageFromFile(resource: String) {
    val bmp = remember {
        useResource(resource) { inputStream ->
            loadImageBitmap(inputStream)
        }
    }

    Image(
        bitmap = bmp,
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop
    )
}