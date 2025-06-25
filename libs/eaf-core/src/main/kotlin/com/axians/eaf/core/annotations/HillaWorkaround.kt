package com.axians.eaf.core.annotations

/**
 * A marker annotation to track all locations where a workaround for a Hilla bug is applied. This
 * allows for easy identification and reversal of the workaround once the upstream fix is available.
 *
 * @property issue The URL of the upstream issue being worked around.
 * @property description A brief explanation of the workaround implemented.
 */
@Target(
        AnnotationTarget.CLASS,
        AnnotationTarget.FUNCTION,
        AnnotationTarget.PROPERTY,
        AnnotationTarget.VALUE_PARAMETER,
)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
annotation class HillaWorkaround(
        val issue: String = "https://github.com/vaadin/hilla/issues/3443",
        val description: String,
)
