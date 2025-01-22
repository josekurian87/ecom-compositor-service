package com.secor.ecomcompositorservice;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1")
public class CompositorController {

    private static final Logger LOG = LoggerFactory.getLogger(CompositorController.class);

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    Producer producer;

    @Autowired
    @Qualifier("catalogWebClient")
    WebClient catalogWebClient;

    @Autowired
    @Qualifier("inventoryWebClient")
    WebClient inventoryWebClient;

    @Autowired
    @Qualifier("orderWebClient")
    WebClient orderWebClient;

    @Autowired
    @Qualifier("paymentWebClient")
    WebClient paymentWebClient;

    public Flux<ProductView> getProducts() {
        LOG.info("getProducts");
        return catalogWebClient.get().retrieve().bodyToFlux(ProductView.class);
    }

    public Mono<ProductView> getProductDetail(Long productId) {
        LOG.info("getProductDetail");
        return catalogWebClient.get().uri("/" + productId).retrieve().bodyToMono(ProductView.class);
    }

    public Flux<InventoryView> getInventory(Long productId) {
        LOG.info("getInventory");
        return inventoryWebClient.get().uri("/" + productId).retrieve().bodyToFlux(InventoryView.class);
    }

    public Mono<OrderView> postOrder(OrderView order) {
        LOG.info("postOrder");
        return orderWebClient.post().body(Mono.just(order), OrderView.class).retrieve().bodyToMono(OrderView.class);
    }

    public Mono<PaymentView> postPayment(PaymentView payment) {
        LOG.info("postPayment");
        return paymentWebClient.post().body(Mono.just(payment), PaymentView.class).retrieve().bodyToMono(PaymentView.class);
    }

    public Mono<PaymentView> putPayment(Long paymentId, PaymentView payment) {
        LOG.info("putPayment");
        return paymentWebClient.put().uri("/" + paymentId).body(Mono.just(payment), PaymentView.class).retrieve().bodyToMono(PaymentView.class);
    }

    public Mono<InventoryView> putInventory(Long inventoryId, InventoryView inventory) {
        LOG.info("putInventory");
        return inventoryWebClient.put().uri("/" + inventoryId).body(Mono.just(inventory), InventoryView.class).retrieve().bodyToMono(InventoryView.class);
    }

    public Mono<OrderView> getOrder(Long orderId) {
        LOG.info("getOrder");
        return orderWebClient.get().uri("/" + orderId).retrieve().bodyToMono(OrderView.class);
    }

    public Mono<OrderView> putOrder(Long orderId, OrderView order) {
        LOG.info("putOrder");
        return orderWebClient.put().uri("/" + orderId).body(Mono.just(order), OrderView.class).retrieve().bodyToMono(OrderView.class);
    }

    public Mono<PaymentView> getPayment(Long orderId) {
        LOG.info("getPayment");
        return paymentWebClient.get().uri("/order/" + orderId).retrieve().bodyToMono(PaymentView.class);
    }

    @GetMapping(value = "productCatalog")
    public Flux<ProductCatalogView> getProductsAndInventories() {
        LOG.info("getProductsAndInventories");
        return getProducts().flatMap(product -> getInventory(product.getProductId()).map(inventory -> {
            ProductCatalogView catalogView = new ProductCatalogView();
            catalogView.setProduct(product);
            catalogView.setInventory(inventory);
            return catalogView;
        }));
    }

    @PostMapping("/createOrder")
    public Mono<ResponseEntity<String>> createOrderAndPayment(@RequestParam Long productId, @RequestParam Integer quantity, @RequestParam Long customerId) {
        LOG.info("createOrderAndPayment");
        return createOrder(productId, quantity, customerId)
                .flatMap(order -> createPayment(order.getOrderId(), order.getTotalAmount())
                        .map(payment -> ResponseEntity.ok("Order created successfully ORDER_ID: " + order.getOrderId())))
                .onErrorResume(error -> Mono.just(ResponseEntity.badRequest().body(error.getMessage())));
    }

    @PutMapping("/processPayment")
    public Mono<ResponseEntity<String>> processPayment(@RequestParam Long orderId) {
        LOG.info("processPayment");
        return processOrder(orderId)
                .map(msg -> ResponseEntity.ok("Order confirmed successfully ORDER_ID: " + orderId))
                .onErrorResume(error -> Mono.just(ResponseEntity.badRequest().body(error.getMessage())));
    }

    public Mono<OrderView> createOrder(Long productId, Integer quantity, Long customerId) {
        LOG.info("createOrder");
        return checkInventory(productId, quantity)
                .flatMap(isAvailable -> {
                    if (isAvailable) {
                        return getProductDetail(productId)
                                .flatMap(product -> {
                                    BigDecimal amount = product.getPrice().multiply(BigDecimal.valueOf(quantity));
                                    OrderView order = new OrderView();
                                    order.setProductId(productId);
                                    order.setQuantity(quantity);
                                    order.setCustomerId(customerId);
                                    order.setStatus("PENDING_PAYMENT");
                                    order.setTotalAmount(amount);
                                    order.setOrderDate(LocalDateTime.now());
                                    return postOrder(order).doFinally(signalType -> {
                                        try {
                                            producer.publishMessage(order.getOrderId() + "", "ORDER CREATED");
                                        } catch (JsonProcessingException e) {
                                            throw new RuntimeException(e);
                                        }
                                    });
                                });
                    } else {
                        LOG.error("Insufficient inventory for product ID: {}", productId);
                        return Mono.error(new RuntimeException("Insufficient inventory for product ID: " + productId));
                    }
                });
    }

    public Mono<PaymentView> createPayment(Long orderId, BigDecimal amount) {
        LOG.info("createPayment");
        PaymentView payment = new PaymentView();
        payment.setOrderId(orderId);
        payment.setAmount(amount);
        payment.setPaymentMethod("ONLINE");
        payment.setStatus("PENDING_PAYMENT");
        return postPayment(payment).doFinally(signalType -> {
            try {
                producer.publishMessage(payment.getOrderId() + "", "PAYMENT CREATED");
            } catch (JsonProcessingException e) {
                LOG.error("Error while creating payment for order ID: {}", orderId);
                throw new RuntimeException(e);
            }
        });
    }

    private Mono<Boolean> checkInventory(Long productId, Integer requestedQuantity) {
        LOG.info("checkInventory");
        return inventoryWebClient
                .get()
                .uri("/" + productId)
                .retrieve()
                .bodyToMono(InventoryView.class)
                .map(inventory -> inventory.getQuantity() >= requestedQuantity);
    }

    public Mono<String> processOrder(Long orderId) {
        LOG.info("processOrder");
        return getOrder(orderId)
                .filter(order -> "PENDING_PAYMENT".equals(order.getStatus()))
                .flatMap(order -> checkInventory(order.getProductId(), order.getQuantity())
                        .flatMap(isAvailable -> {
                            if (isAvailable) {
                                return getInventory(order.getProductId())
                                        .flatMap(inventory -> {
                                            inventory.setQuantity(inventory.getQuantity() - order.getQuantity());
                                            inventory.setLastUpdated(LocalDateTime.now());
                                            return putInventory(inventory.getInventoryId(), inventory);
                                        })
                                        .then(getPayment(orderId)
                                                .flatMap(payment -> {
                                                    payment.setStatus("PAYMENT_COMPLETED");
                                                    payment.setPaymentDate(LocalDateTime.now());
                                                    return putPayment(payment.getPaymentId(), payment).doFinally(signalType -> {
                                                        try {
                                                            producer.publishMessage(payment.getOrderId() + "", "PAYMENT COMPLETED");
                                                        } catch (JsonProcessingException e) {
                                                            throw new RuntimeException(e);
                                                        }
                                                    });
                                                }))
                                        .then(getOrder(orderId)
                                                .flatMap(order2 -> {
                                                    order2.setStatus("PAYMENT_COMPLETED");
                                                    return putOrder(order2.getOrderId(), order2).doFinally(signalType -> {
                                                        try {
                                                            producer.publishMessage(order2.getOrderId() + "", "ORDER COMPLETED");
                                                        } catch (JsonProcessingException e) {
                                                            throw new RuntimeException(e);
                                                        }
                                                    });
                                                }))
                                        .thenReturn("PAYMENT_COMPLETED"); // Return OrderView after processing
                            } else {
                                LOG.error("Insufficient inventory for product ID: {}", order.getProductId());
                                return Mono.error(new RuntimeException("Insufficient inventory for product ID: " + order.getProductId()));
                            }
                        })
                );
    }

}

