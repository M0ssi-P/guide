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
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.useResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mvvm.ShareViewModels.bitmaps
import java.io.File
import javax.imageio.ImageIO

@Composable
fun AsyncImageFromFile(file: File) {
    val bitmapState = remember(file.path) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(file.path) {
        val cached = bitmaps[file.path]
        if (cached != null) {
            bitmapState.value = cached
        } else {
            val bmp = withContext(Dispatchers.IO) {
                ImageIO.read(file)?.toComposeImageBitmap()
            }
            if (bmp != null) {
                bitmaps[file.path] = bmp
                bitmapState.value = bmp
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