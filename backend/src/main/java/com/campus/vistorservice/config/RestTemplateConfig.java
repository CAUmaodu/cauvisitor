package com.campus.vistorservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(8_000);
        factory.setReadTimeout(15_000);
        RestTemplate t = new RestTemplate(factory);
        ClientHttpRequestInterceptor ua = (req, body, ex) -> {
            req.getHeaders().add("User-Agent", "Mozilla/5.0 CAUVisitor/1.0 (path-planning)");
            return ex.execute(req, body);
        };
        t.setInterceptors(Collections.singletonList(ua));
        return t;
    }
}
