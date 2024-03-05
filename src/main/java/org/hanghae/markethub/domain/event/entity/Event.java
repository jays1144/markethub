package org.hanghae.markethub.domain.event.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
@RedisHash(value = "event")
public class Event {

	@Id
	private Long id;

	@Indexed
	private Long itemId;

	private int quantity;

	private int price;

	private int oldPrice;

}
