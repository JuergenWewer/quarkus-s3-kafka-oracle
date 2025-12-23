// src/test/java/com/example/app/TestInfraResource.java
package com.example.app;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.OracleContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.HashMap;
import java.util.Map;

public class TestInfraResource implements QuarkusTestResourceLifecycleManager {

  private KafkaContainer kafka;
  private LocalStackContainer localstack;
  private OracleContainer oracle;

  @Override
  public Map<String, String> start() {
    kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.2"));
    localstack = new LocalStackContainer(DockerImageName.parse("localstack/localstack:3.6"))
      .withServices(LocalStackContainer.Service.S3);
    oracle = new OracleContainer(DockerImageName.parse("gvenzl/oracle-xe:21-slim"))
      .withUsername("test").withPassword("oracle");

    kafka.start();
    localstack.start();
    oracle.start();

    Map<String, String> cfg = new HashMap<>();

    // Kafka für Quarkus
    cfg.put("kafka.bootstrap.servers", kafka.getBootstrapServers());

    // S3 / Localstack für S3Config (app.s3.*)
    cfg.put("app.s3.endpoint", localstack.getEndpointOverride(LocalStackContainer.Service.S3).toString());
    cfg.put("app.s3.region", localstack.getRegion());
    cfg.put("app.s3.access-key", localstack.getAccessKey());
    cfg.put("app.s3.secret-key", localstack.getSecretKey());
    cfg.put("app.s3.bucket", "names-bucket-test"); // beliebiger Test-Bucket

    // Oracle für Quarkus Datasource
    cfg.put("quarkus.datasource.jdbc.url", oracle.getJdbcUrl());
    cfg.put("quarkus.datasource.username", oracle.getUsername());
    cfg.put("quarkus.datasource.password", oracle.getPassword());

    return cfg;
  }

  @Override
  public void stop() {
    if (kafka != null) {
      kafka.stop();
    }
    if (localstack != null) {
      localstack.stop();
    }
    if (oracle != null) {
      oracle.stop();
    }
  }
}
