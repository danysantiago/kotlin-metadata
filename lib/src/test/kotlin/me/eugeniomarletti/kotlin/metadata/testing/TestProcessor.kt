package me.eugeniomarletti.kotlin.metadata.testing

import me.eugeniomarletti.kotlin.metadata.KotlinMetadataUtils
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.TypeElement
import kotlin.reflect.KClass

class TestProcessor private constructor(
        private val handlers: List<(TestInvocation) -> Boolean>,
        private val annotations: MutableSet<String>
) : AbstractProcessor(), KotlinMetadataUtils {
    private var count = 0

    override val processingEnv: ProcessingEnvironment
        get() = super.processingEnv

    override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
        return handlers.getOrNull(count++)?.invoke(
                TestInvocation(super.processingEnv, this, annotations, roundEnv)) ?: true
    }

    override fun getSupportedAnnotationTypes(): MutableSet<String> {
        return annotations
    }

    class Builder {
        private var handlers = arrayListOf<(TestInvocation) -> Boolean>()
        private var annotations = mutableSetOf<String>()

        fun nextRunHandler(f: (TestInvocation) -> Boolean): Builder {
            handlers.add(f)
            return this
        }

        fun forAnnotations(vararg klasses: KClass<*>): Builder {
            annotations.addAll(klasses.map { it.java.canonicalName })
            return this
        }

        fun forAnnotations(vararg names: String): Builder {
            annotations.addAll(names)
            return this
        }

        fun build(): TestProcessor {
            if (annotations.isEmpty()) {
                throw IllegalStateException("must provide at least 1 annotation")
            }
            if (handlers.isEmpty()) {
                throw IllegalStateException("must provide at least 1 handler")
            }
            return TestProcessor(handlers, annotations)
        }
    }

    companion object {
        fun builder(): Builder = Builder()
    }
}