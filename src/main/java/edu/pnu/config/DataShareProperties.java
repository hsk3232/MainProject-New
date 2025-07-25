package edu.pnu.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @ToString
@Builder @NoArgsConstructor @AllArgsConstructor
@Configuration
@ConfigurationProperties(prefix = "datashare")
public class DataShareProperties {
    private int batchSize;
    private int retryMaxAttempts;
    private long retryDelayMs;
    private long batchDelayMs;
    private int restConnectTimeout;
    private int restReadTimeout;
    private String aiApiUrl;
}