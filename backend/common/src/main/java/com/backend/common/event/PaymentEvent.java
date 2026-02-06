package com.backend.common.event;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEvent {
    private String orderId;
    private String userId;
    private Double amount;
    private PaymentStatus status;
    private Instant timestamp;

    public enum PaymentStatus {
        SUCCESS,
        FAILED,
        PENDING
    }
}
