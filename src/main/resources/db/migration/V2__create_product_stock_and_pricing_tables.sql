CREATE TABLE product_stock (
    product_id BIGINT PRIMARY KEY REFERENCES products(id),
    quantity INTEGER NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE product_pricing (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id),
    price NUMERIC(10, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'PLN',
    effective_from TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_product_pricing_product_id_effective_from
ON product_pricing (product_id, effective_from DESC);
