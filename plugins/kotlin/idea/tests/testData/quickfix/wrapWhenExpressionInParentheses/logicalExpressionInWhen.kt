// "Wrap expression in parentheses" "true"
interface A {
    operator fun contains(other: A): Boolean
}

fun test(x: A, b: Boolean) {
    when (b) {
        x in x<caret> -> {}
    }
}