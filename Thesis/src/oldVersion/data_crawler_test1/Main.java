package data_crawler_test1;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.YouTube.Search;
import com.google.api.services.youtube.auth.Auth;

/*
 * (1) video id list
 * (2) videoid -> video info (-> channel id, -> category id): Video, VideoStatistics
 * (2.1) VideoCategory
 * (3) videoid -> TopLevelComment, Reply (-> channel id)
 * (4) channelid -> channel info: Channel, ChannelStatistic 
 */

public class Main {

	private static final String PROPERTIES_FILENAME = "youtube.properties";
	private static YouTube youtube;

	public static void main(String[] args) throws Exception {

		// ----------------- YouTube class initialization.
		// --------------------------//
		Properties properties = new Properties();
		try {
			InputStream in = Search.class.getResourceAsStream("/" + PROPERTIES_FILENAME);
			properties.load(in);

		} catch (IOException e) {
			System.err.println(
					"There was an error reading " + PROPERTIES_FILENAME + ": " + e.getCause() + " : " + e.getMessage());
			System.exit(1);
		}
		youtube = new YouTube.Builder(Auth.HTTP_TRANSPORT, Auth.JSON_FACTORY, new HttpRequestInitializer() {
			public void initialize(HttpRequest request) throws IOException {
			}
		}).setApplicationName("youtube-cmdline").build();
		String apiKey = properties.getProperty("youtube.apikey");
		// -------------------------------------------------------------------------//

		StringBuilder videoIdListBuilder = new StringBuilder();
		StringBuilder channelIdListBuilder = new StringBuilder();

		ArrayList<JSONObject> videoTableList = new ArrayList<JSONObject>();
		ArrayList<JSONObject> videoStatisticTableList = new ArrayList<JSONObject>();
		ArrayList<JSONObject> videoCategoryTableList = new ArrayList<JSONObject>();
		ArrayList<JSONObject> topLevelCommentTableList = new ArrayList<JSONObject>();
		ArrayList<JSONObject> replyTableList = new ArrayList<JSONObject>();
		ArrayList<JSONObject> channelTableList = new ArrayList<JSONObject>();
		ArrayList<JSONObject> channelStatisticTableList = new ArrayList<JSONObject>();

		// List of videoIds:
		VideoListGenerator videoList1 = new VideoListGenerator();
		JSONObject newestFeatured = new JSONObject(videoList1.videoList("date", youtube, apiKey));
		JSONArray jsonVideoId = newestFeatured.getJSONArray("items");
		for (int i = 0; i < jsonVideoId.length(); i++) {
			JSONObject childJSONObj = jsonVideoId.getJSONObject(i);
			String videoId = childJSONObj.getJSONObject("id").getString("videoId");
			// System.out.println(videoId);
			videoIdListBuilder.append(videoId);
			videoIdListBuilder.append(",");
		}
		String[] videoList = videoIdListBuilder.toString().substring(0, videoIdListBuilder.toString().length() - 1)
				.split(",");

		// List of video info:
		VideoInfo videoInfoGenerator = new VideoInfo();

		// --Iterate the video id list
		for (String videoId : videoList) {
			JSONObject oneVideoInfo = new JSONObject(videoInfoGenerator.videoInfo(videoId, youtube, apiKey));
			JSONObject jsonVideoInfo = oneVideoInfo.getJSONArray("items").getJSONObject(0);

			// From videoId to videoTable
			JSONObject videoTable = new JSONObject().put("VideoId", videoId)
					.put("CategoryId", jsonVideoInfo.getJSONObject("snippet").getString("categoryId"))
					.put("ChannelId", jsonVideoInfo.getJSONObject("snippet").getString("channelId"))
					.put("VideoPublishedAt", jsonVideoInfo.getJSONObject("snippet").getString("publishedAt")) // format
																												// not
																												// correct.
					.put("Duration", jsonVideoInfo.getJSONObject("contentDetails").getString("duration"))
					.put("VideoTitle", jsonVideoInfo.getJSONObject("snippet").getString("title"))
					.put("VideoDescription", jsonVideoInfo.getJSONObject("snippet").getString("description"));
			videoTableList.add(videoTable); // store the video table.

			// From videoId to videoStatisticTable
			JSONObject videoStatisticTable = new JSONObject().put("VideoId", videoId).put("VideoTimeStamp",
					new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
			if (jsonVideoInfo.getJSONObject("statistics").has("commentCount")) {
				videoStatisticTable.put("VideoCommentsCount",
						jsonVideoInfo.getJSONObject("statistics").getLong("commentCount"));
			} else {
				videoStatisticTable.put("VideoCommentsCount", 0);
			}
			videoStatisticTable
					.put("VideoDislikeCount", jsonVideoInfo.getJSONObject("statistics").getLong("dislikeCount"))
					.put("VideoFavoriteCount", jsonVideoInfo.getJSONObject("statistics").getLong("favoriteCount"))
					.put("VideoLikeCount", jsonVideoInfo.getJSONObject("statistics").getLong("likeCount"))
					.put("VideoViewCount", jsonVideoInfo.getJSONObject("statistics").getLong("viewCount"));
			// --store the video statistic table.
			videoStatisticTableList.add(videoStatisticTable);

			// From videoId to videoCategoryTable
			JSONObject oneVideoCategory = new JSONObject(
					new VideoCategoryInfo().videoCategory(videoTable.getString("CategoryId"), youtube, apiKey))
							.getJSONArray("items").getJSONObject(0);
			JSONObject videoCategoryTable = new JSONObject().put("CategoryId", oneVideoCategory.getString("id"))
					.put("CategoryTitle", oneVideoCategory.getJSONObject("snippet").getString("title"));
			// --store the category table.
			videoCategoryTableList.add(videoCategoryTable);

			// From videoId to Comments.
			if (videoStatisticTable.getInt("VideoCommentsCount") != 0) {
				// try
				try {

					JSONObject oneVideoComment = new JSONObject(
							new CommentInfo().commentInfo(videoId, youtube, apiKey));
					if (oneVideoComment.has("items")) { // if has comments
						JSONArray oneVideoCommentList = oneVideoComment.getJSONArray("items");

						// Iterate the comment list.
						for (int i = 0; i < oneVideoCommentList.length(); i++) {
							JSONObject oneVideoCommentObject = oneVideoCommentList.getJSONObject(i);

							// First, store the top level comments.
							if (oneVideoCommentObject.has("snippet")) {
								JSONObject topLevelCommentTempObj = oneVideoCommentObject.getJSONObject("snippet")
										.getJSONObject("topLevelComment");
								JSONObject topLevelCommentTable = new JSONObject()
										.put("TLCommentId", topLevelCommentTempObj.getString("id"))
										.put("VideoId",
												oneVideoCommentObject.getJSONObject("snippet").getString("videoId"))
										.put("ChannelId",
												topLevelCommentTempObj.getJSONObject("snippet")
														.getJSONObject("authorChannelId").getString("value"))
										.put("TLCommentLikeCount",
												topLevelCommentTempObj.getJSONObject("snippet").getInt("likeCount"))
										.put("TLCommentPublishedAt",
												topLevelCommentTempObj.getJSONObject("snippet")
														.getString("publishedAt"))
										.put("TLCommentUpdatedAt",
												topLevelCommentTempObj.getJSONObject("snippet").getString("updatedAt"))
										.put("TLCommentTextDisplay",
												topLevelCommentTempObj.getJSONObject("snippet")
														.getString("textDisplay"))
										.put("TotalReplyCount", oneVideoCommentObject.getJSONObject("snippet")
												.getInt("totalReplyCount"));

								topLevelCommentTableList.add(topLevelCommentTable);

								// Save the author's channel id to
								// channelIdList.
								channelIdListBuilder.append(topLevelCommentTempObj.getJSONObject("snippet")
										.getJSONObject("authorChannelId").getString("value"));
								channelIdListBuilder.append(",");
							}
							// Then, store the replies of the top level
							// comments.
							if (oneVideoCommentObject.has("replies")) {
								JSONArray oneCommentReplyList = oneVideoCommentObject.getJSONObject("replies")
										.getJSONArray("comments");
								// For every single reply object:
								for (int j = 0; j < oneCommentReplyList.length(); j++) {
									JSONObject replyObj = oneCommentReplyList.getJSONObject(j);
									JSONObject replyTable = new JSONObject().put("ReplyId", replyObj.getString("id"))
											.put("TLCommentId", replyObj.getJSONObject("snippet").getString("parentId"))
											.put("ChannelId",
													replyObj.getJSONObject("snippet").getJSONObject("authorChannelId")
															.getString("value"))
											.put("ReplyLikeCount",
													replyObj.getJSONObject("snippet").getInt("likeCount"))
											.put("ReplyPublishedAt",
													replyObj.getJSONObject("snippet").getString("publishedAt"))
											.put("ReplyUpdatedAt",
													replyObj.getJSONObject("snippet").getString("updatedAt"))
											.put("ReplyTextDisplay",
													replyObj.getJSONObject("snippet").getString("textDisplay"));

									replyTableList.add(replyTable);

									// Save the author's channel id to
									// channelIdList.
									channelIdListBuilder.append(replyObj.getJSONObject("snippet")
											.getJSONObject("authorChannelId").getString("value"));
									channelIdListBuilder.append(",");
								}
							}
						}
					}
				}
				// catch
				catch (com.google.api.client.googleapis.json.GoogleJsonResponseException e) {
					if (e.getStatusCode() != 403) {
						throw e;
					}
				}
			}

			// Save the video's channel id to channelIdList.
			channelIdListBuilder.append(jsonVideoInfo.getJSONObject("snippet").getString("channelId"));
			channelIdListBuilder.append(",");
		}

		// --Channel id list build.
		String[] channelList = channelIdListBuilder.toString()
				.substring(0, channelIdListBuilder.toString().length() - 1).split(",");

		// Channel related information.
		ChannelInfo channelInfo = new ChannelInfo();
		for (String channelId : channelList) {
			JSONObject oneChannelObject = new JSONObject(channelInfo.channelInfo(channelId, youtube, apiKey))
					.getJSONArray("items").getJSONObject(0);

			// Create and store channel table.
			JSONObject channelTable = new JSONObject().put("ChannelId", channelId)
					.put("ChannelPublishedAt", oneChannelObject.getJSONObject("snippet").getString("publishedAt"))
					.put("ChannelTitle", oneChannelObject.getJSONObject("snippet").getString("title"))
					.put("ChannelDescription", oneChannelObject.getJSONObject("snippet").getString("description"));
			channelTableList.add(channelTable);

			// Create and store channel statistic table.
			// System.out.println(oneChannelObject.getJSONObject("statistics").toString());
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
		}

		// -------------------------------------------------------------------------//
		// -------------------------------------------------------------------------//
		// -------------------------------------------------------------------------//

		MySQLAccess insertionToDatabase = new MySQLAccess();
		insertionToDatabase.establishConnection();
		insertionToDatabase.writeChannelToDataBase(channelTableList);
		insertionToDatabase.writeChannelStatisticToDatebase(channelStatisticTableList);
		insertionToDatabase.writeVideoCategoryToDatabase(videoCategoryTableList);
		insertionToDatabase.writeVideoToDatabase(videoTableList);
		insertionToDatabase.writeVideoStatisticToDatabase(videoStatisticTableList);
		insertionToDatabase.writeTopLevelCommentToDatebase(topLevelCommentTableList);
		insertionToDatabase.writeReplyToDatabase(replyTableList);
		insertionToDatabase.close();

	}
}
