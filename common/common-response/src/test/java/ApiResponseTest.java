import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;

import com.cinema.common.response.model.ApiResponse;

public class ApiResponseTest {
    @Test
    void shouldCreateSuccessResponse() {

        ApiResponse<String> response = new ApiResponse<>(
                true,
                OffsetDateTime.now(),
                "OK",
                null);

        assertTrue(response.success());

        assertEquals("OK", response.data());

        assertNull(response.error());
    }
}
