interface <!HIGHLIGHTING("textAttributesKey='KOTLIN_TRAIT'")!>Base<!>

interface <!HIGHLIGHTING("textAttributesKey='KOTLIN_TRAIT'")!>Foo<!> {
    <!HIGHLIGHTING("textAttributesKey='KOTLIN_BUILTIN_ANNOTATION'")!>operator<!> fun <!HIGHLIGHTING("textAttributesKey='KOTLIN_TRAIT'")!>Base<!>.<!HIGHLIGHTING("textAttributesKey='KOTLIN_FUNCTION_DECLARATION'")!>invoke<!>() {}
}

fun <!HIGHLIGHTING("textAttributesKey='KOTLIN_TRAIT'")!>Foo<!>.<!HIGHLIGHTING("textAttributesKey='KOTLIN_FUNCTION_DECLARATION'")!>test<!>(<!HIGHLIGHTING("textAttributesKey='KOTLIN_PARAMETER'")!>param<!>: <!HIGHLIGHTING("textAttributesKey='KOTLIN_CLASS'")!>Any<!>) {
    if (<!HIGHLIGHTING("textAttributesKey='KOTLIN_PARAMETER'")!>param<!> is <!HIGHLIGHTING("textAttributesKey='KOTLIN_TRAIT'")!>Base<!>) {

        <!HIGHLIGHTING("descr='Smart cast to Base'; textAttributesKey='KOTLIN_SMART_CAST_VALUE'"), HIGHLIGHTING("textAttributesKey='KOTLIN_PARAMETER'")!>param<!>.<!HIGHLIGHTING("textAttributesKey='KOTLIN_EXTENSION_FUNCTION_CALL'")!>invoke<!>() // call 1

        <!HIGHLIGHTING("textAttributesKey='KOTLIN_VARIABLE_AS_FUNCTION_LIKE'")!>(<!HIGHLIGHTING("descr='Smart cast to Base'; textAttributesKey='KOTLIN_SMART_CAST_VALUE'")!>param<!>)<!>() // call 2

        <!HIGHLIGHTING("descr='Smart cast to Base'; textAttributesKey='KOTLIN_SMART_CAST_VALUE'"), HIGHLIGHTING("textAttributesKey='KOTLIN_PARAMETER'"), HIGHLIGHTING("textAttributesKey='KOTLIN_VARIABLE_AS_FUNCTION_LIKE'")!>param<!>() // call 3
    }
}
