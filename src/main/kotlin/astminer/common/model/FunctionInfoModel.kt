package astminer.common.model

import mu.KLogger

interface TreeFunctionSplitter<T : Node> {
    fun splitIntoFunctions(root: T, filePath: String): Collection<FunctionInfo<T>>
}

class FunctionInfoPropertyNotImplementedException(propertyName: String) :
    UnsupportedOperationException(
        "The property `$propertyName` of FunctionInfo for this language and parser type is not implemented yet. " +
            "Consider implementing it."
    )

private fun notImplemented(propertyName: String): Nothing =
    throw FunctionInfoPropertyNotImplementedException(propertyName)

interface FunctionInfo<T : Node> {
    val nameNode: T?
        get() = notImplemented("nameNode")
    val name: String?
        get() = nameNode?.originalToken
    val root: T
        get() = notImplemented("root")
    val body: T?
        get() = notImplemented("body")
    val filePath: String
        get() = notImplemented("filePath")
    val annotations: List<String>?
        get() = notImplemented("annotations")
    val modifiers: List<String>?
        get() = notImplemented("modifiers")
    val parameters: List<FunctionInfoParameter>?
        get() = notImplemented("parameters")
    val returnType: String?
        get() = notImplemented("returnType")
    val enclosingElement: EnclosingElement<T>?
        get() = notImplemented("enclosingElement")
    val isConstructor: Boolean
        get() = notImplemented("isConstructor")

    fun isBlank() = body?.children?.isEmpty() ?: true
    fun isNotBlank() = !isBlank()

    /** Tries to extract the feature. If `IllegalStateException` being thrown
     * returns null and logs the error in useful form **/
    fun <T> extractWithLogger(logger: KLogger, featureExtraction: () -> T): T? {
        return try { featureExtraction() } catch (e: IllegalStateException) {
            logger.warn { e.message + " in function $name in $filePath" }
            null
        }
    }
}

data class FunctionInfoParameter(val name: String, val type: String?)

data class EnclosingElement<T>(val type: EnclosingElementType, val name: String?, val root: T)

enum class EnclosingElementType {
    Class,
    Function,
    Method,
    VariableDeclaration,
}
