package main.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class TelegramMessage {
	private String messageText;
	private List<String> photos;

	public TelegramMessage(String messageText) {
		this.messageText = messageText;
	}

	public TelegramMessage(String messageText, List<String> photos) {
		this.messageText = messageText;
		this.photos = photos;
	}
}
