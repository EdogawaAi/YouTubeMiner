package data_collector_ver4;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

import org.json.JSONObject;

import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Channel;
import com.google.api.services.youtube.model.ChannelListResponse;
import com.google.api.services.youtube.model.Comment;
import com.google.api.services.youtube.model.CommentThread;
import com.google.api.services.youtube.model.CommentThreadListResponse;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoCategory;
import com.google.api.services.youtube.model.VideoCategoryListResponse;
import com.google.api.services.youtube.model.VideoListResponse;

/**
 * Designed to combine the multiple processes into one single block. Input
 * should be a set of video, and then generate a plenty of information of
 * YouTube videos. This is basically the metadata collection part. <b>
 * 
 * @author Tian
 *
 */
public class YouTubeAPIProcess {

	// Initiate the attributes that are required in the information retrieving
	// process.
	private YouTube youtube;
	private String apiKey;

	// "Sets" that hold three kinds of IDs:
	// (1) video ID: simple as it is. In our project, we use the IDs that have
	// been stored in our database as the data source. In this case, we
	// treat the videoIdSet as somewhat "read-only", i.e., once we load
	// this attribute, we don't manipulate it in this API process class.
	// (2) channel ID: this set is empty before this process gets executed,
	// and it is enriched as the the process going. This is because: when
	// we are collecting metadata of videos, such as uploaders, commenters,
	// etc., they all hold some "channel IDs". So, we grow the set of channel
	// IDs when we get more channel IDs as the metadata collection process
	// goes. However, this results in the importance of the order of the
	// process execution.
	// (3) category ID: similar to channel ID, this is also a changing set as
	// the
	// process going. However, this ID set is only influenced by video metadata
	// retrieval, which is much simpler than channel ID.
	private LinkedHashSet<String> videoIdSet = new LinkedHashSet<String>();
	private LinkedHashSet<String> channelIdSet = new LinkedHashSet<String>();
	private LinkedHashSet<String> categoryIdSet = new LinkedHashSet<String>();

	// Initiate the storages of entities that will be inserted to database.
	private ArrayList<JSONObject> videoTableList = new ArrayList<JSONObject>();
	private ArrayList<JSONObject> videoStatisticTableList = new ArrayList<JSONObject>();
	private ArrayList<JSONObject> videoCategoryTableList = new ArrayList<JSONObject>();
	@SuppressWarnings("unchecked")
	// Comments have two type: top level comment, and reply. However, they are
	// retrieved at the same time: when gathering a video's comments, both top
	// level comments and replies are collected at one request. Thus, I used an
	// array to store them. (A better solution can be: create a new class that
	// stores the 2 JSONObject, and return the new class type other than return
	// an array.)
	private ArrayList<JSONObject>[] videoCommentTableList = new ArrayList[2];
	private ArrayList<JSONObject> channelTableList = new ArrayList<JSONObject>();
	private ArrayList<JSONObject> channelStatisticTableList = new ArrayList<JSONObject>();

	// A test attribute for counting users that don't have channel IDs.
	private static long noChannelUserCount = 0;

	// Basic properties for setting and retrieving attributes.
	// -------------------------------------------------------------
	protected YouTubeAPIProcess(YouTube youtube, String apiKey, LinkedHashSet<String> videoIdSet) {
		this.youtube = youtube;
		this.apiKey = apiKey;
		this.videoIdSet = videoIdSet;
	}

	public void setVideoIdSet(LinkedHashSet<String> videoIdSet) {
		this.videoIdSet = videoIdSet;
	}

	public LinkedHashSet<String> getCategoryIdSet() {
		return categoryIdSet;
	}

	public LinkedHashSet<String> getVideoIdSet() {
		return videoIdSet;
	}

	public LinkedHashSet<String> getChannelIdSet() {
		return channelIdSet;
	}

	public ArrayList<JSONObject> getVideoTableList() {
		return videoTableList;
	}

	public ArrayList<JSONObject> getVideoStatisticTableList() {
		return videoStatisticTableList;
	}

	public ArrayList<JSONObject> getVideoCategoryTableList() {
		return videoCategoryTableList;
	}

	public ArrayList<JSONObject>[] getVideoCommentTableList() {
		return videoCommentTableList;
	}

	public ArrayList<JSONObject> getChannelTableList() {
		return channelTableList;
	}

	public ArrayList<JSONObject> getChannelStatisticTableList() {
		return channelStatisticTableList;
	}
	// -------------------------------------------------------------

	// Execution method handles the basic process unit, and each one unit
	// contains multiple API calls.
	// Note: the order does matter. Once the process is modified, it is
	// important to make sure that the order is double checked.
	public YouTubeAPIProcessResult execute() throws Exception {
		generateVideoTableList();
		System.out.println("--Video info retrieved.");
		generateVideoStatisticTableList();
		System.out.println("--Video Statistic info retrieved.");
		generateVideoCategoryTableList();
		System.out.println("--Category info retrieved.");
		generateVideoCommentTableList();
		System.out.println("--Comment info retrieved.");
		generateChannelTableList();
		System.out.println("--Channel info retrieved.");
		generateChannelStatisticTableList();
		System.out.println("--Channel Statistic info retrieved.");
		YouTubeAPIProcessResult processResult = new YouTubeAPIProcessResult(videoTableList, videoStatisticTableList,
				videoCategoryTableList, videoCommentTableList, channelTableList, channelStatisticTableList);
		return processResult;
	}

	// A method that retrieve the metadata of video.
	private void generateVideoTableList() throws IOException {

		// Initialize the container
		videoTableList = new ArrayList<JSONObject>();

		// Prepare an API call
		YouTube.Videos.List videoList = youtube.videos().list("id,snippet,contentDetails").setKey(apiKey).setFields(
				"items(id,contentDetails/duration,snippet(categoryId,channelId,description,publishedAt,title))");

		// Pre-process the video IDs to make it acceptable by the API.
		// Note that the API accept a comma-separate value with at most
		// 50 values in it (i.e., 49 commas in it)
		// First, we merge the IDs into a single string. And next, we split them
		// into small chunks so that the API can accept them.
		String videoIdListCSV = hashSetToCSV(videoIdSet);
		ArrayList<String> splittedVideoIdListCSV = csvSplitter(videoIdListCSV);

		// Each instance contains 50 video IDs separated by commas.
		Iterator<String> videoIdIterator = splittedVideoIdListCSV.iterator();

		while (videoIdIterator.hasNext()) {// Iterate the response list

			videoList.setId(videoIdIterator.next());
			VideoListResponse videoListResponse = videoList.execute();

			java.util.List<Video> videos = videoListResponse.getItems();
			Iterator<Video> videoIterator = videos.iterator();

			// Create a JSONObject that has the same structure as the MySQL
			// database table. Use the JSONObject to save each entity's values.
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

				// channel and category IDs that related to a particular video
				// should be updated and stored into our ID set.
				// Later on, we will use these sets to collect their metadata.
				channelIdSet.add(video.getSnippet().getChannelId());
				categoryIdSet.add(video.getSnippet().getCategoryId());
			}
		}
	}

	// The following processes are similar to the method above :)
	private void generateVideoStatisticTableList() throws IOException {

		videoStatisticTableList = new ArrayList<JSONObject>();

		YouTube.Videos.List videoList = youtube.videos().list("id,statistics").setKey(apiKey)
				.setFields("items(id,statistics)");

		String videoIdListCSV = hashSetToCSV(videoIdSet);
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
	}

	private void generateVideoCategoryTableList() throws IOException {

		StringBuilder categoryIdBuilder = new StringBuilder();
		videoCategoryTableList = new ArrayList<JSONObject>();

		YouTube.VideoCategories.List videoCategories = youtube.videoCategories().list("snippet").setKey(apiKey)
				.setFields("items(id,snippet/title)");

		Iterator<String> categoryIdSetIterator = categoryIdSet.iterator();
		while (categoryIdSetIterator.hasNext()) {
			categoryIdBuilder.append(categoryIdSetIterator.next() + ",");
		}
		String categoryIdCSV = categoryIdBuilder.toString().replaceAll(",$", "");

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
	}

	@SuppressWarnings("unchecked")
	private void generateVideoCommentTableList() throws Exception {
		String videoIdListCSV = hashSetToCSV(videoIdSet);
		String[] videoIdList = videoIdListCSV.split(",");
		// Creation of:
		// 0: top level comment; 1: reply.
		// This is an array of ArrayList with 2 elements.
		videoCommentTableList = new ArrayList[2];
		// Initialization of containers.
		for (int i = 0; i < videoCommentTableList.length; i++) {
			videoCommentTableList[i] = new ArrayList<JSONObject>();
		}

		/// Iteration of all the video ID list to retrieve their comments.
		for (String videoId : videoIdList) {
			String curVideoId = videoId; // Current video ID
			Comment curComment = new Comment(); // Current comment

			try {
				YouTube.CommentThreads.List videoCommentsList = youtube.commentThreads().list("snippet,replies")
						.setKey(apiKey).setVideoId(videoId).setTextFormat("plainText").setMaxResults((long) 100)
						.setFields(
								"items(replies(comments(id,snippet(authorChannelId,likeCount,parentId,publishedAt,textDisplay,updatedAt))),"
										+ "snippet(topLevelComment(id,snippet(authorChannelId,likeCount,publishedAt,textDisplay,updatedAt)),"
										+ "totalReplyCount,videoId)),nextPageToken");
				CommentThreadListResponse videoCommentsListResponse = videoCommentsList.execute();
				List<CommentThread> commentThreadList = videoCommentsListResponse.getItems();
				// Collect every pages: one page contains a limited result, so
				// we need to look for "next page" until there's no "next page".
				// ** Set the upper bound of comments to 1000 for now. This cap
				// can be removed after separating channel metadata collection
				// process, because of its large time consuming.
				while (videoCommentsListResponse.getNextPageToken() != null) {
					videoCommentsList = videoCommentsList.setPageToken(videoCommentsListResponse.getNextPageToken());

					// Followed is an implementation of an exponential backoff
					// algorithm.
					// Set the initial waiting time for waiting for next try.
					long sleepTime = 5000;
					try {
						videoCommentsListResponse = videoCommentsList.execute();
					} catch (Exception e) {
						// retry max 3 times.
						int tryCount = 0;
						int maxRetryTime = 3;
						while (true) {
							try {
								tryCount++;
								System.out.println("**Error occurs, retry time: " + tryCount + "...");
								Thread.sleep(sleepTime);
								videoCommentsListResponse = videoCommentsList.execute();
							} catch (Exception e1) {
								// Double the waiting time if request failed.
								sleepTime = sleepTime * 2;
								// Set maximum waiting time to 1 minute.
								if (sleepTime >= 60000) {
									sleepTime = 60000;
								}
								// If failed 3 times, throw the exception and
								// terminate.
								if (tryCount >= maxRetryTime) {
									throw e1;
								}
							}
						}
					}
					commentThreadList.addAll(videoCommentsListResponse.getItems());
					// Upper bound implementation.
					if (commentThreadList.size() >= 1000) {
						break;
					}
				}

				// Start iterator to organize the response, and "demolish" it
				// into two type:
				// (1) top level comments; (2) replies.
				Iterator<CommentThread> iterComment = commentThreadList.iterator();
				while (iterComment.hasNext()) {
					CommentThread videoComment = iterComment.next();
					Comment topLevelComment = videoComment.getSnippet().getTopLevelComment();
					curComment = topLevelComment;

					// Avoid null author (channel) IDs. Some users may use their
					// Google+ IDs to make comments instead of using YouTube
					// channel IDs. Fortunately, this is not a big number.
					if (!topLevelComment.getSnippet().getAuthorChannelId().toString().isEmpty()) {
						JSONObject topLevelCommentTable = new JSONObject().put("TLCommentId", topLevelComment.getId())
								.put("VideoId", videoComment.getSnippet().getVideoId())
								.put("ChannelId",
										authorChannelIdFormat(
												topLevelComment.getSnippet().getAuthorChannelId().toString()))
								.put("TLCommentLikeCount", topLevelComment.getSnippet().getLikeCount())
								.put("TLCommentPublishedAt", topLevelComment.getSnippet().getPublishedAt().toString())
								.put("TLCommentUpdatedAt", topLevelComment.getSnippet().getUpdatedAt().toString())
								.put("TLCommentTextDisplay", topLevelComment.getSnippet().getTextDisplay())
								.put("TotalReplyCount", videoComment.getSnippet().getTotalReplyCount());

						// Store the result and update the channel ID.
						videoCommentTableList[0].add(topLevelCommentTable);
						channelIdSet.add(
								authorChannelIdFormat(topLevelComment.getSnippet().getAuthorChannelId().toString()));

						// If reply exists, retrieve their metadata as well.
						if (videoComment.getSnippet().getTotalReplyCount() != 0) {
							List<Comment> replies = videoComment.getReplies().getComments();
							Iterator<Comment> iterReply = replies.iterator();
							while (iterReply.hasNext()) {
								Comment reply = iterReply.next();
								if (!reply.getSnippet().getAuthorChannelId().toString().isEmpty()) {
									JSONObject replyTable = new JSONObject().put("ReplyId", reply.getId())
											.put("TLCommentId", reply.getSnippet().getParentId())
											.put("ChannelId",
													authorChannelIdFormat(
															reply.getSnippet().getAuthorChannelId().toString()))
											.put("ReplyLikeCount", reply.getSnippet().getLikeCount())
											.put("ReplyPublishedAt", reply.getSnippet().getPublishedAt().toString())
											.put("ReplyUpdatedAt", reply.getSnippet().getUpdatedAt().toString())
											.put("ReplyTextDisplay", reply.getSnippet().getTextDisplay());

									videoCommentTableList[1].add(replyTable);

									// Save the author's channel id to
									// channelIdList.
									channelIdSet.add(
											authorChannelIdFormat(reply.getSnippet().getAuthorChannelId().toString()));
								}
							}
						}

					} else {
						// Sometimes a user may not have a channel ID. Instead,
						// they are using google+ account to making comments. In
						// this case, although I'm not storing the google+
						// information yet, it can be separately stored in a new
						// table. However, since the number of google+ user is
						// far too small, it's not sure yet whether it deserves
						// a new table to store the information.
						// ** For now we just print out a short info. If it's
						// evaluated as worthy, we may continue developing this
						// part.
						String str = topLevelComment.getSnippet().getAuthorDisplayName();
						String googleplus = topLevelComment.getSnippet().getAuthorGoogleplusProfileUrl();
						System.out.println("--The author \"" + str + "\"'s channel ID not found."
								+ "\n\tThe google+ url is: " + googleplus);
					}
				}
			} catch (com.google.api.client.googleapis.json.GoogleJsonResponseException e) {
				if (e.getStatusCode() != 403) {
					if (e.getStatusCode() == 404) {
						System.out.println("**No video specified.**");
					} else if (e.getStatusCode() == 400) {
						System.out.println("Problem exists in video: " + curVideoId);
						Thread.sleep(5000); // take a break to avoid potential
											// continuous errors.
					} else if (e.getStatusCode() == 500 || e.getStatusCode() == 503) {
						Thread.sleep(5000);
					} else {
						throw e;
					}
				}
			} catch (NullPointerException e) {
				if (!curComment.containsKey("authorChannelId")) {
					// This happens when a user is using google+ account other
					// than the youtube account.
					System.out.println("--Author doesn't have channel ID." + "==> Total: " + ++noChannelUserCount);
				} else {
					throw e;
				}
			}
		}
	}

	private void generateChannelTableList() throws Exception {
		channelTableList = new ArrayList<JSONObject>();
		StringBuilder channelIdBuilder = new StringBuilder();

		// Channel ID set grows when other method are executing and getting new
		// channel
		Iterator<String> channelSetIterator = channelIdSet.iterator();
		while (channelSetIterator.hasNext()) {
			channelIdBuilder.append(channelSetIterator.next() + ",");
		}

		String channelIdCSV = channelIdBuilder.toString().replaceAll(",$", "");

		YouTube.Channels.List channels = youtube.channels().list("id,snippet").setKey(apiKey)
				.setFields("items(id,snippet(country,description,publishedAt,title))");

		ArrayList<String> splittedChannelIdCSV = csvSplitter(channelIdCSV);
		Iterator<String> channelIdIterator = splittedChannelIdCSV.iterator();

		int chanCount = 0;
		while (channelIdIterator.hasNext()) {
			channels.setId(channelIdIterator.next());
			ChannelListResponse channelListResponse = null;

			// exponential back-off.
			long sleepTime = 5000;
			try {
				channelListResponse = channels.execute();
			} catch (Exception e) {
				// retry max 3 times.
				int tryCount = 0;
				int maxRetryTime = 3;
				while (true) {
					try {
						tryCount++;
						System.out.println("**Error occurs, retry time: " + tryCount + "...");
						Thread.sleep(sleepTime);
						channelListResponse = channels.execute();
					} catch (Exception e1) {
						// Double the waiting time if request failed.
						sleepTime = sleepTime * 2;
						// Set maximum waiting time to 1 minute.
						if (sleepTime >= 60000) {
							sleepTime = 60000;
						}
						// If failed 3 times, throw the exception.
						if (tryCount >= maxRetryTime) {
							throw e1;
						}
					}
				}
			}

			List<Channel> channelList = channelListResponse.getItems();
			Iterator<Channel> channelIterator = channelList.iterator();
			while (channelIterator.hasNext()) {
				chanCount++;
				Channel channel = channelIterator.next();
				JSONObject channelTable = new JSONObject().put("ChannelId", channel.getId())
						.put("ChannelPublishedAt", channel.getSnippet().getPublishedAt().toString())
						.put("ChannelTitle", channel.getSnippet().getTitle())
						.put("ChannelDescription", channel.getSnippet().getDescription());
				channelTableList.add(channelTable);
			}
		}
		System.out.println("# of channels: " + chanCount);
	}

	private void generateChannelStatisticTableList() throws Exception {
		channelStatisticTableList = new ArrayList<JSONObject>();
		StringBuilder channelIdBuilder = new StringBuilder();

		Iterator<String> channelSetIterator = channelIdSet.iterator();
		while (channelSetIterator.hasNext()) {
			channelIdBuilder.append(channelSetIterator.next() + ",");
		}

		String channelIdCSV = channelIdBuilder.toString().replaceAll(",$", "");

		YouTube.Channels.List channels = youtube.channels().list("id,statistics").setKey(apiKey)
				.setFields("items(id,statistics)");

		ArrayList<String> splittedChannelIdCSV = csvSplitter(channelIdCSV);
		Iterator<String> channelIdIterator = splittedChannelIdCSV.iterator();

		while (channelIdIterator.hasNext()) {
			channels.setId(channelIdIterator.next());
			ChannelListResponse channelListResponse = null;
			long sleepTime = 5000;
			try {
				channelListResponse = channels.execute();
			} catch (Exception e) {
				// retry max 3 times.
				int tryCount = 0;
				int maxRetryTime = 3;
				while (true) {
					try {
						tryCount++;
						System.out.println("**Error occurs, retry time: " + tryCount + "...");
						Thread.sleep(sleepTime);
						channelListResponse = channels.execute();
					} catch (Exception e1) {
						// Double the waiting time if request failed.
						sleepTime = sleepTime * 2;
						// Set maximum waiting time to 1 minute.
						if (sleepTime >= 60000) {
							sleepTime = 60000;
						}
						// If failed 3 times, throw the exception.
						if (tryCount >= maxRetryTime) {
							throw e1;
						}
					}
				}
			}
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
	}

	// A small function that parse the channel ID string
	private String authorChannelIdFormat(String originalString) {
		return originalString.split("=")[1].replace("}", "");
	}

	// This is a transfer function that making a hashSet to a CSV formatted
	// string, which can be used as inputs for the YouTube Data API.
	// However, the result is a long CSV string, which requires further
	// process. The next process step can be found below this method.
	private String hashSetToCSV(LinkedHashSet<String> idSet) {
		StringBuilder idStringBuilder = new StringBuilder();
		Iterator<String> setIterator = idSet.iterator();
		while (setIterator.hasNext()) {
			idStringBuilder.append(setIterator.next() + ",");
		}
		// Dollar is the symbol of the end of a string. The last time the string
		// builder append a value, there is an extra comma at the end of the
		// string, which should be removed.
		return idStringBuilder.toString().replaceAll(",$", "");
	}

	// YouTube data API only accept less than 50 length's CSV string, so a
	// splitter is needed.
	// In this function, we simply split a long CSV string into smaller blocks,
	// and each block contains maximum 50 values in it.
	private ArrayList<String> csvSplitter(String csvString) {
		ArrayList<String> splittedCSVString = new ArrayList<String>();
		int numberPerChunk = 50; // Split by 50.
		String str = new String();
		// get the index of #50 comma.
		int position = ordinalIndexOf(csvString, ",", numberPerChunk - 1);
		while (position != -1) {
			str = csvString.substring(0, position);
			csvString = csvString.substring(position + 1);
			splittedCSVString.add(str);
			position = ordinalIndexOf(csvString, ",", numberPerChunk - 1);
		}
		splittedCSVString.add(csvString);
		// The returned value includes a series of string, and each of them
		// contains no more than 50 IDs (or more often, contains exactly 50
		// IDs).
		return splittedCSVString;
	}

	// Assistance subroutine for helping the splitter above.
	private int ordinalIndexOf(String string, String subString, int index) {
		int position = string.indexOf(subString, 0);
		while (index-- > 0 && position != -1) {
			position = string.indexOf(subString, position + 1);
		}
		return position;
	}

}
