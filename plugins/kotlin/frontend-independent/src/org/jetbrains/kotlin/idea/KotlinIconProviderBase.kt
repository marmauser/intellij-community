// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package org.jetbrains.kotlin.idea

import com.intellij.ide.IconProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.IndexNotReadyException
import com.intellij.openapi.util.Iconable
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentOfType
import com.intellij.ui.RowIcon
import com.intellij.util.PlatformIcons
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.classes.KtLightClassForFacade
import org.jetbrains.kotlin.asJava.classes.KtLightClassForSourceDeclaration
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.asJava.unwrapped
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil
import org.jetbrains.kotlin.idea.KotlinIcons.*
import org.jetbrains.kotlin.idea.caches.lightClasses.KtLightClassForDecompiledDeclarationBase
import org.jetbrains.kotlin.idea.util.ifTrue
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import javax.swing.Icon

open class KotlinIconProviderBase : IconProvider(), DumbAware {

    private fun KtDeclaration.isExpectDeclaration(): Boolean =
        hasExpectModifier() || (containingClassOrObject?.isExpectDeclaration() == true)

    //TODO FIR add support for correct isMatchingExpected
    protected open fun KtDeclaration.isMatchingExpected() = false

    private fun Icon.addExpectActualMarker(element: PsiElement): Icon {
        val declaration = (element as? KtNamedDeclaration) ?: return this
        val additionalIcon = when {
            declaration.isExpectDeclaration() -> EXPECT
            declaration.isMatchingExpected() -> ACTUAL
            else -> return this
        }
        return RowIcon(2).apply {
            setIcon(this@addExpectActualMarker, 0)
            setIcon(additionalIcon, 1)
        }
    }

    override fun getIcon(psiElement: PsiElement, flags: Int): Icon? {
        if (psiElement is KtFile) {
            if (psiElement.isScript()) {
                return when {
                    psiElement.name.endsWith(".gradle.kts") -> GRADLE_SCRIPT
                    else -> SCRIPT
                }
            }
            val mainClass = getSingleClass(psiElement)
            return if (mainClass != null) getIcon(mainClass, flags) else FILE
        }

        val result = psiElement.getBaseIcon()
        if (flags and Iconable.ICON_FLAG_VISIBILITY > 0 && result != null && (psiElement is KtModifierListOwner && psiElement !is KtClassInitializer)) {
            val list = psiElement.modifierList
            val visibilityIcon = getVisibilityIcon(list)

            val withExpectedActual: Icon = try {
                result.addExpectActualMarker(psiElement)
            } catch (indexNotReady: IndexNotReadyException) {
                result
            }

            return createRowIcon(withExpectedActual, visibilityIcon)
        }
        return result
    }

    companion object {
        fun isSingleClassFile(file: KtFile) = getSingleClass(file) != null

        fun getSingleClass(file: KtFile): KtClassOrObject? =
            getMainClass(file)?.takeIf {
                file.declarations
                    .filterNot { it.isPrivate() || it is KtTypeAlias }
                    .size == 1
            }

        fun getMainClass(file: KtFile): KtClassOrObject? {
            val classes = file.declarations.filterNot { it.isPrivate() }.filterIsInstance<KtClassOrObject>()
            if (classes.size == 1 && StringUtil.getPackageName(file.name) == classes[0].name) {
                return classes[0]
            }
            return null
        }

        private fun createRowIcon(baseIcon: Icon, visibilityIcon: Icon): RowIcon {
            val rowIcon = RowIcon(2)
            rowIcon.setIcon(baseIcon, 0)
            rowIcon.setIcon(visibilityIcon, 1)
            return rowIcon
        }

        fun getVisibilityIcon(list: KtModifierList?): Icon {
            if (list != null) {
                if (list.hasModifier(KtTokens.PRIVATE_KEYWORD)) {
                    return PlatformIcons.PRIVATE_ICON
                }
                if (list.hasModifier(KtTokens.PROTECTED_KEYWORD)) {
                    return PlatformIcons.PROTECTED_ICON
                }
                if (list.hasModifier(KtTokens.INTERNAL_KEYWORD)) {
                    return PlatformIcons.PACKAGE_LOCAL_ICON
                }
            }

            return PlatformIcons.PUBLIC_ICON
        }

        fun PsiElement.getBaseIcon(): Icon? = when (this) {
            is KtPackageDirective -> PlatformIcons.PACKAGE_ICON
            is KtFile, is KtLightClassForFacade -> FILE
            is KtLightClassForSourceDeclaration -> navigationElement.getBaseIcon()
            is KtNamedFunction -> when {
                receiverTypeReference != null ->
                    if (KtPsiUtil.isAbstract(this)) ABSTRACT_EXTENSION_FUNCTION else EXTENSION_FUNCTION
                getStrictParentOfType<KtNamedDeclaration>() is KtClass ->
                    if (KtPsiUtil.isAbstract(this)) PlatformIcons.ABSTRACT_METHOD_ICON else PlatformIcons.METHOD_ICON
                else ->
                    FUNCTION
            }
            is KtConstructor<*> -> PlatformIcons.METHOD_ICON
            is KtLightMethod -> when(val u = unwrapped) {
                is KtProperty -> if (!u.hasBody()) PlatformIcons.ABSTRACT_METHOD_ICON else PlatformIcons.METHOD_ICON
                else -> PlatformIcons.METHOD_ICON
            }
            is KtFunctionLiteral -> LAMBDA
            is KtClass -> when {
                isInterface() -> INTERFACE
                isEnum() -> ENUM
                isAnnotation() -> ANNOTATION
                this is KtEnumEntry && getPrimaryConstructorParameterList() == null -> ENUM
                else -> if (isAbstract()) ABSTRACT_CLASS else CLASS
            }
            is KtObjectDeclaration -> OBJECT
            is KtParameter -> {
                if (KtPsiUtil.getClassIfParameterIsProperty(this) != null) {
                    if (isMutable) FIELD_VAR else FIELD_VAL
                } else
                    PARAMETER
            }
            is KtProperty -> if (isVar) FIELD_VAR else FIELD_VAL
            is KtClassInitializer -> CLASS_INITIALIZER
            is KtTypeAlias -> TYPE_ALIAS
            is KtAnnotationEntry -> {
                (shortName?.asString() == JvmFileClassUtil.JVM_NAME_SHORT).ifTrue {
                    val grandParent = parent.parent
                    if (grandParent is KtPropertyAccessor) {
                        grandParent.parentOfType<KtProperty>()?.getBaseIcon()
                    } else {
                        grandParent.getBaseIcon()
                    }
                }
            }
            is PsiClass -> (this is KtLightClassForDecompiledDeclarationBase).ifTrue {
                val origin = (this as? KtLightClass)?.kotlinOrigin
                //TODO (light classes for decompiled files): correct presentation
                if (origin != null) origin.getBaseIcon() else CLASS
            }
            else -> unwrapped?.takeIf { it != this }?.getBaseIcon()
        }
    }
}
