package tk.frostbit.objectsenumerator.gradle

import org.gradle.api.Project
import org.gradle.api.tasks.compile.AbstractCompile
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinGradleSubplugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

class EnumeratorKotlinSubplugin : KotlinGradleSubplugin<AbstractCompile> {

    override fun apply(
        project: Project,
        kotlinCompile: AbstractCompile,
        javaCompile: AbstractCompile?,
        variantData: Any?,
        androidProjectHandler: Any?,
        kotlinCompilation: KotlinCompilation<KotlinCommonOptions>?
    ): List<SubpluginOption> {

        // no options for now
        return emptyList()
    }

    override fun getCompilerPluginId() = "tk.frostbit.objects-enumerator"

    override fun getPluginArtifact(): SubpluginArtifact {
        return SubpluginArtifact("tk.frostbit.objectsenumerator", "enumerator-compiler-plugin")
    }

    override fun isApplicable(project: Project, task: AbstractCompile): Boolean {
        return project.plugins.hasPlugin(EnumeratorPlugin::class.java)
    }
}
