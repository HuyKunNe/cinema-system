import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.cinema.common.api.mapper.PageResponseMapper;
import com.cinema.common.response.model.PageResponse;

public class PageResponseMapperTest {
    @Test
    void shouldConvertPageToPageResponse() {

        Page<String> page = new PageImpl<>(
                List.of("A", "B"),
                PageRequest.of(0, 2),
                10);

        PageResponse<String> response = PageResponseMapper.map(page);

        assertEquals(2, response.content().size());

        assertEquals(10, response.page().totalElements());

        assertEquals(5, response.page().totalPages());

    }
}
