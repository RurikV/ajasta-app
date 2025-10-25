package top.ajasta.cms.config

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

@Configuration
class WebConfig : WebMvcConfigurer {
    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/api/cms/**")
            .allowedOrigins("*")
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
    }

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(RequestContextInterceptor())
            .addPathPatterns("/api/cms/**")
    }
}

class RequestContextInterceptor : HandlerInterceptor {
    private val log = LoggerFactory.getLogger(RequestContextInterceptor::class.java)

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        // Example middleware: attach a request ID and log
        val rid = request.getHeader("X-Request-Id") ?: java.util.UUID.randomUUID().toString()
        request.setAttribute("requestId", rid)
        response.setHeader("X-Request-Id", rid)
        log.debug("[CMS] ${'$'}{request.method} ${'$'}{request.requestURI} rid=${'$'}rid")
        return true
    }
}
