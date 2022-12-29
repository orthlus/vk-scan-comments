package main.vk;

import com.google.common.collect.Iterables;
import com.vk.api.sdk.client.Lang;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.ServiceActor;
import com.vk.api.sdk.exceptions.ApiAccessException;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import com.vk.api.sdk.objects.enums.WallSort;
import com.vk.api.sdk.objects.groups.responses.GetByIdLegacyResponse;
import com.vk.api.sdk.objects.wall.WallComment;
import com.vk.api.sdk.objects.wall.Wallpost;
import com.vk.api.sdk.objects.wall.WallpostFull;
import com.vk.api.sdk.objects.wall.responses.GetResponse;
import com.vk.api.sdk.queries.wall.WallGetCommentsQuery;
import com.vk.api.sdk.queries.wall.WallGetQuery;
import lombok.extern.slf4j.Slf4j;
import main.dto.Comment;
import main.exceptions.VkPostWasDeletedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static main.vk.VkClientUtils.*;

@Component
@Slf4j
public class VkClient {
	@Value("${vk.app_ids}")
	private String vkAppIds;
	@Value("${vk.secret_keys}")
	private String vkTokens;

	private Iterator<ServiceActor> keys;

	@PostConstruct
	private void init() {
		List<ServiceActor> listKeys = new LinkedList<>();
		String[] ids = vkAppIds.split(",");
		String[] tokens = vkTokens.split(",");
		for (int i = 0; i < Math.min(ids.length, tokens.length); i++) {
			listKeys.add(new ServiceActor(Integer.parseInt(ids[i]), tokens[i]));
		}
		keys = Iterables.cycle(listKeys).iterator();
	}

	@Autowired
	private VkApiWrapper vkApi;

	private final VkApiClient vk = new VkApiClient(HttpTransportClient.getInstance());

	public String getGroupName(String groupId) {
		log.debug("start getGroupName with group {}", groupId);
		try {
			String id = String.valueOf(Math.abs(Integer.parseInt(groupId)));
			List<GetByIdLegacyResponse> response = vkApi.call(vk.groups()
					.getByIdLegacy(keys.next())
					.groupId(id));
			String name = response.get(0).getName();
			log.info("finish getGroupName with group {}, name {}", groupId, name);
			return name;
		} catch (ApiException | ClientException e) {
			log.error("Error getGroupName", e);
		}
		return "";
	}

	public Set<Comment> fillUsersNames(Set<Comment> comments) {
		if (comments == null || comments.isEmpty()) return comments;
		log.debug("start fillUsersNames with {} comments", comments.size());
		try {
			List<Integer> userIds = new ArrayList<>(comments.size());
			List<Integer> groupIds = new LinkedList<>();
			for (Integer fromId : comments.stream().map(Comment::getFromId).collect(Collectors.toSet())) {
				if (fromId > 0)
					userIds.add(fromId);
				else
					groupIds.add(Math.abs(fromId));
			}

			List<com.vk.api.sdk.objects.users.responses.GetResponse> users = new LinkedList<>();
			if (userIds.size() > 0) {
				List<String> userIdsStrings = ints2Strings(userIds);
				if (userIdsStrings.size() < 1000) {
					String join = String.join(",", userIdsStrings);
					users.addAll(vkApi.call(vk.users()
							.get(keys.next())
							.lang(Lang.RU)
							.userIds(join)));
				} else {
					for (int step = 0; step < userIdsStrings.size(); step += 1000) {
						String join = String.join(",", userIdsStrings.subList(step, step + 1000));
						users.addAll(vkApi.call(vk.users()
								.get(keys.next())
								.lang(Lang.RU)
								.userIds(join)));
					}
				}
			}
			log.debug("during fillUsersNames got {} users", users.size());

			List<GetByIdLegacyResponse> groups = new LinkedList<>();
			if (groupIds.size() > 0) {
				List<String> groupIdsStrings = ints2Strings(groupIds);
				if (groupIdsStrings.size() < 500) {
					String join = String.join(",", groupIdsStrings);
					groups.addAll(vkApi.call(vk.groups()
							.getByIdLegacy(keys.next())
							.lang(Lang.RU)
							.groupIds(join)));
				} else {
					for (int step = 0; step < groupIdsStrings.size(); step += 500) {
						String join = String.join(",", groupIdsStrings.subList(step, step + 500));
						groups.addAll(vkApi.call(vk.groups()
								.getByIdLegacy(keys.next())
								.lang(Lang.RU)
								.groupIds(join)));
					}
				}
			}
			log.debug("during fillUsersNames got {} groups", groups.size());

			for (GetByIdLegacyResponse group : groups) {
				comments.stream()
						.filter(comment -> comment.getFromId() != null)
						.filter(comment -> comment.getFromId().equals(group.getId()))
						.forEach(comment -> {
							comment.setName(group.getName());
							comment.setFromId(null);
						});
			}
			for (var user : users) {
				comments.stream()
						.filter(comment -> comment.getFromId() != null)
						.filter(comment -> comment.getFromId().equals(user.getId()))
						.forEach(comment -> {
							comment.setName(user.getFirstName() + " " + user.getLastName());
							comment.setFromId(null);
						});
			}

			String collect = comments.stream()
					.filter(comment -> comment.getFromId() == null)
					.map(Comment::getId)
					.map(String::valueOf)
					.collect(Collectors.joining(","));
			log.debug("comments with null from_id: {}", collect);
		} catch (ApiException | ClientException e) {
			log.error("Error fillUsersNames", e);
		}
		return comments;
	}

	public Set<Comment> getNewCommentsByPostId(int groupId, int wallPostId, int lastCommentId) throws VkPostWasDeletedException {
		log.debug("start getNewCommentsByPostId, group {} post {} last post {}", groupId, wallPostId, lastCommentId);
		try {
			Supplier<WallGetCommentsQuery> querySupp = () -> vk.wall()
					.getComments(keys.next())
					.ownerId(groupId)
					.postId(wallPostId)
					.count(100)
					.sort(WallSort.REVERSE_CHRONOLOGICAL)
					.needLikes(false)
					.previewLength(0);

			Set<WallComment> result = fetchCommentsByPostId(querySupp);

			Set<WallComment> filteredResult = result.stream()
					.filter(comment -> comment.getId() > lastCommentId)
					.collect(Collectors.toSet());
			Set<Comment> r = convertComments(filteredResult);
			log.debug("finish getNewCommentsByPostId, group {}, result {} comments", groupId, filteredResult.size());
			return r;
		} catch (VkPostWasDeletedException e) {
			log.error("Error getNewCommentsByPostId - post was deleted, group id {}, post {}", groupId, wallPostId);
			throw new VkPostWasDeletedException();
		} catch (ApiException | ClientException e) {
			log.error("Error getNewCommentsByPostId, group id {}, post {}", groupId, wallPostId, e);
			return Set.of();
		}
	}

	public int getMaxCommentIdByPostId(int groupId, int wallPostId) {
		log.debug("start getMaxCommentIdByPostId, group {} post {}", groupId, wallPostId);
		try {
			Supplier<WallGetCommentsQuery> querySupp = () -> vk.wall()
					.getComments(keys.next())
					.ownerId(groupId)
					.postId(wallPostId)
					.count(100)
					.sort(WallSort.CHRONOLOGICAL)
					.needLikes(false)
					.previewLength(0);

			Set<WallComment> result = fetchCommentsByPostId(querySupp);

			int s = result.stream().mapToInt(WallComment::getId).max().orElse(1);
			log.debug("finish getMaxCommentIdByPostId, group {}, result {}", groupId, s);
			return s;
		} catch (VkPostWasDeletedException ignored) {
			log.error("Error getMaxCommentIdByPostId - post was deleted, group id {}, post {}", groupId, wallPostId);
		} catch (ApiException | ClientException e) {
			log.error("Error getMaxCommentIdByPostId, group id {}, post {}", groupId, wallPostId, e);
		}
		return 1;
	}

	private Set<WallComment> fetchCommentsByPostId(Supplier<WallGetCommentsQuery> querySupp)
			throws ClientException, ApiException, VkPostWasDeletedException {
		Set<WallComment> result = null;
		try {
			result = fetchCommentsWithOffset(vkApi, querySupp, 0);
			int upperCommentsCount = vkApi.call(querySupp.get().count(1)).getCurrentLevelCount();

			while (!result.isEmpty() && result.size() < upperCommentsCount) {
				result.addAll(fetchCommentsWithOffset(vkApi, querySupp, result.size()));
			}
			result.addAll(extractThread(vkApi, result, querySupp));
		} catch (ApiAccessException e) {
			if (e.getMessage().contains("post was deleted")) {
				log.error("Error fetchCommentsByPostId - post was deleted");
				throw new VkPostWasDeletedException();
			}
		}
		return result;
	}

	@Deprecated
	public Set<Comment> getAllCommentsByPostId(int groupId, int wallPostId) {
		log.info("start getAllCommentsByPostId, group {} post {}", groupId, wallPostId);
		try {
			Supplier<WallGetCommentsQuery> querySupp = () -> vk.wall()
					.getComments(keys.next())
					.ownerId(groupId)
					.postId(wallPostId)
					.count(100)
					.sort(WallSort.REVERSE_CHRONOLOGICAL)
					.needLikes(false)
					.previewLength(0);

			int countComments = vkApi.call(querySupp.get().count(1)).getCount();
			Set<Comment> result = convertComments(fetchCommentsWithOffset(vkApi, querySupp, 0));

			while (result.size() < countComments) {
				result.addAll(convertComments(fetchCommentsWithOffset(vkApi, querySupp, result.size())));
			}

			log.info("finish getAllCommentsByPostId, group {}, result {} comments", groupId, result.size());
			return result;
		} catch (ApiException | ClientException e) {
			log.error("Error getAllCommentsByPostId, group id {}, post {}", groupId, wallPostId, e);
			return Set.of();
		}
	}

	public Set<Integer> getNewPostsIdsByGroupId(int groupId, int lastPostId) {
		log.debug("start getNewPostsIdsByGroupId, group {}, last post {}", groupId, lastPostId);
		try {
			Supplier<WallGetQuery> query = () -> vk.wall()
					.get(keys.next())
					.ownerId(groupId)
					.count(100);
			GetResponse response = vkApi.call(query.get());
			Set<Integer> ids = response.getItems()
					.stream()
					.map(WallpostFull::getId)
					.collect(Collectors.toSet());
			while (!ids.isEmpty() && ids.stream().noneMatch(postId -> postId <= lastPostId)) {
				response = vkApi.call(query.get().offset(ids.size()));
				ids.addAll(
						response.getItems()
								.stream()
								.map(WallpostFull::getId)
								.toList()
				);
			}
			Set<Integer> result = ids.stream()
					.filter(postId -> postId > lastPostId)
					.collect(Collectors.toSet());
			log.debug("finish getNewPostsIdsByGroupId, group {}, result size {}", groupId, result.size());
			return result;
		} catch (ApiException | ClientException e) {
			log.error("Error getNewPostsIdsByGroupId, group id {}", groupId, e);
			return Set.of();
		}
	}

	public Set<Integer> getNLastPostsIdsByGroupId(int groupId, int countPosts) {
		log.info("start getNLastPostsByGroupId, group {}, {} posts", groupId, countPosts);
		try {
			Supplier<WallGetQuery> querySupp = () -> vk.wall()
					.get(keys.next())
					.ownerId(groupId)
					.count(100);
			GetResponse response = vkApi.call(querySupp.get());
			Set<Integer> ids = response.getItems()
					.stream()
					.map(Wallpost::getId)
					.collect(Collectors.toSet());

			if (countPosts <= 100) return ids.stream()
					.sorted((o1, o2) -> Integer.compare(o2, o1))
					.limit(countPosts)
					.collect(Collectors.toSet());

			while (ids.size() < response.getCount()) {
				response = vkApi.call(querySupp.get().offset(ids.size()));
				ids.addAll(
						response.getItems()
								.stream()
								.map(Wallpost::getId)
								.toList()
				);
				if (ids.size() >= countPosts) break;
			}

			log.info("finish getNLastPostsByGroupId, group {}, result size {}", groupId, ids.size());
			return ids;
		} catch (ApiException | ClientException e) {
			log.error("Error getNLastPostsByGroupId, group id {}", groupId, e);
			return Set.of();
		}
	}
}
