package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.KtLint
import com.github.shyiko.ktlint.core.Rule
import com.github.shyiko.ktlint.core.ast.isRoot
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl

class FinalNewlineRule : Rule("final-newline"), Rule.Modifier.RestrictToRoot {

    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        if (node.isRoot()) {
            val editorConfig = node.getUserData(KtLint.EDITOR_CONFIG_USER_DATA_KEY)!!
            val insertFinalNewline = editorConfig.insertFinalNewline ?: return
            val lastNode = lastChildNodeOf(node)
            if (insertFinalNewline) {
                if (lastNode !is PsiWhiteSpace || !lastNode.textContains('\n')) {
                    emit(0, "File must end with a newline (\\n)", true)
                    if (autoCorrect) {
                        node.addChild(PsiWhiteSpaceImpl("\n"), null)
                    }
                }
            } else {
                if (lastNode is PsiWhiteSpace && lastNode.textContains('\n')) {
                    emit(lastNode.startOffset, "Redundant newline (\\n) at the end of file", true)
                    if (autoCorrect) {
                        lastNode.node.treeParent.removeChild(lastNode.node)
                    }
                }
            }
        }
    }

    private tailrec fun lastChildNodeOf(node: ASTNode): ASTNode? =
        if (node.lastChildNode == null) node else lastChildNodeOf(node.lastChildNode)
}
