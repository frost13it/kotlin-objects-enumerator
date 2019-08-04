import tk.frostbit.objectsenumerator.api.ObjectsEnumerator

sealed class KindaEnum {

    object Foo : KindaEnum()
    object Bar : KindaEnum()
    object Baz : KindaEnum()

    companion object : ObjectsEnumerator<KindaEnum>

}

fun main() {
    // prints: [Foo,Bar,Baz]
    println(KindaEnum.objects)
}
