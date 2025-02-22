// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.kotlin.idea.debugger.test

import com.intellij.debugger.engine.dfaassist.DfaAssistTest
import com.intellij.debugger.mockJDI.MockStackFrame
import com.intellij.debugger.mockJDI.MockVirtualMachine
import com.intellij.debugger.mockJDI.values.MockBooleanValue
import com.intellij.debugger.mockJDI.values.MockIntegerValue
import com.intellij.debugger.mockJDI.values.MockObjectReference
import com.intellij.debugger.mockJDI.values.MockValue
import com.intellij.testFramework.LightProjectDescriptor
import org.jetbrains.kotlin.idea.test.ProjectDescriptorWithStdlibSources
import java.util.function.BiConsumer

class KotlinDfaAssistTest : DfaAssistTest() {
    override fun getProjectDescriptor(): LightProjectDescriptor = ProjectDescriptorWithStdlibSources.INSTANCE

    fun testSimple() {
        doTest("""fun test(x: Int) {
                    <caret>if (x > 0/*TRUE*/) {
                        
                    }
                    if (x in 1..6/*TRUE*/) {
                
                    }
                    if (1 in x..10/*FALSE*/) {
                
                    }
                }
                
                fun main() {
                    test(5)
                }""") { vm, frame -> frame.addVariable("x", MockIntegerValue(vm, 5)) }
    }

    fun testSuppression() {
        doTest("""fun test(x: Boolean, y: Boolean) {
               <caret>if(!x/*FALSE*/) {}
               if (x/*TRUE*/ || y) {}
               if (y || x/*TRUE*/) {}
               var z: Boolean
               z = x/*TRUE*/
            }""") { vm, frame -> frame.addVariable("x", MockBooleanValue(vm, true)) }
    }

    fun testWhen() {
        doTest("""fun obj(x: Any) {
                    <caret>when(x) {
                        is String/*FALSE*/ -> {}
                        is Int/*TRUE*/ -> {}
                    }
                }

                fun main() {
                    obj(5)
                }""") { vm, frame -> frame.addVariable("x", MockValue.createValue(1, Integer::class.java, vm)) }
    }

    fun testWrapped() {
        doTest("""fun test(x: Int) {
                    var y = x
                    val fn = { y++ }
                    fn()
                    <caret>if (y > 0/*TRUE*/) {
                
                    }
                    if (y == 0/*FALSE*/) {}
                    if (y < 0/*FALSE*/) {}
                }
                
                fun main() {
                    test(5)
                }""") { vm, frame ->
            val cls = Class.forName("kotlin.jvm.internal.Ref\$IntRef")
            val box = cls.getConstructor().newInstance()
            cls.getDeclaredField("element").set(box, 6)
            frame.addVariable("y", MockValue.createValue(box, cls, vm))
        }
    }

    class Nested(@Suppress("unused") val x:Int)

    fun testThis() {
        doTest("""package org.jetbrains.kotlin.idea.debugger.test
            class KotlinDfaAssistTest {
              class Nested(val x:Int) {
                fun test() {
                  <caret>if (x == 5/*TRUE*/) {}
                }
              }
            }
        """) { vm, frame ->
            frame.setThisValue(MockObjectReference.createObjectReference(Nested(5), Nested::class.java, vm))
        }
    }

    fun testQualified() {
        doTest("""package org.jetbrains.kotlin.idea.debugger.test
            class KotlinDfaAssistTest {
              class Nested(val x:Int)
            }
            
            fun use(n : KotlinDfaAssistTest.Nested) {
                <caret>if (n.x > 5/*TRUE*/) {}
            }
        """) { vm, frame ->
            frame.addVariable("n", MockValue.createValue(Nested(6), Nested::class.java, vm))
        }
    }

    fun testExtensionMethod() {
        doTest("""fun String.isLong(): Boolean {
                      <caret>return this.length > 5/*FALSE*/
                  }

                  fun main() {
                      "xyz".isLong()
                  }""") { vm, frame ->
            frame.addVariable("\$this\$isLong", MockValue.createValue("xyz", String::class.java, vm))
        }
    }

    fun testDeconstruction() {
        doTest("""fun test(x: Pair<Int, Int>) {
                      val (a, b) = x
                      <caret>if (a > b/*FALSE*/) {}
                  }
                  
                  fun main() {
                      test(3 to 5)
                  }""") { vm, frame ->
            frame.addVariable("a", MockIntegerValue(vm, 3))
            frame.addVariable("b", MockIntegerValue(vm, 5))
        }
    }

    private fun doTest(text: String, mockValues: BiConsumer<MockVirtualMachine, MockStackFrame>) {
        doTest(text, mockValues, "Test.kt")
    }
}