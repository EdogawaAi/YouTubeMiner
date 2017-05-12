package test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import org.json.JSONObject;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.auth.Auth;
import com.google.api.services.youtube.model.Comment;
import com.google.api.services.youtube.model.CommentSnippet;
import com.google.api.services.youtube.model.CommentThread;
import com.google.api.services.youtube.model.CommentThreadListResponse;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class CommentTest {

	public static YouTube youtube;

	public static void main(String[] args) {

		List<String> scopes = Lists.newArrayList("https://www.googleapis.com/auth/youtube.force-ssl");
		try {
			Credential credential = Auth.authorize(scopes, "commentthreads");

			youtube = new YouTube.Builder(Auth.HTTP_TRANSPORT, Auth.JSON_FACTORY, credential)
					.setApplicationName("youtube-cmdline-commentthreads-info").build();

			String videoId = getVideoId();
			CommentSnippet snippet;

			// Call the YouTube Data API's commentThreads.list method to
			// retrieve video comment threads.
			YouTube.CommentThreads.List videoCommentsList = youtube.commentThreads().list("snippet,replies")
					.setVideoId(videoId).setTextFormat("plainText").setMaxResults((long) 10);
			CommentThreadListResponse videoCommentsListResponse = videoCommentsList.execute();

			List<CommentThread> videoComments = videoCommentsListResponse.getItems();
			String pageToken = videoCommentsListResponse.getNextPageToken();
			// System.out.println(pageToken);

			videoCommentsList.setPageToken(pageToken);
			videoCommentsListResponse = videoCommentsList.execute();

			File output = new File("CommentResult.txt");
			FileWriter writer = new FileWriter(output);

			if (videoComments.isEmpty()) {
				System.out.println("Can't get video comments.");
			} else {
				// Print information from the API response.
				for (CommentThread videoComment : videoComments) {

					snippet = videoComment.getSnippet().getTopLevelComment().getSnippet();

					JSONObject obj = new JSONObject();
					obj.append("id", videoComment.getId()).append("authorDisplayName", snippet.getAuthorDisplayName())
							.append("parentId", snippet.getParentId()).append("text", snippet.getTextDisplay());

					long replyCount = videoComment.getSnippet().getTotalReplyCount().longValue();
					if (replyCount != 0) {
						List<Comment> replies = videoComment.getReplies().getComments();
						for (Comment reply : replies) {
							JSONObject replyObj = new JSONObject();
							replyObj.append("id", reply.getId())
									.append("authorDisplayName", reply.getSnippet().getAuthorDisplayName())
									.append("parentId", reply.getSnippet().getParentId())
									.append("text", reply.getSnippet().getTextDisplay());
							obj.append("replies", replyObj);
						}
					}

					prettyPrint(obj, writer);
				}
			}

			while (pageToken != null) {
				videoCommentsList.setPageToken(pageToken);
				videoCommentsListResponse = videoCommentsList.execute();

				videoComments = videoCommentsListResponse.getItems();
				pageToken = videoCommentsListResponse.getNextPageToken();
				// System.out.println(pageToken);

				if (videoComments.isEmpty()) {
					System.out.println("Can't get video comments.");
				} else {
					// Print information from the API response.
					// System.out.println("\n================== Returned Video
					// Comments ==================\n");
					for (CommentThread videoComment : videoComments) {

						snippet = videoComment.getSnippet().getTopLevelComment().getSnippet();

						JSONObject obj = new JSONObject();
						obj.append("id", videoComment.getId())
								.append("authorDisplayName", snippet.getAuthorDisplayName())
								.append("parentId", snippet.getParentId()).append("text", snippet.getTextDisplay());

						long replyCount = videoComment.getSnippet().getTotalReplyCount().longValue();
						if (replyCount != 0) {
							List<Comment> replies = videoComment.getReplies().getComments();
							for (Comment reply : replies) {
								JSONObject replyObj = new JSONObject();
								replyObj.append("id", reply.getId())
										.append("authorDisplayName", reply.getSnippet().getAuthorDisplayName())
										.append("parentId", reply.getSnippet().getParentId())
										.append("text", reply.getSnippet().getTextDisplay());
								obj.append("replies", replyObj);
							}
						}

						prettyPrint(obj, writer);
					}
				}
			}

			writer.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/*
	 * Prompt the user to enter a video ID. Then return the ID.
	 */
	private static String getVideoId() throws IOException {

		String videoId = "";

		System.out.print("Please enter a video id: ");
		BufferedReader bReader = new BufferedReader(new InputStreamReader(System.in));
		videoId = bReader.readLine();

		return videoId;
	}

	private static void prettyPrint(JSONObject obj, FileWriter writer) throws IOException {
		Gson myGson = new GsonBuilder().setPrettyPrinting().create();
		JsonParser jParser = new JsonParser();
		JsonElement jElement = jParser.parse(obj.toString());
		String prettyJsonString = myGson.toJson(jElement);
		System.out.println(prettyJsonString);

		writer.append(prettyJsonString);
	}
}
