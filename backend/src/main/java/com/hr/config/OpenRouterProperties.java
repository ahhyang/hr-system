package com.hr.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.openrouter")
@Getter
@Setter
public class OpenRouterProperties {
    private String apiKey = "";
    private String model = "google/gemini-2.0-flash-001";
    private String siteUrl = "http://localhost:5173";
    private String siteName = "PulseHR";
}
