package pl.dybcio.ordered.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record UpdateProductRequest(
    @NotBlank String name,
    String description,
    @Positive BigDecimal price,
    @PositiveOrZero Integer stockQuantity) {}
