package pl.dybcio.ordered.checkout.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import pl.dybcio.ordered.checkout.entity.CheckoutReservation;

public interface CheckoutReservationRepository extends JpaRepository<CheckoutReservation, UUID> {}
