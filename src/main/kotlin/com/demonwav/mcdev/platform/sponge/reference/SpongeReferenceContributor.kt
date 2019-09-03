/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.sponge.reference

import com.demonwav.mcdev.platform.sponge.util.SpongePatterns
import com.intellij.patterns.PsiJavaPatterns
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceRegistrar

class SpongeReferenceContributor : PsiReferenceContributor() {

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            SpongePatterns.GETTER_ANNOTATION_VALUE,
            GetterEventListenerReferenceResolver
        )

        registrar.registerReferenceProvider(PsiJavaPatterns.or(
            SpongePatterns.PLUGIN_ANNOTATION_ID,
            SpongePatterns.PLUGIN_ID_USAGES
        ), SpongePluginIdReferenceResolver)
    }
}