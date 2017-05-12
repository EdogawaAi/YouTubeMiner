package test;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.YouTube.Search;
import com.google.api.services.youtube.auth.Auth;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoContentDetails;
import com.google.api.services.youtube.model.VideoListResponse;
import com.google.api.services.youtube.model.VideoSnippet;
import com.google.api.services.youtube.model.VideoStatistics;
import com.google.api.services.youtube.model.VideoStatus;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import org.json.JSONObject;

public class VideoTest {

	/**
	 * Define a global variable that identifies the name of a file that contains
	 * the developer's API key.
	 */
	private static final String PROPERTIES_FILENAME = "youtube.properties";

	/**
	 * Define a global instance of a Youtube object, which will be used to make
	 * YouTube Data API requests.
	 */
	private static YouTube youtube;

	/**
	 * Initialize a YouTube object to search for videos on YouTube. Then display
	 * the name and thumbnail image of each video in the result set.
	 *
	 * @param args
	 *            command line args.
	 */
	public static void main(String[] args) {
		// Read the developer key from the properties file.
		Properties properties = new Properties();
		try {
			InputStream in = Search.class.getResourceAsStream("/" + PROPERTIES_FILENAME);
			properties.load(in);

		} catch (IOException e) {
			System.err.println(
					"There was an error reading " + PROPERTIES_FILENAME + ": " + e.getCause() + " : " + e.getMessage());
			System.exit(1);
		}

		try {
			// This object is used to make YouTube Data API requests. The last
			// argument is required, but since we don't need anything
			// initialized when the HttpRequest is initialized, we override
			// the interface and provide a no-op function.
			youtube = new YouTube.Builder(Auth.HTTP_TRANSPORT, Auth.JSON_FACTORY, new HttpRequestInitializer() {
				public void initialize(HttpRequest request) throws IOException {
				}
			}).setApplicationName("youtube-cmdline-video").build();

			// Prompt the user to enter an input id.
			String inputId = getInputId();

			// Define the API request for retrieving search results.
			YouTube.Videos.List videos = youtube.videos().list("id,snippet,contentDetails,status,statistics");

			// Set your developer key from the {{ Google Cloud Console }} for
			// non-authenticated requests. See:
			// {{ https://cloud.google.com/console }}
			String apiKey = properties.getProperty("youtube.apikey");
			videos.setKey(apiKey);
			videos.setId(inputId);

			// Call the API and print results.
			VideoListResponse videoResponse = videos.execute();

			System.out.println("Total result:" + videoResponse.getPageInfo().getTotalResults());

			File output = new File("VideoReuslt.txt");
			FileWriter writer = new FileWriter(output);
			
			List<Video> videoResultList = videoResponse.getItems();
			if (videoResultList != null) {
				prettyPrint(videoResultList.iterator(), inputId, writer);
			}
			
			writer.close();

		} catch (GoogleJsonResponseException e) {
			System.err.println(
					"There was a service error: " + e.getDetails().getCode() + " : " + e.getDetails().getMessage());
		} catch (IOException e) {
			System.err.println("There was an IO error: " + e.getCause() + " : " + e.getMessage());
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	/*
	 * Prompt the user to enter a video id and return the user-specified value.
	 */
	private static String getInputId() throws IOException {

		String inputQuery = "";

		System.out.print("Please enter video id: ");
		BufferedReader bReader = new BufferedReader(new InputStreamReader(System.in));
		inputQuery = bReader.readLine();

		if (inputQuery.length() < 1) {
			// Use the string "YouTube Developers Live" as a default.
			inputQuery = "YouTube Developers Live";
		}
		return inputQuery;
	}

	private static void prettyPrint(Iterator<Video> iterator, String query, FileWriter writer) throws IOException {

		if (!iterator.hasNext()) {
			System.out.println(" There aren't any results for your query.");
		}

		while (iterator.hasNext()) {

			Video theVideo = iterator.next();
			System.out.println(theVideo.getId() + ", " + theVideo.getSnippet().getTitle());

			VideoSnippet snippet = theVideo.getSnippet();
			VideoContentDetails cDetails = theVideo.getContentDetails();
			VideoStatus vStatus = theVideo.getStatus();
			VideoStatistics vStatistics = theVideo.getStatistics();

			// Build JSON object that store the video's information.
			JSONObject obj = new JSONObject();
			obj.append("id", theVideo.getId());
			obj.append("snippet", new JSONObject().append("publishAt", snippet.getPublishedAt())
					.append("channelId", snippet.getChannelId()).append("title", snippet.getTitle())
					.append("description", snippet.getDescription()).append("channelTitle", snippet.getChannelTitle())
					.append("tags", snippet.getTags()).append("categoryId", snippet.getCategoryId())
					.append("liveBroadcastContent", snippet.getLiveBroadcastContent())
					.append("defaultLanguage", snippet.getDefaultLanguage())
					.append("localized",
							new JSONObject().append("title", snippet.getLocalized().getTitle()).append("description",
									snippet.getLocalized().getDescription()))
					.append("defaultAudioLanguage", snippet.getDefaultAudioLanguage()));
			obj.append("contentDetails",
					new JSONObject().append("duration", cDetails.getDuration())
							.append("dimension", cDetails.getDimension()).append("definition", cDetails.getDefinition())
							.append("caption", cDetails.getCaption())
							.append("licensedContent", cDetails.getLicensedContent()));
			obj.append("status", new JSONObject().append("uploadStatus", vStatus.getUploadStatus())
					.append("failureReason", vStatus.getFailureReason())
					.append("rejectionReason", vStatus.getRejectionReason())
					.append("privacyStatus", vStatus.getPrivacyStatus()).append("publishAt", vStatus.getPublishAt())
					.append("license", vStatus.getLicense()).append("embeddable", vStatus.getEmbeddable())
					.append("publicStatsViewable", vStatus.getPublicStatsViewable()));
			obj.append("statistics",
					new JSONObject().append("viewCount", vStatistics.getViewCount())
							.append("likeCount", vStatistics.getLikeCount())
							.append("dislikeCount", vStatistics.getDislikeCount())
							.append("favoriteCount", vStatistics.getFavoriteCount())
							.append("commentCount", vStatistics.getCommentCount()));

			// Print out in formatted json.
			Gson myGson = new GsonBuilder().setPrettyPrinting().create();
			JsonParser jParser = new JsonParser();
			JsonElement jElement = jParser.parse(obj.toString());
			String prettyJsonString = myGson.toJson(jElement);
			System.out.println(prettyJsonString);
			
			writer.append(prettyJsonString);
		}
	}
}
