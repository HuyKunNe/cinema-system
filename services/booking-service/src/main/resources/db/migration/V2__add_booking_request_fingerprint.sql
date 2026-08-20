ALTER TABLE bookings
    ADD COLUMN request_fingerprint CHAR(64) NULL
    AFTER client_request_id;

UPDATE bookings
SET request_fingerprint =
        SHA2(
            CONCAT(
                'legacy:',
                HEX(id)
            ),
            256
        )
WHERE request_fingerprint IS NULL;

ALTER TABLE bookings
    MODIFY COLUMN request_fingerprint CHAR(64) NOT NULL;
