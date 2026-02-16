package top.ajasta.api.v1

import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule

val apiV1Mapper = JsonMapper.builder().run {
    enable(MapperFeature.USE_BASE_TYPE_AS_DEFAULT_IMPL)
    addModule(KotlinModule.Builder().build())
    build()
}

@Suppress("unused")
fun <T : Any> apiV1Serialize(obj: T): String = apiV1Mapper.writeValueAsString(obj)

@Suppress("unused")
fun <T : Any> apiV1Deserialize(json: String, clazz: Class<T>): T =
    apiV1Mapper.readValue(json, clazz)
