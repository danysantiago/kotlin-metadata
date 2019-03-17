package me.eugeniomarletti.kotlin.metadata.jvm

import com.google.auto.common.MoreElements
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import com.google.testing.compile.CompileTester
import com.google.testing.compile.JavaFileObjects
import com.google.testing.compile.JavaSourcesSubjectFactory
import me.eugeniomarletti.kotlin.metadata.testing.TestProcessor
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import javax.tools.JavaFileObject

@RunWith(JUnit4::class)
class JvmDescriptorUtilsTest {

    private val describeAnnotation = """
        package me.eugeniomarletti.kotlin.test;

        import java.lang.annotation.ElementType;
        import java.lang.annotation.Target;

        @Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
        public @interface Describe { }
        """.toJFO("me.eugeniomarletti.kotlin.test.Describe")

    @Test
    fun method_descriptor_simple() {
        singleRun("""
            package me.eugeniomarletti.kotlin.test;
            
            public class DummyClass {
                @Describe
                public void emptyMethod() {
                }
            }
            """.toJFO("me.eugeniomarletti.kotlin.test.DummyClass")
        ) { descriptors ->
            assertThat(descriptors.first())
                    .isEqualTo("emptyMethod()V")
        }.compilesWithoutError()
    }

    @Test
    fun method_descriptor_primitiveParams() {
        singleRun(
                """
            package me.eugeniomarletti.kotlin.test;
            
            class DummyClass {
                @Describe
                void method1(boolean yesOrNo, int number) { }
                
                @Describe
                byte method2(char letter) { return 0; }
                
                @Describe
                void method3(double realNumber1, float realNumber2) { }
                
                @Describe
                void method4(long bigNumber, short littlerNumber) { }
            }
            """.toJFO("me.eugeniomarletti.kotlin.test.DummyClass")
        ) { descriptors ->
            assertThat(descriptors)
                    .isEqualTo(setOf("method1(ZI)V", "method2(C)B", "method3(DF)V", "method4(JS)V"))
        }.compilesWithoutError()
    }

    @Test
    fun method_descriptor_classParam_javaTypes() {
        singleRun(
                """
            package me.eugeniomarletti.kotlin.test;
            
            import java.util.ArrayList;
            import java.util.List;
            
            class DummyClass {
                @Describe
                void method1(Object something) { }
                
                @Describe
                Object method2() { return null; }
                
                @Describe
                List<String> method3(ArrayList<Integer> list) { return null; }
            }
            """.toJFO("me.eugeniomarletti.kotlin.test.DummyClass")
        ) { descriptors ->
            assertThat(descriptors).isEqualTo(
                    setOf(
                            "method1(Ljava/lang/Object;)V",
                            "method2()Ljava/lang/Object;",
                            "method3(Ljava/util/ArrayList;)Ljava/util/List;"
                    )
            )
        }.compilesWithoutError()
    }

    @Test
    fun method_descriptor_classParam_testClass() {
        val extraJfo = """
            package me.eugeniomarletti.kotlin.test;
            class DataClass { }
            """.toJFO("me.eugeniomarletti.kotlin.test.DataClass")
        singleRun("""
            package me.eugeniomarletti.kotlin.test;
            
            class DummyClass {
                @Describe
                void method1(DataClass data) { }
                
                @Describe
                DataClass method2() { return null; }
            }
            """.toJFO("me.eugeniomarletti.kotlin.test.DummyClass"), extraJfo
        ) { descriptors ->
            assertThat(descriptors).isEqualTo(
                    setOf(
                            "method1(Lme/eugeniomarletti/kotlin/test/DataClass;)V",
                            "method2()Lme/eugeniomarletti/kotlin/test/DataClass;"
                    )
            )
        }.compilesWithoutError()
    }

    @Test
    fun method_descriptor_classParam_innerTestClass() {
        val extraJfo = """
            package me.eugeniomarletti.kotlin.test;
            
            class DataClass {
            
                class MemberInnerData { }
                
                static class StaticInnerData { }
                
                enum EnumData {
                    VALUE1, VALUE2
                }
            }
            """.toJFO("me.eugeniomarletti.kotlin.test.DataClass")
        singleRun("""
            package me.eugeniomarletti.kotlin.test;
            
            class DummyClass {
                @Describe
                void method1(DataClass.MemberInnerData data) { }
                
                @Describe
                void method2(DataClass.StaticInnerData data) { }
                
                @Describe
                void method3(DataClass.EnumData enumData) { }
                
                @Describe
                DataClass.StaticInnerData method4() { return null; }
            }
            """.toJFO("me.eugeniomarletti.kotlin.test.DummyClass"), extraJfo
        ) { descriptors ->
            assertThat(descriptors).isEqualTo(
                    setOf(
                            "method1(Lme/eugeniomarletti/kotlin/test/DataClass\$MemberInnerData;)V",
                            "method2(Lme/eugeniomarletti/kotlin/test/DataClass\$StaticInnerData;)V",
                            "method3(Lme/eugeniomarletti/kotlin/test/DataClass\$EnumData;)V",
                            "method4()Lme/eugeniomarletti/kotlin/test/DataClass\$StaticInnerData;"
                    )
            )
        }.compilesWithoutError()
    }

    @Test
    fun method_descriptor_arrayParams() {
        val extraJfo = """
            package me.eugeniomarletti.kotlin.test;
            class DataClass { }
            """.toJFO("me.eugeniomarletti.kotlin.test.DataClass")
        singleRun("""
            package me.eugeniomarletti.kotlin.test;
            
            class DummyClass {
                @Describe
                void method1(DataClass[] data) { }
                
                @Describe
                DataClass[] method2() { return null; }
                
                @Describe
                void method3(int[] array) { }
                
                @Describe
                void method4(int... array) { }
            }
            """.toJFO("me.eugeniomarletti.kotlin.test.DummyClass"), extraJfo
        ) { descriptors ->
            assertThat(descriptors).isEqualTo(
                    setOf(
                            "method1([Lme/eugeniomarletti/kotlin/test/DataClass;)V",
                            "method2()[Lme/eugeniomarletti/kotlin/test/DataClass;",
                            "method3([I)V",
                            "method4([I)V"
                    )
            )
        }.compilesWithoutError()
    }

    private fun String.toJFO(qName: String): JavaFileObject = JavaFileObjects.forSourceLines(qName, this)

    // Run TestProcessor to gather jvm signature of method elements annotated with @Describe
    private fun singleRun(
            vararg jfo: JavaFileObject,
            handler: (Set<String>) -> Unit
    ): CompileTester = Truth.assertAbout(JavaSourcesSubjectFactory.javaSources())
            .that(listOf(describeAnnotation) + jfo)
            .processedWith(TestProcessor.builder()
                    .nextRunHandler { invocation ->
                        invocation.roundEnv.getElementsAnnotatedWith(invocation.annotations.first()).map { element ->
                            with(invocation.metadataUtils) { MoreElements.asExecutable(element).jvmMethodSignature }
                        }.toSet().let(handler)
                        true
                    }
                    .forAnnotations("me.eugeniomarletti.kotlin.test.Describe")
                    .build())

}