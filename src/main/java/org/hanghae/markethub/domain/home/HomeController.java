package org.hanghae.markethub.domain.home;

import lombok.RequiredArgsConstructor;
import org.hanghae.markethub.domain.item.repository.ItemRepository;
import org.hanghae.markethub.domain.item.service.ItemService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequiredArgsConstructor
public class HomeController {
	private final ItemService itemService;
	@GetMapping
	public String getHome(Model model) {
		model.addAttribute("items", itemService.getItems());
		//
		return "index";
	}
}
