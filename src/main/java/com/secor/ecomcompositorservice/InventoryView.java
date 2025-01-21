package com.secor.ecomcompositorservice;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter @Setter
public class InventoryView implements Serializable {
    private Long inventoryId;

    private Long productId;
    private Integer quantity;

    private LocalDateTime lastUpdated;

    // Getters and Setters
}
