package top.ajasta.cms.core

import com.fasterxml.jackson.annotation.JsonAnyGetter
import com.fasterxml.jackson.annotation.JsonInclude
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component

/**
 * A CMS component renderer is a plugin that can render a particular component type
 * into a DTO for the frontend to interpret. New components can be added by simply
 * adding a new Spring bean implementing this interface (no changes to core code).
 */
interface ComponentRenderer {
    /** A unique type key, e.g. "text", "image", "hero" */
    fun type(): String

    /** Validate and normalize params, then return a render DTO to be sent to client */
    fun render(params: Map<String, Any?>, ctx: RenderContext = RenderContext.EMPTY): RenderedComponent
}

/**
 * Render context shared among components; can be extended. Demonstrates DI use.
 */
data class RenderContext(
    val requestId: String? = null,
    val locale: String? = null,
) {
    companion object { val EMPTY = RenderContext() }
}

/**
 * Frontend-agnostic rendered component DTO
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class RenderedComponent(
    val type: String,
    val props: Map<String, Any?> = emptyMap(),
    val children: List<RenderedComponent>? = null
) {
    @JsonAnyGetter
    fun any(): Map<String, Any?> = mapOf(
        "type" to type,
        "props" to props
    ) + (if (children != null) mapOf("children" to children) else emptyMap())
}

/**
 * Universal page definition
 */
data class PageDefinition(
    val slug: String,
    val title: String? = null,
    val components: List<ComponentInstance> = emptyList()
)

/**
 * Instance of a component defined by type and params. Supports children.
 */
data class ComponentInstance(
    val type: String,
    val params: Map<String, Any?> = emptyMap(),
    val children: List<ComponentInstance> = emptyList()
)

/**
 * Registry that discovers all ComponentRenderer beans.
 */
@Component
class ComponentRegistry(private val applicationContext: ApplicationContext) {
    private val renderers: Map<String, ComponentRenderer> by lazy {
        applicationContext.getBeansOfType(ComponentRenderer::class.java).values.associateBy { it.type() }
    }

    fun get(type: String): ComponentRenderer? = renderers[type]

    fun render(instance: ComponentInstance, ctx: RenderContext = RenderContext.EMPTY): RenderedComponent {
        val renderer = get(instance.type)
            ?: throw IllegalArgumentException("Unknown component type: ${'$'}{instance.type}")
        val renderedChildren = if (instance.children.isNotEmpty()) instance.children.map { render(it, ctx) } else null
        val base = renderer.render(instance.params, ctx)
        return base.copy(children = renderedChildren)
    }
}
