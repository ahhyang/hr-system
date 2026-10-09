package com.hr.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Configuration
public class DatabaseConfig {

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties dataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Primary
    public DataSource dataSource(DataSourceProperties properties) {
        String url = properties.getUrl();
        // Neon/Render style URLs (postgres[ql]://user:pass@host/db) carry credentials the JDBC driver won't parse
        if (url != null && (url.startsWith("postgresql://") || url.startsWith("postgres://"))) {
            URI uri = URI.create(url);
            String userInfo = uri.getRawUserInfo();
            if (userInfo != null) {
                String[] parts = userInfo.split(":", 2);
                properties.setUsername(URLDecoder.decode(parts[0], StandardCharsets.UTF_8));
                properties.setPassword(parts.length > 1 ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : null);
            }
            properties.setUrl("jdbc:postgresql://" + uri.getHost()
                    + (uri.getPort() > 0 ? ":" + uri.getPort() : "")
                    + uri.getRawPath()
                    + (uri.getRawQuery() != null ? "?" + uri.getRawQuery() : ""));
        }
        HikariDataSource dataSource = properties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
        dataSource.setMaximumPoolSize(5);
        return dataSource;
    }
}
