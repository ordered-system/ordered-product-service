package pl.dybcio.ordered.catalog.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record CreateProductRequest(
    @NotBlank(message = "Nazwa produktu jest wymagana") String name,
    String description,
    @NotNull(message = "Cena jest wymagana")
        @Positive(message = "Cena musi być większa od zera")
        @Digits(
            integer = 8,
            fraction = 2,
            message = "Cena może mieć maksymalnie 2 miejsca po przecinku")
        BigDecimal price,
    @NotNull(message = "Ilość jest wymagana") @PositiveOrZero(message = "Ilość nie może być ujemna")
        Integer stockQuantity) {}
