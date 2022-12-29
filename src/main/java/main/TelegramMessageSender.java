package main;

import lombok.extern.slf4j.Slf4j;
import main.dto.TelegramMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.LinkedList;
import java.util.List;

@Slf4j
@Component
public class TelegramMessageSender extends TelegramLongPollingBot {
	@Value("${telegram.bot.nickname}")
	private String botNickname;
	@Value("${telegram.bot.token}")
	private String botToken;
	@Value("${telegram.admin_id}")
	private String adminId;
	@Value("${telegram.channel_id}")
	private String channelId;

	public void sendAdminMessage(String message) {
		sendMessage(adminId, message);
	}

	public void sendChannelMessage(String message) {
		sendMessage(channelId, message);
	}

	public void sendChannelMessage(TelegramMessage telegramMessage) {
		if (telegramMessage.getPhotos() != null)
			sendChannelMessage(telegramMessage.getMessageText(), telegramMessage.getPhotos());
		else
			sendChannelMessage(telegramMessage.getMessageText());
	}

	public void sendChannelMessage(String message, List<String> photos) {
		if (photos.size() == 0) sendChannelMessage(message);
		else if (photos.size() == 1) sendPhoto(channelId, message, photos.get(0));
		else sendPhotos(channelId, message, photos);
	}

	private void sendPhotos(String userId, String message, List<String> photos) {
		try {
			List<InputMedia> medias = new LinkedList<>();
			for (String photoUrl : photos) {
				medias.add(new InputMediaPhoto(photoUrl));
			}
			medias.get(0).setCaption(message);
			SendMediaGroup sendMediaGroup = new SendMediaGroup(userId, medias);
			execute(sendMediaGroup);
		} catch (TelegramApiException e) {
			log.error("Ошибка отправки сообщения", e);
		}
	}

	private void sendPhoto(String userId, String message, String photoUrl) {
		try {
			SendPhoto sendPhoto = new SendPhoto(userId, new InputFile(photoUrl));
			sendPhoto.setCaption(message);
			execute(sendPhoto);
		} catch (TelegramApiException e) {
			log.error("Ошибка отправки сообщения", e);
		}
	}

	private void sendMessage(String userId, String message) {
		try {
			SendMessage sendMessage = new SendMessage(userId, message);
			sendMessage.setDisableWebPagePreview(true);
			execute(sendMessage);
		} catch (TelegramApiException e) {
			log.error("Ошибка отправки сообщения", e);
		}
	}

	@Override
	public void onUpdateReceived(Update update) {
		sendMessage(String.valueOf(update.getMessage().getChatId()), "Работает");
	}

	@Override
	public String getBotUsername() {
		return botNickname;
	}

	@Override
	public String getBotToken() {
		return botToken;
	}
}
