package top.ajasta.cms

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class CmsApplication {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            runApplication<CmsApplication>(*args)
        }
    }
}
