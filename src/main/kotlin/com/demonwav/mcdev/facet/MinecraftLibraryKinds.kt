/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.facet

import com.demonwav.mcdev.platform.bukkit.framework.BUKKIT_LIBRARY_KIND
import com.demonwav.mcdev.platform.bukkit.framework.PAPER_LIBRARY_KIND
import com.demonwav.mcdev.platform.bukkit.framework.SPIGOT_LIBRARY_KIND
import com.demonwav.mcdev.platform.bungeecord.framework.BUNGEECORD_LIBRARY_KIND
import com.demonwav.mcdev.platform.canary.framework.CANARY_LIBRARY_KIND
import com.demonwav.mcdev.platform.canary.framework.NEPTUNE_LIBRARY_KIND
import com.demonwav.mcdev.platform.forge.framework.FORGE_LIBRARY_KIND
import com.demonwav.mcdev.platform.liteloader.framework.LITELOADER_LIBRARY_KIND
import com.demonwav.mcdev.platform.mcp.framework.MCP_LIBRARY_KIND
import com.demonwav.mcdev.platform.mixin.framework.MIXIN_LIBRARY_KIND
import com.demonwav.mcdev.platform.sponge.framework.SPONGE_LIBRARY_KIND

@JvmField
val MINECRAFT_LIBRARY_KINDS = setOf(
    BUKKIT_LIBRARY_KIND,
    SPIGOT_LIBRARY_KIND,
    PAPER_LIBRARY_KIND,
    SPONGE_LIBRARY_KIND,
    FORGE_LIBRARY_KIND,
    LITELOADER_LIBRARY_KIND,
    MCP_LIBRARY_KIND,
    MIXIN_LIBRARY_KIND,
    BUNGEECORD_LIBRARY_KIND,
    CANARY_LIBRARY_KIND,
    NEPTUNE_LIBRARY_KIND
)
