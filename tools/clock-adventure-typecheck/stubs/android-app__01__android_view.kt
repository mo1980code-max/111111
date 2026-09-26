package android.view

class Window {
    val decorView: View = View()
    fun setFlags(flags: Int, mask: Int) {}
}

class View {
    val context: android.content.Context = android.content.Context()
}
