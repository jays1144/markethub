package org.hanghae.markethub.domain.purchase.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;


public record PaymentRequestDto(
        String email,
        String impUid,
        List<PurchaseItemDto> items,
        double amount
){
    public record PurchaseItemDto(
            Long itemId,
            int quantity
    ) {

    }

    public record getToken(
            String imp_key,
            String imp_secret
    ) {}
}