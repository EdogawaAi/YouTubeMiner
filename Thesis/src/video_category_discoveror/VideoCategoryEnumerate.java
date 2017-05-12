package video_category_discoveror;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.VideoCategory;
import com.google.api.services.youtube.model.VideoCategoryListResponse;

public class VideoCategoryEnumerate {

	private YouTube youtube;
	private String apiKey;

	public VideoCategoryEnumerate(YouTube youtube, String apiKey) {
		this.youtube = youtube;
		this.apiKey = apiKey;
	}

	public ArrayList<String> getCategoryID() throws IOException {
		ArrayList<String> result = new ArrayList<String>();

		YouTube.VideoCategories.List cList = youtube.videoCategories().list("snippet").setKey(apiKey);
		int intID = 0;
		int emptyCount = 0;
		for (intID = 0; emptyCount < 15; intID++) {
			String sID = String.valueOf(intID);
			cList.setId(sID);
			VideoCategoryListResponse cResponse = cList.execute();

			if (cResponse.getItems().isEmpty()) {
				emptyCount++;
			} else {
				emptyCount = 0; // Reset empty counter.
				Iterator<VideoCategory> itor = cResponse.getItems().iterator();
				while (itor.hasNext()) {
					VideoCategory vCategory = itor.next();
					System.out.println("Category ID: " + sID + "\tCategory Title: " + vCategory.getSnippet().getTitle()
							+ "\tAssignable: " + vCategory.getSnippet().getAssignable());
					result.add(vCategory.getId());
				}
			}

		}

		// **After verifying, the categories with ID "18", "21", "38", and "42"
		// are empty, i.e., no videos are assigned with these categories. Thus
		// they should be removed from the set.
		result.remove("18");
		result.remove("21");
		result.remove("38");
		result.remove("42");

		return result;
	}

}
