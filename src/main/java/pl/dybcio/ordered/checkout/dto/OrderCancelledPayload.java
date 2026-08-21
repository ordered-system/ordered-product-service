package pl.dybcio.ordered.checkout.dto;

import java.time.Instant;

public record OrderCancelledPayload(Long orderId, String reservationId, Instant cancelledAt) {}
