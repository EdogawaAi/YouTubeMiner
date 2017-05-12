package test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.auth.Auth;
import com.google.api.services.youtube.model.VideoCategoryListResponse;
import com.google.common.collect.Lists;

public class VideoCategories {
	public static YouTube youtube;

	public static void main(String[] args) throws IOException {
		List<String> scopes = Lists.newArrayList("https://www.googleapis.com/auth/youtube.force-ssl");
		Credential credential = Auth.authorize(scopes, "videoCategories");

		youtube = new YouTube.Builder(Auth.HTTP_TRANSPORT, Auth.JSON_FACTORY, credential)
				.setApplicationName("youtube-cmdline-videocategories-info").build();

		String videoId = getVideoId();
		YouTube.VideoCategories.List videoCategories = youtube.videoCategories().list("snippet");
		videoCategories.setId(videoId);
		VideoCategoryListResponse videoCategoryListResponse = videoCategories.execute();
		System.out.println(videoCategoryListResponse.toPrettyString());
	}

	private static String getVideoId() throws IOException {

		String videoId = "";

		System.out.print("Please enter a video CATEGORY id: ");
		BufferedReader bReader = new BufferedReader(new InputStreamReader(System.in));
		videoId = bReader.readLine();

		return videoId;
	}
}
