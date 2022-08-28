package main;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import main.dto.CommentsSource;
import main.vk.VkClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

@Component
@Slf4j
public class CommentsSourceService {
	public static final int FETCH_POSTS_COUNT = 1000;
	@Value("${VK_GROUPS_IDS}")
	private String groupsIds;
	@Getter
	private List<CommentsSource> groups;

	@Autowired
	private VkClient vkClient;

	@PostConstruct
	private void init() {
		log.info("Start initialisation");
		groups = new LinkedList<>();
		for (String groupId : groupsIds.split(",")) {
			CommentsSource source = new CommentsSource(Integer.parseInt(groupId));
			source.setGroupName(vkClient.getGroupName(groupId));
			groups.add(source);
		}
		for (CommentsSource group : groups) {
			Set<Integer> posts = vkClient.getNLastPostsIdsByGroupId(group.getGroupId(), FETCH_POSTS_COUNT);
			group.setPosts(posts);
			group.setLastCommentId(posts.stream().mapToInt(Integer::intValue).max().orElse(1));
		}
		for (CommentsSource group : groups) {
			for (Integer postId : group.getPosts()) {
				group.setLastCommentId(Math.max(
						group.getLastCommentId(),
						vkClient.getMaxCommentIdByPostId(group.getGroupId(), postId)
				));
			}
			log.info("Got last post/comment id {} to group {}", group.getLastCommentId(), group.getGroupId());
		}
		log.info("Finish initialisation");
	}
}
