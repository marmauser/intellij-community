// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package org.jetbrains.uast.kotlin

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.uast.*

@ApiStatus.Internal
class KotlinUAnnotationMethod(
    psi: KtLightMethod,
    givenParent: UElement?
) : KotlinUMethod(psi, psi.kotlinOrigin, givenParent), UAnnotationMethod {
    override val psi: KtLightMethod = unwrap<UMethod, KtLightMethod>(psi)

    override val uastDefaultValue: UExpression? by lz {
        val annotationParameter = sourcePsi as? KtParameter ?: return@lz null
        val defaultValue = annotationParameter.defaultValue ?: return@lz null
        languagePlugin?.convertElement(defaultValue, this) as? UExpression
    }
}
