package main.shedule;

import lombok.extern.slf4j.Slf4j;
import main.CommentsSourceService;
import main.TelegramMessageSender;
import main.dto.Comment;
import main.dto.CommentsSource;
import main.dto.TelegramMessage;
import main.exceptions.VkPostWasDeletedException;
import main.vk.VkClient;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@DisallowConcurrentExecution
@Component
@Slf4j
public class SendNewCommentsJob implements Job {
	@Autowired
	private TelegramMessageSender telegram;
	@Autowired
	private CommentsSourceService commentsSourceService;
	@Autowired
	private VkClient vkClient;

	@Override
	public void execute(JobExecutionContext context) {
		log.info("SendNewCommentsJob start");
		List<TelegramMessage> telegramMessages = new LinkedList<>();

		for (CommentsSource group : commentsSourceService.getGroups()) {
			Set<Comment> allNewComments = new HashSet<>();
			for (Integer post : group.getPosts()) {
				telegramMessages.addAll(getTelegramMessagesFromPost(post, group, allNewComments));
			}
			Set<Integer> newPosts = vkClient.getNewPostsIdsByGroupId(group.getGroupId(), group.getLastCommentId());
			for (Integer post : newPosts) {
				telegramMessages.addAll(getTelegramMessagesFromPost(post, group, allNewComments));
			}
			Set<Integer> posts = group.getPosts();
			posts.addAll(newPosts);
			group.setPosts(posts.stream()
					.sorted((o1, o2) -> Integer.compare(o2, o1))
					.limit(CommentsSourceService.FETCH_POSTS_COUNT)
					.collect(Collectors.toSet()));
			group.setLastCommentId(Math.max(
					group.getLastCommentId(),
					allNewComments.stream()
							.map(Comment::getId)
							.mapToInt(Integer::intValue)
							.max()
							.orElse(group.getLastCommentId())
			));
		}
		for (TelegramMessage telegramMessage : telegramMessages) {
			telegram.sendChannelMessage(telegramMessage);
		}
		for (CommentsSource group : commentsSourceService.getGroups()) {
			group.setLastCommentId(Math.max(
					group.getLastCommentId(),
					group.getPosts().stream()
							.mapToInt(Integer::intValue)
							.max()
							.orElse(group.getLastCommentId())
			));
		}

		log.info("SendNewCommentsJob finish");
	}

	private List<TelegramMessage> getTelegramMessagesFromPost(int postId, CommentsSource group, Set<Comment> allNewComments) {
		List<TelegramMessage> result = new LinkedList<>();
		Set<Comment> newComments = null;
		try {
			newComments = vkClient.getNewCommentsByPostId(group.getGroupId(), postId, group.getLastCommentId());
		} catch (VkPostWasDeletedException e) {
			group.getPosts().remove(postId);
		}
		newComments = vkClient.fillUsersNames(newComments);
		for (Comment comment : newComments) {
			result.add(makeTelegramMessage(comment, postId, group));
		}
		allNewComments.addAll(newComments);
		return result;
	}

	private TelegramMessage makeTelegramMessage(Comment comment, int postId, CommentsSource group) {
		String postUrl = "https://vk.com/wall%d_%d"
				.formatted(group.getGroupId(), postId);
		String commentUrl = "https://vk.com/wall%d_%d?reply=%d"
				.formatted(group.getGroupId(), postId, comment.getId());
		String message = """
				Группа: %s
				Запись: %s
				Комментарий: %s
				Автор: %s
				
				%s
				""".formatted(group.getGroupName(), postUrl, commentUrl, comment.getName(), comment.getText());
		if (comment.getFilesUrls() != null)
			return new TelegramMessage(message, comment.getFilesUrls());
		return new TelegramMessage(message);
	}
}
