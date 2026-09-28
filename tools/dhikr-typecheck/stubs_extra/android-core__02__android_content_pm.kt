package android.content.pm

class PackageManager {
    companion object {
        const val PERMISSION_GRANTED = 0
    }
}

/** Only the orientation-lock constants the activity needs to switch between phone and tablet. */
object ActivityInfo {
    const val SCREEN_ORIENTATION_UNSPECIFIED = -1
    const val SCREEN_ORIENTATION_PORTRAIT = 1
    const val SCREEN_ORIENTATION_USER = 2
}
