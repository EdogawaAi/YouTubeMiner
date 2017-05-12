package test;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.YouTube.Search;
import com.google.api.services.youtube.auth.Auth;
import com.google.api.services.youtube.model.ResourceId;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
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

/**
 * Print a list of videos matching a search term.
 *
 * @author Jeremy Walker
 */
public class SearchTest {

	/**
	 * Define a global variable that identifies the name of a file that contains
	 * the developer's API key.
	 */
	private static final String PROPERTIES_FILENAME = "youtube.properties";

	private static final long NUMBER_OF_VIDEOS_RETURNED = 10;

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
			}).setApplicationName("youtube-cmdline-search").build();

			// Prompt the user to enter a query term.
			String queryTerm = getInputQuery();

			// Define the API request for retrieving search results.
			YouTube.Search.List search = youtube.search().list("id,snippet");

			// Set your developer key from the {{ Google Cloud Console }} for
			// non-authenticated requests. See:
			// {{ https://cloud.google.com/console }}
			String apiKey = properties.getProperty("youtube.apikey");
			search.setKey(apiKey);
			search.setQ(queryTerm);

			// Restrict the search results to only include videos. See:
			// https://developers.google.com/youtube/v3/docs/search/list#type
			search.setType("video");

			// To increase efficiency, only retrieve the fields that the
			// application uses.
			// search.setFields(
			// "items(id/kind,id/videoId,snippet/title,snippet/thumbnails/default/url),
			// nextPageToken, pageInfo, prevPageToken");
			search.setMaxResults(NUMBER_OF_VIDEOS_RETURNED);

			// Call the API and print results.
			SearchListResponse searchResponse = search.execute();

			// Get token.
			String pageToken = searchResponse.getNextPageToken();
			// System.out.println("Next pageToken:" + pageToken);
			// String prePageToken = searchResponse.getPrevPageToken();
			// System.out.println("Prev pageToken:" + prePageToken);

			System.out.println("Total result:" + searchResponse.getPageInfo().getTotalResults());

			File output = new File("SearchResult.txt");
			FileWriter writer = new FileWriter(output);
			
			int outCount = 0;
			List<SearchResult> searchResultList = searchResponse.getItems();
			if (searchResultList != null) {
				prettyPrint(searchResultList.iterator(), queryTerm, writer);
			}
			outCount++;

			// Keep searching until there's no result left.
			while ((pageToken != null) && outCount < 2) {
				search.setPageToken(pageToken);
				searchResponse = search.execute();
				pageToken = searchResponse.getNextPageToken();
				// System.out.println("Next pageToken:" + pageToken);
				searchResultList = searchResponse.getItems();
				if (searchResultList != null) {
					prettyPrint(searchResultList.iterator(), queryTerm, writer);
				}
				outCount++;
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
	 * Prompt the user to enter a query term and return the user-specified term.
	 */
	private static String getInputQuery() throws IOException {

		String inputQuery = "";

		System.out.print("Please enter a search term: ");
		BufferedReader bReader = new BufferedReader(new InputStreamReader(System.in));
		inputQuery = bReader.readLine();

		if (inputQuery.length() < 1) {
			// Use the string "YouTube Developers Live" as a default.
			inputQuery = "YouTube Developers Live";
		}
		return inputQuery;
	}

	/*
	 * Prints out all results in the Iterator. For each result, print the title,
	 * video ID, and thumbnail.
	 *
	 * @param iteratorSearchResults Iterator of SearchResults to print
	 *
	 * @param query Search query (String)
	 */
	private static void prettyPrint(Iterator<SearchResult> iteratorSearchResults, String query, FileWriter writer) throws IOException {

		// System.out.println("\n=============================================================");
		// System.out.println(
		// " First " + (NUMBER_OF_VIDEOS_RETURNED + count) + " videos for search
		// on \"" + query + "\".");
		// System.out.println("=============================================================\n");

		if (!iteratorSearchResults.hasNext()) {
			System.out.println(" There aren't any results for your query.");
		}

		while (iteratorSearchResults.hasNext()) {

			SearchResult singleVideo = iteratorSearchResults.next();
			ResourceId rId = singleVideo.getId();

			// Confirm that the result represents a video. Otherwise, the
			// item will not contain a video ID.
			if (rId.getKind().equals("youtube#video")) {

				JSONObject obj = new JSONObject();

				obj.append("kind", singleVideo.getKind());
				obj.append("etag", singleVideo.getEtag());
				obj.append("id", new JSONObject().append("kind", rId.getKind()).append("videoId", rId.getVideoId())
						.append("channelId", rId.getChannelId()).append("playlistID", rId.getPlaylistId()));
				obj.append("snippet",
						new JSONObject().append("publishAt", singleVideo.getSnippet().getPublishedAt())
								.append("description", singleVideo.getSnippet().getDescription())
								.append("channelId", singleVideo.getSnippet().getChannelId())
								.append("title", singleVideo.getSnippet().getTitle()));
				obj.append("channelTitle", singleVideo.getSnippet().getChannelTitle());
				obj.append("liveBroadcastContent", singleVideo.getSnippet().getLiveBroadcastContent());

				Gson myGson = new GsonBuilder().setPrettyPrinting().create();
				JsonParser jParser = new JsonParser();
				JsonElement jElement = jParser.parse(obj.toString());
				String prettyJsonString = myGson.toJson(jElement);
				
				writer.append(prettyJsonString);
				
				System.out.println(prettyJsonString);
			}
		}
	}
}
