package data_crawler_test1;

import java.io.IOException;

import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.VideoCategoryListResponse;

public class VideoCategoryInfo {

	public String videoCategory(String categoryId, YouTube youtube, String apiKey) throws IOException {
		YouTube.VideoCategories.List videoCategories = youtube.videoCategories().list("snippet").setKey(apiKey)
				.setId(categoryId).setFields("items(id,snippet/title)");
		VideoCategoryListResponse videoCategoryListResponse = videoCategories.execute();

		return videoCategoryListResponse.toString();
	}

}
