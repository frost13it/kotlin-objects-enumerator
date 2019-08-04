package tk.frostbit.objectsenumerator.plugin

import org.jetbrains.kotlin.codegen.ClassBuilder
import org.jetbrains.kotlin.codegen.DelegatingClassBuilder
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.kotlin.psi.synthetics.findClassDescriptor
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

class EnumeratorClassBuilder(
    private val delegate: ClassBuilder,
    private val bindingContext: BindingContext
) : DelegatingClassBuilder() {

    private var objects: Collection<ClassDescriptor>? = null

    override fun getDelegate() = delegate

    override fun defineClass(
        origin: PsiElement?,
        version: Int,
        access: Int,
        name: String,
        signature: String?,
        superName: String,
        interfaces: Array<out String>
    ) {

        super.defineClass(origin, version, access, name, signature, superName, interfaces)

        if (interfaces.contains(ENUMERATOR_INTERFACE_SIGNATURE)) {
            // todo: proper error handling

            val objectDeclaration = requireNotNull(origin as? KtObjectDeclaration) { "Not an object" }
            require(objectDeclaration.isCompanion()) { "Not a companion" }

            require(objectDeclaration.declarations.none { it is KtProperty && it.name == OBJECTS_PROPERTY }) {
                "Implementation of ObjectsEnumerator must not override property '$OBJECTS_PROPERTY'"
            }

            val parentClass = checkNotNull(objectDeclaration.containingClass()) { "Could not find containing class" }
            require(parentClass.isSealed()) { "Not a sealed class" }

            val classDescriptor = parentClass.findClassDescriptor(bindingContext)

            val subclasses = classDescriptor.sealedSubclasses
            for (subclass in subclasses) {
                require(subclass.kind == ClassKind.OBJECT) {
                    "Not an object: ${subclass.name}"
                }
            }

            generateField()

            this.objects = subclasses
        } else {
            this.objects = null
        }
    }

    override fun newMethod(
        origin: JvmDeclarationOrigin,
        access: Int,
        name: String,
        desc: String,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {

        return processMethod(origin, access, name, desc, signature, exceptions)
            ?: super.newMethod(origin, access, name, desc, signature, exceptions)
    }

    private fun processMethod(
        origin: JvmDeclarationOrigin,
        access: Int,
        name: String,
        desc: String,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor? {

        val objects = objects ?: return null

        when {
            name == "<init>" && desc == "()V" -> {
                val upstream = super.newMethod(origin, access, name, desc, signature, exceptions)
                return interceptInit(upstream, objects)
            }

            name == "getObjects" && desc == "()Ljava/util/List;" -> {
                val visitor = super.newMethod(origin, access, name, desc, signature, exceptions)
                generateAccessor(visitor)
                return nopMethodVisitor
            }
        }

        return null
    }

    private fun interceptInit(visitor: MethodVisitor, objects: Collection<ClassDescriptor>): MethodVisitor {
        return object : MethodVisitor(Opcodes.API_VERSION, visitor) {

            var instrumented = false

            override fun visitVarInsn(opcode: Int, varIndex: Int) {
                if (!instrumented) {
                    instrumented = true
                    generateInitialization(this, objects)
                }

                super.visitVarInsn(opcode, varIndex)
            }
        }
    }

    private fun generateInitialization(visitor: MethodVisitor, objects: Collection<ClassDescriptor>) {
        val iv = InstructionAdapter(visitor)

        // for putfield
        iv.load(0, AsmTypes.OBJECT_TYPE)

        iv.iconst(objects.size)
        iv.newarray(AsmTypes.OBJECT_TYPE)

        for ((index, objDescriptor) in objects.withIndex()) {
            // todo: rewrite with proper type name mapping
            val objType = Type.getObjectType(thisName.substringBeforeLast('$') + "$" + objDescriptor.name.identifier)

            iv.dup()
            iv.iconst(index)
            iv.getstatic(
                objType.className,
                JvmAbi.INSTANCE_FIELD,
                objType.descriptor
            )
            iv.astore(AsmTypes.OBJECT_TYPE)
        }

        iv.invokestatic(
            "kotlin/collections/CollectionsKt",
            "listOf",
            "([Ljava/lang/Object;)Ljava/util/List;",
            false
        )

        iv.putfield(thisName, objectsFieldName, objectsFieldDescriptor)
    }

    private fun generateField() {
        newField(
            JvmDeclarationOrigin.NO_ORIGIN,
            Opcodes.ACC_PRIVATE or Opcodes.ACC_FINAL or Opcodes.ACC_SYNTHETIC,
            objectsFieldName,
            objectsFieldDescriptor,
            null,
            null
        )
    }

    private fun generateAccessor(visitor: MethodVisitor) {
        val iv = InstructionAdapter(visitor)
        iv.visitCode()
        iv.load(0, AsmTypes.OBJECT_TYPE)
        iv.getfield(thisName, objectsFieldName, objectsFieldDescriptor)
        iv.areturn(AsmTypes.OBJECT_TYPE)
        iv.visitEnd()
    }

    companion object {
        private const val ENUMERATOR_INTERFACE = "tk.frostbit.objectsenumerator.api.ObjectsEnumerator"
        private val ENUMERATOR_INTERFACE_SIGNATURE = ENUMERATOR_INTERFACE.replace('.', '/')
        private const val OBJECTS_PROPERTY = "objects"

        private val objectsFieldName = '$' + OBJECTS_PROPERTY
        private val objectsFieldDescriptor = Type.getDescriptor(List::class.java)

        private val nopMethodVisitor = object : MethodVisitor(Opcodes.API_VERSION) {}

    }

}
