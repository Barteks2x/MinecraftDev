package com.demonwav.mcdev.platform.sponge.inspection

import com.demonwav.mcdev.platform.sponge.SpongeModuleType
import com.demonwav.mcdev.platform.sponge.util.SpongeConstants
import com.demonwav.mcdev.platform.sponge.util.isInSpongePluginClass
import com.demonwav.mcdev.platform.sponge.util.spongePluginClassId
import com.demonwav.mcdev.util.constantStringValue
import com.demonwav.mcdev.util.findModule
import com.demonwav.mcdev.util.fullQualifiedName
import com.intellij.codeInsight.intention.AddAnnotationFix
import com.intellij.codeInsight.intention.QuickFixFactory
import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.LocalQuickFixOnPsiElement
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.ide.util.PackageUtil
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiField
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaCodeReferenceElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifierListOwner
import com.intellij.psi.PsiPrimitiveType
import com.intellij.psi.PsiVariable
import com.intellij.psi.impl.source.PsiClassReferenceType
import com.intellij.psi.util.createSmartPointer

class SpongeInjectionInspection : AbstractBaseJavaLocalInspectionTool() {

    override fun getStaticDescription() = "Invalid @Inject usage in Sponge plugin class."

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return if (SpongeModuleType.isInModule(holder.file)) {
            Visitor(holder)
        } else {
            PsiElementVisitor.EMPTY_VISITOR
        }
    }

    override fun processFile(file: PsiFile, manager: InspectionManager): List<ProblemDescriptor> {
        return if (SpongeModuleType.isInModule(file)) {
            super.processFile(file, manager)
        } else {
            emptyList()
        }
    }

    private class Visitor(private val holder: ProblemsHolder) : JavaElementVisitor() {

        override fun visitField(field: PsiField) {
            if (!field.hasAnnotation(SpongeConstants.INJECT_ANNOTATION) || !field.isInSpongePluginClass()) {
                return
            }

            checkInjection(field, field)
        }

        override fun visitMethod(method: PsiMethod) {
            if (!method.hasAnnotation(SpongeConstants.INJECT_ANNOTATION) || !method.isInSpongePluginClass()) {
                return
            }

            method.parameterList.parameters.forEach { variable -> this.checkInjection(variable, variable) }
        }

        private fun checkInjection(variable: PsiVariable, annotationsOwner: PsiModifierListOwner) {
            val typeElement = variable.typeElement ?: return
            if (variable.type is PsiPrimitiveType) {
                holder.registerProblem(
                    typeElement,
                    "Primitive types cannot be injected by Sponge.",
                    ProblemHighlightType.GENERIC_ERROR
                )
                return
            }

            val classType = variable.type as? PsiClassReferenceType ?: return
            val name = classType.fullQualifiedName ?: return

            // Check if injection is possible with the current type
            if (name !in injectableTypes) {
                holder.registerProblem(
                    typeElement,
                    "${classType.presentableText} cannot be injected by Sponge.",
                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING
                )
                return
            }

            if (name == "org.spongepowered.api.asset.Asset") {
                val assetId = variable.getAnnotation(SpongeConstants.ASSET_ID_ANNOTATION)
                if (assetId == null) {
                    holder.registerProblem(
                        variable.nameIdentifier ?: variable,
                        "Injected Assets must be annotated with @AssetId.",
                        ProblemHighlightType.GENERIC_ERROR,
                        AddAnnotationFix(SpongeConstants.ASSET_ID_ANNOTATION, annotationsOwner)
                    )
                } else {
                    variable.findModule()?.let { module ->
                        val assetPathAttributeValue = assetId.findAttributeValue(null)
                        val assetPath = assetPathAttributeValue?.constantStringValue?.replace('\\', '/') ?: return@let

                        val pluginId = variable.spongePluginClassId() ?: return@let
                        val relativeAssetPath = "assets/$pluginId/$assetPath"

                        val roots = ModuleRootManager.getInstance(module).getSourceRoots(false)
                        val pathIsDirectory = roots.any { root ->
                            root.findFileByRelativePath(relativeAssetPath)?.isDirectory == true
                        }
                        if (pathIsDirectory || relativeAssetPath.endsWith('/')) {
                            holder.registerProblem(
                                assetPathAttributeValue,
                                "AssetId must point to a file.",
                                ProblemHighlightType.GENERIC_ERROR
                            )
                            return@let
                        }

                        if (roots.none { it.findFileByRelativePath(relativeAssetPath) != null }) {
                            val fix = roots.firstOrNull()?.let { contentRoot ->
                                variable.manager.findDirectory(contentRoot)?.let { directory ->
                                    CreateAssetFileFix(assetPath, module, pluginId, directory)
                                }
                            }

                            holder.registerProblem(
                                assetPathAttributeValue,
                                "Asset '$assetPath' does not exist.",
                                ProblemHighlightType.GENERIC_ERROR,
                                fix
                            )
                        }
                    }
                }
            }

            // @ConfigDir and @DefaultConfig usages
            val configDir = variable.getAnnotation(SpongeConstants.CONFIG_DIR_ANNOTATION)
            val defaultConfig = variable.getAnnotation(SpongeConstants.DEFAULT_CONFIG_ANNOTATION)
            if (name == "java.io.File" || name == "java.nio.file.Path") {
                if (configDir != null && defaultConfig != null) {
                    val quickFixFactory = QuickFixFactory.getInstance()
                    holder.registerProblem(
                        variable.nameIdentifier ?: variable,
                        "@ConfigDir and @DefaultConfig cannot be used on the same field.",
                        ProblemHighlightType.GENERIC_ERROR,
                        quickFixFactory.createDeleteFix(configDir, "Remove @ConfigDir"),
                        quickFixFactory.createDeleteFix(defaultConfig, "Remove @DefaultConfig")
                    )
                } else if (configDir == null && defaultConfig == null) {
                    holder.registerProblem(
                        variable.nameIdentifier ?: variable,
                        "An injected ${classType.name} must be annotated with either @ConfigDir or @DefaultConfig.",
                        ProblemHighlightType.GENERIC_ERROR,
                        AddAnnotationFix(SpongeConstants.CONFIG_DIR_ANNOTATION, annotationsOwner),
                        AddAnnotationFix(SpongeConstants.DEFAULT_CONFIG_ANNOTATION, annotationsOwner)
                    )
                }
            } else if (name == "ninja.leaping.configurate.loader.ConfigurationLoader") {
                if (defaultConfig == null) {
                    holder.registerProblem(
                        variable.nameIdentifier ?: variable,
                        "Injected ConfigurationLoader must be annotated with @DefaultConfig.",
                        ProblemHighlightType.GENERIC_ERROR,
                        AddAnnotationFix(SpongeConstants.DEFAULT_CONFIG_ANNOTATION, annotationsOwner)
                    )
                }

                if (configDir != null) {
                    holder.registerProblem(
                        configDir,
                        "Injected ConfigurationLoader cannot be annotated with @ConfigDir.",
                        ProblemHighlightType.GENERIC_ERROR,
                        QuickFixFactory.getInstance().createDeleteFix(configDir, "Remove @ConfigDir")
                    )
                }

                if (classType.isRaw) {
                    val ref = classType.reference
                    holder.registerProblem(
                        ref,
                        "Injected ConfigurationLoader must have a generic parameter.",
                        ProblemHighlightType.GENERIC_ERROR,
                        MissingConfLoaderTypeParamFix(ref)
                    )
                } else {
                    classType.parameters.firstOrNull()?.let { param ->
                        val paramType = param as? PsiClassReferenceType ?: return@let
                        val paramTypeFQName = paramType.fullQualifiedName ?: return@let
                        if (paramTypeFQName != "ninja.leaping.configurate.commented.CommentedConfigurationNode") {
                            val ref = param.reference
                            holder.registerProblem(
                                ref,
                                "Injected ConfigurationLoader generic parameter must be CommentedConfigurationNode.",
                                ProblemHighlightType.GENERIC_ERROR,
                                WrongConfLoaderTypeParamFix(ref)
                            )
                        }
                    }
                }
            } else {
                val quickFixFactory = QuickFixFactory.getInstance()
                if (configDir != null) {
                    holder.registerProblem(
                        configDir,
                        "${classType.name} cannot be annotated with @ConfigDir.",
                        ProblemHighlightType.GENERIC_ERROR,
                        quickFixFactory.createDeleteFix(configDir, "Remove @ConfigDir")
                    )
                }

                if (defaultConfig != null) {
                    holder.registerProblem(
                        defaultConfig,
                        "${classType.name} cannot be annotated with @DefaultConfig.",
                        ProblemHighlightType.GENERIC_ERROR,
                        quickFixFactory.createDeleteFix(defaultConfig, "Remove @DefaultConfig")
                    )
                }
            }
        }
    }

    companion object {

        val injectableTypes = listOf(
            // https://github.com/SpongePowered/SpongeCommon/blob/f92cef4/src/main/java/org/spongepowered/common/inject/SpongeImplementationModule.java
            "org.spongepowered.api.Game",
            "org.spongepowered.api.MinecraftVersion",
            "org.spongepowered.api.service.ServiceManager",
            "org.spongepowered.api.asset.AssetManager",
            "org.spongepowered.api.GameRegistry",
            "org.spongepowered.api.world.TeleportHelper",
            "org.spongepowered.api.scheduler.Scheduler",
            "org.spongepowered.api.command.CommandManager",
            "org.spongepowered.api.data.DataManager",
            "org.spongepowered.api.config.ConfigManager",
            "org.spongepowered.api.data.property.PropertyRegistry",
            "org.spongepowered.api.event.CauseStackManager",
            "org.spongepowered.api.util.metric.MetricsConfigManager",
            "org.spongepowered.api.Platform",
            "org.spongepowered.api.plugin.PluginManager",
            "org.spongepowered.api.event.EventManager",
            "org.spongepowered.api.network.ChannelRegistrar",
            "org.slf4j.Logger",
            // https://github.com/SpongePowered/SpongeCommon/blob/855ffc1/src/main/java/org/spongepowered/common/inject/SpongeModule.java
            "java.io.File",
            "java.nio.file.Path",
            "ninja.leaping.configurate.loader.ConfigurationLoader",

            "ninja.leaping.configurate.objectmapping.GuiceObjectMapperFactory",
            "com.google.inject.Injector",
            "org.spongepowered.api.plugin.PluginContainer",
            "org.spongepowered.api.asset.Asset"
        )
    }

    class WrongConfLoaderTypeParamFix(ref: PsiJavaCodeReferenceElement) : LocalQuickFixOnPsiElement(ref) {

        override fun getFamilyName(): String = name

        override fun getText(): String = "Set to CommentedConfigurationNode"

        override fun invoke(project: Project, file: PsiFile, startElement: PsiElement, endElement: PsiElement) {
            val newRef = JavaPsiFacade.getElementFactory(project).createReferenceFromText(
                "ninja.leaping.configurate.commented.CommentedConfigurationNode",
                startElement
            )
            startElement.replace(newRef)
        }
    }

    class MissingConfLoaderTypeParamFix(ref: PsiJavaCodeReferenceElement) : LocalQuickFixOnPsiElement(ref) {

        override fun getFamilyName(): String = name

        override fun getText(): String = "Insert generic parameter"

        override fun invoke(project: Project, file: PsiFile, startElement: PsiElement, endElement: PsiElement) {
            val newRef = JavaPsiFacade.getElementFactory(project).createReferenceFromText(
                "ninja.leaping.configurate.loader.ConfigurationLoader<ninja.leaping.configurate.commented.CommentedConfigurationNode>",
                startElement
            )
            startElement.replace(newRef)
        }
    }

    class CreateAssetFileFix(
        private val assetPath: String,
        private val module: Module,
        private val pluginId: String,
        directory: PsiDirectory
    ) : LocalQuickFix {

        private val dirPointer = directory.createSmartPointer()

        override fun getFamilyName(): String = "Create asset file"

        override fun startInWriteAction(): Boolean = false

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val index = assetPath.lastIndexOf('/')
            val assetDir = if (index > 0)
                "." + assetPath.substring(0, index).replace('/', '.')
            else ""
            val fileName = assetPath.substring(index + 1)
            val createdDir = PackageUtil.findOrCreateDirectoryForPackage(
                module,
                "assets.$pluginId$assetDir",
                dirPointer.element,
                false
            )
            val createdFile = runWriteAction {
                createdDir?.createFile(fileName)
            } ?: return
            if (createdFile.canNavigate()) {
                createdFile.navigate(true)
            }
        }
    }
}
