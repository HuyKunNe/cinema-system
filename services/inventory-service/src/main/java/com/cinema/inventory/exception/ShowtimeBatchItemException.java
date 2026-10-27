package com.cinema.inventory.exception;

import com.cinema.common.exception.exception.BusinessException;

public final class ShowtimeBatchItemException extends RuntimeException {

    private final int itemIndex;

    public ShowtimeBatchItemException(
            int itemIndex,
            BusinessException original) {

        super(original.getMessage(), original);

        if (itemIndex < 0) {
            throw new IllegalArgumentException(
                    "itemIndex must not be negative");
        }

        this.itemIndex = itemIndex;
    }

    public int getItemIndex() {
        return itemIndex;
    }

    public String getFieldPath() {
        return "showtimes[" + itemIndex + "]";
    }

    public BusinessException getOriginal() {
        return (BusinessException) getCause();
    }
}
