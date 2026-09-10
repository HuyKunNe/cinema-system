package com.cinema.inventory.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.outbox.entity.OutboxEventEntity;
import com.cinema.common.outbox.model.OutboxEventMessage;
import com.cinema.common.outbox.repository.OutboxRepository;
import com.cinema.common.test.annotation.IntegrationTest;
import com.cinema.inventory.entity.Cinema;
import com.cinema.inventory.entity.Room;
import com.cinema.inventory.entity.Seat;
import com.cinema.inventory.entity.ShowSeat;
import com.cinema.inventory.entity.Showtime;
import com.cinema.inventory.enums.RoomType;
import com.cinema.inventory.enums.SeatType;
import com.cinema.inventory.enums.ShowSeatStatus;
import com.cinema.inventory.event.InventoryEventContract;
import com.cinema.inventory.event.payload.RequestedSeatPayload;
import com.cinema.inventory.event.payload.SeatReservationRequestedPayload;
import com.cinema.inventory.repository.CinemaRepository;
import com.cinema.inventory.repository.ProcessedEventRepository;
import com.cinema.inventory.repository.RoomRepository;
import com.cinema.inventory.repository.SeatRepository;
import com.cinema.inventory.repository.ShowSeatRepository;
import com.cinema.inventory.repository.ShowtimeRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

@IntegrationTest
@Testcontainers(disabledWithoutDocker = true)
@Import(SeatReservationRequestedKafkaIntegrationTest.FixedClockConfiguration.class)
class SeatReservationRequestedKafkaIntegrationTest {

    private static final String TOPIC = InventoryEventContract.SEAT_RESERVATION_REQUESTED;

    private static final Instant CURRENT_INSTANT = Instant.parse("2026-08-24T10:00:00Z");

    private static final OffsetDateTime NOW =
            OffsetDateTime.ofInstant(CURRENT_INSTANT, ZoneOffset.UTC);

    private static final String MYSQL_IMAGE = "mysql:8.4.0";

    private static final String KAFKA_IMAGE = "apache/kafka:4.0.0";

    @Container
    static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>(MYSQL_IMAGE)
                    .withDatabaseName("inventory_kafka_test")
                    .withUsername("cinema")
                    .withPassword("cinema");

    @Container static final KafkaContainer KAFKA = new KafkaContainer(KAFKA_IMAGE);

    @DynamicPropertySource
    static void registerInfrastructure(DynamicPropertyRegistry registry) {

        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);

        registry.add("spring.datasource.username", MYSQL::getUsername);

        registry.add("spring.datasource.password", MYSQL::getPassword);

        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);

        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");

        registry.add("spring.flyway.enabled", () -> true);

        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);

        registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");

        registry.add("spring.kafka.consumer.enable-auto-commit", () -> false);

        registry.add("spring.kafka.listener.ack-mode", () -> "record");

        registry.add("cinema.inventory.kafka.enabled", () -> true);

        registry.add(
                "cinema.inventory.kafka.consumer-group",
                () -> "inventory-seat-reservation-integration");

        registry.add("cinema.inventory.kafka.topics." + "seat-reservation-requested", () -> TOPIC);

        registry.add("cinema.outbox.enabled", () -> false);

        registry.add("cinema.outbox.producer", () -> "inventory-service");
    }

    @Autowired private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired private ObjectMapper objectMapper;

    @Autowired private CinemaRepository cinemaRepository;

    @Autowired private RoomRepository roomRepository;

    @Autowired private SeatRepository seatRepository;

    @Autowired private ShowtimeRepository showtimeRepository;

    @Autowired private ShowSeatRepository showSeatRepository;

    @Autowired private ProcessedEventRepository processedEventRepository;

    @Autowired private OutboxRepository outboxRepository;

    private UUID showtimeId;

    @BeforeEach
    void setUp() {

        cleanDatabase();
        createInventory();
    }

    @Test
    void kafkaMessageShouldHoldSeatsAndCreateResultOutbox() throws Exception {

        UUID bookingId = UuidGenerator.next();

        OutboxEventMessage message = message(bookingId, List.of("H8", "H7"));

        publish(bookingId, message);

        await(() -> processedEventRepository.count() == 1L);

        await(() -> outboxRepository.count() == 1L);

        List<ShowSeat> showSeats =
                showSeatRepository.findAllByShowtime_IdOrderBySeatNumberAsc(showtimeId);

        assertThat(showSeats).hasSize(2);

        assertThat(showSeats)
                .allSatisfy(
                        showSeat -> {
                            assertThat(showSeat.getStatus()).isEqualTo(ShowSeatStatus.HELD);

                            assertThat(showSeat.getHeldByBookingId()).isEqualTo(bookingId);

                            assertThat(showSeat.getHoldExpiresAt()).isEqualTo(NOW.plusMinutes(10));
                        });

        OutboxEventEntity result = outboxRepository.findAll().getFirst();

        assertThat(result.getEventType()).isEqualTo(InventoryEventContract.SEAT_RESERVED);

        assertThat(result.getAggregateId()).isEqualTo(bookingId);

        assertThat(result.getPartitionKey()).isEqualTo(bookingId.toString());

        assertThat(result.getCorrelationId()).isEqualTo(message.correlationId());

        assertThat(result.getCausationId()).isEqualTo(message.eventId());

        assertThat(result.getPayload())
                .contains("\"seatNumber\":\"H7\"")
                .contains("\"seatNumber\":\"H8\"")
                .contains("\"currency\":\"VND\"");
    }

    @Test
    void duplicateKafkaDeliveryShouldCreateOneResultOnly() throws Exception {

        UUID bookingId = UuidGenerator.next();

        OutboxEventMessage message = message(bookingId, List.of("H7", "H8"));

        publish(bookingId, message);
        publish(bookingId, message);

        await(() -> processedEventRepository.count() == 1L);

        await(() -> outboxRepository.count() == 1L);

        /*
         * Cho listener đủ thời gian xử lý record duplicate.
         * Assertion sau đó chứng minh duplicate không tạo thêm row.
         */
        waitForListenerDrain();

        assertThat(processedEventRepository.count()).isEqualTo(1);

        assertThat(outboxRepository.count()).isEqualTo(1);

        List<ShowSeat> showSeats =
                showSeatRepository.findAllByShowtime_IdOrderBySeatNumberAsc(showtimeId);

        assertThat(showSeats)
                .allSatisfy(
                        showSeat -> {
                            assertThat(showSeat.getStatus()).isEqualTo(ShowSeatStatus.HELD);

                            assertThat(showSeat.getHeldByBookingId()).isEqualTo(bookingId);
                        });
    }

    @Test
    void malformedMessageShouldBeSentDirectlyToDeadLetterTopic() throws Exception {

        UUID partitionKey = UuidGenerator.next();

        String malformedMessage =
                """
                {
                  "eventType": "seat-reservation-requested",
                  "eventVersion": "1",
                  "payload":
                }
                """;

        kafkaTemplate
                .send(TOPIC, partitionKey.toString(), malformedMessage)
                .get(10, TimeUnit.SECONDS);

        ConsumerRecord<String, String> deadLetterRecord = awaitDeadLetterRecord();

        assertThat(deadLetterRecord.topic()).isEqualTo(TOPIC + ".dlt");

        assertThat(deadLetterRecord.key()).isEqualTo(partitionKey.toString());

        assertThat(deadLetterRecord.value()).isEqualTo(malformedMessage);

        /*
         * DLT must not expose internal exception details.
         */
        Headers headers = deadLetterRecord.headers();

        assertThat(headers.lastHeader("kafka_dlt-exception-message")).isNull();

        assertThat(headers.lastHeader("kafka_dlt-exception-stacktrace")).isNull();

        assertThat(processedEventRepository.count()).isZero();

        assertThat(outboxRepository.count()).isZero();

        assertThat(showSeatRepository.findAllByShowtime_IdOrderBySeatNumberAsc(showtimeId))
                .allSatisfy(
                        showSeat ->
                                assertThat(showSeat.getStatus())
                                        .isEqualTo(ShowSeatStatus.AVAILABLE));
    }

    private void publish(UUID bookingId, OutboxEventMessage message) throws Exception {

        String serialized = objectMapper.writeValueAsString(message);

        kafkaTemplate.send(TOPIC, bookingId.toString(), serialized).get(10, TimeUnit.SECONDS);
    }

    private OutboxEventMessage message(UUID bookingId, List<String> seatNumbers) {

        SeatReservationRequestedPayload payload =
                new SeatReservationRequestedPayload(
                        bookingId,
                        UuidGenerator.next(),
                        showtimeId,
                        seatNumbers.stream().map(RequestedSeatPayload::new).toList(),
                        NOW,
                        NOW.plusMinutes(10));

        return new OutboxEventMessage(
                UuidGenerator.next(),
                bookingId,
                "BOOKING",
                InventoryEventContract.SEAT_RESERVATION_REQUESTED,
                InventoryEventContract.SEAT_RESERVATION_REQUESTED_VERSION,
                NOW,
                "booking-service",
                UuidGenerator.next(),
                null,
                objectMapper.valueToTree(payload));
    }

    private void createInventory() {

        Cinema cinema =
                cinemaRepository.saveAndFlush(
                        new Cinema("Cinema Kafka Test", "123 Main Street", "Ho Chi Minh City"));

        Room room =
                roomRepository.saveAndFlush(new Room(cinema, "Room Kafka Test", RoomType.STANDARD));

        Seat h7 = seatRepository.saveAndFlush(new Seat(room, "H7", "H", SeatType.STANDARD));

        Seat h8 = seatRepository.saveAndFlush(new Seat(room, "H8", "H", SeatType.VIP));

        Showtime showtime =
                new Showtime(
                        UuidGenerator.next(), room, NOW.plusDays(1), NOW.plusDays(1).plusHours(2));

        showtime.openForBooking();

        Showtime savedShowtime = showtimeRepository.saveAndFlush(showtime);

        showtimeId = savedShowtime.getId();

        showSeatRepository.saveAllAndFlush(
                List.of(
                        new ShowSeat(savedShowtime, h7, new BigDecimal("90000.00")),
                        new ShowSeat(savedShowtime, h8, new BigDecimal("120000.00"))));
    }

    private void cleanDatabase() {

        outboxRepository.deleteAll();
        processedEventRepository.deleteAll();
        showSeatRepository.deleteAll();
        showtimeRepository.deleteAll();
        seatRepository.deleteAll();
        roomRepository.deleteAll();
        cinemaRepository.deleteAll();
    }

    private void await(BooleanSupplier condition) throws InterruptedException {

        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);

        while (System.nanoTime() < deadline) {

            if (condition.getAsBoolean()) {
                return;
            }

            Thread.sleep(100);
        }

        throw new AssertionError("Asynchronous Kafka processing did not complete");
    }

    private void waitForListenerDrain() throws InterruptedException {

        Thread.sleep(500);
    }

    private ConsumerRecord<String, String> awaitDeadLetterRecord() {

        Map<String, Object> properties =
                Map.of(
                        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                        KAFKA.getBootstrapServers(),
                        ConsumerConfig.GROUP_ID_CONFIG,
                        "inventory-dlt-verification-" + UUID.randomUUID(),
                        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                        "earliest",
                        ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                        StringDeserializer.class,
                        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                        StringDeserializer.class);

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties)) {

            consumer.subscribe(List.of(TOPIC + ".dlt"));

            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);

            while (System.nanoTime() < deadline) {

                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(250));

                for (ConsumerRecord<String, String> record : records) {

                    return record;
                }
            }
        }

        throw new AssertionError("Dead-letter record was not published");
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FixedClockConfiguration {

        @Bean
        @Primary
        Clock fixedInventoryKafkaTestClock() {

            return Clock.fixed(CURRENT_INSTANT, ZoneOffset.UTC);
        }
    }
}
