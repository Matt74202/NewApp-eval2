package com.newApp.config;

import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.cookie.CookieStore;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class ErpNextClientConfig {

    @Bean
    public CookieStore cookieStore() {
        System.out.println("Initializing CookieStore...");
        return new BasicCookieStore();
    }

    @Bean
    public RestTemplate restTemplate(CookieStore cookieStore) {
        System.out.println("Creating RestTemplate with cookie support...");
        HttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .build();

        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultCookieStore(cookieStore)
                .build();

        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setHttpClient(httpClient);

        System.out.println("RestTemplate created successfully.");
        return new RestTemplate(factory);
    }
}