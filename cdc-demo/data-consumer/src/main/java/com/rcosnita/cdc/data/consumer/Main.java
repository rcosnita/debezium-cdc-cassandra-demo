package com.rcosnita.cdc.data.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.prometheus.metrics.core.metrics.Counter;
import io.prometheus.metrics.core.metrics.Histogram;
import io.prometheus.metrics.exporter.httpserver.HTTPServer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.Stores;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Calendar;
import java.util.Properties;

public class Main {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class.getName());

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String INPUT_TOPIC = "my_cluster.cdc_experiment.persons";
    private static final String OUTPUT_TOPIC = "my_cluster.cdc_experiment.persons.processed";

    private Counter numOfMessagesMetric;
    private Histogram latencyMetric;

    /**
     * Builds the configuration for Kafka Streams topology.
     * @return the Kafka configuration
     */
    private Properties buildKafkaConfig() {
        final var streamsConfiguration = new Properties();

        streamsConfiguration.put(StreamsConfig.APPLICATION_ID_CONFIG, "cdc-e2e-time-measure");
        streamsConfiguration.put(StreamsConfig.CLIENT_ID_CONFIG, "cdc-e2e-time-measure");
        streamsConfiguration.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        streamsConfiguration.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        streamsConfiguration.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        streamsConfiguration.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        streamsConfiguration.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 100 * 1000);

        return streamsConfiguration;
    }

    /**
     * Provides a simple deduplication processor for persons changes. We assume a single change event is relevant
     * for a given notification.
     */
    private static class DedupProcessor implements Processor<String, String, String, String> {
        private ProcessorContext<String, String> context;
        private final String storeName;

        public DedupProcessor(String storeName) {
            this.storeName = storeName;
        }

        @Override
        public void process(Record<String, String> record) {
            KeyValueStore<String, Boolean> store = context.getStateStore(this.storeName);
            var personId = record.key();

            if (store.putIfAbsent(personId, true) == null) {
                this.context.forward(record);
            } else {
                this.context.commit();
            }
        }

        @Override
        public void init(ProcessorContext<String, String> context) {
            this.context = context;
        }
    }

    /**
     * Builds the Kafka Streams application and publishes the E2E latency time metric to prometheus.
     */
    private KafkaStreams buildApp() {
        final var builder = new StreamsBuilder();
        builder.addStateStore(Stores.keyValueStoreBuilder(
                        Stores.persistentKeyValueStore("persons-dedup"),
                        Serdes.String(),
                        Serdes.Boolean()))
                .stream(INPUT_TOPIC, Consumed.with(Serdes.String(), Serdes.String()))
                .process(() -> new DedupProcessor("persons-dedup"), "persons-dedup")
                .mapValues((entry) -> {
                   try {
                       var tree = OBJECT_MAPPER.readTree(entry);
                       var creationTime = tree.get("after").get("creation_time").get("value").asLong();
                       var latency = Calendar.getInstance().getTimeInMillis() - creationTime;

                       numOfMessagesMetric.inc(1);
                       latencyMetric.observe(latency * 1.0 / 1000);

                       return entry;
                   } catch (IOException ioex) {
                       throw new RuntimeException(ioex);
                   }
                }).to(OUTPUT_TOPIC);

        var app = new KafkaStreams(builder.build(), buildKafkaConfig());
        app.start();

        return app;
    }

    /**
     * Starts the prometheus exporter scraping endpoint used for collecting data points.
     */
    private HTTPServer startPrometheus() throws IOException  {
        latencyMetric = Histogram.builder()
                .name("experiment.latency.ms")
                .help("Provides the latency between the time an object was generated until it reached kafka over CDC/Debezium")
                .classicUpperBounds(1, 5, 10, 15, 20, 25, 30, 35, 40, 50, 75, 100, 250, 500, 750, 1000, 2500, 5000, 7500,
                        10_000, 12_500, 15_000, 17_500, 20_000, 50_000, 70_000, 100_000, 250_000, 500_000)
                .register();

        numOfMessagesMetric = Counter.builder()
                .name("experiment.num_messages")
                .help("Provides the number of messages processed.")
                .register();

        return HTTPServer.builder().port(12400).buildAndStart();
    }

    public static void main(String[] args) throws IOException {
        var appCls = new Main();
        var httpServer = appCls.startPrometheus();

        var app = appCls.buildApp();

        Runtime.getRuntime().addShutdownHook(new Thread(app::close));
        Runtime.getRuntime().addShutdownHook(new Thread(httpServer::close));
    }
}