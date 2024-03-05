package org.hanghae.markethub.domain.event.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.hanghae.markethub.domain.event.dto.CreateEventDto;
import org.hanghae.markethub.domain.event.dto.EventItemResponseDto;
import org.hanghae.markethub.domain.event.entity.Event;
import org.hanghae.markethub.domain.event.repository.EventRepository;
import org.hanghae.markethub.domain.item.dto.ItemUpdateRequestDto;
import org.hanghae.markethub.domain.item.dto.ItemsResponseDto;
import org.hanghae.markethub.domain.item.dto.RedisItemResponseDto;
import org.hanghae.markethub.domain.item.entity.Item;
import org.hanghae.markethub.domain.item.service.ItemService;
import org.hanghae.markethub.domain.user.entity.User;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

@Service
@RequiredArgsConstructor
@EnableRedisRepositories(basePackages = "org.hanghae.markethub.domain.event.repository")
public class EventService {
	private final ItemService itemService;
	private final EventRepository eventRepository;
	private final List<EventItemResponseDto> eventItemResponseDtos = new ArrayList<>();
	private final TaskScheduler taskScheduler;
	private final RedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;
	private ScheduledFuture<?> startEventScheduledFuture;
	private ScheduledFuture<?> endEventScheduledFuture;
	private String time ;

	public void setEventSchedule() {
		LocalTime startTime = LocalTime.now().plusSeconds(5);
		LocalTime endTime = LocalTime.now().plusSeconds(30);
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HHmmss");
		time = startTime.format(formatter);
		int[] start = timeConvert(startTime);
		int[] end = timeConvert(endTime);

		String startCronExpression = convertToCronExpression(start[0], start[1], start[2]);
		String endCronExpression = convertToCronExpression(end[0], end[1], end[2]);

		if (startEventScheduledFuture != null) {
			startEventScheduledFuture.cancel(true);
		}
		if (endEventScheduledFuture != null) {
			endEventScheduledFuture.cancel(true);
		}

		// 시작 시간 스케줄 설정
		startEventScheduledFuture = taskScheduler.schedule(new Runnable() {
			@Override
			public void run() {
				try {
					startEvent();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		}, new CronTrigger(startCronExpression));

		// 종료 시간 스케줄 설정
		endEventScheduledFuture = taskScheduler.schedule(new Runnable() {
			@Override
			public void run() {
				try {
					time = null;
					endEvent();
				} catch (JsonProcessingException e) {
					throw new RuntimeException(e);
				}
			}
		}, new CronTrigger(endCronExpression));
	}


	public void createEvent(User user, CreateEventDto createEventDto) {
		Item item = itemService.getItemValid(createEventDto.getItemId());
		if (item.getUser().getId() != user.getId()) {
			throw new IllegalArgumentException("본인 상품만 등록 가능합니다.");
		}

		Event findEvent = eventRepository.findByItemId(createEventDto.getItemId());
		if (findEvent != null) {
			eventRepository.delete(findEvent);
		}
		Event event = Event.builder()
				.itemId(createEventDto.getItemId())
				.quantity(createEventDto.getQuantity())
				.price(createEventDto.getPrice())
				.oldPrice(item.getPrice())
				.build();

		eventRepository.save(event);
	}

	public void deleteEvent(User user, Long itemId) {
		Item item = itemService.getItemValid(itemId);
		if (item.getUser().getId() != user.getId()) {
			throw new IllegalArgumentException("본인 상품만 삭제가 가능합니다.");
		}
		Event event = eventRepository.findByItemId(itemId);
		if (event != null) {
			eventRepository.delete(event);
		}
	}

	public void startEvent() throws JsonProcessingException {
		List<Event> events = eventRepository.findAll();

		for (Event event : events) {
			Item item = itemService.getItemValid(event.getItemId());

			ItemUpdateRequestDto requestDto = ItemUpdateRequestDto.builder()
					.itemName(item.getItemName())
					.quantity(event.getQuantity())
					.price(event.getPrice())
					.itemInfo(item.getItemInfo())
					.category(item.getCategory())
					.build();
			itemService.updateItem(item.getId(), requestDto, item.getUser());

		}
	}

	@Transactional
	public void endEvent() throws JsonProcessingException {
		List<Event> events = eventRepository.findAll();
		if (!events.isEmpty()) {
			for (Event event : events) {
				Item item = itemService.getItemValid(event.getItemId());
				ItemUpdateRequestDto requestDto = ItemUpdateRequestDto.builder()
						.itemName(item.getItemName())
						.quantity(item.getQuantity())
						.price(event.getOldPrice())
						.itemInfo(item.getItemInfo())
						.category(item.getCategory())
						.build();
				itemService.updateItem(item.getId(), requestDto, item.getUser());
			}
		}
		this.eventRepository.deleteAll();
	}

	@Transactional
	public List<EventItemResponseDto> getEventItemsResponseDtos() throws JsonProcessingException {

		if (!eventItemResponseDtos.isEmpty()) {
			eventItemResponseDtos.clear();
		}

		List<Event> events = eventRepository.findAll();
		if (!events.isEmpty()) {
			for (Event event : events) {
				Item item = itemService.getItemValid(event.getItemId());
				String key = "item";
				String findKey = key + ":" + event.getItemId();
				String getKey = (String) redisTemplate.opsForValue().get(findKey);

				RedisItemResponseDto redisItemResponseDto = objectMapper.readValue(getKey, RedisItemResponseDto.class);

				ItemsResponseDto itemsResponseDto = ItemsResponseDto.builder()
						.id(redisItemResponseDto.getId())
						.itemName(redisItemResponseDto.getItemName())
						.price(redisItemResponseDto.getPrice())
						.quantity(redisItemResponseDto.getQuantity())
						.itemInfo(redisItemResponseDto.getItemInfo())
						.category(redisItemResponseDto.getCategory())
						.pictureUrls(redisItemResponseDto.getPictureUrls())
						.build();

				EventItemResponseDto eventItemResponseDto = EventItemResponseDto.builder()
						.items(itemsResponseDto)
						.oldPrice(event.getOldPrice())
						.build();

				eventItemResponseDtos.add(eventItemResponseDto);
			}

		}
		return this.eventItemResponseDtos;
	}

	public String getEventTime() {
		return this.time;
	}

	public int[] timeConvert(LocalTime localTime) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
		String formattedTime = localTime.format(formatter);
		String[] time = formattedTime.split(":");
		int[] timeInt = new int[time.length];
		for (int i = 0; i < time.length; i++) {
			timeInt[i] = Integer.parseInt(time[i]);
		}
		return timeInt;
	}

	private static String convertToCronExpression(int hour, int minute, int second) {
		return String.format("%d %d %d * * ?", second, minute, hour);
	}
}