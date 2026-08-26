package pl.dybcio.ordered;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class OrderedProductServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(OrderedProductServiceApplication.class, args);
  }
}
