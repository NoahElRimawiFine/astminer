package astminer.common.model

import astminer.problem.LabeledResult
import astminer.parse.ParsingException
import mu.KotlinLogging
import java.io.File
import java.io.InputStream
import java.util.*
import kotlin.collections.HashMap

abstract class Node{
    abstract val typeLabel: String
    abstract val children: List<Node>
    abstract val parent: Node?
    abstract val token: String

    val metadata: MutableMap<String, Any> = HashMap()
    fun isLeaf() = children.isEmpty()

    fun prettyPrint(indent: Int = 0, indentSymbol: String = "--") {
        repeat(indent) { print(indentSymbol) }
        print(typeLabel)
        if (token.isNotEmpty()) {
            println(" : $token")
        } else {
            println()
        }
        children.forEach { it.prettyPrint(indent + 1, indentSymbol) }
    }

    open fun getChildrenOfType(typeLabel: String) = children.filter { it.typeLabel == typeLabel }
    open fun getChildOfType(typeLabel: String) = getChildrenOfType(typeLabel).firstOrNull()

    abstract fun removeChildrenOfType(typeLabel: String)

    fun preOrderIterator(): Iterator<Node> = PreOrderIterator(this)
    open fun preOrder(): List<Node> = PreOrderIterator(this).asSequence().toList()

    fun postOrderIterator(): Iterator<Node> = PostOrderIterator(this)
    open fun postOrder(): List<Node> = PostOrderIterator(this).asSequence().toList()
}

class PreOrderIterator(root: Node): Iterator<Node> {
    private val stack = ArrayDeque<Node>()

    init {
        stack.push(root)
    }

    override fun hasNext(): Boolean {
        return stack.isNotEmpty()
    }

    override fun next(): Node {
        val currentNode = stack.pop()
        currentNode.children.asReversed().forEach { stack.push(it) }
        return currentNode
    }
}

class PostOrderIterator(root: Node): Iterator<Node> {
    private data class NodeWrapper(val node: Node, var isChecked: Boolean = false)

    private val tree = mutableListOf(NodeWrapper(root))

    private fun fillWithChildren(wrapper: NodeWrapper){
        if (!wrapper.isChecked) {
            tree.addAll(wrapper.node.children.asReversed().map { NodeWrapper(it) })
            wrapper.isChecked = true
        }
    }

    override fun hasNext(): Boolean = tree.isNotEmpty()

    override fun next(): Node {
        while (!tree.last().isChecked) {
            fillWithChildren(tree.last())
        }
        return tree.removeLast().node
    }
}

interface Parser<T : Node> {
    /**
     * Parse input stream into an AST.
     * @param content input stream to parse
     * @return root of the AST
     */
    fun parseInputStream(content: InputStream): T

    /**
     * Parse file into an AST.
     * @param file file to parse
     * @return ParseResult instance
     */
    fun parseFile(file: File) = ParseResult(parseInputStream(file.inputStream()), file.path)
}

data class ParseResult<T : Node>(val root: T, val filePath: String) {
    fun labeledWith(label: String): LabeledResult<T> = LabeledResult(root, label, filePath)
}
