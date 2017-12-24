package compat.rt

annotation class ExistsIn(val version: String)

annotation class CompatibleWith(val version: String)

@Target(AnnotationTarget.LOCAL_VARIABLE)
@Retention(AnnotationRetention.RUNTIME)
annotation class VersionConst