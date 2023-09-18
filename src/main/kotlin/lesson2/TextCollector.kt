package lesson2

interface TextCollector {

    fun add(line: String)
    fun add(index: Int, line: String)
    fun remove(index: Int, length: Int)
    operator fun get(indexTo: Int, indexFrom: Int = indexTo): String

}