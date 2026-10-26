package com.cinema.inventory.exception;

import com.cinema.common.exception.exception.ServiceUnavailableException;

public final class MovieCatalogUnavailableException
        extends ServiceUnavailableException {

    public MovieCatalogUnavailableException() {
        super(InventoryErrorCode.MOVIE_CATALOG_UNAVAILABLE);
    }
}
