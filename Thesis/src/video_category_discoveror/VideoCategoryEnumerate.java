package video_category_discoveror;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.VideoCategory;
import com.google.api.services.youtube.model.VideoCategoryListResponse;

/**
 * 
 * This class gathers ALL youtube categories IDs.
 * 
 * In YouTube, the ID of each category is a numerical string. Since YouTube
 * doesn't mention how long the string is, we are trying to find out all the
 * numbers that are used as the category IDs.
 * 
 * This class is mostly used in the video ID discovery process, so that we can
 * ensure the seed IDs are coming from EVERY category.
 * 
 * @author Tian
 *
 */
public class VideoCategoryEnumerate {

	// Attributes, used for setting properties of the API.
	private YouTube youtube;
	private String apiKey;

	public VideoCategoryEnumerate(YouTube youtube, String apiKey) {
		this.youtube = youtube;
		this.apiKey = apiKey;
	}

	// The major method in this class.
	// Basically, we specify the API requests and send them repeatly, until
	// there's no new category ID discovered.
	// The idea is, if there's no new ID found after 15 counts of a discovery of
	// an ID, then
	// most probably there's no any new ID afterward. For example, if an ID is
	// founded as "60", then if there's no ID between "60" and "75", we will
	// terminate the process and won't discover further.
	// As a matter of fact, no 3-digits ID has ever been found in our
	// experiment. At the beginning, we set the count '15' to a much larger one,
	// like '150', '1000'. What we discovered was that no category ID is longer
	// than 2-digit, and there's only small gap in between different category ID
	// numerics.
	// So, we decrease the count to '15' so that it can cover all possible IDs
	// as well as ensure that the process gets terminated in time.
	public ArrayList<String> getCategoryID() throws IOException {

		ArrayList<String> result = new ArrayList<String>(); // A result
															// container

		
		// Prepare an API call and execute it. 
		YouTube.VideoCategories.List cList = youtube.videoCategories().list("snippet").setKey(apiKey);
		
		int intID = 0;
		int emptyCount = 0;	// count how many numerics are empty after one ID discovered. 
		
		for (intID = 0; emptyCount < 15; intID++) {
			String sID = String.valueOf(intID);
			cList.setId(sID);	// Set a "potential" category ID to the API request. 
			
			VideoCategoryListResponse cResponse = cList.execute();

			if (cResponse.getItems().isEmpty()) {
				emptyCount++;	// If nothing returned, make an increment of the empty count.  
			} else {
				emptyCount = 0; // Reset empty counter.
				
				// The response result is a list. So we create a iterator to iterate it. 
				// Though there's only one item in that list.
				Iterator<VideoCategory> itor = cResponse.getItems().iterator();
				while (itor.hasNext()) {
					VideoCategory vCategory = itor.next();
					System.out.println("Category ID: " + sID + "\tCategory Title: " + vCategory.getSnippet().getTitle()
							+ "\tAssignable: " + vCategory.getSnippet().getAssignable());
					result.add(vCategory.getId());
				}
			}

		}

		// **After manually verifying, the categories with ID "18", "21", "38",
		// and "42" are empty, i.e., no videos are assigned with these
		// categories. So, they should be removed from the set.
		result.remove("18");
		result.remove("21");
		result.remove("38");
		result.remove("42");

		return result;
	}

}
