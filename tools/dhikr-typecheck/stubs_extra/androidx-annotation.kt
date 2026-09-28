package androidx.annotation

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY, AnnotationTarget.CLASS, AnnotationTarget.CONSTRUCTOR)
annotation class MainThread

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY, AnnotationTarget.CLASS, AnnotationTarget.CONSTRUCTOR)
annotation class WorkerThread

@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION, AnnotationTarget.FIELD)
annotation class StringRes

@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION, AnnotationTarget.FIELD)
annotation class DrawableRes
