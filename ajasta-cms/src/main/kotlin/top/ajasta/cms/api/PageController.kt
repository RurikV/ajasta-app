package top.ajasta.cms.api

import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import top.ajasta.cms.core.*
import java.util.concurrent.ConcurrentHashMap

@RestController
@RequestMapping("/api/cms")
class PageController(
    private val registry: ComponentRegistry
) {
    private val log = LoggerFactory.getLogger(PageController::class.java)

    // Very simple in-memory page store (for demo). Could be DB/FS/remote store.
    private val pages = ConcurrentHashMap<String, PageDefinition>()

    init {
        // demo page available at slug "home"
        if (!pages.containsKey("home")) {
            pages["home"] = PageDefinition(
                slug = "home",
                title = "Welcome to CMS",
                components = listOf(
                    ComponentInstance(
                        type = "text",
                        params = mapOf("text" to "Hello from component-driven CMS!", "tag" to "h2")
                    ),
                    ComponentInstance(
                        type = "image",
                        params = mapOf(
                            "url" to "https://placehold.co/600x600",
                            "alt" to "Placeholder banner",
                            "width" to 600,
                            "height" to 600
                        )
                    )
                )
            )
        }
    }

    data class PageRenderDTO(
        val slug: String,
        val title: String? = null,
        val components: List<RenderedComponent>
    )

    @GetMapping("/pages/{slug}")
    fun getPage(@PathVariable slug: String, @RequestHeader(name = "Accept-Language", required = false) locale: String?): ResponseEntity<PageRenderDTO> {
        val pd = pages[slug] ?: return ResponseEntity.notFound().build()
        val ctx = RenderContext(
            requestId = null,
            locale = locale
        )
        val rendered = pd.components.map { registry.render(it, ctx) }
        return ResponseEntity.ok(PageRenderDTO(slug = pd.slug, title = pd.title, components = rendered))
    }

    // Simple endpoint to upsert a page definition (accepts JSON). In real-world: validation + auth.
    @PutMapping("/pages/{slug}")
    fun upsertPage(@PathVariable slug: String, @RequestBody def: PageDefinition): ResponseEntity<PageDefinition> {
        require(slug == def.slug) { "Slug path and body.slug must match" }
        pages[slug] = def
        log.info("Upserted page: {} with {} components", slug, def.components.size)
        return ResponseEntity.ok(def)
    }
}
