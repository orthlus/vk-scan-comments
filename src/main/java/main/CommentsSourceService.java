package main;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import main.dto.CommentsSource;
import main.vk.VkClient;
import org.jooq.DSLContext;
import org.jooq.Record5;
import org.jooq.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;

import static main.Tables.GROUPS;
import static main.Tables.GROUPS_SCAN_INFO;

@Component
@Slf4j
public class CommentsSourceService {
	public static final int FETCH_POSTS_COUNT = 1000;
	@Getter
	private List<CommentsSource> groups;

	@Autowired
	private VkClient vkClient;
	@Autowired
	private Db db;

	@PostConstruct
	private void init() {
		log.info("Start initialisation");
		List<Integer> groupsIds = db.getGroups();
		groups = new LinkedList<>();
		for (int groupId : groupsIds) {
			CommentsSource source = new CommentsSource(groupId);
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

	public List<CommentsSource> getGroups() {
		Result<Record5<Integer, String, Integer, Integer, Integer[]>> dbGroups = db
				.select(GROUPS.ID, GROUPS.NAME,
						GROUPS_SCAN_INFO.GROUP_ID,
						GROUPS_SCAN_INFO.LAST_COMMENT_ID,
						GROUPS_SCAN_INFO.POSTS)
				.from(GROUPS)
				.join(GROUPS_SCAN_INFO)
				.on(GROUPS.ID.eq(GROUPS_SCAN_INFO.GROUP_ID))
				.where(GROUPS.ACTIVE.eq(true))
				.fetch();
		return dbGroups.stream()
				.map(group -> new CommentsSource(
						group.get(GROUPS.ID),
						group.get(GROUPS.NAME),
						new HashSet<>(Arrays.asList(group.get(GROUPS_SCAN_INFO.POSTS))),
						group.get(GROUPS_SCAN_INFO.LAST_COMMENT_ID)
				))
				.toList();
	}
}
