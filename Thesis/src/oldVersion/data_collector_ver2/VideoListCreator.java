package data_collector_ver2;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.SearchListResponse;

public class VideoListCreator {

	private YouTube youtube;
	private String apiKey;
	private int maximumResult = 5; // integer, 0-50.
	private int totalPage = 2;
	private StringBuilder videoListBuilder = new StringBuilder();
	private HashSet<String> videoListSet = new HashSet<String>();

	public VideoListCreator(YouTube youtube, String apiKey) {
		this.youtube = youtube;
		this.apiKey = apiKey;
	}

	/**
	 * 
	 * @param order:
	 *            value can be: 1. date 2. rating 3. relevance (by default) 4.
	 *            title (alphabetically) 5. videoCount (DON'T use because it's
	 *            related to setType(channel)) 6. viewCount
	 * @param youtube
	 * @param apiKey
	 * @return
	 * @throws IOException
	 */
	public String videoList(String order) throws IOException {

		YouTube.Search.List videoList = youtube.search().list("id").setKey(apiKey)
				.setFields("items/id/videoId,nextPageToken,pageInfo").setOrder(order).setType("video")
				.setMaxResults((long) maximumResult);

		SearchListResponse videoListResponse = videoList.execute();

		// First page:
		totalPage--;

		JSONObject videoListJSONObject = new JSONObject(videoListResponse.toString());
		JSONArray videoListItems = videoListJSONObject.getJSONArray("items");
		for (int i = 0; i < videoListItems.length(); i++) {
			JSONObject childJSONObj = videoListItems.getJSONObject(i);
			String videoId = childJSONObj.getJSONObject("id").getString("videoId");
			videoListSet.add(videoId);
		}

		// The rest pages:
		while ((totalPage > 0) && (videoListResponse.getNextPageToken() != null)) {
			videoList.setPageToken(videoListResponse.getNextPageToken());
			videoListResponse = videoList.execute();

			videoListJSONObject = new JSONObject(videoListResponse.toString());
			videoListItems = videoListJSONObject.getJSONArray("items");
			for (int i = 0; i < videoListItems.length(); i++) {
				JSONObject childJSONObj = videoListItems.getJSONObject(i);
				String videoId = childJSONObj.getJSONObject("id").getString("videoId");
				videoListSet.add(videoId);
			}
			totalPage--;
		}

		Iterator<String> videoListIterator = videoListSet.iterator();
		while (videoListIterator.hasNext()) {
			videoListBuilder.append(videoListIterator.next() + ",");
		}

		String videoListResult = videoListBuilder.toString().substring(0, videoListBuilder.toString().length() - 1);
		return videoListResult;
	}
}
