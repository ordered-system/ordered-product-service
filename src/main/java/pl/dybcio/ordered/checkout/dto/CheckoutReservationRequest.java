package pl.dybcio.ordered.checkout.dto;

import jakarta.validation.constraints.NotNull;

public record CheckoutReservationRequest(@NotNull Long buyerId) {}
