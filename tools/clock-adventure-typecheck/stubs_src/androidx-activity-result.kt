package androidx.activity.result

class ActivityResultLauncher<I> {
    fun launch(input: I?) {}
    fun unregister() {}
}

class ActivityResultCaller

package androidx.activity.result.contract

import androidx.activity.result.ActivityResultLauncher

abstract class ActivityResultContract<I, O> {
    abstract fun createIntent(input: I): Any
    fun getSynchronousResult(input: I): Any? = null
}

object ActivityResultContracts {
    class RequestPermission : ActivityResultContract<String, Boolean>() {
        override fun createIntent(input: String): Any = Any()
    }

    class RequestMultiplePermissions : ActivityResultContract<Array<String>, Map<String, Boolean>>() {
        override fun createIntent(input: Array<String>): Any = Any()
    }

    class StartActivityForResult : ActivityResultContract<Any, Any?>() {
        override fun createIntent(input: Any): Any = Any()
    }
}

package androidx.activity.compose

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
fun <I, O> rememberLauncherForActivityResult(
    contract: ActivityResultContract<I, O>,
    onResult: (O) -> Unit
): ActivityResultLauncher<I> = remember { ActivityResultLauncher() }
