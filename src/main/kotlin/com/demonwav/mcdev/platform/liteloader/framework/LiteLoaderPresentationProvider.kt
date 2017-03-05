/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.liteloader.framework

import com.demonwav.mcdev.asset.PlatformAssets
import com.intellij.framework.library.LibraryVersionProperties
import com.intellij.openapi.roots.libraries.LibraryPresentationProvider
import com.intellij.openapi.roots.libraries.LibraryProperties
import com.intellij.openapi.util.io.JarUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import java.util.jar.Attributes

class LiteLoaderPresentationProvider : LibraryPresentationProvider<LibraryVersionProperties>(LITELOADER_LIBRARY_KIND) {

    override fun getIcon(properties: LibraryProperties<*>?) = PlatformAssets.LITELOADER_ICON

    override fun detect(classesRoots: MutableList<VirtualFile>): LibraryVersionProperties? {
        for (classesRoot in classesRoots) {
            val file = VfsUtilCore.virtualToIoFile(classesRoot)
            val title = JarUtil.getJarAttribute(file, Attributes.Name.IMPLEMENTATION_TITLE) ?: continue

            if (!title.startsWith("LiteLoader")) {
                continue
            }

            val version = JarUtil.getJarAttribute(file, Attributes.Name.IMPLEMENTATION_VERSION) ?: continue
            return LibraryVersionProperties(version)
        }
        return null
    }
}
