package data_collector_ver4;

import java.util.LinkedHashSet;

import com.google.api.services.youtube.YouTube;

/**
 * This is a class that combines the API process and MySQL Access. The original
 * idea for creating this class is to build up as multithread. Now even though
 * the plan has changed, we still keep this class for potential requirement.
 * Also, this class doesn't generating any problem. It still works fine for now.
 * 
 * @author Tian
 *
 */
public class YouTubeAPIProcessThread extends Thread {

	private YouTube youtube;
	private String apiKey;
	private LinkedHashSet<String> videoIdSet;

	public YouTubeAPIProcessThread(YouTube youtube, String apiKey, LinkedHashSet<String> videoIdSet) {
		this.youtube = youtube;
		this.apiKey = apiKey;
		this.videoIdSet = videoIdSet;
	}

	@Override
	public void run() {

		try {
			// Execute the API process to collect the result.
			YouTubeAPIProcess aProcess = new YouTubeAPIProcess(youtube, apiKey, videoIdSet);
			YouTubeAPIProcessResult processResult = aProcess.execute();

			MySQLAccess insertionToDatabase = new MySQLAccess();
			insertionToDatabase.writeToDatabase(processResult.getChannelTableList(),
					processResult.getChannelStatisticTableList(), processResult.getVideoCategoryTableList(),
					processResult.getVideoTableList(), processResult.getVideoStatisticTableList(),
					processResult.getVideoCommentTableList()[0], processResult.getVideoCommentTableList()[1]);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
