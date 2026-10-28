package com.cinema.inventory.service;

import com.cinema.common.exception.exception.ValidationException;
import com.cinema.inventory.dto.response.FareMatrixResponse;
import com.cinema.inventory.dto.response.FareMatrixResponse.CellResponse;
import com.cinema.inventory.dto.response.FareMatrixResponse.ScopeResponse;
import com.cinema.inventory.dto.response.FareMatrixValidationResponse;
import com.cinema.inventory.dto.response.FareMatrixValidationResponse.CellReference;
import com.cinema.inventory.dto.response.FareMatrixValidationResponse.Issue;
import com.cinema.inventory.entity.FareMatrixCell.Key;
import com.cinema.inventory.entity.FareMatrixCell.Value;
import com.cinema.inventory.enums.FareCellState;
import com.cinema.inventory.enums.PricingPolicyVersionStatus;
import com.cinema.inventory.exception.InventoryErrorCode;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DraftFareMatrixValidationService {

    public FareMatrixValidationResponse validate(FareMatrixResponse matrix) {
        List<Issue> issues = new ArrayList<>();
        Map<Key, Value> configured = new LinkedHashMap<>();

        if (matrix.scopes().isEmpty()) {
            issues.add(global(InventoryErrorCode.PRICING_MATRIX_SCOPE_REQUIRED));
        }

        long defaultGroupCount =
                matrix.ticketGroups().stream().filter(group -> group.defaultGroup()).count();

        boolean defaultGroupMatches =
                matrix.ticketGroups().stream()
                        .anyMatch(
                                group ->
                                        group.defaultGroup()
                                                && group.id().equals(matrix.defaultTicketGroupId())
                                                && group.eligibilityDescription() == null);

        if (defaultGroupCount != 1 || !defaultGroupMatches) {
            issues.add(global(InventoryErrorCode.INVALID_PRICING_DEFINITION));
        }

        if (matrix.seatTypes().isEmpty()) {
            issues.add(global(InventoryErrorCode.PRICING_MATRIX_SEAT_TYPES_REQUIRED));
        }

        for (CellResponse cell : matrix.cells()) {
            ScopeResponse scope = new ScopeResponse(cell.experienceType(), cell.contentFormat());

            CellReference reference = reference(cell);

            if (cell.state() == FareCellState.UNCONFIGURED) {
                issues.add(
                        new Issue(
                                InventoryErrorCode.PRICING_MATRIX_INCOMPLETE.code(),
                                InventoryErrorCode.PRICING_MATRIX_INCOMPLETE.message(),
                                scope,
                                reference));
                continue;
            }

            try {
                Key key =
                        new Key(
                                cell.experienceType(),
                                cell.contentFormat(),
                                cell.timeBandId(),
                                cell.ticketGroupId(),
                                cell.seatType());

                Value value = new Value(key, cell.state(), cell.amount());

                if (configured.putIfAbsent(key, value) != null) {
                    issues.add(
                            new Issue(
                                    InventoryErrorCode.PRICING_POLICY_DATA_INCONSISTENT.code(),
                                    InventoryErrorCode.PRICING_POLICY_DATA_INCONSISTENT.message(),
                                    scope,
                                    reference));
                }

            } catch (ValidationException exception) {
                issues.add(
                        new Issue(
                                exception.getErrorCode().code(),
                                exception.getErrorCode().message(),
                                scope,
                                reference));
            }
        }

        try {
            FareMatrixRules.validateUniformFares(configured);
        } catch (ValidationException exception) {
            issues.add(
                    new Issue(
                            exception.getErrorCode().code(),
                            exception.getErrorCode().message(),
                            null,
                            null));
        }

        boolean valid = issues.isEmpty();

        return new FareMatrixValidationResponse(
                matrix.policyId(),
                matrix.policyVersionId(),
                matrix.revision(),
                matrix.version(),
                matrix.status(),
                valid,
                valid && matrix.status() == PricingPolicyVersionStatus.DRAFT,
                matrix.requiredCellCount(),
                matrix.configuredCellCount(),
                matrix.missingCellCount(),
                List.copyOf(issues));
    }

    private Issue global(InventoryErrorCode code) {
        return new Issue(code.code(), code.message(), null, null);
    }

    private CellReference reference(CellResponse cell) {
        return new CellReference(
                cell.experienceType(),
                cell.contentFormat(),
                cell.timeBandId(),
                cell.ticketGroupId(),
                cell.seatType());
    }
}
