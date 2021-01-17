package dev.ekvedaras.intellijilluminatequerybuilderintegration.utils

import com.intellij.openapi.project.DumbService
import com.intellij.psi.PsiElement
import com.intellij.util.ArrayUtil
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.lang.psi.elements.MethodReference
import com.jetbrains.php.lang.psi.elements.ParameterList
import com.jetbrains.php.lang.psi.elements.PhpClass
import com.jetbrains.php.lang.psi.elements.impl.PhpClassImpl

class MethodUtils {
    companion object {
        /**
         * Resolve method reference from given position
         */
        fun resolveMethodReference(element: PsiElement?, depthLimit: Int = 10): MethodReference? {
            if (element == null || depthLimit <= 0) {
                return null
            }

            if (element.parent is MethodReference) {
                return element.parent as MethodReference
            }

            return resolveMethodReference(element.parent, depthLimit - 1)
        }

        /**
         * Resolve in which classes given method may be defined
         */
        fun resolveMethodClasses(method: MethodReference): List<PhpClassImpl> {
            if (DumbService.isDumb(method.project) || method.classReference == null) {
                return listOf()
            }

            val classes = mutableListOf<PhpClassImpl>()

            PhpIndex
                .getInstance(method.project)
                .completeType(method.project, method.classReference!!.declaredType, null)
                .types
                .toList()
                .forEach {
                    PhpIndex.getInstance(method.project)
                        .getClassesByFQN(it)
                        .forEach { clazz -> classes.add(clazz as PhpClassImpl) }
                }

            return classes
        }

        /**
         * Resolve given parameter index in its parent method call
         */
        fun findParameterIndex(psiElement: PsiElement): Int {
            val parent = psiElement.parent ?: return -1
            return if (parent is ParameterList) {
                ArrayUtil.indexOf(parent.parameters, psiElement)
            } else findParameterIndex(parent)
        }

        /**
         * Resolve all child method references in given element and return the list.
         * Usually the root element should be a first child of PSI Statement
         */
        fun findMethodsInTree(root: PsiElement): List<MethodReference> {
            if (root.text == "return" || root.text == " ") {
                return findMethodsInTree(root.nextSibling)
            }

            val list = mutableListOf<MethodReference>()
            findMethodsInTree(root, list)
            return list
        }

        /**
         * Resolve all child method references in given element and add to given list.
         */
        private fun findMethodsInTree(root: PsiElement, list: MutableList<MethodReference>) {
            for (child in root.children) {
                if (child is MethodReference) {
                    list.add(child)
                    findMethodsInTree(child, list)
                }
            }
        }
    }
}