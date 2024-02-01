package org.hanghae.markethub.domain.order.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hanghae.markethub.domain.cart.entity.Cart;
import org.hanghae.markethub.domain.item.entity.Item;
import org.hanghae.markethub.global.constant.Status;
import org.hanghae.markethub.global.date.BaseTimeEntity;

import java.util.List;

@Entity
@Getter
@NoArgsConstructor
public class Orders extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @OneToMany(mappedBy = "orders", cascade = CascadeType.ALL)
    private List<Cart> cart;

    @OneToOne
    private Item item;

    @Enumerated(EnumType.STRING)
    private Status status;


}
