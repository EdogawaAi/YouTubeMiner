package data_crawler_test1;

import java.io.IOException;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.VideoListResponse;

public class VideoInfo {

	public String videoInfo(String videoId, YouTube youtube, String apiKey) throws IOException {

		YouTube.Videos.List videoList = youtube.videos().list("snippet,contentDetails,statistics").setKey(apiKey)
				.setFields(
						"items(contentDetails/duration,snippet(categoryId,channelId,description,publishedAt,title),statistics)")
				.setId(videoId);
		VideoListResponse videoListResponse = videoList.execute();

		return videoListResponse.toString();
	}
}
