package com.xihe_lab.yance.idea.p3c.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.*

/**
 * if/else/for/while/do-while 语句必须使用大括号
 */
class ControlFlowStatementWithoutBracesInspection : LocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {
            override fun visitIfStatement(statement: PsiIfStatement) {
                checkBranch(statement.thenBranch, "if")
                val elseBranch = statement.elseBranch
                if (elseBranch != null && elseBranch !is PsiIfStatement) {
                    checkBranch(elseBranch, "else")
                }
            }

            override fun visitForStatement(statement: PsiForStatement) {
                checkBranch(statement.body, "for")
            }

            override fun visitWhileStatement(statement: PsiWhileStatement) {
                checkBranch(statement.body, "while")
            }

            override fun visitDoWhileStatement(statement: PsiDoWhileStatement) {
                checkBranch(statement.body, "do-while")
            }

            private fun checkBranch(body: PsiStatement?, keyword: String) {
                if (body == null) return
                if (body is PsiBlockStatement) return
                P3cProblemHelper.register(holder, body, "$keyword 语句体必须使用大括号")
            }
        }
    }
}
