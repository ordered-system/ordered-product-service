# ordered-product-service

[![CI](https://github.com/ordered-system/ordered-product-service/actions/workflows/ci.yml/badge.svg)](https://github.com/ordered-system/ordered-product-service/actions/workflows/ci.yml)

Product catalog, cart, and checkout-reservation bounded context for [ordered-system](https://github.com/ordered-system), extracted from the [`ordered-backend`](https://github.com/ordered-system/ordered-backend) monolith. Owns `product_db` (PostgreSQL) and a Redis cache — no other service touches either directly.

## What it does

- **Product catalog**: CRUD for listings, with seller ownership checks (`ProductOwnershipException`) and an active/inactive lifecycle.
- **Redis cache-aside** on catalog reads — under the load test in [`ordered-load-tests`](https://github.com/ordered-system/ordered-load-tests), this cut error rates from 46% to 0% at the same traffic level, and surfaced HikariCP pool sizing as the next bottleneck once the cache was in place.
- **Cart**, scoped per user, with quantity updates and an "empty cart" guard on checkout.
- **Checkout reservations**: when an order is placed, `order-service` calls this service to reserve stock (`CheckoutReservation` / `CheckoutReservationItem`) before payment is attempted — if the order is later cancelled, an `order-cancelled` Kafka event (consumed by `OrderCancelledListener`) releases the reservation back to available stock.
- **Price history** tracking (`PriceHistory`) alongside current pricing.
- Publishes/consumes Kafka events idempotently — `ProcessedEvent` records which event IDs have already been handled, so a redelivered message from the outbox pattern elsewhere in the system doesn't double-apply.

## API

Base path `/api/v1/products` and `/api/v1/cart`, reached through [`ordered-gateway`](https://github.com/ordered-system/ordered-gateway). Product browsing (`GET /api/v1/products/**`) is public; everything else requires auth. OpenAPI docs at `/v3/api-docs`.

## Stack

Java 21 · Spring Boot 4.1.0 · PostgreSQL + Flyway · Redis (cache-aside) · Kafka · Eureka Client · Spring Cloud Config Client · Micrometer / Prometheus / OpenTelemetry tracing · [`ordered-commons`](https://github.com/ordered-system/ordered-commons)

## Running it locally

```bash
git clone https://github.com/ordered-system/ordered-commons.git
(cd ordered-commons && make install)

git clone https://github.com/ordered-system/ordered-product-service.git
cd ordered-product-service
make up     # this service's own Postgres (+ check docker-compose.yml for Redis if included)
make run
```

Runs on **port 9092**. Needs [`ordered-eureka`](https://github.com/ordered-system/ordered-eureka), [`ordered-config-server`](https://github.com/ordered-system/ordered-config-server), Kafka, and Redis reachable — running the full stack via [`ordered-infra`](https://github.com/ordered-system/ordered-infra) is the path of least resistance.

### Docker

Same as the other business services — the `Dockerfile` needs `ordered-commons` supplied as an additional build context; build via `ordered-infra`'s compose files rather than a bare `docker build .`.

## Testing

```bash
make test-unit
make test-integration    # Testcontainers: Postgres + Redis, real containers
```

`CheckoutFlowIntegrationTest` is the one worth reading first — it exercises reserve → confirm/cancel end-to-end against real containers rather than mocks.

## Where this fits

| Service | Database | Role |
|---|---|---|
| [ordered-order-service](https://github.com/ordered-system/ordered-order-service) | PostgreSQL | Orders, cart checkout, payments (Stripe) |
| **ordered-product-service** | PostgreSQL + Redis | Product catalog, stock reservation |
| [ordered-user-service](https://github.com/ordered-system/ordered-user-service) | PostgreSQL | Users, auth, JWT issuance |
| [ordered-engagement-service](https://github.com/ordered-system/ordered-engagement-service) | MongoDB | Reviews, browsing history |

Part of the [ordered-system](https://github.com/ordered-system) organization.

## License

MIT — see [LICENSE](LICENSE).
