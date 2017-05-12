package video_category_discoveror;

import java.io.IOException;
import java.util.ArrayList;
import com.google.api.services.youtube.YouTube;

import data_collector_ver4.YouTubeAuth;

public class Test {

	public static void main(String[] args) throws IOException {
		YouTubeAuth yAuth = new YouTubeAuth();
		YouTube youtube = yAuth.getYouTube();
		String apiKey = yAuth.getApiKey();
		VideoCategoryEnumerate vc = new VideoCategoryEnumerate(youtube, apiKey);

		vc.getCategoryID();
	}

}
