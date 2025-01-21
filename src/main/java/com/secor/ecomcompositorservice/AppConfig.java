package com.secor.ecomcompositorservice;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.netflix.eureka.EurekaDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;


import java.util.List;

@Configuration
public class AppConfig {

    @Autowired
    private EurekaDiscoveryClient discoveryClient;

    public ServiceInstance getServiceInstance(String serviceName) {
        List<ServiceInstance> instances = discoveryClient.getInstances(serviceName);
        if (instances.isEmpty()) {
            throw new RuntimeException("No instances found for " + serviceName);
        }
        return instances.get(0); // LOAD BALANCING ALGORITHM WILL GO HERE
    }

    @Bean("catalogWebClient")
    public WebClient webClientProfileService(WebClient.Builder webClientBuilder) {
        ServiceInstance instance = getServiceInstance("ecom-catalog-service");
        String hostname = instance.getHost();
        int port = instance.getPort();

        return webClientBuilder
                .baseUrl("http://" + hostname + ":" + port + "/products")
                .filter(new LoggingWebClientFilter())
                .build();
    }

    @Bean("inventoryWebClient")
    public WebClient webClientInventoryService(WebClient.Builder webClientBuilder) {
        ServiceInstance instance = getServiceInstance("ecom-inventory-service");
        String hostname = instance.getHost();
        int port = instance.getPort();

        return webClientBuilder
                .baseUrl("http://" + hostname + ":" + port + "/inventory")
                .filter(new LoggingWebClientFilter())
                .build();
    }

    @Bean("orderWebClient")
    public WebClient webClientOrderService(WebClient.Builder webClientBuilder) {
        ServiceInstance instance = getServiceInstance("ecom-order-service");
        String hostname = instance.getHost();
        int port = instance.getPort();

        return webClientBuilder
                .baseUrl("http://" + hostname + ":" + port + "/orders")
                .filter(new LoggingWebClientFilter())
                .build();
    }

    @Bean("paymentWebClient")
    public WebClient webClientPaymentService(WebClient.Builder webClientBuilder) {
        ServiceInstance instance = getServiceInstance("ecom-payment-service");
        String hostname = instance.getHost();
        int port = instance.getPort();

        return webClientBuilder
                .baseUrl("http://" + hostname + ":" + port + "/payments")
                .filter(new LoggingWebClientFilter())
                .build();
    }

}
