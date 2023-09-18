package lesson2

import lesson2.table.PieceTable

fun main() {
    addLineTest1()
    addLineTest2()
    addLineTest3()
    addLineTest4()

    println("SUCCEED")
}

private fun addLineTest1() {
    val text = "Hello"
    val add = ", world"

    val table = PieceTable(text)
    table.add(text.length, add)

    assert(table.toString() == "$text$add")
}

private fun addLineTest2() {
    val text = "Hello"
    val add = "\nworld!"

    val table = PieceTable(text)
    table.add(text.length, add)

    assert(table.toString() == "$text$add")
}

private fun addLineTest3() {
    val text = "Hello\n"
    val add = "world!"

    val table = PieceTable(text)
    table.add(text.length, add)

    assert(table.toString() == "$text$add")
}

private fun addLineTest4() {
    val text = "\nHello\n"
    val add = "world"

    val table = PieceTable(text)
    table.add(text.length, add)

    assert(table.toString() == "$text$add")
}
