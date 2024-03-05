package org.hanghae.markethub.domain.item.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.hanghae.markethub.domain.item.config.ElasticSearchConfig;
import org.hanghae.markethub.domain.item.dto.ItemCreateRequestDto;
import org.hanghae.markethub.domain.item.dto.ItemUpdateRequestDto;
import org.hanghae.markethub.domain.item.dto.ItemsResponseDto;
import org.hanghae.markethub.domain.item.dto.RedisItemResponseDto;
import org.hanghae.markethub.domain.item.entity.Item;
import org.hanghae.markethub.domain.item.repository.ItemRepository;
import org.hanghae.markethub.domain.store.service.StoreService;
import org.hanghae.markethub.global.service.AwsS3Service;
import org.hanghae.markethub.domain.user.entity.User;
import org.hanghae.markethub.global.constant.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ItemService {
	private final ItemRepository itemRepository;
	private final AwsS3Service awsS3Service;
	private final StoreService storeService;
	private final ElasticSearchService elasticSearchService;
	private final ElasticSearchConfig elasticSearchConfig;
	private final RedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;

	public Item getItemValid(Long itemId) {
		return itemRepository.findById(itemId).orElse(null);
	}

	public void createItem(ItemCreateRequestDto requestDto,
						   List<MultipartFile> files,
						   User user) {

		if (requestDto.getQuantity() < 0 || requestDto.getPrice() < 0) {
			throw new IllegalArgumentException("가격 또는 재고는 0 이하일 수 없습니다.");
		}

		Item item = Item.builder()
				.itemName(requestDto.getItemName())
				.itemInfo(requestDto.getItemInfo())
				.price(requestDto.getPrice())
				.quantity(requestDto.getQuantity())
				.category(requestDto.getCategory())
				.status(Status.EXIST)
				.user(user)
				.store(storeService.findByUsergetStore(user.getId()))
				.build();

		Item save = itemRepository.save(item);
		createItemForRedis(save, files);
		elasticSearchConfig.syncItemToElasticsearch(save);

	}

	public void createItemForRedis(Item item, List<MultipartFile> file) {
		String key = "item";
		try {
			awsS3Service.uploadFiles(file, item);
			updateForRedis(item, key);

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public Page<ItemsResponseDto> getItems(int page, int size) {
		Pageable pageable = PageRequest.of(page, size);
		Page<Item> itemPage = itemRepository.findAll(pageable);
		return itemPage.map(item -> {
			List<String> pictureUrls = awsS3Service.getObjectUrlsForItem(item.getId());
			return ItemsResponseDto.fromEntity(item, pictureUrls);
		});
	}


	public ItemsResponseDto getItem(Long itemId) throws JsonProcessingException {
		String key = "item";
		String findKey = key + ":" + itemId;
		String getKey = (String) redisTemplate.opsForValue().get(findKey);
		if (getKey == null) {
			Item item = itemRepository.findById(itemId).orElseThrow(() -> new IllegalArgumentException("No such Item"));
			updateForRedis(item, key);
			return ItemsResponseDto.fromEntity(item, awsS3Service.getObjectUrlsForItemTest(item));
		}
		RedisItemResponseDto redisItemResponseDto = objectMapper.readValue(getKey, RedisItemResponseDto.class);
		return ItemsResponseDto.builder()
				.id(redisItemResponseDto.getId())
				.itemName(redisItemResponseDto.getItemName())
				.price(redisItemResponseDto.getPrice())
				.quantity(redisItemResponseDto.getQuantity())
				.itemInfo(redisItemResponseDto.getItemInfo())
				.category(redisItemResponseDto.getCategory())
				.pictureUrls(redisItemResponseDto.getPictureUrls())
				.build();

	}

	@Transactional
	public Item updateItem(Long itemId, ItemUpdateRequestDto requestDto, User user) throws JsonProcessingException {

		Item item = itemRepository.findById(itemId).orElseThrow(
				() -> new IllegalArgumentException("No such item"));

		if (item.getUser().getId() != user.getId()) {
			throw new IllegalArgumentException("본인 상품만 수정이 가능합니다.");
		}
		item.updateItem(requestDto);
		updateItemForRedis(item);
		elasticSearchConfig.syncItemToElasticsearch(item);
		return item;
	}

	public void updateItemForRedis(Item item) throws JsonProcessingException {

		String itemKey = "item:" + item.getId();
		List<String> pictureUrls = awsS3Service.getObjectUrlsForItem(item.getId());
		RedisItemResponseDto dto = item.convertToDto(item, pictureUrls);
		String json = objectMapper.writeValueAsString(dto);
		redisTemplate.opsForValue().set(itemKey, json);

	}

	@Transactional
	public void deleteItem(Long itemId, User user) {

		Item item = itemRepository.findById(itemId).orElseThrow(
				() -> new IllegalArgumentException("No such item"));
		if (item.getUser().getId() != user.getId()) {
			throw new IllegalArgumentException("본인 상품만 수정이 가능합니다.");
		}
		deleteItemForRedis(itemId);
		elasticSearchConfig.deleteItemForElasticSearch(item);
		item.deleteItem();

	}

	public void deleteItemForRedis(Long itemId) {
		String key = "item:" + itemId;
		redisTemplate.delete(key);
	}

	public Page<ItemsResponseDto> findByKeyWord(String keyword, int page, int size) {
		Page<ItemsResponseDto> itemsResponseDtos = elasticSearchService.searchNativeQuery(keyword, page, size);
		return itemsResponseDtos;
	}

	@Transactional
	public void decreaseQuantity(Long itemId, int quantity) throws JsonProcessingException {
		Item item = itemRepository.findById(itemId).orElseThrow();
		if (quantity > item.getQuantity()) {
			throw new IllegalArgumentException("상품의 재고가 부족합니다.");
		}
		item.decreaseItemQuantity(quantity);
		elasticSearchConfig.syncItemToElasticsearch(item);
		updateItemForRedis(item);
	}

	@Transactional
	public void increaseQuantity(Long itemId, int quantity) throws JsonProcessingException {
		Item item = itemRepository.findById(itemId).orElseThrow();
		item.increaseItemQuantity(quantity);
		elasticSearchConfig.syncItemToElasticsearch(item);
		updateItemForRedis(item);
	}

	public boolean isSoldOut(Long itemId) {
		Item item = itemRepository.findById(itemId).orElseThrow(() -> new IllegalArgumentException("No such item"));
		if (item.getQuantity() == 0) {
			return true;
		}
		return false;
	}

	@PostConstruct
	public void createRedisItem() {
		String key = "item";
		List<Item> items = itemRepository.findAllNotPageable();
		for (Item item : items) {
			try {
				updateForRedis(item, key);

			} catch (JsonProcessingException e) {
				System.out.println(e.getMessage());
			}
		}
	}

	public boolean validItem(Long itemId, int quantity) throws JsonProcessingException {
		String key = "item:" + itemId;
		String json = (String) redisTemplate.opsForValue().get(key);
		RedisItemResponseDto redisItemResponseDto = objectMapper.readValue(json, RedisItemResponseDto.class);
		if (redisItemResponseDto.getQuantity() < quantity) {
			throw new IllegalArgumentException("재고가 부족합니다.");
		}
		return true;
	}


	private void updateForRedis(Item item, String key) throws JsonProcessingException {
		List<String> objectUrlsForItem = awsS3Service.getObjectUrlsForItemTest(item);
		RedisItemResponseDto dto = item.convertToDto(item, objectUrlsForItem);
		String json = objectMapper.writeValueAsString(dto);
		String itemKey = "item:" + item.getId();
		double score = item.getId();
		redisTemplate.opsForZSet().add(key, itemKey, score);
		redisTemplate.opsForValue().set(itemKey, json);
	}
}
