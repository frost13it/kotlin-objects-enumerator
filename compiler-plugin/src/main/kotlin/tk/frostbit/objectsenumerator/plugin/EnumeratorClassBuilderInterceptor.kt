package tk.frostbit.objectsenumerator.plugin

import org.jetbrains.kotlin.codegen.ClassBuilder
import org.jetbrains.kotlin.codegen.ClassBuilderFactory
import org.jetbrains.kotlin.codegen.extensions.ClassBuilderInterceptorExtension
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin

object EnumeratorClassBuilderInterceptor : ClassBuilderInterceptorExtension {

    override fun interceptClassBuilderFactory(
        interceptedFactory: ClassBuilderFactory,
        bindingContext: BindingContext,
        diagnostics: DiagnosticSink
    ): ClassBuilderFactory {

        return object : ClassBuilderFactory by interceptedFactory {
            override fun newClassBuilder(origin: JvmDeclarationOrigin): ClassBuilder {
                return EnumeratorClassBuilder(interceptedFactory.newClassBuilder(origin), bindingContext)
            }
        }
    }

}
