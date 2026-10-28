package com.cinema.inventory.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.jpa.audit.JpaAuditingConfiguration;
import com.cinema.inventory.client.MovieCatalogClient;
import com.cinema.inventory.client.MovieCatalogClient.MovieSchedulingDetails;
import com.cinema.inventory.dto.request.CreateShowtimeBatchRequest;
import com.cinema.inventory.dto.request.CreateShowtimeRequest;
import com.cinema.inventory.dto.response.ShowtimeResponse;
import com.cinema.inventory.entity.Cinema;
import com.cinema.inventory.entity.Room;
import com.cinema.inventory.entity.Seat;
import com.cinema.inventory.entity.ShowSeat;
import com.cinema.inventory.entity.Showtime;
import com.cinema.inventory.entity.ShowtimeScreeningVersion;
import com.cinema.inventory.enums.ContentFormat;
import com.cinema.inventory.enums.PresentationType;
import com.cinema.inventory.enums.RoomType;
import com.cinema.inventory.enums.SeatType;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.cinema.inventory.exception.MovieCatalogUnavailableException;
import com.cinema.inventory.exception.ShowtimeBatchItemException;
import com.cinema.inventory.mapper.ShowtimeMapper;
import com.cinema.inventory.repository.CinemaRepository;
import com.cinema.inventory.repository.RoomRepository;
import com.cinema.inventory.repository.SeatRepository;
import com.cinema.inventory.repository.ShowSeatRepository;
import com.cinema.inventory.repository.ShowtimeBatchRequestRepository;
import com.cinema.inventory.repository.ShowtimeRepository;
import com.cinema.inventory.service.FareMatrixService;
import com.cinema.inventory.service.ShowSeatGenerationService;
import com.cinema.inventory.service.ShowtimeBatchPayloadCodec;
import com.cinema.inventory.service.ShowtimeCreationService;
import com.cinema.inventory.service.ShowtimeLayoutService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@DataJpaTest(
        properties = {
            "spring.cloud.config.enabled=false",
            "spring.jpa.hibernate.ddl-auto=validate",
            "spring.flyway.enabled=true"
        })
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@ContextConfiguration(
        classes = ShowtimeBatchIdempotencyIntegrationTest.Config.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Testcontainers(disabledWithoutDocker = true)
@Execution(ExecutionMode.SAME_THREAD)
@Tag("integration")
class ShowtimeBatchIdempotencyIntegrationTest {

    private static final OffsetDateTime NOW =
            OffsetDateTime.parse("2099-01-01T00:00:00Z");

    @Container
    static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.4.0")
                    .withDatabaseName("showtime_batch_test")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add(
                "spring.datasource.driver-class-name",
                MYSQL::getDriverClassName);
    }

    @Autowired private ShowtimeCreationService creationService;
    @Autowired private ShowtimeBatchRequestRepository batchRepository;
    @Autowired private CinemaRepository cinemaRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private SeatRepository seatRepository;
    @Autowired private ShowtimeRepository showtimeRepository;
    @Autowired private ShowSeatRepository showSeatRepository;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private MovieCatalogClient movieCatalogClient;
    @Autowired private Clock clock;

    private TransactionTemplate transactions;
    private UUID roomId;
    private UUID seatId;
    private UUID movieId;
    private UUID screeningVersionId;

    @BeforeEach
    void setUp() {
        reset(movieCatalogClient, clock);

        when(clock.instant()).thenReturn(NOW.toInstant());
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);

        movieId = UUID.randomUUID();
        screeningVersionId = UUID.randomUUID();

        when(movieCatalogClient.getMovieForScheduling(movieId))
                .thenReturn(movieDetails());

        when(movieCatalogClient.getActiveVersion(movieId, screeningVersionId))
                .thenAnswer(
                        invocation ->
                                new ShowtimeScreeningVersion(
                                        screeningVersionId,
                                        1L,
                                        ContentFormat.TWO_D,
                                        "en",
                                        PresentationType.SUBTITLED,
                                        "vi"));

        transactions = new TransactionTemplate(transactionManager);
        transactions.setIsolationLevel(
                TransactionDefinition.ISOLATION_READ_COMMITTED);

        transactions.executeWithoutResult(
                status -> {
                    Cinema cinema =
                            cinemaRepository.saveAndFlush(
                                    new Cinema(
                                            "Batch " + UUID.randomUUID(),
                                            "Test address",
                                            "Ho Chi Minh"));

                    Room room =
                            new Room(
                                    cinema,
                                    "Room 01",
                                    RoomType.STANDARD);

                    room.replaceSupportedContentFormats(
                            Set.of(ContentFormat.TWO_D));

                    room = roomRepository.saveAndFlush(room);
                    roomId = room.getId();

                    Seat seat =
                            seatRepository.saveAndFlush(
                                    new Seat(
                                            room,
                                            "A1",
                                            "A",
                                            SeatType.STANDARD));

                    seatId = seat.getId();
                });
    }

    @Test
    void completedRequestShouldReplayAfterTimePassesAndCatalogFails() {
        UUID requestId = UUID.randomUUID();
        CreateShowtimeBatchRequest request =
                batch(requestId, item(NOW.plusHours(2), 15));

        List<ShowtimeResponse> first =
                creationService.createBatch(request);

        when(clock.instant()).thenReturn(NOW.plusDays(2).toInstant());

        when(movieCatalogClient.getMovieForScheduling(movieId))
                .thenThrow(new MovieCatalogUnavailableException());

        clearInvocations(movieCatalogClient);

        List<ShowtimeResponse> replay =
                creationService.createBatch(request);

        assertThat(replay).isEqualTo(first);
        assertThat(showtimeCount()).isEqualTo(1);
        assertThat(batchRepository.find(requestId)).isPresent();

        verifyNoInteractions(movieCatalogClient);
    }

    @Test
    void completedIdentifierShouldRejectDifferentPayload() {
        UUID requestId = UUID.randomUUID();

        creationService.createBatch(
                batch(requestId, item(NOW.plusHours(2), 15)));

        clearInvocations(movieCatalogClient);

        CreateShowtimeBatchRequest changed =
                batch(requestId, item(NOW.plusHours(2), 30));

        assertThatThrownBy(
                        () -> creationService.createBatch(changed))
                .isInstanceOfSatisfying(
                        ConflictException.class,
                        exception ->
                                assertThat(exception.getErrorCode())
                                        .isSameAs(
                                                InventoryErrorCode
                                                        .SHOWTIME_BATCH_REQUEST_CONFLICT));

        assertThat(showtimeCount()).isEqualTo(1);
        verifyNoInteractions(movieCatalogClient);
    }

    @Test
    void concurrentSameIdentifierShouldCreateOnlyOneBatch()
            throws Exception {

        UUID requestId = UUID.randomUUID();
        CreateShowtimeBatchRequest request =
                batch(requestId, item(NOW.plusHours(2), 15));

        CountDownLatch preparing = new CountDownLatch(2);

        when(movieCatalogClient.getMovieForScheduling(movieId))
                .thenAnswer(
                        invocation -> {
                            preparing.countDown();

                            if (!preparing.await(10, TimeUnit.SECONDS)) {
                                throw new AssertionError(
                                        "Both requests must enter preparation");
                            }

                            return movieDetails();
                        });

        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<List<ShowtimeResponse>> first =
                    executor.submit(
                            () -> creationService.createBatch(request));

            Future<List<ShowtimeResponse>> second =
                    executor.submit(
                            () -> creationService.createBatch(request));

            List<ShowtimeResponse> firstResult =
                    first.get(30, TimeUnit.SECONDS);

            List<ShowtimeResponse> secondResult =
                    second.get(30, TimeUnit.SECONDS);

            assertThat(secondResult).isEqualTo(firstResult);
            assertThat(firstResult).hasSize(1);
            assertThat(showtimeCount()).isEqualTo(1);
            assertThat(batchRepository.find(requestId)).isPresent();

        } finally {
            executor.shutdownNow();

            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS))
                    .isTrue();
        }
    }

    @Test
    void laterConflictShouldRollbackEarlierShowtimeAndRequestClaim() {
        UUID requestId = UUID.randomUUID();

        CreateShowtimeBatchRequest conflicting =
                batch(
                        requestId,
                        item(NOW.plusHours(2), 15),
                        item(NOW.plusHours(3), 15));

        assertThatThrownBy(
                        () -> creationService.createBatch(conflicting))
                .isInstanceOfSatisfying(
                        ShowtimeBatchItemException.class,
                        exception -> {
                            assertThat(exception.getItemIndex()).isEqualTo(1);

                            assertThat(
                                            exception.getOriginal()
                                                    .getErrorCode())
                                    .isSameAs(
                                            InventoryErrorCode.SHOWTIME_OVERLAP);
                        });

        assertThat(showtimeCount()).isZero();
        assertThat(batchRepository.find(requestId)).isEmpty();

        List<ShowtimeResponse> retry =
                creationService.createBatch(
                        batch(
                                requestId,
                                item(NOW.plusHours(2), 15)));

        assertThat(retry).hasSize(1);
        assertThat(showtimeCount()).isEqualTo(1);
    }

    @Test
    void jdbcClaimAndJpaShowtimeAndSeatShouldRollbackTogether() {
        UUID requestId = UUID.randomUUID();

        assertThatThrownBy(
                        () ->
                                transactions.executeWithoutResult(
                                        status -> {
                                            batchRepository.claim(
                                                    requestId,
                                                    "a".repeat(64));

                                            Room room =
                                                    roomRepository
                                                            .findById(roomId)
                                                            .orElseThrow();

                                            Seat seat =
                                                    seatRepository
                                                            .findById(seatId)
                                                            .orElseThrow();

                                            Showtime showtime =
                                                    showtimeRepository.saveAndFlush(
                                                            new Showtime(
                                                                    movieId,
                                                                    room,
                                                                    NOW.plusHours(2),
                                                                    NOW.plusHours(4)));

                                            showSeatRepository.saveAndFlush(
                                                    new ShowSeat(
                                                            showtime,
                                                            seat,
                                                            new BigDecimal("100000")));

                                            throw new ConflictException(
                                                    InventoryErrorCode.SHOWTIME_OVERLAP);
                                        }))
                .isInstanceOf(ConflictException.class);

        assertThat(batchRepository.find(requestId)).isEmpty();
        assertThat(showtimeCount()).isZero();
        assertThat(showSeatCount()).isZero();
    }

    private CreateShowtimeRequest item(
            OffsetDateTime startsAt,
            int breakMinutes) {

        return new CreateShowtimeRequest(
                movieId,
                roomId,
                startsAt,
                startsAt.plusMinutes(120),
                null,
                screeningVersionId,
                breakMinutes,
                null);
    }

    private CreateShowtimeBatchRequest batch(
            UUID requestId,
            CreateShowtimeRequest... items) {

        return new CreateShowtimeBatchRequest(
                requestId,
                List.of(items));
    }

    private MovieSchedulingDetails movieDetails() {
        return new MovieSchedulingDetails(
                120,
                1L,
                LocalDate.of(2020, 1, 1));
    }

    private long showtimeCount() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM showtimes WHERE room_id = ?",
                Long.class,
                binary(roomId));
    }

    private long showSeatCount() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM show_seats WHERE seat_id = ?",
                Long.class,
                binary(seatId));
    }

    private static byte[] binary(UUID value) {
        return ByteBuffer.allocate(16)
                .putLong(value.getMostSignificantBits())
                .putLong(value.getLeastSignificantBits())
                .array();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement
    @EnableJpaAuditing(
            dateTimeProviderRef = "auditingDateTimeProvider")
    @EntityScan(basePackageClasses = Showtime.class)
    @EnableJpaRepositories(
            basePackageClasses = ShowtimeRepository.class)
    @Import({
        JpaAuditingConfiguration.class,
        ShowtimeCreationService.class,
        ShowSeatGenerationService.class,
        ShowtimeBatchRequestRepository.class,
        ShowtimeBatchPayloadCodec.class
    })
    static class Config {

        @Bean
        Clock clock() {
            return mock(Clock.class);
        }

        @Bean
        MovieCatalogClient movieCatalogClient() {
            return mock(MovieCatalogClient.class);
        }

        @Bean
        ShowtimeLayoutService showtimeLayoutService() {
            return mock(ShowtimeLayoutService.class);
        }

        @Bean
        FareMatrixService fareMatrixService() {
            return mock(FareMatrixService.class);
        }

        @Bean
        ShowtimeMapper showtimeMapper() {
            return Mappers.getMapper(ShowtimeMapper.class);
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper()
                    .findAndRegisterModules()
                    .disable(
                            SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        }
    }
}
