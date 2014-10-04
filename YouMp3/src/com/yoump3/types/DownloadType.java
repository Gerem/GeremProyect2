package com.yoump3.types;

public enum DownloadType {
	VIDEO("Video"),
	MP3("Mp3");
	
	private final String name;
	
	DownloadType(String name){
		this.name = name;
	}

	public String getName() {
		return name;
	}
}
