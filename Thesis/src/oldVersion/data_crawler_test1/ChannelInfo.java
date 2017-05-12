package data_crawler_test1;

import java.io.IOException;

import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.ChannelListResponse;

public class ChannelInfo {

	public String channelInfo(String channelId, YouTube youtube, String apiKey) throws IOException {

		YouTube.Channels.List channels = youtube.channels().list("id,snippet,statistics").setKey(apiKey)
				.setId(channelId).setFields("items(id,snippet(country,description,publishedAt,title),statistics)");
		ChannelListResponse channelListResponse = channels.execute();

		return channelListResponse.toString();
	}

}
