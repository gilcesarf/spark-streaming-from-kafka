package com.myspark;

import kafka.server.KafkaConfig;
import kafka.server.KafkaServerStartable;
import org.apache.kafka.clients.producer.*;
import org.apache.zookeeper.server.ZooKeeperServerMain;
import org.junit.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Properties;


public class JavaKafkaWordCountTest {
    private static final String TOPIC = "topic-1";
    private static final String BROKERHOST = "127.0.0.1";
    private static final String BROKERPORT = "9092";
    private static final String ZKPORT = "2181";

    private String nodeId = "0";
    private String zkConnect = "localhost:" + ZKPORT;
    private KafkaServerStartable server;
    KafkaProducer<Integer, byte[]> producer;


    @Before
    public void setup() throws IOException {
        //zookeeper
        startZK();
        //start kafka
        startKafka();
        // setup producer
        setupProducer();
    }

    @After
    public void tearDown() throws Exception {
        server.shutdown();
        server.awaitShutdown();
    }

    private static void startZK() throws IOException {
        final File zkTmpDir = File.createTempFile("zookeeper", "test");
        zkTmpDir.delete();
        zkTmpDir.mkdir();

        new Thread() {
            @Override
            public void run() {
                ZooKeeperServerMain.main(new String [] {ZKPORT,  zkTmpDir.getAbsolutePath()});
            }
        }.start();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }
    }

    private void startKafka() {
        Properties props = new Properties();
        props.put("broker.id", nodeId);
        props.put("port", BROKERPORT);
        props.put("zookeeper.connect", zkConnect);
        props.put("host.name", "127.0.0.1");
        KafkaConfig conf = new KafkaConfig(props);
        server = new KafkaServerStartable(conf);
        server.startup();
    }

    private void setupProducer() {
        Properties producerProps = new Properties();
        producerProps.setProperty("bootstrap.servers", BROKERHOST + ":" + BROKERPORT);
        producerProps.setProperty("key.serializer","org.apache.kafka.common.serialization.IntegerSerializer");
        producerProps.setProperty("value.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer");
        producer = new KafkaProducer<>(producerProps);
    }

    @Test
    public void testSparkWordCount() throws Exception {
        Thread t =  new Thread(() -> {
            String[] args = {"localhost", "grp-1", TOPIC, "2"};
            JavaKafkaWordCount.main(args);
            System.out.println("End Child Thread");
        });
        t.start();

        for (int i=0; i<1000; i++){
            producer.send(new ProducerRecord<>(TOPIC, 0, 1, ("There are some words here to count -" + Integer.toString(i)).getBytes(Charset.forName("UTF-8"))));
            Thread.sleep(10);
        }
        System.out.println("End Test");
    }

}

