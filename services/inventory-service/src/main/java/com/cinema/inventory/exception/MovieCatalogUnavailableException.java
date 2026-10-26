package com.cinema.inventory.exception;

import com.cinema.common.exception.exception.BusinessException;

public final class MovieCatalogUnavailableException extends BusinessException {

    public MovieCatalogUnavailableException() {
        super(InventoryErrorCode.MOVIE_CATALOG_UNAVAILABLE);
    }
}
