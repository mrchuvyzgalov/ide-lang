package compiler

import java.io.File

fun main() {
    val fileName = "examples/fib.idl" //Provide the exact location of your file

    val text = File(fileName).readText()

    val compiler = Compiler(text)
    compiler.run()
}