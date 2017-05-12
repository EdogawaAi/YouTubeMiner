package data_crawler_test1;

import java.io.IOException;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.SearchListResponse;

public class VideoListGenerator {

	private int maximumResult = 5;

	/*
	 * order value can be: 1. date 2. rating 3. relevance (by default) 4. title
	 * (alphabetically) 5. videoCount (DON'T use because it's related to
	 * setType(channel)) 6. viewCount
	 * 
	 */

	public String videoList(String order, YouTube youtube, String apiKey) throws IOException {

		YouTube.Search.List videoList = youtube.search().list("id").setKey(apiKey).setFields("items/id/videoId")
				.setOrder(order).setType("video").setMaxResults((long) maximumResult);

		SearchListResponse videoListResponse = videoList.execute();

		return videoListResponse.toString();
	}

}
