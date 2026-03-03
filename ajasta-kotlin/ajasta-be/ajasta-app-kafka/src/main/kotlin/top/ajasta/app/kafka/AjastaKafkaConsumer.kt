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
import top.ajasta.api.v1.models.IRequest
import top.ajasta.api.v1.models.IResponse
import top.ajasta.app.common.IAjastaAppSettings
import top.ajasta.biz.BizContext
import top.ajasta.repo.IRepoBooking
import top.ajasta.repo.IRepoResource
import top.ajasta.repo.inmemory.RepoBookingInMemory
import top.ajasta.repo.inmemory.RepoResourceInMemory
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Kafka consumer for processing Ajasta requests.
 * Uses POSTful API with polymorphic IRequest/IResponse interfaces.
 * Maintains singleton repositories for data persistence across requests.
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
     * Singleton repositories for data persistence across requests.
     */
    private val repoBooking: IRepoBooking = RepoBookingInMemory()
    private val repoResource: IRepoResource = RepoResourceInMemory()

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

        val request = deserializeRequest(record.value())
        val response = processRequest(request)
        val jsonResponse = serializeResponse(response)

        sendResponse(record.key(), jsonResponse)
    }

    /**
     * Deserializes JSON to IRequest using Jackson's polymorphic type handling.
     * The requestType field determines the concrete class instantiated.
     */
    private fun deserializeRequest(json: String): IRequest {
        return apiV1Mapper.readValue(json, IRequest::class.java)
    }

    private suspend fun processRequest(request: IRequest): IResponse {
        val ctx = BizContext().apply {
            // Use singleton repositories (shared across all requests)
            this.repoBooking = this@AjastaKafkaConsumer.repoBooking
            this.repoResource = this@AjastaKafkaConsumer.repoResource
        }
        ctx.fromTransport(request)
        processor.exec(ctx)
        return ctx.toTransport()
    }

    private fun serializeResponse(response: IResponse): String {
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
