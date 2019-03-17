package me.eugeniomarletti.kotlin.metadata.testing

import me.eugeniomarletti.kotlin.metadata.KotlinMetadataUtils
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.TypeElement

data class TestInvocation(
        val processingEnv: ProcessingEnvironment,
        val metadataUtils: KotlinMetadataUtils,
        val annotations: Set<TypeElement>,
        val roundEnv: RoundEnvironment
)