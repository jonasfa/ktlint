package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType.FUN
import com.pinterest.ktlint.core.ast.ElementType.IDENTIFIER
import com.pinterest.ktlint.core.ast.ElementType.IMPORT_DIRECTIVE
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtImportDirective

/**
 * https://kotlinlang.org/docs/coding-conventions.html#function-names
 */
public class FunctionNamingRule : Rule("$EXPERIMENTAL_RULE_SET_ID:function-naming") {
    private var isTestClass = false

    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        if (!isTestClass && node.elementType == IMPORT_DIRECTIVE) {
            (node.psi as KtImportDirective)
                .importPath
                ?.pathStr
                ?.takeIf {
                    it.startsWith(ORG_JUNIT) || it.startsWith(ORG_TESTNG) || it.startsWith(KOTLIN_TEST)
                }
                ?.let {
                    // Assume that each file that imports a Junit Jupiter Api class is a test class
                    isTestClass = true
                }
        }

        node
            .takeIf { node.elementType == FUN }
            ?.takeUnless {
                node.isFactoryMethod() ||
                    node.isTestMethod() ||
                    node.hasValidFunctionName()
            }?.let {
                val identifierOffset =
                    node
                        .findChildByType(IDENTIFIER)
                        ?.startOffset
                        ?: 1
                emit(identifierOffset, "Function name should start with a lowercase letter (except factory methods) and use camel case", false)
            }
    }

    private fun ASTNode.isFactoryMethod() =
        (this.psi as KtFunction)
            .let { it.hasDeclaredReturnType() && it.name == it.typeReference?.text }

    private fun ASTNode.isTestMethod() =
        isTestClass && hasValidTestFunctionName()

    private fun ASTNode.hasValidTestFunctionName() =
        findChildByType(IDENTIFIER)
            ?.text
            .orEmpty()
            .matches(VALID_TEST_FUNCTION_NAME_REGEXP)

    private fun ASTNode.hasValidFunctionName() =
        findChildByType(IDENTIFIER)
            ?.text
            .orEmpty()
            .matches(VALID_FUNCTION_NAME_REGEXP)

    private companion object {
        val VALID_FUNCTION_NAME_REGEXP = Regex("[a-z][A-Za-z\\d]*")
        val VALID_TEST_FUNCTION_NAME_REGEXP = Regex("(`.*`)|([a-z][A-Za-z\\d_]*)")
        private const val KOTLIN_TEST = "kotlin.test"
        private const val ORG_JUNIT = "org.junit"
        private const val ORG_TESTNG = "org.testng"
    }
}
