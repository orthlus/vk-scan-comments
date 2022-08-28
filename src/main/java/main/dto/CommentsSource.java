package main.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.Set;

@RequiredArgsConstructor
@Setter
@Getter
public class CommentsSource {
	private final int groupId;
	private String groupName;
	private Set<Integer> posts;
	private int lastCommentId;
}
