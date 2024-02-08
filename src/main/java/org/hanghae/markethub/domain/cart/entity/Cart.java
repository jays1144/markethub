package org.hanghae.markethub.domain.cart.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hanghae.markethub.domain.cart.dto.CartRequestDto;
import org.hanghae.markethub.domain.item.entity.Item;
import org.hanghae.markethub.domain.item.repository.ItemRepository;
import org.hanghae.markethub.domain.purchase.entity.Purchase;
import org.hanghae.markethub.domain.user.entity.User;

import org.hanghae.markethub.global.constant.Status;
import org.hanghae.markethub.global.date.BaseTimeEntity;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "cart")
@AllArgsConstructor
@Builder
public class Cart extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cartId;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "item_id")
    private Item item;

    @Column(nullable = false)
    private int price;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    @Enumerated(value = EnumType.STRING)
    private Status status;

    @ManyToOne
    @JoinColumn(name = "purchase_id")
    private Purchase purchase;


    public void update(CartRequestDto requestDto, Item item){

            this.item = item;
            this.quantity = requestDto.getQuantity().get(0);
            this.price = item.getPrice() * requestDto.getQuantity().get(0);

    }

    public void delete() {
        this.status = Status.DELETED;
    }

    public void updateCart(CartRequestDto requestDto, Item item) {
        this.quantity = requestDto.getQuantity().get(0);
        this.price = item.getPrice() * requestDto.getQuantity().get(0);
    }

    public void updateDelete(CartRequestDto requestDto, Item item) {
        this.quantity = requestDto.getQuantity().get(0);
        this.status = Status.EXIST;
        this.price = item.getPrice() * requestDto.getQuantity().get(0);
    }
}
