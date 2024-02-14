package org.hanghae.markethub.domain.item.controller;

import lombok.RequiredArgsConstructor;
import org.hanghae.markethub.domain.item.dto.ItemCreateRequestDto;
import org.hanghae.markethub.domain.item.dto.ItemUpdateRequestDto;
import org.hanghae.markethub.domain.item.entity.Item;
import org.hanghae.markethub.domain.item.repository.ItemRepository;
import org.hanghae.markethub.domain.item.service.ItemService;
import org.hanghae.markethub.domain.user.security.UserDetailsImpl;


import org.hanghae.markethub.global.config.RedissonFairLock;
import org.hanghae.markethub.global.redis.RedisConfiguration;
import org.redisson.Redisson;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RQueue;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api/items")
public class ItemController {
	private final ItemService itemService;
	private final ItemRepository itemRepository;
	private final RedissonFairLock redissonFairLock;
	private final RedisTemplate<String, String> redisTemplate;
	private final RedissonClient redissonClient;
	int i = 1;

	@GetMapping
	public String getAllItems(Model model) {
		model.addAttribute("items", itemService.getItems());
		return "items";
	}

	@GetMapping("/{itemId}")
	public String getItem(@PathVariable Long itemId, Model model,
						  @AuthenticationPrincipal UserDetailsImpl userDetails) {
		model.addAttribute("items", itemService.getItem(itemId));
		model.addAttribute("email", userDetails.getUser().getEmail());
		return "item";
	}

	@GetMapping("/posts/{postsId}")
	@ResponseBody
	public String getPosts(@PathVariable Long postsId) {
		return "perfTest postsId : " + postsId;
	}

	@GetMapping("/category")
	public String findByCategory(@RequestParam String category, Model model) {
		model.addAttribute("items", itemService.findByCategory(category));
		return "items";
	}

	@PostMapping
	@ResponseBody
	public void createItem(@RequestPart("itemData") ItemCreateRequestDto itemCreateRequestDto,
						   @RequestPart("files") List<MultipartFile> file,
						   @AuthenticationPrincipal UserDetailsImpl userDetails) throws IOException {
		itemService.createItem(itemCreateRequestDto, file, userDetails.getUser());
	}

	@PatchMapping("/{itemId}")
	@ResponseBody
	private void updateItem(@PathVariable Long itemId,
							@RequestPart("itemData") ItemUpdateRequestDto itemUpdateRequestDto,
							@AuthenticationPrincipal UserDetailsImpl userDetails) {
		itemService.updateItem(itemId, itemUpdateRequestDto, userDetails.getUser());
	}

	@GetMapping("/de/{number}")
	@ResponseBody
	public void de(@PathVariable Long number) {

		// Redis 메시지 큐에 요청 추가
		RQueue<String> orderQueue = redissonClient.getQueue("orderQueue");
		orderQueue.add(number.toString());
		if (i<60) {
			System.out.println(i+"번째 수 : "+number);
		}
		// 요청 처리 메소드 호출
		processOrders();
	}

	public void processOrders() {
		// 레디스 메시지 큐에서 요청을 하나씩 가져와 처리
		RQueue<String> orderQueue = redissonClient.getQueue("orderQueue");
		String orderNumber = orderQueue.poll();
		if (orderNumber != null) {
			redissonFairLock.performWithFairLock("dementLock", () -> {
				// Item 로직 수행
				Item item = itemRepository.findById(1L).orElseThrow();
				if (item.getQuantity() > 0) {

					itemService.decreaseQuantity(1L, 1);
					System.out.println(i+"Success for order number: " + orderNumber);
					i++;
				}
			});
		} else {
			// 큐에 더 이상 처리할 요청이 없을 때의 처리
			System.out.println("No orders to process.");
		}
	}
}

//	@GetMapping("/de/{number}")
//	@ResponseBody
//	public void de(@PathVariable Long number) {
//		RBucket<Long> requestBucket = redisson.getBucket("request_order_" + number);
//		requestBucket.set(number);
//
//		Long orderNumber = requestBucket.get();
//
//		redissonFairLock.performWithFairLock("dementLock", () -> {
//			Item item = itemRepository.findById(1L).orElseThrow();
//			if(item.getQuantity() > 0) {
//				itemService.decreaseQuantity(1L, 1);
//				System.out.println("success nunber : " + orderNumber);
//			}else {
//				System.out.println("fail number :" + orderNumber);
//			}
//		});
//	}

//	@GetMapping("/de/{number}")
//	@ResponseBody
//	public void de(@PathVariable Long number) throws InterruptedException {
//		Config config = new Config();
//		config.useSingleServer().setAddress("redis://127.0.0.1:6379");
//
//		// Redisson 클라이언트 생성
//		RedissonClient redisson = Redisson.create(config);
//
//		// 공정락(Fair Lock) 사용 예제
//		RLock fairLock = redisson.getFairLock("myFairLock");
//		fairLock.lock(10, TimeUnit.MILLISECONDS);
//		boolean res = fairLock.tryLock(100, 10, TimeUnit.MILLISECONDS);
//		if (res) {
//			try {
//				System.out.println("공정락 획득");
//				Item item = itemRepository.findById(1L).orElseThrow();
//				if(item.getQuantity() > 0) {
//					itemService.decreaseQuantity(1L, 1);
//					System.out.println("success nunber : " + number);
//				}else {
//					System.out.println("fail number :" + number);
//				}
//			} finally {
//				fairLock.unlock();
//				System.out.println("공정락 해제");
//			}
//
//		} else {
//			throw new RuntimeException("공정락을 획득할 수 없습니다.");
//		}
//	}

//	@GetMapping("/de/{number}")
//	@ResponseBody
//	public void de(@PathVariable Long number) {
//			Item item = itemRepository.findById(1L).orElseThrow();
//			if(item.getQuantity() > 0) {
//				itemService.decreaseQuantity(1L, 1);
//				System.out.println("success nunber : " + number);
//			}else {
//				System.out.println("fail number :" + number);
//			}
//	}

