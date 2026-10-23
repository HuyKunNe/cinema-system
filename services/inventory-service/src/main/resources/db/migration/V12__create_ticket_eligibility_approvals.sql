CREATE TABLE ticket_eligibility_approvals (
    id BINARY(16) NOT NULL,

    user_id BINARY(16) NOT NULL,
    show_seat_id BINARY(16) NOT NULL,

    ticket_category_code VARCHAR(64) NOT NULL,
    eligibility_rule_code VARCHAR(64) NOT NULL,

    attendee_name VARCHAR(150) NOT NULL,
    verification_reference VARCHAR(120) NOT NULL,

    verified_by BINARY(16) NOT NULL,
    verified_at DATETIME(6) NOT NULL,
    valid_until DATETIME(6) NOT NULL,

    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,

    CONSTRAINT pk_ticket_eligibility_approvals PRIMARY KEY (id),

    CONSTRAINT fk_ticket_eligibility_approval_show_seat
        FOREIGN KEY (show_seat_id)
        REFERENCES show_seats (id),

    CONSTRAINT chk_ticket_eligibility_approval_expiration
        CHECK (valid_until > verified_at)
);

CREATE INDEX idx_ticket_eligibility_approval_owner_seat
    ON ticket_eligibility_approvals (user_id, show_seat_id);
