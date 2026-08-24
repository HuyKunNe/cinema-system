package com.cinema.booking.service;

import java.util.UUID;

public interface BookingExpirationService {

    boolean expireIfDue(UUID bookingId);
}
