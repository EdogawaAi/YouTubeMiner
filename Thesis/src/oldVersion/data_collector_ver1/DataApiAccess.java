package data_collector_ver1;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.ChannelListResponse;
import com.google.api.services.youtube.model.CommentThreadListResponse;
import com.google.api.services.youtube.model.VideoCategoryListResponse;
import com.google.api.services.youtube.model.VideoListResponse;

public class DataApiAccess {

	private YouTube youtube;
	private String apiKey;

	// Help passing channel id between methods.
	private StringBuilder channelIdBuilder = new StringBuilder();
	// Help passing category id between methods.
	private StringBuilder categoryIdBuilder = new StringBuilder();

	public DataApiAccess(YouTube youtube, String apiKey) {
		this.youtube = youtube;
		this.apiKey = apiKey;
	}

	/**
	 * 
	 * @param videoIdList
	 * @return
	 * @throws IOException
	 */
	public ArrayList<JSONObject> videoTableList(String[] videoIdList) throws IOException {

		ArrayList<JSONObject> videoTableList = new ArrayList<JSONObject>();
		for (String videoId : videoIdList) {
			YouTube.Videos.List videoList = youtube.videos().list("snippet,contentDetails").setKey(apiKey)
					.setFields(
							"items(contentDetails/duration,snippet(categoryId,channelId,description,publishedAt,title))")
					.setId(videoId);
			VideoListResponse videoListResponse = videoList.execute();

			JSONObject jsonVideoInfo = new JSONObject(videoListResponse.toString()).getJSONArray("items")
					.getJSONObject(0);

			JSONObject videoTable = new JSONObject().put("VideoId", videoId)
					.put("CategoryId", jsonVideoInfo.getJSONObject("snippet").getString("categoryId"))
					.put("ChannelId", jsonVideoInfo.getJSONObject("snippet").getString("channelId"))
					.put("VideoPublishedAt", jsonVideoInfo.getJSONObject("snippet").getString("publishedAt"))
					.put("Duration", jsonVideoInfo.getJSONObject("contentDetails").getString("duration"))
					.put("VideoTitle", jsonVideoInfo.getJSONObject("snippet").getString("title"))
					.put("VideoDescription", jsonVideoInfo.getJSONObject("snippet").getString("description"));
			videoTableList.add(videoTable); // store the video table.
			channelIdBuilder.append(jsonVideoInfo.getJSONObject("snippet").getString("channelId"));
			channelIdBuilder.append(",");
			categoryIdBuilder.append(jsonVideoInfo.getJSONObject("snippet").getString("categoryId"));
			categoryIdBuilder.append(",");
		}

		return videoTableList;
	}

	/**
	 * 
	 * @param videoIdList
	 * @return
	 * @throws IOException
	 */
	public ArrayList<JSONObject> videoStatisticTableList(String[] videoIdList) throws IOException {

		ArrayList<JSONObject> videoStatisticTableList = new ArrayList<JSONObject>();
		for (String videoId : videoIdList) {
			YouTube.Videos.List videoList = youtube.videos().list("statistics").setKey(apiKey)
					.setFields("items/statistics").setId(videoId);
			VideoListResponse videoListResponse = videoList.execute();

			JSONObject jsonVideoStatisticInfo = new JSONObject(videoListResponse.toString()).getJSONArray("items")
					.getJSONObject(0);

			JSONObject videoStatisticTable = new JSONObject().put("VideoId", videoId).put("VideoTimeStamp",
					new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
			if (jsonVideoStatisticInfo.getJSONObject("statistics").has("commentCount")) {
				videoStatisticTable.put("VideoCommentsCount",
						jsonVideoStatisticInfo.getJSONObject("statistics").getLong("commentCount"));
			} else {
				videoStatisticTable.put("VideoCommentsCount", 0);
			}
			if (jsonVideoStatisticInfo.getJSONObject("statistics").has("dislikeCount")) {
				videoStatisticTable.put("VideoDislikeCount",
						jsonVideoStatisticInfo.getJSONObject("statistics").getLong("dislikeCount"));
			} else {
				videoStatisticTable.put("VideoDislikeCount", 0);
			}
			if (jsonVideoStatisticInfo.getJSONObject("statistics").has("likeCount")) {
				videoStatisticTable.put("VideoLikeCount",
						jsonVideoStatisticInfo.getJSONObject("statistics").getLong("likeCount"));
			} else {
				videoStatisticTable.put("VideoLikeCount", 0);
			}
			try {
				videoStatisticTable
						.put("VideoFavoriteCount",
								jsonVideoStatisticInfo.getJSONObject("statistics").getLong("favoriteCount"))
						.put("VideoViewCount", jsonVideoStatisticInfo.getJSONObject("statistics").getLong("viewCount"));
			} catch (org.json.JSONException e) {
				System.out.println(jsonVideoStatisticInfo.toString());
			}
			// --store the video statistic table.
			videoStatisticTableList.add(videoStatisticTable);
		}

		return videoStatisticTableList;
	}

	/**
	 * 
	 * @return
	 * @throws IOException
	 */
	public ArrayList<JSONObject> videoCategoryTableList() throws IOException {
		String[] categoryIdList = categoryIdBuilder.toString().substring(0, categoryIdBuilder.toString().length() - 1)
				.split(",");
		ArrayList<JSONObject> videoCategoryTableList = new ArrayList<JSONObject>();

		for (String categoryId : categoryIdList) {
			YouTube.VideoCategories.List videoCategories = youtube.videoCategories().list("snippet").setKey(apiKey)
					.setId(categoryId).setFields("items(id,snippet/title)");
			VideoCategoryListResponse videoCategoryListResponse = videoCategories.execute();

			JSONObject jsonVideoCategoryInfo = new JSONObject(videoCategoryListResponse.toString())
					.getJSONArray("items").getJSONObject(0);

			JSONObject videoCategoryTable = new JSONObject().put("CategoryId", jsonVideoCategoryInfo.getString("id"))
					.put("CategoryTitle", jsonVideoCategoryInfo.getJSONObject("snippet").getString("title"));
			// --store the category table.
			videoCategoryTableList.add(videoCategoryTable);
		}

		return videoCategoryTableList;
	}

	/**
	 * 
	 * @param videoIdList
	 * @return videoCommentTableList[]: 0: top level comment; 1: reply.
	 * @throws IOException
	 */
	public ArrayList<JSONObject>[] videoCommentTableList(String[] videoIdList) throws IOException {
		// 0: top level comment; 1: reply.
		@SuppressWarnings("unchecked")
		ArrayList<JSONObject>[] videoCommentTableList = (ArrayList<JSONObject>[]) new ArrayList[2];
		for (int i = 0; i < videoCommentTableList.length; i++) {
			videoCommentTableList[i] = new ArrayList<JSONObject>();
		}
		int count = 0;
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
							channelIdBuilder.append(topLevelCommentTempObj.getJSONObject("snippet")
									.getJSONObject("authorChannelId").getString("value"));
							channelIdBuilder.append(",");
						} catch (org.json.JSONException e) {
							System.out.println(e.getMessage());
						}
						if ((++count) % 100 == 0) {
							System.out.println("comment count: " + count);
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
								channelIdBuilder.append(replyObj.getJSONObject("snippet")
										.getJSONObject("authorChannelId").getString("value"));
								channelIdBuilder.append(",");
							} catch (org.json.JSONException e) {
								System.out.println(e.getMessage());
							}
							if ((++count) % 100 == 0) {
								System.out.println("comment count: " + count);
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

	/**
	 * 
	 * @return
	 * @throws IOException
	 */
	public ArrayList<JSONObject> channelTableList() throws IOException {
		ArrayList<JSONObject> channelTableList = new ArrayList<JSONObject>();
		String[] channelList = channelIdBuilder.toString().substring(0, channelIdBuilder.toString().length() - 1)
				.split(",");
		System.out.println("channel list length: " + channelList.length);

		int count = 0;
		for (String channelId : channelList) {
			YouTube.Channels.List channels = youtube.channels().list("id,snippet").setKey(apiKey).setId(channelId)
					.setFields("items(id,snippet(country,description,publishedAt,title))");
			ChannelListResponse channelListResponse = channels.execute();

			JSONObject oneChannelObject = new JSONObject(channelListResponse.toString()).getJSONArray("items")
					.getJSONObject(0);
			JSONObject channelTable = new JSONObject().put("ChannelId", channelId)
					.put("ChannelPublishedAt", oneChannelObject.getJSONObject("snippet").getString("publishedAt"))
					.put("ChannelTitle", oneChannelObject.getJSONObject("snippet").getString("title"))
					.put("ChannelDescription", oneChannelObject.getJSONObject("snippet").getString("description"));
			channelTableList.add(channelTable);

			if ((++count) % 10 == 0) {
				System.out.println("channel count: " + count);
			}
		}

		return channelTableList;
	}

	/**
	 * 
	 * @return
	 * @throws IOException
	 */
	public ArrayList<JSONObject> channelStatisticTableList() throws IOException {
		ArrayList<JSONObject> channelStatisticTableList = new ArrayList<JSONObject>();
		String[] channelList = channelIdBuilder.toString().substring(0, channelIdBuilder.toString().length() - 1)
				.split(",");

		int count = 0;
		for (String channelId : channelList) {
			YouTube.Channels.List channels = youtube.channels().list("statistics").setKey(apiKey).setId(channelId)
					.setFields("items/statistics");
			ChannelListResponse channelListResponse = channels.execute();

			JSONObject oneChannelObject = new JSONObject(channelListResponse.toString()).getJSONArray("items")
					.getJSONObject(0);
			JSONObject channelStatisticTable = new JSONObject().put("ChannelId", channelId)
					.put("ChannelTimeStamp", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()))
					.put("ChannelCommentCount",
							Long.valueOf(oneChannelObject.getJSONObject("statistics").getString("commentCount")))
					.put("ChannelSubscriberCount",
							Long.valueOf(oneChannelObject.getJSONObject("statistics").getString("subscriberCount")))
					.put("ChannelVideoCount",
							Long.valueOf(oneChannelObject.getJSONObject("statistics").getString("videoCount")))
					.put("ChannelViewCount",
							Long.valueOf(oneChannelObject.getJSONObject("statistics").getString("viewCount")));
			channelStatisticTableList.add(channelStatisticTable);
			if ((++count) % 10 == 0) {
				System.out.println("channel count: " + count);
			}
		}

		return channelStatisticTableList;
	}

}
