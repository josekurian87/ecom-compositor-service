package com.secor.ecomcompositorservice;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter
public class ProductView implements Serializable {
    private Long productId;

    private String productName;
    private String description;
    private BigDecimal price;
    private String category;
    //private Integer quantity;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Getters and Setters
}
