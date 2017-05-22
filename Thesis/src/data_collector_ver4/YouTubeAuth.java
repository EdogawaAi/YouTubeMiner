package data_collector_ver4;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.YouTube.Search;

import youtube.auth.Auth;

/**
 * This class is for maintaining the authorization of YouTube instances. 
 * The template is provided by YouTube Data API documentation sample.
 * I modified it somewhat so that it fits our requirement. 
 * 
 *  @author Tian
 *
 */
public class YouTubeAuth {

	private final String PROPERTIES_FILENAME = "youtube.properties";
	private Properties properties = new Properties();
	private YouTube youtube;
	private String apiKey;

	public YouTubeAuth() {
		try {
			InputStream in = Search.class.getResourceAsStream("/" + PROPERTIES_FILENAME);
			properties.load(in);

		} catch (IOException e) {
			System.err.println(
					"There was an error reading " + PROPERTIES_FILENAME + ": " + e.getCause() + " : " + e.getMessage());
			System.exit(1);
		}

		youtube = new YouTube.Builder(Auth.HTTP_TRANSPORT, Auth.JSON_FACTORY, new HttpRequestInitializer() {
			@Override
			public void initialize(HttpRequest request) throws IOException {
			}
		}).setApplicationName("youtube-cmdline").build();
		apiKey = properties.getProperty("youtube.apikey");
	}

	public YouTube getYouTube() {
		return youtube;
	}

	public String getApiKey() {
		return apiKey;
	}
}
