package com.cinema.inventory.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.cinema.inventory.entity.Cinema;

import jakarta.persistence.EntityManager;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@ActiveProfiles("test")
class CinemaRepositoryTest {

    @Autowired
    private CinemaRepository cinemaRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void findAllByActiveTrueOrderByNameAscShouldReturnActiveCinemasSortedByName() {
        Cinema beta = cinema(
                "Beta Cinema",
                "12 Tran Hung Dao",
                "Ho Chi Minh");

        Cinema alpha = cinema(
                "Alpha Cinema",
                "10 Nguyen Hue",
                "Ho Chi Minh");

        Cinema inactive = cinema(
                "Inactive Cinema",
                "20 Le Loi",
                "Ho Chi Minh");

        inactive.deactivate();

        cinemaRepository.saveAllAndFlush(
                List.of(beta, alpha, inactive));

        entityManager.clear();

        List<Cinema> result = cinemaRepository.findAllByActiveTrueOrderByNameAsc();

        assertThat(result)
                .extracting(Cinema::getName)
                .containsExactly(
                        "Alpha Cinema",
                        "Beta Cinema");

        assertThat(result)
                .allMatch(Cinema::isActive);
    }

    @Test
    void findAllByCityIgnoreCaseAndActiveTrueOrderByNameAscShouldFilterByCity() {
        Cinema beta = cinema(
                "Beta Cinema",
                "12 Tran Hung Dao",
                "Ho Chi Minh");

        Cinema alpha = cinema(
                "Alpha Cinema",
                "10 Nguyen Hue",
                "HO CHI MINH");

        Cinema hanoi = cinema(
                "Hanoi Cinema",
                "15 Trang Tien",
                "Ha Noi");

        Cinema inactive = cinema(
                "Inactive Cinema",
                "20 Le Loi",
                "Ho Chi Minh");

        inactive.deactivate();

        cinemaRepository.saveAllAndFlush(
                List.of(beta, alpha, hanoi, inactive));

        entityManager.clear();

        List<Cinema> result = cinemaRepository
                .findAllByCityIgnoreCaseAndActiveTrueOrderByNameAsc(
                        "ho chi minh");

        assertThat(result)
                .extracting(Cinema::getName)
                .containsExactly(
                        "Alpha Cinema",
                        "Beta Cinema");
    }

    @Test
    void saveShouldPersistCinemaWithGeneratedId() {
        Cinema saved = cinemaRepository.saveAndFlush(
                cinema(
                        "CGV Vincom",
                        "72 Le Thanh Ton",
                        "Ho Chi Minh"));

        entityManager.clear();

        Cinema persisted = cinemaRepository
                .findById(saved.getId())
                .orElseThrow();

        assertThat(persisted.getId()).isNotNull();
        assertThat(persisted.getName()).isEqualTo("CGV Vincom");
        assertThat(persisted.getAddress())
                .isEqualTo("72 Le Thanh Ton");
        assertThat(persisted.getCity())
                .isEqualTo("Ho Chi Minh");
        assertThat(persisted.isActive()).isTrue();
    }

    private Cinema cinema(
            String name,
            String address,
            String city) {

        return new Cinema(name, address, city);
    }
}
