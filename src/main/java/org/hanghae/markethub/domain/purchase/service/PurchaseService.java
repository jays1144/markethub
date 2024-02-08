package org.hanghae.markethub.domain.purchase.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

import org.hanghae.markethub.domain.cart.entity.Cart;
import org.hanghae.markethub.domain.cart.repository.CartRepository;
import org.hanghae.markethub.domain.item.entity.Item;
import org.hanghae.markethub.domain.item.repository.ItemRepository;
import org.hanghae.markethub.domain.purchase.dto.PurchaseRequestDto;
import org.hanghae.markethub.domain.purchase.dto.PurchaseResponseDto;
import org.hanghae.markethub.domain.purchase.entity.Purchase;
import org.hanghae.markethub.domain.purchase.repository.PurchaseRepository;
import org.hanghae.markethub.domain.user.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PurchaseService {

    private final PurchaseRepository purchaseRepository;
    private final CartRepository cartRepository;
    private final ItemRepository itemRepository;

    //C
    @Transactional
    public PurchaseResponseDto createOrder(PurchaseRequestDto purchaseRequestDto, User user) {
        List<Cart> cart = cartRepository.findAllByUser(user);
        Purchase purchase = Purchase.builder()
                .status(purchaseRequestDto.status())
                .cart(cart)
                .build();

        purchaseRepository.save(purchase);

        return PurchaseResponseDto.fromPurchase(purchase);



    }
    @Transactional
    public PurchaseResponseDto createSingleOrder(PurchaseRequestDto.SinglePurchaseRequestDto singlePurchaseRequestDto) {
        Item item = itemRepository.findById(singlePurchaseRequestDto.itemId()).orElseThrow(() -> new IllegalArgumentException("Item not found for ID: " + singlePurchaseRequestDto.itemId()));

        Purchase purchase = Purchase.builder()
                .status(singlePurchaseRequestDto.status())
                .item(item)
                .build();

        return PurchaseResponseDto.fromPurchase(purchase);
    }

    //R
    //다건 조회 유저 email 기반
    @Transactional(readOnly = true)
    public List<PurchaseResponseDto> findAllPurchaseByEmail(String email) {
        List<Purchase> purchase = purchaseRepository.findByUserId(email);
        if (purchase == null) {
            throw new EntityNotFoundException("Purchase not found for email: " + email);
        }
        return PurchaseResponseDto.fromListPurchaseEntity(purchase);
    }

    // 단건조회 email 기반
    @Transactional(readOnly = true)
    public PurchaseResponseDto findPurchaseByEmail(String email) {
        Purchase purchase = purchaseRepository.findByUserEmail(email);
        if (purchase == null) {
            throw new EntityNotFoundException("Purchase not found for email: " + email);
        }

        return PurchaseResponseDto.fromPurchase(purchase);
    }

    //U
    @Transactional
    public void deletePurchase(Long id) {
        Purchase purchase = purchaseRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Purchase not found"));

        purchase.setStatusToDelete();

    }


    //D
}
