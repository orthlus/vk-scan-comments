package main.vk;

import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.objects.base.Image;
import com.vk.api.sdk.objects.photos.PhotoSizes;
import com.vk.api.sdk.objects.wall.CommentAttachment;
import com.vk.api.sdk.objects.wall.CommentAttachmentType;
import com.vk.api.sdk.objects.wall.WallComment;
import com.vk.api.sdk.objects.wall.responses.GetCommentsResponse;
import com.vk.api.sdk.queries.wall.WallGetCommentsQuery;
import main.dto.Comment;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.vk.api.sdk.objects.wall.CommentAttachmentType.PHOTO;
import static com.vk.api.sdk.objects.wall.CommentAttachmentType.STICKER;

public class VkClientUtils {
	private static final Function<WallComment, Comment> convert = wallComment -> {
		List<CommentAttachment> attachments = wallComment.getAttachments();
		Comment resultComment = new Comment(wallComment.getId(), wallComment.getText(), wallComment.getFromId());
		if (attachments == null || attachments.isEmpty()) {
			return resultComment;
		}

		List<CommentAttachmentType> types = getTypes(attachments);
		if (types.contains(PHOTO)) {
			if (isContainsNotOnlyPhoto(types)) {
				resultComment.setText(resultComment.getText() + "\n\nсодержит необрабатываемые вложения");
			}
			resultComment.addFilesUrls(getLargestPhotosUrls(attachments));
		} else if (types.get(0).equals(STICKER)) {
			resultComment.setText("комментарий содержит стикер");
			resultComment.addFileUrl(getStickerUrl(attachments));
		} else {
			resultComment.setText(resultComment.getText() + "\n\nсодержит необрабатываемые вложения");
		}
		return resultComment;
	};

	private static boolean isContainsNotOnlyPhoto(List<CommentAttachmentType> attachmentTypes) {
		return !attachmentTypes.stream()
				.allMatch(commentAttachmentType -> commentAttachmentType.equals(PHOTO));
	}

	private static List<CommentAttachmentType> getTypes(List<CommentAttachment> attachments) {
		return attachments.stream()
				.map(CommentAttachment::getType)
				.distinct()
				.toList();
	}

	private static List<String> getLargestPhotosUrls(List<CommentAttachment> attachments) {
		List<String> result = new LinkedList<>();
		List<CommentAttachment> photoAttachments = attachments.stream().filter(attachment -> attachment.getType().equals(PHOTO)).toList();
		for (CommentAttachment attachment : photoAttachments) {
			List<PhotoSizes> sizes = attachment.getPhoto().getSizes();
			PhotoSizes photo = sizes.stream().max(Comparator.comparingInt(PhotoSizes::getHeight)).orElse(sizes.get(0));
			result.add(photo.getUrl().toString());
		}
		return result;
	}

	private static String getStickerUrl(List<CommentAttachment> attachments) {
		int imageSideLength = 128;
		List<Image> images = attachments.get(0)
				.getSticker().getImages();
		Image result = images.stream()
				.filter(image -> image.getWidth() == imageSideLength)
				.findFirst()
				.orElse(images.get(0));
		return result.getUrl().toString();
	}

	static Set<Comment> convertComments(Set<WallComment> comments) {
		return comments.stream().map(convert).collect(Collectors.toSet());
	}

	static Set<WallComment> fetchCommentsWithOffset(VkApiWrapper vkApi,
													Supplier<WallGetCommentsQuery> querySupp,
													int offset) throws ClientException, ApiException {
		GetCommentsResponse response = vkApi.call(querySupp.get().offset(offset));
		return new HashSet<>(response.getItems());
	}

	static List<WallComment> extractThread(VkApiWrapper vkApi, Set<WallComment> comments, Supplier<WallGetCommentsQuery> query)
			throws ClientException, ApiException {
		List<WallComment> result = new LinkedList<>();

		for (WallComment comment : comments) {
			if (comment.getThread().getCount() == 0) {
				continue;
			}

			GetCommentsResponse response = vkApi.call(query.get()
					.commentId(comment.getId()));
			List<WallComment> thread = response.getItems();

			while (thread.size() < response.getCount()) {
				response = vkApi.call(query.get()
						.offset(thread.size())
						.commentId(comment.getId()));
				thread.addAll(response.getItems());
			}
			result.addAll(thread);
		}

		return result;
	}
}
