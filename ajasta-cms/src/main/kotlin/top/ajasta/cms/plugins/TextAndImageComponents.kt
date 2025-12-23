package top.ajasta.cms.plugins

import org.springframework.stereotype.Component
import top.ajasta.cms.core.ComponentRenderer
import top.ajasta.cms.core.RenderContext
import top.ajasta.cms.core.RenderedComponent

@Component
class TextComponent : ComponentRenderer {
    override fun type(): String = "text"

    override fun render(params: Map<String, Any?>, ctx: RenderContext): RenderedComponent {
        val text = (params["text"] as? String)?.takeIf { it.isNotBlank() } ?: ""
        val tag = (params["tag"] as? String)?.lowercase()?.takeIf { it in setOf("p","h1","h2","h3","h4","h5","h6","span") } ?: "p"
        val align = (params["align"] as? String)?.lowercase()?.takeIf { it in setOf("left","right","center") }
        val props = buildMap<String, Any?> {
            put("text", text)
            put("tag", tag)
            if (align != null) put("align", align)
        }
        return RenderedComponent(type(), props)
    }
}

@Component
class ImageComponent : ComponentRenderer {
    override fun type(): String = "image"

    override fun render(params: Map<String, Any?>, ctx: RenderContext): RenderedComponent {
        val url = params["url"] as? String ?: ""
        val alt = params["alt"] as? String
        val width = (params["width"] as? Number)?.toInt()
        val height = (params["height"] as? Number)?.toInt()
        val props = buildMap<String, Any?> {
            put("url", url)
            if (!alt.isNullOrBlank()) put("alt", alt)
            if (width != null) put("width", width)
            if (height != null) put("height", height)
        }
        return RenderedComponent(type(), props)
    }
}
