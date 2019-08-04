objects-enumerator
==================

Kotlin compiler plugin for automatic compile-time enumeration of sealed class's objects.

For now it's a PoC, don't use it in the production.

Example (full project is in the [samples](samples) directory):
```kotlin
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
```

### Build & development

To build project just run `gradle build`.

To build samples run `gradle -p samples build`.

When debugging don't forget to pass `-Dkotlin.compiler.execution.strategy="in-process"`
JVM option to Gradle.
