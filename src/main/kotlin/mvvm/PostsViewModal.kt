package mvvm

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import db.controller.user.IImages
import presentation.ProjectionViewModel

class PostsViewModal: ViewModel(), ProjectionViewModel {
    var currentPost: IImages? by mutableStateOf(null)
       private set

    fun setPost(post: IImages) {
        currentPost = post
    }
}