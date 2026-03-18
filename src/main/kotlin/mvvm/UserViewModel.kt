package mvvm

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import db.controller.user.IImages
import db.controller.user.User.deleteImages
import db.controller.user.User.getImages
import db.controller.user.User.importImages
import ui.config.DB
import java.sql.Connection
import java.sql.DriverManager

class UserViewModel : ViewModel() {
    private val db: Connection = DB.connection("main.db")
    var images by mutableStateOf<List<IImages>>(emptyList())
        private set

    init {
        loadImages()
    }

    fun saveImage(location: String) {
        db.importImages(location)
        loadImages()
    }

    fun deleteImage(image: IImages) {
        db.deleteImages(image.id)
        loadImages()
    }

    fun loadImages() {
        images = db.getImages()
    }
}