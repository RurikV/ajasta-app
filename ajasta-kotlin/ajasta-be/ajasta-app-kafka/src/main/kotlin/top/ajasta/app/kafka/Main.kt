package top.ajasta.app.kafka

import top.ajasta.app.common.AjastaStubProcessor

fun main() {
    val config = AjastaKafkaConfig(
        processor = AjastaStubProcessor()
    )
    val consumer = AjastaKafkaConsumer(config)
    Runtime.getRuntime().addShutdownHook(Thread {
        consumer.close()
    })
    consumer.start()
}
