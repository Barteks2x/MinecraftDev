/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.framework

import com.intellij.openapi.roots.libraries.LibraryKind

@JvmField
val MIXIN_LIBRARY_KIND: LibraryKind = LibraryKind.create("mixin-library")
