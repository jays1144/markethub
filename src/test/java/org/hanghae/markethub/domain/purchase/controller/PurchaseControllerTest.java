package org.hanghae.markethub.domain.purchase.controller;


import jakarta.persistence.EntityNotFoundException;
import org.hanghae.markethub.domain.purchase.dto.PurchaseRequestDto;
import org.hanghae.markethub.domain.purchase.dto.PurchaseResponseDto;
import org.hanghae.markethub.domain.purchase.service.PurchaseService;
import org.hanghae.markethub.global.constant.Status;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.List;


@WebMvcTest(PurchaseController.class)
@MockBean(JpaMetamodelMappingContext.class)
class PurchaseControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PurchaseService purchaseService;

    @Test
    @DisplayName("주문 생성 200.OK 확인")
    public void 주문생성확인() throws Exception {
        PurchaseRequestDto requestDto = new PurchaseRequestDto(Status.EXIST); // Assuming Status.PENDING is valid
        PurchaseResponseDto responseDto = new PurchaseResponseDto(1L, Status.EXIST, Collections.emptyList(), null);

        given(purchaseService.createOrder(any(PurchaseRequestDto.class), anyString())).willReturn(responseDto);

        mockMvc.perform(post("/api/purchase/{userId}", "user123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(content().string("Purchase created successfully."));
    }

    @Test
    @DisplayName("주문 생성 예외처리 테스트")
    public void 주문생성예외처리확인() throws Exception {
        PurchaseRequestDto requestDto = new PurchaseRequestDto(Status.EXIST);

        given(purchaseService.createOrder(any(PurchaseRequestDto.class), anyString())).willThrow(new RuntimeException("Unexpected error"));

        mockMvc.perform(post("/api/purchase/{userId}", "user123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(requestDto)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(containsString("Error creating purchase")));
    }

    @Test
    @DisplayName("단일 아이템 주문 생성 테스트")
    public void 단일아이템주문생성확인() throws Exception {
        PurchaseRequestDto.SinglePurchaseRequestDto singlePurchaseRequestDto = new PurchaseRequestDto.SinglePurchaseRequestDto(Status.EXIST, 1L);
        String requestBody = new ObjectMapper().writeValueAsString(singlePurchaseRequestDto);

        given(purchaseService.createSingleOrder(any(PurchaseRequestDto.SinglePurchaseRequestDto.class)))
                .willReturn(new PurchaseResponseDto(1L, Status.EXIST, Collections.emptyList(), null));

        mockMvc.perform(post("/api/purchase/singleBuy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("주문 등록 완료")));
    }

    @Test
    @DisplayName("단일 아이템 주문 생성 실패 테스트")
    public void 단일아이템주문생성실패확인() throws Exception {
        PurchaseRequestDto.SinglePurchaseRequestDto singlePurchaseRequestDto = new PurchaseRequestDto.SinglePurchaseRequestDto(Status.EXIST, 1L);
        String requestBody = new ObjectMapper().writeValueAsString(singlePurchaseRequestDto);

        given(purchaseService.createSingleOrder(any(PurchaseRequestDto.SinglePurchaseRequestDto.class)))
                .willThrow(new RuntimeException("Item not found"));

        mockMvc.perform(post("/api/purchase/singleBuy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(containsString("Error")));
    }

    @Test
    @DisplayName("이메일로 단일 구매 내역 조회 테스트")
    public void findPurchaseByEmailTest() throws Exception {
        String email = "user@example.com";
        PurchaseResponseDto responseDto = new PurchaseResponseDto(1L, Status.EXIST, Collections.emptyList(), null);

        given(purchaseService.findPurchaseByEmail(email)).willReturn(responseDto);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/purchase/search/single/{email}", email))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.purchaseId").value(responseDto.purchaseId()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(responseDto.status().toString()));
    }
    @Test
    @DisplayName("이메일로 모든 구매 내역 조회 테스트")
    public void findAllPurchaseByEmailTest() throws Exception {
        String email = "user@example.com";
        List<PurchaseResponseDto> responseDtoList = Collections.singletonList(new PurchaseResponseDto(1L, Status.EXIST, Collections.emptyList(), null));

        given(purchaseService.findAllPurchaseByEmail(email)).willReturn(responseDtoList);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/purchase/search/{email}", email))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].purchaseId").value(responseDtoList.get(0).purchaseId()))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].status").value(responseDtoList.get(0).status().toString()));
    }
    @Test
    @DisplayName("이메일로 모든 구매 내역 조회 시 예외 처리 테스트")
    public void findAllPurchaseByEmailExceptionTest() throws Exception {
        String email = "nonexistent@example.com";

        given(purchaseService.findAllPurchaseByEmail(email)).willThrow(new RuntimeException("No purchases found"));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/purchase/search/{email}", email))
                .andExpect(MockMvcResultMatchers.status().isInternalServerError())
                .andExpect(MockMvcResultMatchers.content().string(containsString("No purchases found")));
    }
    @Test
    @DisplayName("구매취소 테스트")
    void deletePurchaseByIdTest() throws Exception {
        Long purchaseId = 1L;
        willDoNothing().given(purchaseService).deletePurchase(purchaseId);

        mockMvc.perform(delete("/api/purchase/{id}", purchaseId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("delete successfully"));
    }
    @Test
    @DisplayName("구매 취소 예외처리 테스트")
    void deletePurchaseByIdWhenEntityNotFound() throws Exception {
        Long purchaseId = 1L;
        doThrow(new EntityNotFoundException("Purchase not found")).when(purchaseService).deletePurchase(purchaseId);

        mockMvc.perform(delete("/api/purchase/{id}", purchaseId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Purchase not found"));
    }

}