import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.cinema.common.mapper.mapper.CollectionMapper;
import com.cinema.common.mapper.mapper.PageMapper;
import com.cinema.common.mapper.util.MappingUtils;
import com.cinema.common.response.model.PageResponse;

public class CollectionMapperTest {
    @Test
    void shouldCopyList() {

        List<String> list = List.of("A", "B");

        List<String> copy = CollectionMapper.toList(list);

        assertEquals(list, copy);

        assertThrows(
                UnsupportedOperationException.class,
                () -> copy.add("C"));
    }

    @Test
    void shouldConvertPage() {

        Page<String> page = new PageImpl<>(
                List.of("A", "B"),
                PageRequest.of(0, 2),
                10);

        PageResponse<String> response = PageMapper.toPage(page);

        assertEquals(10, response.page().totalElements());

        assertEquals(2, response.content().size());

    }

    @Test
    void shouldReturnTrueWhenCollectionNull() {

        assertTrue(
                MappingUtils.isEmpty(null));

    }

    @Test
    void shouldReturnTrueWhenCollectionEmpty() {

        assertTrue(
                MappingUtils.isEmpty(List.of()));

    }

    @Test
    void shouldReturnFalseWhenCollectionHasElements() {

        assertFalse(
                MappingUtils.isEmpty(
                        List.of("A")));

    }
}
