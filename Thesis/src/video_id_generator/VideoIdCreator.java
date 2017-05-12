package video_id_generator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;

import data_collector_ver4.MySQLAccess;
import video_category_discoveror.VideoCategoryEnumerate;

public class VideoIdCreator {

	private YouTube youtube;
	private String apiKey;

	// This attribute passes the initial seed videos' size.
	private int iniSeedSize;
	// Set the maximum seed size for each time expanding.
	private final int expSeedSize = 10000;
	private String order;
	// Page * maximumResult = Related video size.
	// i.e. 3 pages and 50 maximum results gives 150 result for each seed video
	// when searching for its related videos.
	private int page;
	private long maximumResult;

	// Set the path of where to write the result.
	private String filepath;

	/**
	 * Order can be: date, viewCount, rating, relevance, title (alphabetically)
	 * maximumResult cannot exceed 100.
	 * 
	 * @param youtube
	 * @param apiKey
	 * @param order
	 * @param page
	 * @param maximumResult
	 */
	public VideoIdCreator(YouTube youtube, String apiKey, String order, int seedSize, int page, long maximumResult,
			String filepath) {
		this.youtube = youtube;
		this.apiKey = apiKey;
		this.order = order;
		this.iniSeedSize = seedSize;
		this.page = page;
		this.maximumResult = maximumResult;
		this.filepath = filepath;
	}

	/**
	 * 
	 * @param expandTime
	 * @return
	 * @throws Exception
	 */
	public LinkedHashSet<String> videoIdSetCreate(int expandTime) throws Exception {

		// Create two "cursors".
		LinkedHashSet<String> currentResultSet = new LinkedHashSet<String>();
		LinkedHashSet<String> tempSeedSet = new LinkedHashSet<String>();

		// Initiate the start point.
		currentResultSet = videoIdSeed();
		tempSeedSet = currentResultSet;

		// Store the initial set of videoIDs.
		fileWrite(currentResultSet);
		dbWrite(currentResultSet);

		// If expandTime is -1, then go infinite loops. 
		if (expandTime != -1) {
			while (expandTime > 0) {
				System.out.println("Current seed size: " + tempSeedSet.size());
				System.out.println("Maximum seed size is set to: " + expSeedSize);
				System.out.println("--Start expanding...\n(Remaining expand time: " + expandTime + ")");
				expandTime--;
				tempSeedSet = videoIdExpand(tempSeedSet, currentResultSet);
				currentResultSet.addAll(tempSeedSet);
				System.out.println("--current result set size: " + currentResultSet.size());
				System.out.println("-----------------------------------------------");
				// Store the expanded set of videoIDs.
				fileWrite(tempSeedSet);
				dbWrite(tempSeedSet);
			}
		}else {
			while (true) {
				System.out.println("Current seed size: " + tempSeedSet.size());
				System.out.println("Maximum seed size is set to: " + expSeedSize);
				System.out.println("--Start expanding...\n(Remaining expand time: " + "\u221E" + ")");
				tempSeedSet = videoIdExpand(tempSeedSet, currentResultSet);
				currentResultSet.addAll(tempSeedSet);
				System.out.println("--current result set size: " + currentResultSet.size());
				System.out.println("-----------------------------------------------");
				// Store the expanded set of videoIDs.
				fileWrite(tempSeedSet);
				dbWrite(tempSeedSet);
			}
		}
		return currentResultSet;

	}

	// Use video category enumerate to ensure that the seed videos are from
	// distinct categories.
	private LinkedHashSet<String> videoIdSeed() throws IOException {

		// Get all the categories of YouTube videos.
		VideoCategoryEnumerate vCE = new VideoCategoryEnumerate(youtube, apiKey);
		ArrayList<String> vCList = vCE.getCategoryID();

		// Calculate how many videos should be selected from each category.
		int categorySize = vCList.size(); // Actually this is constant: 32

		int blockVideoNum = iniSeedSize / categorySize;
		int extraVideoNum = iniSeedSize % categorySize;

		System.out.println();
		System.out.println("blockVideoNum: " + blockVideoNum);
		System.out.println("extraVideoNum: " + extraVideoNum);
		LinkedHashSet<String> videoListSet = new LinkedHashSet<String>();

		Iterator<String> vCItor = vCList.iterator();
		while (vCItor.hasNext()) {
			String vCategory = vCItor.next();
			int nMaximumResult = blockVideoNum;
			if (extraVideoNum > 0) {
				nMaximumResult += 1;
				extraVideoNum -= 1;
			}
			System.out.println("-------------------------");
			System.out.println("Current Category: " + vCategory + "\tnew maximum result: " + nMaximumResult);
			YouTube.Search.List videoList = youtube.search().list("id").setKey(apiKey)
					.setFields("items/id/videoId,nextPageToken,pageInfo").setOrder(order).setType("video")
					.setMaxResults((long) nMaximumResult).setVideoCategoryId(vCategory);

			SearchListResponse videoListResponse = videoList.execute();
			List<SearchResult> videoResultList = videoListResponse.getItems();

			// while (videoListResponse.getNextPageToken() != null) {
			// videoListResponse =
			// videoList.setPageToken(videoListResponse.getNextPageToken()).execute();
			// videoResultList.addAll(videoListResponse.getItems());
			// System.out.println("Infinite loop...");
			// }

			if (videoResultList.isEmpty()) {
				System.out.println("Empty category in ID: " + vCategory);
			} else {
				Iterator<SearchResult> videoResultIter = videoResultList.iterator();
				while (videoResultIter.hasNext()) {
					SearchResult video = videoResultIter.next();
					System.out.println(video.toString());
					videoListSet.add(video.getId().getVideoId());
				}
			}
		}
		return videoListSet;
	}

	private LinkedHashSet<String> videoIdExpand(LinkedHashSet<String> seedSet, LinkedHashSet<String> currentResultSet)
			throws Exception {

		// Create a container that stores the result for returning.
		LinkedHashSet<String> expandedSet = new LinkedHashSet<String>();

		int page = this.page;

		YouTube.Search.List videoList = youtube.search().list("id").setKey(apiKey)
				.setFields("items/id/videoId,nextPageToken,pageInfo").setType("video").setMaxResults(maximumResult);

		// Iterate the seed set, and for each of them, generate its related
		// video IDs.
		Iterator<String> seedSetIter = seedSet.iterator();

		int cnt = 0;
		long sleepTime = 10000;
		// Set a size limit to the the expand seeds
		long seedLimit = expSeedSize;

		// Calculate total time usage.
		long startTime = System.currentTimeMillis();
		long endTime = startTime;
		double timeDiff = 0;
		// Calculate API execution time usage.
		long exeStartTime = System.currentTimeMillis();
		long exeEndTime = exeStartTime;
		double exeTimeDiff = 0;

		while (seedSetIter.hasNext() && (seedLimit > 0)) {
			cnt++;
			seedLimit--;
			if ((cnt % 500) == 0) {
				endTime = System.currentTimeMillis();
				timeDiff = ((double) endTime - startTime) / 1000;
				System.out.println(cnt + " seeds searched,\tTime used: " + timeDiff + " sec"
						+ "\tCalling API time used: " + exeTimeDiff / 1000 + " sec.");
				startTime = endTime;
				exeTimeDiff = 0;
			}

			videoList.setRelatedToVideoId(seedSetIter.next());
			SearchListResponse videoListResponse = new SearchListResponse();

			try {
				exeStartTime = System.currentTimeMillis();
				videoListResponse = videoList.execute();
				exeEndTime = System.currentTimeMillis();
				exeTimeDiff += (exeEndTime - exeStartTime);
			} catch (Exception e) {
				// Retry at most 3 times to make the request.
				int tryCount = 0;
				int maxRetryTime = 3;
				while (true) {
					try {
						System.out.print("Error occurs. Retrying...");
						// Wait for a few seconds and re-try.
						Thread.sleep(sleepTime);
						exeStartTime = System.currentTimeMillis();
						videoListResponse = videoList.execute();
						exeEndTime = System.currentTimeMillis();
						exeTimeDiff += (exeEndTime - exeStartTime);
						break;
					} catch (Exception e1) {
						// Double the waiting time if request failed.
						sleepTime = sleepTime * 2;
						// Set maximum waiting time to 1 minute.
						if (sleepTime >= 60000) {
							sleepTime = 60000;
						}
						// If failed 3 times, throw the exception.
						if (++tryCount >= maxRetryTime) {
							throw e1;
						}
					}
				}
				System.out.println("Done.");
			}

			List<SearchResult> videoResultList = videoListResponse.getItems();
			page--;

			// Keep fetching the next page until it's null or reaching the page
			// limit that has been set in the attribute field.
			while (videoListResponse.getNextPageToken() != null && page > 0) {
				// Reset the sleep time in case that it has already grown to a
				// big number (even 1 minute is a little bit long for the first
				// few tries.)
				sleepTime = 10000;
				page--;
				videoList.setPageToken(videoListResponse.getNextPageToken());
				try {
					exeStartTime = System.currentTimeMillis();
					videoListResponse = videoList.execute();
					exeEndTime = System.currentTimeMillis();
					exeTimeDiff += (exeEndTime - exeStartTime);
				} catch (Exception e) {
					// Retry at most 3 times to make the request.
					int tryCount = 0;
					int maxRetryTime = 3;
					while (true) {
						try {
							System.out.print("Error occurs. Retrying...");
							// Wait for a few seconds and re-try.
							Thread.sleep(sleepTime);
							exeStartTime = System.currentTimeMillis();
							videoListResponse = videoList.execute();
							exeEndTime = System.currentTimeMillis();
							exeTimeDiff += (exeEndTime - exeStartTime);
							break;
						} catch (Exception e1) {
							// Double the waiting time if request failed.
							sleepTime = sleepTime * 2;
							// Set maximum waiting time to 1 minute.
							if (sleepTime >= 60000) {
								sleepTime = 60000;
							}
							// If failed 3 times, throw the exception.
							if (++tryCount >= maxRetryTime) {
								throw e1;
							}
						}
					}
					System.out.println("Done.");
				}
				videoResultList.addAll(videoListResponse.getItems());
			}

			// Add the result to the container set.
			Iterator<SearchResult> videoResultIter = videoResultList.iterator();
			while (videoResultIter.hasNext()) {
				expandedSet.add(videoResultIter.next().getId().getVideoId());
			}
		}

		// Remove the IDs that already exist in the result set.
		System.out.println("--Before removing duplicate size: " + expandedSet.size());
		expandedSet.removeAll(currentResultSet);
		System.out.println("--After removing duplicate size:  " + expandedSet.size());
		return expandedSet;
	}

	// A short method that writes the result into a file.
	private void fileWrite(LinkedHashSet<String> result) throws IOException {

		try {
			File file = new File(filepath);

			BufferedWriter bWriter = new BufferedWriter(new FileWriter(file, true));

			Iterator<String> resultItor = result.iterator();
			while (resultItor.hasNext()) {
				bWriter.write(resultItor.next() + ",");
			}

			bWriter.close();
		} catch (IOException e) {
			throw e;
		}
	}

	private void dbWrite(LinkedHashSet<String> result) throws SQLException {

		MySQLAccess dbAccess = new MySQLAccess();
		dbAccess.videoIDListCreator(result);

	}
}
