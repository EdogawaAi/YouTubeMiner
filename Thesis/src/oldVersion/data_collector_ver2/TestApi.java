package data_collector_ver2;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Channel;
import com.google.api.services.youtube.model.ChannelListResponse;
import com.google.api.services.youtube.model.CommentThreadListResponse;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoCategory;
import com.google.api.services.youtube.model.VideoCategoryListResponse;
import com.google.api.services.youtube.model.VideoListResponse;

public class TestApi {
	private YouTube youtube;
	private String apiKey;

	// Help passing channel id between methods.
	private HashSet<String> channelIdSet = new HashSet<String>();
	private StringBuilder channelIdBuilder = new StringBuilder();
	// Help passing category id between methods.
	private HashSet<String> categoryIdSet = new HashSet<String>();
	private StringBuilder categoryIdBuilder = new StringBuilder();

	public TestApi(YouTube youtube, String apiKey) {
		this.youtube = youtube;
		this.apiKey = apiKey;
	}

	// YouTube data API only accept less than 50 length's CSV string, so a
	// splitter is needed.
	private ArrayList<String> csvSplitter(String csvString) {
		ArrayList<String> splittedCSVString = new ArrayList<String>();
		int numberPerChunk = 5;
		String str = new String();
		int position = ordinalIndexOf(csvString, ",", numberPerChunk - 1);
		while (position != -1) {
			str = csvString.substring(0, position);
			csvString = csvString.substring(position + 1);
			splittedCSVString.add(str);
			position = ordinalIndexOf(csvString, ",", numberPerChunk - 1);
		}
		splittedCSVString.add(csvString);
		return splittedCSVString;
	}

	private int ordinalIndexOf(String string, String subString, int index) {
		int position = string.indexOf(subString, 0);
		while (index-- > 0 && position != -1) {
			position = string.indexOf(subString, position + 1);
		}
		return position;
	}

	/**
	 * 
	 * @param videoIdListCSV
	 * @return videoTableList
	 * @throws IOException
	 */
	public ArrayList<JSONObject> videoTableList(String videoIdListCSV) throws IOException {
		ArrayList<JSONObject> videoTableList = new ArrayList<JSONObject>();

		YouTube.Videos.List videoList = youtube.videos().list("id,snippet,contentDetails").setKey(apiKey).setFields(
				"items(id,contentDetails/duration,snippet(categoryId,channelId,description,publishedAt,title))");

		ArrayList<String> splittedVideoIdListCSV = csvSplitter(videoIdListCSV);
		Iterator<String> videoIdIterator = splittedVideoIdListCSV.iterator();

		while (videoIdIterator.hasNext()) {

			videoList.setId(videoIdIterator.next());
			VideoListResponse videoListResponse = videoList.execute();

			java.util.List<Video> videos = videoListResponse.getItems();
			Iterator<Video> videoIterator = videos.iterator();

			while (videoIterator.hasNext()) {
				Video video = videoIterator.next();
				JSONObject videoInfoTable = new JSONObject().put("VideoId", video.getId())
						.put("CategoryId", video.getSnippet().getCategoryId())
						.put("ChannelId", video.getSnippet().getChannelId())
						.put("VideoPublishedAt", video.getSnippet().getPublishedAt().toString())
						.put("Duration", video.getContentDetails().getDuration())
						.put("VideoTitle", video.getSnippet().getTitle())
						.put("VideoDescription", video.getSnippet().getDescription());
				videoTableList.add(videoInfoTable);

				channelIdSet.add(video.getSnippet().getChannelId());
				categoryIdSet.add(video.getSnippet().getCategoryId());
			}
		}
		return videoTableList;
	}

	public ArrayList<JSONObject> videoStatisticTableList(String videoIdListCSV) throws IOException {

		ArrayList<JSONObject> videoStatisticTableList = new ArrayList<JSONObject>();

		YouTube.Videos.List videoList = youtube.videos().list("id,statistics").setKey(apiKey)
				.setFields("items(id,statistics)");

		ArrayList<String> splittedVideoIdListCSV = csvSplitter(videoIdListCSV);
		Iterator<String> videoIdIterator = splittedVideoIdListCSV.iterator();

		while (videoIdIterator.hasNext()) {

			videoList.setId(videoIdIterator.next());
			VideoListResponse videoListResponse = videoList.execute();

			java.util.List<Video> videos = videoListResponse.getItems();
			Iterator<Video> videoIterator = videos.iterator();

			while (videoIterator.hasNext()) {
				Video video = videoIterator.next();
				JSONObject videoStatisticTable = new JSONObject().put("VideoId", video.getId())
						.put("VideoTimeStamp", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()))
						.put("VideoCommentsCount", video.getStatistics().getCommentCount())
						.put("VideoDislikeCount", video.getStatistics().getDislikeCount())
						.put("VideoLikeCount", video.getStatistics().getLikeCount())
						.put("VideoFavoriteCount", video.getStatistics().getFavoriteCount())
						.put("VideoViewCount", video.getStatistics().getViewCount());

				videoStatisticTableList.add(videoStatisticTable);
			}
		}

		return videoStatisticTableList;
	}

	public ArrayList<JSONObject> videoCategoryTableList() throws IOException {

		ArrayList<JSONObject> videoCategoryTableList = new ArrayList<JSONObject>();

		YouTube.VideoCategories.List videoCategories = youtube.videoCategories().list("snippet").setKey(apiKey)
				.setFields("items(id,snippet/title)");

		Iterator<String> categoryIdSetIterator = categoryIdSet.iterator();
		while (categoryIdSetIterator.hasNext()) {
			categoryIdBuilder.append(categoryIdSetIterator.next() + ",");
		}
		String categoryIdCSV = categoryIdBuilder.toString().substring(0, categoryIdBuilder.toString().length() - 1);

		ArrayList<String> splittedCategoryIdCSV = csvSplitter(categoryIdCSV);
		Iterator<String> categoryIdIterator = splittedCategoryIdCSV.iterator();

		while (categoryIdIterator.hasNext()) {
			videoCategories.setId(categoryIdIterator.next());
			VideoCategoryListResponse videoCategoryListResponse = videoCategories.execute();

			List<VideoCategory> videoCategoryList = videoCategoryListResponse.getItems();
			Iterator<VideoCategory> videoCategoryIterator = videoCategoryList.iterator();
			while (videoCategoryIterator.hasNext()) {
				VideoCategory videoCategory = videoCategoryIterator.next();
				JSONObject videoCategoryTable = new JSONObject().put("CategoryId", videoCategory.getId())
						.put("CategoryTitle", videoCategory.getSnippet().getTitle());
				videoCategoryTableList.add(videoCategoryTable);
			}
		}

		return videoCategoryTableList;
	}

	public ArrayList<JSONObject>[] videoCommentTableList(String videoIdListCSV) throws IOException {
		String[] videoIdList = videoIdListCSV.split(",");
		// 0: top level comment; 1: reply.
		@SuppressWarnings("unchecked")
		ArrayList<JSONObject>[] videoCommentTableList = (ArrayList<JSONObject>[]) new ArrayList[2];
		for (int i = 0; i < videoCommentTableList.length; i++) {
			videoCommentTableList[i] = new ArrayList<JSONObject>();
		}
		for (String videoId : videoIdList) {
			try {
				YouTube.CommentThreads.List videoCommentsList = youtube.commentThreads().list("snippet,replies")
						.setKey(apiKey).setVideoId(videoId).setTextFormat("plainText").setMaxResults((long) 100)
						.setFields(
								"items(replies(comments(id,snippet(authorChannelId,likeCount,parentId,publishedAt,textDisplay,updatedAt))),"
										+ "snippet(topLevelComment(id,snippet(authorChannelId,likeCount,publishedAt,textDisplay,updatedAt)),"
										+ "totalReplyCount,videoId)),nextPageToken");
				CommentThreadListResponse videoCommentsListResponse = videoCommentsList.execute();

				// first page comments.
				JSONArray videoCommentsListJSONArray = new JSONObject(videoCommentsListResponse.toString())
						.getJSONArray("items");
				// append the remaining pages.
				while (videoCommentsListResponse.getNextPageToken() != null) {
					videoCommentsList.setPageToken(videoCommentsListResponse.getNextPageToken());
					videoCommentsListResponse = videoCommentsList.execute();

					JSONArray newCommentsListJSONArray = new JSONObject(videoCommentsListResponse.toString())
							.getJSONArray("items");

					for (int i = 0; i < newCommentsListJSONArray.length(); i++) {
						videoCommentsListJSONArray.put(newCommentsListJSONArray.getJSONObject(i));
					}
				}

				for (int i = 0; i < videoCommentsListJSONArray.length(); i++) {
					JSONObject oneVideoCommentObject = videoCommentsListJSONArray.getJSONObject(i);

					// First, store the top level comments.
					if (oneVideoCommentObject.has("snippet")) {
						JSONObject topLevelCommentTempObj = oneVideoCommentObject.getJSONObject("snippet")
								.getJSONObject("topLevelComment");
						try {
							JSONObject topLevelCommentTable = new JSONObject()
									.put("TLCommentId", topLevelCommentTempObj.getString("id"))
									.put("VideoId", oneVideoCommentObject.getJSONObject("snippet").getString("videoId"))
									.put("ChannelId",
											topLevelCommentTempObj.getJSONObject("snippet")
													.getJSONObject("authorChannelId").getString("value"))
									.put("TLCommentLikeCount",
											topLevelCommentTempObj.getJSONObject("snippet").getInt("likeCount"))
									.put("TLCommentPublishedAt",
											topLevelCommentTempObj.getJSONObject("snippet").getString("publishedAt"))
									.put("TLCommentUpdatedAt",
											topLevelCommentTempObj.getJSONObject("snippet").getString("updatedAt"))
									.put("TLCommentTextDisplay",
											topLevelCommentTempObj.getJSONObject("snippet").getString("textDisplay"))
									.put("TotalReplyCount",
											oneVideoCommentObject.getJSONObject("snippet").getInt("totalReplyCount"));

							videoCommentTableList[0].add(topLevelCommentTable);

							// Save the author's channel id to channelIdList.
							channelIdSet.add(topLevelCommentTempObj.getJSONObject("snippet")
									.getJSONObject("authorChannelId").getString("value"));

						} catch (org.json.JSONException e) {
							System.out.println(e.getMessage());
						}
					}
					// Then, store the replies of the top level comments.
					if (oneVideoCommentObject.has("replies")) {
						JSONArray oneCommentReplyList = oneVideoCommentObject.getJSONObject("replies")
								.getJSONArray("comments");
						// For every single reply object:
						for (int j = 0; j < oneCommentReplyList.length(); j++) {
							JSONObject replyObj = oneCommentReplyList.getJSONObject(j);
							try {
								JSONObject replyTable = new JSONObject().put("ReplyId", replyObj.getString("id"))
										.put("TLCommentId", replyObj.getJSONObject("snippet").getString("parentId"))
										.put("ChannelId",
												replyObj.getJSONObject("snippet").getJSONObject("authorChannelId")
														.getString("value"))
										.put("ReplyLikeCount", replyObj.getJSONObject("snippet").getInt("likeCount"))
										.put("ReplyPublishedAt",
												replyObj.getJSONObject("snippet").getString("publishedAt"))
										.put("ReplyUpdatedAt", replyObj.getJSONObject("snippet").getString("updatedAt"))
										.put("ReplyTextDisplay",
												replyObj.getJSONObject("snippet").getString("textDisplay"));

								videoCommentTableList[1].add(replyTable);

								// Save the author's channel id to
								// channelIdList.
								channelIdSet.add(replyObj.getJSONObject("snippet").getJSONObject("authorChannelId")
										.getString("value"));

							} catch (org.json.JSONException e) {
								System.out.println(e.getMessage());
							}
						}
					}
				}

			} catch (com.google.api.client.googleapis.json.GoogleJsonResponseException e) {
				if (e.getStatusCode() != 403) {
					throw e;
				}
			}
		}
		return videoCommentTableList;
	}

	public ArrayList<JSONObject> channelTableList() throws IOException {
		ArrayList<JSONObject> channelTableList = new ArrayList<JSONObject>();

		Iterator<String> channelSetIterator = channelIdSet.iterator();
		while (channelSetIterator.hasNext()) {
			channelIdBuilder.append(channelSetIterator.next() + ",");
		}

		String channelIdCSV = channelIdBuilder.toString().substring(0, channelIdBuilder.toString().length() - 1);

		YouTube.Channels.List channels = youtube.channels().list("id,snippet").setKey(apiKey)
				.setFields("items(id,snippet(country,description,publishedAt,title))");

		ArrayList<String> splittedChannelIdCSV = csvSplitter(channelIdCSV);
		Iterator<String> channelIdIterator = splittedChannelIdCSV.iterator();

		while (channelIdIterator.hasNext()) {
			channels.setId(channelIdIterator.next());
			ChannelListResponse channelListResponse = channels.execute();

			List<Channel> channelList = channelListResponse.getItems();
			Iterator<Channel> channelIterator = channelList.iterator();
			while (channelIterator.hasNext()) {
				Channel channel = channelIterator.next();
				JSONObject channelTable = new JSONObject().put("ChannelId", channel.getId())
						.put("ChannelPublishedAt", channel.getSnippet().getPublishedAt().toString())
						.put("ChannelTitle", channel.getSnippet().getTitle())
						.put("ChannelDescription", channel.getSnippet().getDescription());
				channelTableList.add(channelTable);
			}
		}
		return channelTableList;
	}

	public ArrayList<JSONObject> channelStatisticTableList() throws IOException {
		ArrayList<JSONObject> channelStatisticTableList = new ArrayList<JSONObject>();

		String channelIdCSV = channelIdBuilder.toString().substring(0, channelIdBuilder.toString().length() - 1);

		YouTube.Channels.List channels = youtube.channels().list("id,statistics").setKey(apiKey)
				.setFields("items(id,statistics)");

		ArrayList<String> splittedChannelIdCSV = csvSplitter(channelIdCSV);
		Iterator<String> channelIdIterator = splittedChannelIdCSV.iterator();

		while (channelIdIterator.hasNext()) {
			channels.setId(channelIdIterator.next());
			ChannelListResponse channelListResponse = channels.execute();

			List<Channel> channelList = channelListResponse.getItems();
			Iterator<Channel> channelIterator = channelList.iterator();
			while (channelIterator.hasNext()) {
				Channel channel = channelIterator.next();
				JSONObject channelStatisticTable = new JSONObject().put("ChannelId", channel.getId())
						.put("ChannelTimeStamp", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()))
						.put("ChannelCommentCount", channel.getStatistics().getCommentCount())
						.put("ChannelSubscriberCount", channel.getStatistics().getSubscriberCount())
						.put("ChannelVideoCount", channel.getStatistics().getVideoCount())
						.put("ChannelViewCount", channel.getStatistics().getViewCount());
				channelStatisticTableList.add(channelStatisticTable);
			}
		}
		return channelStatisticTableList;
	}
}
