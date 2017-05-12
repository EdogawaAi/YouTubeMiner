package test;

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

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.YouTube.Search;
import com.google.api.services.youtube.auth.Auth;
import com.google.api.services.youtube.model.Channel;
import com.google.api.services.youtube.model.ChannelContentDetails;
import com.google.api.services.youtube.model.ChannelListResponse;
import com.google.api.services.youtube.model.ChannelSnippet;
import com.google.api.services.youtube.model.ChannelStatistics;
import com.google.api.services.youtube.model.ChannelStatus;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class ChannelTest {
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
			}).setApplicationName("youtube-cmdline-channel-info").build();

			// Prompt the user to enter an input id.
			String inputId = getInputId();

			// Define the API request for retrieving search results.
			YouTube.Channels.List channels = youtube.channels().list("id,snippet,statistics,status,contentDetails");

			// Set your developer key from the {{ Google Cloud Console }} for
			// non-authenticated requests. See:
			// {{ https://cloud.google.com/console }}
			String apiKey = properties.getProperty("youtube.apikey");

			channels.setKey(apiKey);
			channels.setId(inputId);

			// Call the API and print results.
			ChannelListResponse channelResponse = channels.execute();

			// System.out.println("Total result:" +
			// videoResponse.getPageInfo().getTotalResults());

			File output = new File("ChannelReuslt.txt");
			FileWriter writer = new FileWriter(output);

			List<Channel> channelResultList = channelResponse.getItems();
			if (channelResultList != null) {
				prettyPrint(channelResultList.iterator(), inputId, writer);
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

		System.out.print("Please enter channel id: ");
		BufferedReader bReader = new BufferedReader(new InputStreamReader(System.in));
		inputQuery = bReader.readLine();

		if (inputQuery.length() < 1) {
			// Use the string "YouTube Developers Live" as a default.
			inputQuery = "YouTube Developers Live";
		}
		return inputQuery;
	}

	private static void prettyPrint(Iterator<Channel> iterator, String inputId, FileWriter writer) throws IOException {
		if (!iterator.hasNext()) {
			System.out.println(" There aren't any results for your query.");
		}

		while (iterator.hasNext()) {
			Channel theChannel = iterator.next();
			System.out.println(theChannel.getId() + ", " + theChannel.getSnippet().getTitle());

			ChannelSnippet snippet = theChannel.getSnippet();
			ChannelStatus cStatus = theChannel.getStatus();
			ChannelStatistics cStatistics = theChannel.getStatistics();
			ChannelContentDetails cDetails = theChannel.getContentDetails();

			JSONObject obj = new JSONObject();
			obj.append("id", theChannel.getId());
			obj.append("snippet",
					new JSONObject().append("title", snippet.getTitle()).append("description", snippet.getDescription())
							.append("customUrl", snippet.getCustomUrl()).append("publishAt", snippet.getPublishedAt())
							.append("defaultLanguage", snippet.getDefaultLanguage())
							.append("localized",
									new JSONObject().append("title", snippet.getLocalized().getTitle())
											.append("description", snippet.getLocalized().getDescription()))
					.append("country", snippet.getCountry()));
			obj.append("status",
					new JSONObject().append("privacyStatus", cStatus.getPrivacyStatus())
							.append("isLinked", cStatus.getIsLinked())
							.append("longUploadsStatus", cStatus.getLongUploadsStatus()));
			obj.append("statistics",
					new JSONObject().append("viewCount", cStatistics.getViewCount())
							.append("commentCount", cStatistics.getCommentCount())
							.append("subscriberCount", cStatistics.getSubscriberCount())
							.append("hiddenSubcsriberCount", cStatistics.getHiddenSubscriberCount())
							.append("videoCount", cStatistics.getVideoCount()));
			obj.append("contentDetails",
					new JSONObject()
							.append("relatedPlaylists",
									new JSONObject().append("likes", cDetails.getRelatedPlaylists().getLikes())
											.append("favorites", cDetails.getRelatedPlaylists().getFavorites())
											.append("uploads", cDetails.getRelatedPlaylists().getUploads())
											.append("watchHistory", cDetails.getRelatedPlaylists().getWatchHistory())
											.append("watchLater", cDetails.getRelatedPlaylists().getWatchLater()))
							.append("googlePlusUserId", cDetails.getGooglePlusUserId()));

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
