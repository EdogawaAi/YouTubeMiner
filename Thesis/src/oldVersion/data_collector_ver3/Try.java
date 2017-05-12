package data_collector_ver3;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.json.JSONObject;

import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Comment;
import com.google.api.services.youtube.model.CommentThread;
import com.google.api.services.youtube.model.CommentThreadListResponse;

public class Try {
	public static void main(String[] args) throws IOException, InterruptedException {
		YouTubeAuth auth = new YouTubeAuth();
		YouTube youtube = auth.getYouTube();
		String apiKey = auth.getApiKey();
		String videoId = new String();
		videoId = "YD4rHgX1z_w";
		commentSubroutineTest(youtube, apiKey, videoId);
		System.out.println("succeed.");
	}

	// private static void myPrinter(LinkedHashSet<String> theSet) {
	// Iterator<String> it = theSet.iterator();
	// while (it.hasNext()) {
	// System.out.println("==>" + it.next());
	// }
	// }

	private static void commentSubroutineTest(YouTube youtube, String apiKey, String videoId) throws IOException {

		try {
			YouTube.CommentThreads.List videoCommentsList = youtube.commentThreads().list("snippet,replies")
					.setKey(apiKey).setVideoId(videoId).setTextFormat("plainText").setMaxResults((long) 100).setFields(
							"items(replies(comments(id,snippet(authorChannelId,likeCount,parentId,publishedAt,textDisplay,updatedAt))),"
									+ "snippet(topLevelComment(id,snippet(authorChannelId,likeCount,publishedAt,textDisplay,updatedAt)),"
									+ "totalReplyCount,videoId)),nextPageToken");
			CommentThreadListResponse videoCommentsListResponse = videoCommentsList.execute();
			List<CommentThread> commentThreadList = videoCommentsListResponse.getItems();
			System.out.println(commentThreadList.size());
			// Collect every pages.
			while (videoCommentsListResponse.getNextPageToken() != null) {
				videoCommentsListResponse = videoCommentsList.setPageToken(videoCommentsListResponse.getNextPageToken())
						.execute();
				commentThreadList.addAll(videoCommentsListResponse.getItems());
			}

			// Start iterator.
			Iterator<CommentThread> iterComment = commentThreadList.iterator();
			while (iterComment.hasNext()) {
				CommentThread videoComment = iterComment.next();
				Comment topLevelComment = videoComment.getSnippet().getTopLevelComment();

				// avoid null author IDs.
				if (!topLevelComment.getSnippet().getAuthorChannelId().toString().isEmpty()) {
					// count++;

					JSONObject topLevelCommentTable = new JSONObject().put("TLCommentId", videoComment.getId())
							.put("VideoId", videoComment.getSnippet().getVideoId())
							.put("ChannelId",
									authorChannelIdFormat(topLevelComment.getSnippet().getAuthorChannelId().toString()))
							.put("TLCommentLikeCount", topLevelComment.getSnippet().getLikeCount())
							.put("TLCommentPublishedAt", topLevelComment.getSnippet().getPublishedAt().toString())
							.put("TLCommentUpdatedAt", topLevelComment.getSnippet().getUpdatedAt().toString())
							.put("TLCommentTextDisplay", topLevelComment.getSnippet().getTextDisplay())
							.put("TotalReplyCount", videoComment.getSnippet().getTotalReplyCount());
					System.out.println("tlc table OK. The length is: " + topLevelCommentTable.length());

					// If reply exists, add them as well.
					if (videoComment.getSnippet().getTotalReplyCount() != 0) {
						List<Comment> replies = videoComment.getReplies().getComments();
						Iterator<Comment> iterReply = replies.iterator();
						while (iterReply.hasNext()) {
							Comment reply = iterReply.next();
							if (!reply.getSnippet().getAuthorChannelId().toString().isEmpty()) {
								reply.getSnippet().getAuthorChannelId().toString();

								JSONObject replyTable = new JSONObject().put("ReplyId", reply.getId())
										.put("TLCommentId", reply.getSnippet().getParentId())
										.put("ChannelId",
												authorChannelIdFormat(
														reply.getSnippet().getAuthorChannelId().toString()))
										.put("ReplyLikeCount", reply.getSnippet().getLikeCount())
										.put("ReplyPublishedAt", reply.getSnippet().getPublishedAt().toString())
										.put("ReplyUpdatedAt", reply.getSnippet().getUpdatedAt().toString())
										.put("ReplyTextDisplay", reply.getSnippet().getTextDisplay());

								System.out.println("reply table OK. The length is: " + replyTable.length());

								// Save the author's channel id to
								// channelIdList.
							}
						}
					}

				} else {
					String str = topLevelComment.getSnippet().getAuthorDisplayName();
					String googleplus = topLevelComment.getSnippet().getAuthorGoogleplusProfileUrl();
					System.out.println("The author \"" + str + "\"'s channel ID not found." + "\n\tThe google+ url is: "
							+ googleplus);
				}
			}
		} catch (com.google.api.client.googleapis.json.GoogleJsonResponseException e) {
			if (e.getStatusCode() != 403) {
				if (e.getStatusCode() == 404) {
					System.out.println("**No video specified.**");
				} else if (e.getStatusCode() == 400) {
					System.out.println("error 400.");
					throw e;
				} else {
					System.out.println("error!");
					throw e;
				}
			}
		} catch (NullPointerException e) {
			// TODO: handle exception
			throw e;
		}
	}

	private static String authorChannelIdFormat(String originalString) {
		return originalString.split("=")[1].replace("}", "");
	}
}
