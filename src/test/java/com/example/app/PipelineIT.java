package com.example.app;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.common.QuarkusTestResource;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.OracleContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.*;

import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.common.serialization.StringDeserializer;


import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@QuarkusTestResource(KafkaResource.class)
public class PipelineIT {

    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.2"));
    static LocalStackContainer localstack = new LocalStackContainer(DockerImageName.parse("localstack/localstack:3.6"))
            .withServices(LocalStackContainer.Service.S3);
    static OracleContainer oracle = new OracleContainer(DockerImageName.parse("gvenzl/oracle-xe:21-slim"))
            .withUsername("system").withPassword("oracle");

    static S3Client s3;
    static String bucket = "names-bucket";

    @BeforeAll
    public static void beforeAll() {
        kafka.start();
        localstack.start();
        oracle.start();

        System.setProperty("kafka.bootstrap.servers", kafka.getBootstrapServers());
        System.setProperty("S3_ENDPOINT", localstack.getEndpointOverride(LocalStackContainer.Service.S3).toString());
        System.setProperty("S3_ACCESS_KEY", localstack.getAccessKey());
        System.setProperty("S3_SECRET_KEY", localstack.getSecretKey());
        System.setProperty("S3_BUCKET", bucket);
        System.setProperty("ORACLE_JDBC_URL", oracle.getJdbcUrl());
        System.setProperty("ORACLE_USERNAME", oracle.getUsername());
        System.setProperty("ORACLE_PASSWORD", oracle.getPassword());

        s3 = S3Client.builder()
                .endpointOverride(localstack.getEndpointOverride(LocalStackContainer.Service.S3))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(localstack.getAccessKey(), localstack.getSecretKey())))
                .region(Region.of("us-east-1"))
                .forcePathStyle(true)
                .build();

        try {
            s3.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
        } catch (BucketAlreadyOwnedByYouException ignored) {}
    }

    @AfterAll
    public static void afterAll() {
        oracle.stop();
        localstack.stop();
        kafka.stop();
    }

    @Test
    @Order(1)
    public void endToEnd() {
        var producerProps = new Properties();
        producerProps.put("bootstrap.servers", kafka.getBootstrapServers());
        producerProps.put("key.serializer", StringSerializer.class.getName());
        producerProps.put("value.serializer", StringSerializer.class.getName());
        try (KafkaProducer<String, String> producer = new KafkaProducer<>(producerProps)) {
            String json = "{"name":"Alice-" + UUID.randomUUID() + ""}";
            producer.send(new ProducerRecord<>("names-in", json));
            producer.flush();
        }

        var consumerProps = new Properties();
        consumerProps.put("bootstrap.servers", kafka.getBootstrapServers());
        consumerProps.put("group.id", "test-" + UUID.randomUUID());
        consumerProps.put("auto.offset.reset", "earliest");
        consumerProps.put("enable.auto.commit", "false");
        consumerProps.put("key.deserializer", StringDeserializer.class.getName());
        consumerProps.put("value.deserializer", StringDeserializer.class.getName());
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps)) {
            consumer.subscribe(List.of("names-out"));
            long deadline = System.currentTimeMillis() + Duration.ofSeconds(60).toMillis();
            String id = null;
            while (System.currentTimeMillis() < deadline && id == null) {
                var records = consumer.poll(Duration.ofMillis(500));
                if (!records.isEmpty()) {
                    id = records.iterator().next().value();
                }
            }
            assertThat(id).as("ID from names-out").isNotNull();
        }

        var listed = s3.listObjectsV2(ListObjectsV2Request.builder().bucket(bucket).build());
        assertThat(listed.hasContents()).isTrue();
    }
}

