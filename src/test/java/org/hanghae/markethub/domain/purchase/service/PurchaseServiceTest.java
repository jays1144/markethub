package org.hanghae.markethub.domain.purchase.service;

import jakarta.persistence.EntityNotFoundException;
import org.hanghae.markethub.domain.cart.entity.Cart;
import org.hanghae.markethub.domain.cart.repository.CartRepository;
import org.hanghae.markethub.domain.item.entity.Item;
import org.hanghae.markethub.domain.item.repository.ItemRepository;
import org.hanghae.markethub.domain.purchase.dto.PurchaseRequestDto;
import org.hanghae.markethub.domain.purchase.dto.PurchaseResponseDto;
import org.hanghae.markethub.domain.purchase.entity.Purchase;
import org.hanghae.markethub.domain.purchase.repository.PurchaseRepository;
import org.hanghae.markethub.global.constant.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PurchaseServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private PurchaseRepository purchaseRepository;

    @InjectMocks
    private PurchaseService purchaseService;

    @Mock
    private ItemRepository itemRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("주문 생성 로직 테스트")
    void 주문하기테스트() {
        // Given
        String userId = "testUser";
        Status status = Status.EXIST;
        PurchaseRequestDto requestDto = new PurchaseRequestDto(status);

        List<Cart> carts = new ArrayList<>();

        when(cartRepository.findByUserId(userId)).thenReturn(carts);
        when(purchaseRepository.save(any(Purchase.class))).thenAnswer(i -> i.getArgument(0));

        // When
        PurchaseResponseDto responseDto = purchaseService.createOrder(requestDto, userId);

        // Then
        assertNotNull(responseDto);
        assertEquals(responseDto.status(), status);
        assertEquals(responseDto.carts().size(), carts.size());
        verify(cartRepository).findByUserId(userId);
        verify(purchaseRepository).save(any(Purchase.class));
    }

    @Test
    @DisplayName("단일 아이템 주문 등록 테스트")
    void 단일아이템주문테스트(){
        Long itemId = 1L;
        Status status = Status.EXIST;
        PurchaseRequestDto.SinglePurchaseRequestDto requestDto = new PurchaseRequestDto.SinglePurchaseRequestDto(status, itemId);
        Item item = new Item(); // 적절한 아이템 객체 생성

        when(itemRepository.findById(itemId)).thenReturn(Optional.of(item));
        when(purchaseRepository.save(any(Purchase.class))).thenAnswer(i -> i.getArgument(0));

        // When
        PurchaseResponseDto responseDto = purchaseService.createSingleOrder(requestDto);

        // Then
        assertNotNull(responseDto);
        assertEquals(responseDto.status(), status);
    }

    @Test
    @DisplayName("단일 아이템 주문 예외처리 테스트")
    void 단일아이템주문예외처리테스트() {
        Long itemId = 1L;
        Status status = Status.EXIST;
        PurchaseRequestDto.SinglePurchaseRequestDto requestDto = new PurchaseRequestDto.SinglePurchaseRequestDto(status, itemId);

        when(itemRepository.findById(itemId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            purchaseService.createSingleOrder(requestDto);
        });
    }

    @Test
    @DisplayName("이메일로 모든 구매 내역 조회 테스트")
    void findAllPurchaseByEmailTest() {
        // Given
        String email = "user@example.com";
        List<Purchase> mockPurchases = List.of(new Purchase(Status.EXIST, null)); // 테스트 데이터 준비
        when(purchaseRepository.findAllByUserEmail(email)).thenReturn(mockPurchases);

        // When
        List<PurchaseResponseDto> responseDtoList = purchaseService.findAllPurchaseByEmail(email);

        // Then
        assertNotNull(responseDtoList);
        assertFalse(responseDtoList.isEmpty());
        assertEquals(mockPurchases.size(), responseDtoList.size());
        verify(purchaseRepository).findAllByUserEmail(email);
    }

    @Test
    @DisplayName("이메일로 단일 구매 내역 조회 테스트")
    void findPurchaseByEmailTest() {
        // Given
        String email = "user@example.com";
        Purchase mockPurchase = new Purchase(Status.EXIST, null); // 테스트 데이터 준비
        when(purchaseRepository.findByUserEmail(email)).thenReturn(mockPurchase);

        // When
        PurchaseResponseDto responseDto = purchaseService.findPurchaseByEmail(email);

        // Then
        assertNotNull(responseDto);
        assertEquals(mockPurchase.getStatus(), responseDto.status());
        verify(purchaseRepository).findByUserEmail(email);
    }

    @Test
    @DisplayName("구매 내역 삭제 로직 테스트")
    void deletePurchaseTest() {
        // Given
        Long purchaseId = 1L;
        Purchase mockPurchase = mock(Purchase.class);
        when(purchaseRepository.findById(purchaseId)).thenReturn(Optional.of(mockPurchase));

        // When
        purchaseService.deletePurchase(purchaseId);

        // Then
        verify(purchaseRepository).findById(purchaseId);
        verify(mockPurchase).setStatusToDelete(); // 상태가 DELETED로 설정되었는지 확인
        verify(purchaseRepository, never()).save(any(Purchase.class)); // 더티 체킹으로 인해 save 호출이 없는 것을 확인
    }

    @Test
    @DisplayName("존재하지 않는 구매 내역 삭제 시 EntityNotFoundException 발생")
    void deleteNonexistentPurchaseThrowsExceptionTest() {
        // Given
        Long nonexistentPurchaseId = 2L;
        when(purchaseRepository.findById(nonexistentPurchaseId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(EntityNotFoundException.class, () -> purchaseService.deletePurchase(nonexistentPurchaseId),
                "EntityNotFoundException should be thrown for a nonexistent purchase.");
    }
}