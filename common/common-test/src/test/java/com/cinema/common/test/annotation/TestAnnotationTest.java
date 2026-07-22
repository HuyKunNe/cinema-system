package com.cinema.common.test.annotation;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

class TestAnnotationTest {

    @Test
    void unitTestShouldContainUnitTag() {

        Tag tag = UnitTest.class.getAnnotation(
                Tag.class);

        assertNotNull(
                tag);

        assertTrue(
                tag.value().equals(
                        "unit"));

    }

    @Test
    void integrationTestShouldContainIntegrationMetadata() {

        Tag tag = IntegrationTest.class.getAnnotation(
                Tag.class);

        SpringBootTest springBootTest = IntegrationTest.class.getAnnotation(
                SpringBootTest.class);

        ActiveProfiles activeProfiles = IntegrationTest.class.getAnnotation(
                ActiveProfiles.class);

        assertNotNull(
                tag);

        assertNotNull(
                springBootTest);

        assertNotNull(
                activeProfiles);

        assertTrue(
                tag.value().equals(
                        "integration"));

        assertTrue(
                activeProfiles.value().length == 1);

        assertTrue(
                activeProfiles.value()[0].equals(
                        "test"));

    }

}
