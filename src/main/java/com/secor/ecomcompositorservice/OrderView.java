package com.secor.ecomcompositorservice;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter
public class OrderView implements Serializable {
    private Long orderId;

    private Long productId;
    private Integer quantity;
    private Long customerId;
    private LocalDateTime orderDate;
    private String status;
    private BigDecimal totalAmount;

    // Getters and Setters
}
