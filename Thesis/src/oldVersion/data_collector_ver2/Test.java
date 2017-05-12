package data_collector_ver2;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Properties;
import org.json.JSONObject;

import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.YouTube.Search;
import com.google.api.services.youtube.auth.Auth;

public class Test {

	private static final String PROPERTIES_FILENAME = "youtube.properties";
	private static YouTube youtube;

	public static void main(String[] args) throws Exception {
		Properties properties = new Properties();
		try {
			InputStream in = Search.class.getResourceAsStream("/" + PROPERTIES_FILENAME);
			properties.load(in);

		} catch (IOException e) {
			System.err.println(
					"There was an error reading " + PROPERTIES_FILENAME + ": " + e.getCause() + " : " + e.getMessage());
			System.exit(1);
		}
		youtube = new YouTube.Builder(Auth.HTTP_TRANSPORT, Auth.JSON_FACTORY, new HttpRequestInitializer() {
			public void initialize(HttpRequest request) throws IOException {
			}
		}).setApplicationName("youtube-cmdline").build();
		String apiKey = properties.getProperty("youtube.apikey");

		VideoListCreator myCreator = new VideoListCreator(youtube, apiKey);
		String videoIdListCSV = myCreator.videoList("date");

		TestApi mTestApi = new TestApi(youtube, apiKey);

		// _______________________
		MySQLAccess insertionToDatabase = new MySQLAccess();

		long taskStartTime = System.currentTimeMillis();

		// NOTICE: order CANNOT be changed.
		ArrayList<JSONObject> videoTableList = mTestApi.videoTableList(videoIdListCSV);
		System.out.println("videoTableList created." + "-->List size: " + videoTableList.size());
		ArrayList<JSONObject> videoStatisticTableList = mTestApi.videoStatisticTableList(videoIdListCSV);
		System.out.println("videoStatisticTableList created." + "-->List size: " + videoStatisticTableList.size());
		ArrayList<JSONObject> videoCategoryTableList = mTestApi.videoCategoryTableList();
		System.out.println("videoCategoryTableList created." + "-->List size: " + videoCategoryTableList.size());

		ArrayList<JSONObject>[] commentTableList = mTestApi.videoCommentTableList(videoIdListCSV);
		ArrayList<JSONObject> topLevelCommentTableList = commentTableList[0];
		System.out.println("topLevelCommentTableList created." + "-->List size: " + topLevelCommentTableList.size());
		ArrayList<JSONObject> replyTableList = commentTableList[1];
		System.out.println("replyTableList created." + "-->List size: " + replyTableList.size());

		ArrayList<JSONObject> channelTableList = mTestApi.channelTableList();
		System.out.println("channelTableList created." + "-->List size: " + channelTableList.size());
		ArrayList<JSONObject> channelStatisticTableList = mTestApi.channelStatisticTableList();
		System.out.println("channelStatisticTableList created." + "-->List size: " + channelStatisticTableList.size());

		long taskStopTime = System.currentTimeMillis();
		long timeDelta = taskStopTime - taskStartTime;
		System.out.println("Information retrieving time: " + ((Double) (timeDelta / 1000.0)) + " sec");

		taskStartTime = System.currentTimeMillis();
		// NOTICE: order CANNOT be changed.
		insertionToDatabase.establishConnection();
		insertionToDatabase.writeChannelToDataBase(channelTableList);
		insertionToDatabase.writeChannelStatisticToDatebase(channelStatisticTableList);
		insertionToDatabase.writeVideoCategoryToDatabase(videoCategoryTableList);
		insertionToDatabase.writeVideoToDatabase(videoTableList);
		insertionToDatabase.writeVideoStatisticToDatabase(videoStatisticTableList);
		insertionToDatabase.writeTopLevelCommentToDatebase(topLevelCommentTableList);
		insertionToDatabase.writeReplyToDatabase(replyTableList);
		insertionToDatabase.close();

		taskStopTime = System.currentTimeMillis();
		timeDelta = taskStopTime - taskStartTime;
		System.out.println("Database inserting time: " + ((Double) (timeDelta / 1000.0)) + " sec");

	}

}
