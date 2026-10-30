package com.cinema.payment.mapper;

import com.cinema.common.mapper.config.MapperConfiguration;
import com.cinema.payment.controller.response.PaymentManagementResponse;
import com.cinema.payment.controller.response.PaymentTransactionManagementResponse;
import com.cinema.payment.entity.Payment;
import com.cinema.payment.entity.PaymentTransaction;

import org.mapstruct.Mapper;

@Mapper(config = MapperConfiguration.class)
public interface PaymentManagementMapper {

    PaymentManagementResponse toResponse(Payment payment);

    PaymentTransactionManagementResponse toResponse(PaymentTransaction transaction);
}
