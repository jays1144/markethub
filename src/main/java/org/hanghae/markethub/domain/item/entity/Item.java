package org.hanghae.markethub.domain.item.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hanghae.markethub.domain.picture.Picture;
import org.hanghae.markethub.domain.store.entity.Store;
import org.hanghae.markethub.domain.user.User;
import org.hanghae.markethub.global.Status;

import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "item")
public class Item {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String itemName;

	@Column(nullable = false)
	private int price;

	@Column(nullable = false)
	private int quantity;

	@Column(nullable = false)
	private String itemInfo;

	@Column(nullable = false)
	private String category;

	@Enumerated(value = EnumType.STRING)
	private Status status = Status.EXIST;

	@ManyToOne
	@JoinColumn(name ="store_id",nullable = false)
	@JsonIgnore
	private Store store;

	@ManyToOne
	@JoinColumn(name ="user_id",nullable = false)
	@JsonIgnore
	private User user;

	@OneToMany(mappedBy = "item")
	@Builder.Default
	private List<Picture> pictures = new ArrayList<>();
}
