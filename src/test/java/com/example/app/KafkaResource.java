package com.example.app;

import org.testcontainers.containers.KafkaContainer;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

import java.util.HashMap;
import java.util.Map;

public class KafkaResource implements QuarkusTestResourceLifecycleManager {

  KafkaContainer kafka = new KafkaContainer(
    DockerImageName.parse("confluentinc/cp-kafka:7.5.2")
  );

  @Override
  public Map<String, String> start() {
    kafka.start();

    Map<String,String> config = new HashMap<>();
    config.put("kafka.bootstrap.servers", kafka.getBootstrapServers());

    return config;
  }

  @Override
  public void stop() {
    kafka.stop();
  }
}
