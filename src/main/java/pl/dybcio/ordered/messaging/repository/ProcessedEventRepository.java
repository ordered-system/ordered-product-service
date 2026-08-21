package pl.dybcio.ordered.messaging.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.dybcio.ordered.messaging.entity.ProcessedEvent;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, String> {}
