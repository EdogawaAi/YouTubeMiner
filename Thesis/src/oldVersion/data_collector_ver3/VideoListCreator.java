package data_collector_ver3;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Iterator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;

public class VideoListCreator {

	private YouTube youtube;
	private String apiKey;
	private int totalPage = 1;
	private LinkedHashSet<String> videoListSet = new LinkedHashSet<String>();

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
	public LinkedHashSet<String> videoIdSet(String order, long maximumResult) throws IOException {

		YouTube.Search.List videoList = youtube.search().list("id").setKey(apiKey)
				.setFields("items/id/videoId,nextPageToken,pageInfo").setOrder(order).setType("video")
				.setMaxResults(maximumResult);

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

		return videoListSet;
	}

	public LinkedHashSet<String> videoIdExpandedSet(LinkedHashSet<String> originVideoId, long maximumResult)
			throws IOException {
		LinkedHashSet<String> expandedSet = new LinkedHashSet<String>();

		YouTube.Search.List videoList = youtube.search().list("id").setKey(apiKey).setFields("items/id/videoId")
				.setType("video").setMaxResults(maximumResult);

		Iterator<String> originIdIter = originVideoId.iterator();
		while (originIdIter.hasNext()) {
			videoList.setRelatedToVideoId(originIdIter.next());
			SearchListResponse videoListResponse = videoList.execute();

			List<SearchResult> videoResultList = videoListResponse.getItems();
			Iterator<SearchResult> videoResultIter = videoResultList.iterator();
			while (videoResultIter.hasNext()) {
				expandedSet.add(videoResultIter.next().getId().getVideoId());
			}
		}

		return expandedSet;
	}

}
