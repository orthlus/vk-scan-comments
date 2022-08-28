package main.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.LinkedList;
import java.util.List;

@Getter
@Setter
public class Comment {
	private int id;
	private String text;
	private Integer fromId;
	private String name;
	private List<String> filesUrls;

	public void addFilesUrls(List<String> urls) {
		if (filesUrls == null)
			filesUrls = new LinkedList<>();
		filesUrls.addAll(urls);
	}

	public void addFileUrl(String url) {
		if (filesUrls == null)
			filesUrls = new LinkedList<>();
		filesUrls.add(url);
	}

	public Comment(int id, String text, Integer fromId) {
		this.id = id;
		this.text = text;
		this.fromId = fromId;
	}

	public Comment(String text, Integer fromId) {
		this.text = text;
		this.fromId = fromId;
	}
}
