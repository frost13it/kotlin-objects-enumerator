package tk.frostbit.objectsenumerator.api

interface ObjectsEnumerator<T : Any> {

    val objects: List<T>
        get() {
            TODO("to be replaced by compiler plugin")
        }

}
