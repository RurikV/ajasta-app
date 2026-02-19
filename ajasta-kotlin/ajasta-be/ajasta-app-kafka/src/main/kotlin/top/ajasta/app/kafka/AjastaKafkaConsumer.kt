package top.ajasta.app.kafka

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.errors.WakeupException
import org.slf4j.LoggerFactory
import top.ajasta.api.v1.apiV1Mapper
import top.ajasta.api.v1.mappers.fromTransport
import top.ajasta.api.v1.mappers.toTransport
import top.ajasta.api.v1.models.*
import top.ajasta.app.common.AjastaStubProcessor
import top.ajasta.app.common.IAjastaAppSettings
import top.ajasta.biz.BizContext
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Kafka consumer for processing Ajasta requests.
 */
class AjastaKafkaConsumer(
    private val config: AjastaKafkaConfig,
    private val consumer: KafkaConsumer<String, String> = config.createKafkaConsumer(),
    private val producer: KafkaProducer<String, String> = config.createKafkaProducer()
) : AutoCloseable, IAjastaAppSettings {

    private val log = LoggerFactory.getLogger(this::class.java)
    private val running = AtomicBoolean(true)
    override val processor = config.processor

    /**
     * Blocking start of the consumer.
     */
    fun start(): Unit = runBlocking {
        consumer.subscribe(listOf(config.inputTopic))
        log.info("Kafka consumer started, listening to topic: ${config.inputTopic}")

        try {
            while (running.get()) {
                val records = withContext(Dispatchers.IO) {
                    consumer.poll(Duration.ofSeconds(1))
                }

                for (record in records) {
                    try {
                        processRecord(record)
                    } catch (e: Exception) {
                        log.error("Error processing record: ${record.key()}", e)
                    }
                }
            }
        } catch (e: WakeupException) {
            if (running.get()) throw e
        } finally {
            withContext(NonCancellable) {
                consumer.close()
                producer.close()
            }
        }
    }

    private suspend fun processRecord(record: ConsumerRecord<String, String>) {
        log.info("Received message: key=${record.key()}")

        val request = deserialize(record.value())
        val response = processRequest(request)
        val jsonResponse = serialize(response)

        sendResponse(record.key(), jsonResponse)
    }

    private fun deserialize(json: String): Any {
        val tree = apiV1Mapper.readTree(json)
        val requestType = tree.get("requestType")?.asText()

        return when (requestType) {
            "createBooking" -> apiV1Mapper.readValue(json, BookingCreateRequest::class.java)
            "readBooking" -> apiV1Mapper.readValue(json, BookingReadRequest::class.java)
            "updateBooking" -> apiV1Mapper.readValue(json, BookingUpdateRequest::class.java)
            "deleteBooking" -> apiV1Mapper.readValue(json, BookingDeleteRequest::class.java)
            "searchBookings" -> apiV1Mapper.readValue(json, BookingSearchRequest::class.java)
            "createResource" -> apiV1Mapper.readValue(json, ResourceCreateRequest::class.java)
            "readResource" -> apiV1Mapper.readValue(json, ResourceReadRequest::class.java)
            "updateResource" -> apiV1Mapper.readValue(json, ResourceUpdateRequest::class.java)
            "deleteResource" -> apiV1Mapper.readValue(json, ResourceDeleteRequest::class.java)
            "searchResources" -> apiV1Mapper.readValue(json, ResourceSearchRequest::class.java)
            "getAvailability" -> apiV1Mapper.readValue(json, AvailabilityRequest::class.java)
            else -> throw IllegalArgumentException("Unknown request type: $requestType")
        }
    }

    private suspend fun processRequest(request: Any): Any {
        val ctx = BizContext()
        ctx.fromTransport(request)
        processor.exec(ctx)
        return ctx.toTransport()
    }

    private fun serialize(response: Any): String {
        return apiV1Mapper.writeValueAsString(response)
    }

    private suspend fun sendResponse(key: String?, json: String) {
        val record = ProducerRecord<String, String>(
            config.outputTopic,
            key,
            json
        )
        log.info("Sending response to topic ${config.outputTopic}")
        withContext(Dispatchers.IO) {
            producer.send(record)
        }
    }

    override fun close() {
        running.set(false)
        consumer.wakeup()
    }
}
