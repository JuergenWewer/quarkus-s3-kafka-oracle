package com.example.app;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "app.s3")
public interface S3Config {
    String endpoint();
    @WithDefault("us-east-1") String region();
    @WithDefault("test") String accessKey();
    @WithDefault("test") String secretKey();
    @WithDefault("names-bucket") String bucket();
}