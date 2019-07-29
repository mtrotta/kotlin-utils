package org.matteo.utils.collection

fun <T> chunker(iterable: Iterable<T>, keyExtractor: (T) -> Any, onChunk: (Collection<T>) -> Unit) {
    val iterator = iterable.iterator()
    var previousKey: Any? = null
    var chunk = mutableListOf<T>()
    while (iterator.hasNext()) {
        val item = iterator.next()
        val key = keyExtractor(item)
        if (previousKey == null || previousKey == key) {
            chunk.add(item)
        } else {
            onChunk(chunk)
            chunk = mutableListOf(item)
        }
        previousKey = key
    }
    if (chunk.isNotEmpty()) {
        onChunk(chunk)
    }
}