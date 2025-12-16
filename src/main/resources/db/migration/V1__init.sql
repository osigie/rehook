CREATE TABLE deliveries
(
    id            UUID NOT NULL,
    created_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at    TIMESTAMP WITHOUT TIME ZONE,
    event_id      UUID,
    endpoint_id   UUID,
    status        VARCHAR(255),
    next_retry_at TIMESTAMP WITHOUT TIME ZONE,
    retry_count   INTEGER,
    tenant        VARCHAR(255),
    CONSTRAINT pk_deliveries PRIMARY KEY (id)
);

CREATE TABLE delivery_attempts
(
    id               UUID NOT NULL,
    created_at       TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at       TIMESTAMP WITHOUT TIME ZONE,
    status_code      INTEGER,
    delivery_id      UUID,
    executed_at      TIMESTAMP WITHOUT TIME ZONE,
    response_body    JSONB,
    response_headers JSONB,
    duration         INTEGER,
    CONSTRAINT pk_delivery_attempts PRIMARY KEY (id)
);

CREATE TABLE endpoints
(
    id              UUID    NOT NULL,
    created_at      TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITHOUT TIME ZONE,
    url             VARCHAR(255),
    is_active       BOOLEAN NOT NULL,
    subscription_id UUID    NOT NULL,
    tenant          VARCHAR(255),
    CONSTRAINT pk_endpoints PRIMARY KEY (id)
);

CREATE TABLE endpoints_auth
(
    id             UUID NOT NULL,
    created_at     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at     TIMESTAMP WITHOUT TIME ZONE,
    auth_type      VARCHAR(255),
    endpoint_id    UUID NOT NULL,
    api_key_name   VARCHAR(255),
    api_key_value  VARCHAR(255),
    hmac_secret    VARCHAR(255),
    basic_username VARCHAR(255),
    basic_password VARCHAR(255),
    tenant         VARCHAR(255),
    CONSTRAINT pk_endpoints_auth PRIMARY KEY (id)
);

CREATE TABLE events
(
    id              UUID NOT NULL,
    created_at      TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITHOUT TIME ZONE,
    payload         JSONB,
    headers         JSONB,
    received_at     TIMESTAMP WITHOUT TIME ZONE,
    subscription_id UUID NOT NULL,
    idempotency_key VARCHAR(50),
    tenant          VARCHAR(255),
    CONSTRAINT pk_events PRIMARY KEY (id)
);

CREATE TABLE subscriptions
(
    id           UUID         NOT NULL,
    created_at   TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at   TIMESTAMP WITHOUT TIME ZONE,
    name         VARCHAR(255) NOT NULL,
    ingestion_id VARCHAR(36)  NOT NULL,
    tenant       VARCHAR(20)  NOT NULL,
    CONSTRAINT pk_subscriptions PRIMARY KEY (id)
);

CREATE TABLE tenants
(
    id         UUID        NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    name       VARCHAR(50) NOT NULL,
    CONSTRAINT pk_tenants PRIMARY KEY (id)
);

CREATE TABLE users
(
    id         UUID         NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    email      VARCHAR(255) NOT NULL,
    password   VARCHAR(255) NOT NULL,
    tenant_id  UUID,
    CONSTRAINT pk_users PRIMARY KEY (id)
);

ALTER TABLE endpoints_auth
    ADD CONSTRAINT uc_endpoints_auth_endpoint UNIQUE (endpoint_id);

ALTER TABLE tenants
    ADD CONSTRAINT uc_tenants_name UNIQUE (name);

ALTER TABLE users
    ADD CONSTRAINT uc_users_email UNIQUE (email);

ALTER TABLE users
    ADD CONSTRAINT uc_users_tenant UNIQUE (tenant_id);

ALTER TABLE events
    ADD CONSTRAINT uk_event_subscription_idempotency UNIQUE (subscription_id, idempotency_key);

ALTER TABLE subscriptions
    ADD CONSTRAINT uk_subscription_name_tenant UNIQUE (name, tenant);

CREATE UNIQUE INDEX ingestion_id_index ON subscriptions (ingestion_id);

ALTER TABLE deliveries
    ADD CONSTRAINT FK_DELIVERIES_ON_ENDPOINT FOREIGN KEY (endpoint_id) REFERENCES endpoints (id);

ALTER TABLE deliveries
    ADD CONSTRAINT FK_DELIVERIES_ON_EVENT FOREIGN KEY (event_id) REFERENCES events (id);

ALTER TABLE delivery_attempts
    ADD CONSTRAINT FK_DELIVERY_ATTEMPTS_ON_DELIVERY FOREIGN KEY (delivery_id) REFERENCES deliveries (id);

ALTER TABLE endpoints_auth
    ADD CONSTRAINT FK_ENDPOINTS_AUTH_ON_ENDPOINT FOREIGN KEY (endpoint_id) REFERENCES endpoints (id);

ALTER TABLE endpoints
    ADD CONSTRAINT FK_ENDPOINTS_ON_SUBSCRIPTION FOREIGN KEY (subscription_id) REFERENCES subscriptions (id);

ALTER TABLE events
    ADD CONSTRAINT FK_EVENTS_ON_SUBSCRIPTION FOREIGN KEY (subscription_id) REFERENCES subscriptions (id);

ALTER TABLE users
    ADD CONSTRAINT FK_USERS_ON_TENANT FOREIGN KEY (tenant_id) REFERENCES tenants (id);