package tk.frostbit.objectsenumerator.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

class EnumeratorPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        // no need to do anything here yet
        // todo: configuration extension

        pluginManager.withPlugin("java") {
            dependencies.constraints.add("implementation", "tk.frostbit.objectsenumerator:enumerator-api:0.1")
        }
    }

}
