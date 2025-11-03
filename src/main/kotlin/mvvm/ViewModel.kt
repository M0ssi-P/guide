package mvvm

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel

open class ViewModel {
    private val viewModelJob = Job()
    protected val viewModelScope = CoroutineScope(Dispatchers.IO + viewModelJob)

    open fun onCleared() {
        viewModelScope.cancel()
    }
}