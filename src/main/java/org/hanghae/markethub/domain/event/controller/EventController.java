package org.hanghae.markethub.domain.event.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import org.hanghae.markethub.domain.event.dto.CreateEventDto;
import org.hanghae.markethub.domain.event.dto.EventItemResponseDto;
import org.hanghae.markethub.domain.event.service.EventService;
import org.hanghae.markethub.domain.user.entity.User;
import org.hanghae.markethub.global.security.impl.UserDetailsImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api/event")
public class EventController {
	private final EventService eventService;

	@GetMapping
	public String eventPage(Model model) throws JsonProcessingException {
		model.addAttribute("items", eventService.getEventItemsResponseDtos());
		return "event";
	}

	@PostMapping
	@ResponseBody
	public void createEventItem(@AuthenticationPrincipal UserDetailsImpl userDetails,
								  @RequestPart("EventData") CreateEventDto createEventDto) {
		eventService.createEvent(userDetails.getUser(), createEventDto);
	}

	@DeleteMapping("/{itemId}")
	public void deleteEventItem(@AuthenticationPrincipal UserDetailsImpl userDetails, @PathVariable Long itemId) {
		eventService.deleteEvent(userDetails.getUser(), itemId);
	}

	@GetMapping("/info")
	@ResponseBody
	public List<EventItemResponseDto> getEventItems() throws JsonProcessingException {
		List<EventItemResponseDto> itemsResponseDtos = eventService.getEventItemsResponseDtos();
		return itemsResponseDtos;
	}

	@GetMapping("/schedule")
	@ResponseBody
	public ResponseEntity<String> setSchedule() {
		eventService.setEventSchedule();
		return ResponseEntity.ok("success");
	}

}
