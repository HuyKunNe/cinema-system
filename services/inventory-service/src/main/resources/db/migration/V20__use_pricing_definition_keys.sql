ALTER TABLE pricing_fare_cells
    DROP FOREIGN KEY fk_pricing_fare_cells_category,
    DROP CHECK chk_pricing_fare_time_band,
    DROP INDEX uk_pricing_fare_cell,
    DROP COLUMN time_band,
    DROP COLUMN ticket_category;

DROP TABLE pricing_fare_categories;
