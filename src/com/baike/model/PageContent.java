package com.baike.model;

public class PageContent {
	private String content;
	private String Id;
	private boolean isSub = false;
	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content;
	}
	public PageContent(){}
	public PageContent(String content,String Id, boolean isSub){
		this.content = content;
		this.isSub = isSub;
		this.Id = Id;
	}
	public boolean isSub() {
		return isSub;
	}
	public String getId() {
		return Id;
	}

}
