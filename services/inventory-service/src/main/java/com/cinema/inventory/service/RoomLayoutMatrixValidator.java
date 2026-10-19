package com.cinema.inventory.service;

import com.cinema.common.exception.exception.ValidationException;
import com.cinema.inventory.dto.request.RoomLayoutMatrixInput;
import com.cinema.inventory.dto.request.RoomLayoutMatrixInput.MatrixSeatInput;
import com.cinema.inventory.exception.InventoryErrorCode;

import jakarta.validation.Validator;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
public class RoomLayoutMatrixValidator {

    private final Validator validator;

    public RoomLayoutMatrixValidator(Validator validator) {
        this.validator = validator;
    }

    public void validate(RoomLayoutMatrixInput matrix) {
        if (matrix == null || !validator.validate(matrix).isEmpty()) {
            throw invalid();
        }

        Set<Long> positions = new HashSet<>();
        Set<UUID> seatIds = new HashSet<>();
        Map<Integer, List<MatrixSeatInput>> rows = new HashMap<>();

        for (MatrixSeatInput seat : matrix.seats()) {
            if (seat.rowIndex() >= matrix.rowCount()
                    || seat.columnIndex() >= matrix.columnCount()) {
                throw invalid();
            }

            long position =
                    ((long) seat.rowIndex() << 32)
                            | seat.columnIndex().longValue();

            if (!positions.add(position)) {
                throw invalid();
            }

            if (seat.seatId() != null && !seatIds.add(seat.seatId())) {
                throw invalid();
            }

            rows.computeIfAbsent(
                            seat.rowIndex(),
                            ignored -> new ArrayList<>())
                    .add(seat);
        }

        for (List<MatrixSeatInput> row : rows.values()) {
            row.sort(Comparator.comparingInt(MatrixSeatInput::columnIndex));

            for (int index = 0; index < row.size(); index++) {
                if (row.get(index).number() != index + 1) {
                    throw invalid();
                }
            }
        }
    }

    public String rowLabel(int rowIndex) {
        if (rowIndex < 0 || rowIndex >= 100) {
            throw invalid();
        }

        int value = rowIndex + 1;
        StringBuilder result = new StringBuilder();

        while (value > 0) {
            value--;
            result.append((char) ('A' + value % 26));
            value /= 26;
        }

        return result.reverse().toString();
    }

    private ValidationException invalid() {
        return new ValidationException(
                InventoryErrorCode.ROOM_LAYOUT_DATA_INVALID);
    }
}
