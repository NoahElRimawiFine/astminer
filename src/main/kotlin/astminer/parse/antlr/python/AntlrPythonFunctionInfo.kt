package astminer.parse.antlr.python

import astminer.common.model.*
import astminer.parse.antlr.*

class AntlrPythonFunctionInfo(override val root: AntlrNode) : FunctionInfo<AntlrNode> {
    override val nameNode: AntlrNode? = collectNameNode()
    override val parameters: List<MethodInfoParameter> = collectParameters()
    override val enclosingElement: EnclosingElement<AntlrNode>? = collectEnclosingElement()

    companion object {
        private const val METHOD_NODE = "funcdef"
        private const val METHOD_NAME_NODE = "NAME"

        private const val CLASS_DECLARATION_NODE = "classdef"
        private const val CLASS_NAME_NODE = "NAME"

        private const val METHOD_PARAMETER_NODE = "parameters"
        private const val METHOD_PARAMETER_INNER_NODE = "typedargslist"
        private const val METHOD_SINGLE_PARAMETER_NODE = "tfpdef"
        private const val PARAMETER_NAME_NODE = "NAME"
        private const val PARAMETER_TYPE_NODE = "test"
        //It's seems strange but it works because actual type label will be
        //test|or_test|and_test|not_test|comparison|expr|xor_expr...
        // ..|and_expr|shift_expr|arith_expr|term|factor|power|atom_expr|atom|NAME

        private val POSSIBLE_ENCLOSING_ELEMENTS = listOf(CLASS_DECLARATION_NODE, METHOD_NODE)
        private const val BODY = "suite"
    }

    private fun collectNameNode(): AntlrNode? {
        return root.getChildOfType(METHOD_NAME_NODE)
    }

    private fun collectParameters(): List<MethodInfoParameter> {
        val parametersRoot = root.getChildOfType(METHOD_PARAMETER_NODE)
        val innerParametersRoot = parametersRoot?.getChildOfType(METHOD_PARAMETER_INNER_NODE) ?: return emptyList()

        val methodHaveOnlyOneParameter =
            innerParametersRoot.lastLabelIn(listOf(METHOD_SINGLE_PARAMETER_NODE, PARAMETER_NAME_NODE))
        if (methodHaveOnlyOneParameter) {
            return listOf(assembleMethodInfoParameter(innerParametersRoot))
        }

        return innerParametersRoot.getChildrenOfType(METHOD_SINGLE_PARAMETER_NODE).map { node ->
            assembleMethodInfoParameter(node)
        }
    }

    private fun assembleMethodInfoParameter(parameterNode: AntlrNode): MethodInfoParameter {
        val parameterHaveNoDefaultOrType = parameterNode.hasLastLabel(PARAMETER_NAME_NODE)
        val parameterName = if (parameterHaveNoDefaultOrType) {
            parameterNode.getToken()
        } else {
            parameterNode.getChildOfType(PARAMETER_NAME_NODE)?.getToken()
        }
        require(parameterName != null) { "Method name was not found" }

        val parameterType = parameterNode.getChildOfType(PARAMETER_TYPE_NODE)?.getTokensFromSubtree()

        return MethodInfoParameter(
            name = parameterName,
            type = parameterType
        )
    }

    //TODO: refactor
    private fun collectEnclosingElement(): EnclosingElement<AntlrNode>? {
        val enclosingNode = findEnclosingNode(root.getParent() as AntlrNode?) ?: return null
        val type = when {
            enclosingNode.hasLastLabel(CLASS_DECLARATION_NODE) -> EnclosingElementType.Class
            enclosingNode.hasLastLabel(METHOD_NODE) -> {
                when {
                    enclosingNode.isMethod() -> EnclosingElementType.Method
                    else -> EnclosingElementType.Function
                }
            }
            else -> throw IllegalStateException("Enclosing node can only be function or class")
        }
        val name = when (type) {
            EnclosingElementType.Class -> enclosingNode.getChildOfType(CLASS_NAME_NODE)
            EnclosingElementType.Method, EnclosingElementType.Function -> enclosingNode.getChildOfType(METHOD_NAME_NODE)
            else -> throw IllegalStateException("Enclosing node can only be function or class")
        }?.getToken()
        return EnclosingElement(
            type = type,
            name = name,
            root = enclosingNode
        )
    }

    private fun findEnclosingNode(node: AntlrNode?): AntlrNode? {
        if (node == null || node.lastLabelIn(POSSIBLE_ENCLOSING_ELEMENTS)) {
            return node
        }
        return findEnclosingNode(node.getParent() as AntlrNode?)
    }

    private fun Node.isMethod(): Boolean {
        val outerBody = getParent()
        if (outerBody?.getTypeLabel() != BODY) return false

        val enclosingNode = outerBody.getParent()
        require(enclosingNode != null) { "Found body without enclosing element" }

        val lastLabel = decompressTypeLabel(enclosingNode.getTypeLabel()).last()
        return lastLabel == CLASS_DECLARATION_NODE
    }
}