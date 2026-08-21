CREATE TABLE checkout_reservations (
    id             UUID PRIMARY KEY,
    buyer_id       BIGINT NOT NULL,
    status         VARCHAR(20) NOT NULL,
    total_amount   NUMERIC(19, 2) NOT NULL,
    created_at     TIMESTAMP NOT NULL,
    released_at    TIMESTAMP
);

CREATE INDEX idx_checkout_reservations_buyer_id ON checkout_reservations (buyer_id);

CREATE TABLE checkout_reservation_items (
    id             BIGSERIAL PRIMARY KEY,
    reservation_id UUID NOT NULL REFERENCES checkout_reservations (id) ON DELETE CASCADE,
    product_id     BIGINT NOT NULL REFERENCES product_stock (product_id),
    product_name   VARCHAR(255) NOT NULL,
    quantity       INTEGER NOT NULL CHECK (quantity > 0),
    unit_price     NUMERIC(19, 2) NOT NULL,
    subtotal       NUMERIC(19, 2) NOT NULL
);

CREATE INDEX idx_checkout_reservation_items_reservation_id ON checkout_reservation_items (reservation_id);
