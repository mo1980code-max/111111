package androidx.activity

import android.os.Bundle

open class ComponentActivity : android.app.Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }
    val lifecycle: androidx.lifecycle.Lifecycle = androidx.lifecycle.Lifecycle()
}
