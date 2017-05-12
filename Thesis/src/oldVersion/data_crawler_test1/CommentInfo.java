package data_crawler_test1;

import java.io.IOException;

import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.CommentThreadListResponse;

public class CommentInfo {

	public String commentInfo(String videoId, YouTube youtube, String apiKey) throws IOException {

		YouTube.CommentThreads.List videoCommentsList = youtube.commentThreads().list("snippet,replies").setKey(apiKey)
				.setVideoId(videoId).setTextFormat("plainText").setMaxResults((long) 50).setFields(
						"items(replies(comments(id,snippet(authorChannelId,likeCount,parentId,publishedAt,textDisplay,updatedAt))),snippet(topLevelComment(id,snippet(authorChannelId,likeCount,publishedAt,textDisplay,updatedAt)),totalReplyCount,videoId))");
		CommentThreadListResponse videoCommentsListResponse = videoCommentsList.execute();

		return videoCommentsListResponse.toString();
	}

}
