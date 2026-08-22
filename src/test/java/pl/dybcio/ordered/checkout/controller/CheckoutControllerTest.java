package pl.dybcio.ordered.checkout.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import pl.dybcio.ordered.checkout.dto.CheckoutReservationResponse;
import pl.dybcio.ordered.checkout.service.CheckoutService;
import pl.dybcio.ordered.common.exception.GlobalExceptionHandler;

@ExtendWith(MockitoExtension.class)
class CheckoutControllerTest {

  @Mock private CheckoutService checkoutService;
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    CheckoutController controller = new CheckoutController(checkoutService);
    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  @Test
  void reserve_returns201WithReservation() throws Exception {
    UUID reservationId = UUID.randomUUID();
    var response =
        new CheckoutReservationResponse(
            reservationId,
            List.of(
                new CheckoutReservationResponse.ReservedLine(
                    10L, "Keyboard", 2, BigDecimal.valueOf(50), BigDecimal.valueOf(100))),
            BigDecimal.valueOf(100));
    when(checkoutService.reserveForCheckout(42L)).thenReturn(response);

    mockMvc
        .perform(
            post("/internal/v1/checkout/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"buyerId":42}
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.reservationId").value(reservationId.toString()))
        .andExpect(jsonPath("$.totalAmount").value(100));
  }

  @Test
  void reserve_returns400_whenBuyerIdMissing() throws Exception {
    mockMvc
        .perform(
            post("/internal/v1/checkout/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void release_returns204_andDelegatesToService() throws Exception {
    UUID reservationId = UUID.randomUUID();

    mockMvc
        .perform(post("/internal/v1/checkout/" + reservationId + "/release"))
        .andExpect(status().isNoContent());

    verify(checkoutService).release(reservationId);
  }
}
