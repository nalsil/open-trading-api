package com.kis.sample.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "kis")
public class KisConfig {

    private String mode = "demo"; // prod 또는 demo

    private ProdConfig prod = new ProdConfig();
    private DemoConfig demo = new DemoConfig();
    private AccountConfig account = new AccountConfig();

    @Getter
    @Setter
    public static class ProdConfig {
        private String appKey;
        private String appSecret;
        private String baseUrl;
    }

    @Getter
    @Setter
    public static class DemoConfig {
        private String appKey;
        private String appSecret;
        private String baseUrl;
    }

    @Getter
    @Setter
    public static class AccountConfig {
        private String number;
        private String productCode;
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    public String getAppKey() {
        return "demo".equals(mode) ? demo.getAppKey() : prod.getAppKey();
    }

    public String getAppSecret() {
        return "demo".equals(mode) ? demo.getAppSecret() : prod.getAppSecret();
    }

    public String getBaseUrl() {
        return "demo".equals(mode) ? demo.getBaseUrl() : prod.getBaseUrl();
    }

    public boolean isProd() {
        return "prod".equals(mode);
    }
}
