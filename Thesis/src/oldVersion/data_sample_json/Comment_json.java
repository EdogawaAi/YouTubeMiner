package data_sample_json;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.auth.Auth;
import com.google.api.services.youtube.model.CommentThreadListResponse;
import com.google.common.collect.Lists;

public class Comment_json {

	public static YouTube youtube;

	public static void main(String[] args) {

		List<String> scopes = Lists.newArrayList("https://www.googleapis.com/auth/youtube.force-ssl");
		try {
			Credential credential = Auth.authorize(scopes, "commentthreads");

			youtube = new YouTube.Builder(Auth.HTTP_TRANSPORT, Auth.JSON_FACTORY, credential)
					.setApplicationName("youtube-cmdline-commentthreads-info").build();

			String videoId = getVideoId();

			// Call the YouTube Data API's commentThreads.list method to
			// retrieve video comment threads.
			YouTube.CommentThreads.List videoCommentsList = youtube.commentThreads().list("snippet,replies")
					.setVideoId(videoId).setTextFormat("plainText").setMaxResults((long) 50);
			CommentThreadListResponse videoCommentsListResponse = videoCommentsList.execute();

			 System.out.println(videoCommentsListResponse.toPrettyString());


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
}
