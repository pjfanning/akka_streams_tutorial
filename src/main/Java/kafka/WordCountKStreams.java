package kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KStreamBuilder;
import org.apache.kafka.streams.kstream.KTable;

import java.util.Arrays;
import java.util.Properties;

/**
 * Inspired by:
 * https://github.com/gwenshap/kafka-streams-wordcount
 *
 * and adapted to latest fashion using KTable - see:
 * https://kafka.apache.org/documentation/streams
 *
 */
public class WordCountKStreams {

    public static void main(String[] args) throws Exception{

        Properties config = new Properties();
        config.put(StreamsConfig.APPLICATION_ID_CONFIG, "wordcount");
        config.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        config.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        config.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());

        //What to do when there is no initial offset in Kafka or if the current offset does not exist any more on the server (e.g. because that data has been deleted)
        //earliest: automatically reset the offset to the earliest offset
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        KStreamBuilder builder = new KStreamBuilder();
        KStream<String, String> textLines = builder.stream("wordcount-input");
        KTable<String, Long> wordCounts = textLines
                .flatMapValues(textLine -> Arrays.asList(textLine.toLowerCase().split("\\W+")))
                .filter((key, value) -> (!(value.equals("truth")))) //we don't want that do we?
                .filter((key, value) -> (!(value.equals(""))))
                .peek((key, value) -> System.out.println("Processing value: " + value))
                .groupBy((key, word) -> word)
                .count("Counts");
        wordCounts.to(Serdes.String(), Serdes.Long(), "wordcount-output");

        KafkaStreams streams = new KafkaStreams(builder, config);
        streams.start();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                System.out.println("Shutdown Hook is running - about to close stream!");
                streams.close();
                System.out.println("Shutdown Hook is running - stream closed!");
            }
        });
        System.out.println("Application running for a loooong time...");
        Thread.sleep(500000000000000L);
    }
}