/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.bukkit.framework

import com.intellij.openapi.roots.libraries.LibraryKind

@JvmField
val BUKKIT_LIBRARY_KIND: LibraryKind = LibraryKind.create("bukkit-api")
@JvmField
val SPIGOT_LIBRARY_KIND: LibraryKind = LibraryKind.create("spigot-api")
@JvmField
val PAPER_LIBRARY_KIND: LibraryKind = LibraryKind.create("paper-api")
