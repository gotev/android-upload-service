package net.gotev.uploadservice.schemehandlers

import java.lang.reflect.InvocationTargetException
import java.util.*

/**
 * Factory which instantiates the proper scheme handler based on the scheme passed.
 *
 * @author gotev
 */
object UploadServiceSchemeHandlers {

    private const val fileScheme = "/"
    private const val contentScheme = "content://"

    private val handlers by lazy {
        LinkedHashMap<String, Class<out SchemeHandler>>().apply {
            this[fileScheme] = FileSchemeHandler::class.java
            this[contentScheme] = ContentResolverSchemeHandler::class.java
        }
    }

    /**
     * Register a custom scheme handler.
     * You cannot override existing File and content:// schemes.
     * @param scheme scheme to support (e.g. content:// , yourCustomScheme://)
     * @param handler scheme handler implementation
     */
    fun register(scheme: String, handler: Class<out SchemeHandler>) {
        if (scheme == fileScheme || scheme == contentScheme)
            throw IllegalArgumentException("Cannot override bundled scheme: $scheme! If you found a bug, please open an issue: https://github.com/gotev/android-upload-service")

        handlers[scheme] = handler
    }

    @Throws(NoSuchMethodException::class, IllegalAccessException::class, InvocationTargetException::class, InstantiationException::class)
    operator fun get(path: String): SchemeHandler {
        val trimmedPath = path.trim()

        for ((scheme, handler) in handlers) {
            if (trimmedPath.startsWith(scheme, ignoreCase = true)) {
                return handler.newInstance().apply {
                    init(trimmedPath)
                }
            }
        }

        throw UnsupportedOperationException("Unsupported scheme for $path. Currently supported schemes are ${handlers.keys}")
    }
}
