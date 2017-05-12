package data_collector_ver3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;

import org.json.JSONObject;

import com.google.api.services.youtube.YouTube;

public class InitialThread extends Thread {

	private YouTube youtube;
	private String apiKey = new String();

	public InitialThread(YouTube youtube, String apiKey) {
		// TODO Auto-generated constructor stub
		this.youtube = youtube;
		this.apiKey = apiKey;
	}

	@Override
	public void run() {

		try {
			FileCreator fileCreator = new FileCreator();

			String videoIdFilePath = fileCreator.createFile("video");
			String channelIdFilePath = fileCreator.createFile("channel");
			String categoryIdFilePath = fileCreator.createFile("category");

			/*
			 * Generate a initial id list for videos.
			 */
			VideoListCreator myCreator = new VideoListCreator(youtube, apiKey);
			LinkedHashSet<String> videoIdSet = myCreator.videoIdSet("date", 20);
			// Enlarge the id set size.
			videoIdSet.addAll(myCreator.videoIdExpandedSet(videoIdSet, 20));
			// videoIdSet.addAll(myCreator.videoIdExpandedSet(videoIdSet, 50));
			System.out.println("id size: " + videoIdSet.size());

			LinkedHashSet<String> channelIdSet = new LinkedHashSet<String>();
			LinkedHashSet<String> categoryIdSet = new LinkedHashSet<String>();
			IdListController idListController = new IdListController();

			TestApi mTestApi = new TestApi(youtube, apiKey);

			System.out.println("********************Initial Start**********************");
			ArrayList<LinkedHashSet<String>> videoIdSetArray = new ArrayListSplit<LinkedHashSet<String>>(videoIdSet, 50)
					.split();
			Iterator<LinkedHashSet<String>> vIt = videoIdSetArray.iterator();
			while (vIt.hasNext()) {
				LinkedHashSet<String> videoIdSetBlock = vIt.next();
				blockedMainProcess(videoIdSetBlock, channelIdSet, categoryIdSet, mTestApi, idListController,
						videoIdFilePath, channelIdFilePath, categoryIdFilePath);
				Thread.sleep(5000);
			}
		} catch (Exception e) {
			System.out.println("Terminated.");
			try {
				throw e;
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}

	}

	// The main process that is blocked so that it can be called and repeated
	// easier.
	private void blockedMainProcess(LinkedHashSet<String> videoIdSet, LinkedHashSet<String> channelIdSet,
			LinkedHashSet<String> categoryIdSet, TestApi mTestApi, IdListController idListController,
			String videoIdFilePath, String channelIdFilePath, String categoryIdFilePath) throws Exception {

		MySQLAccess insertionToDatabase = new MySQLAccess();

		long taskStartTime = System.currentTimeMillis();

		// ---------------------------------------------------------------------------//
		// ---------------------------------------------------------------------------//
		// ---------------------------------------------------------------------------//
		// *Since now the size is big, we need divide the work.
		// NOTICE: order CANNOT be changed.
		// **Type 1: New ID comes.**
		//
		// STEP 1: Video ID based tables:

		ArrayList<JSONObject> videoTableList = mTestApi.videoTableList(videoIdSet, channelIdSet, categoryIdSet);
		System.out.println("video created." + "\t\t\t-->List size: " + videoTableList.size());
		ArrayList<JSONObject> videoStatisticTableList = mTestApi.videoStatisticTableList(videoIdSet);
		System.out.println("videoStatistic created." + "\t\t-->List size: " + videoStatisticTableList.size());

		idListController.idSetUpdate(videoIdSet, videoIdFilePath);

		// --Comment tables:
		// **Divide required**
		ArrayList<JSONObject> topLevelCommentTableList = new ArrayList<JSONObject>();
		ArrayList<JSONObject> replyTableList = new ArrayList<JSONObject>();
		// Split:
		ArrayList<LinkedHashSet<String>> videoIdSetArray = new ArrayListSplit<LinkedHashSet<String>>(videoIdSet, 10)
				.split();// split(videoIdSet, 10);
		Iterator<LinkedHashSet<String>> vIt = videoIdSetArray.iterator();
		int count = 0;
		System.out.println("Retrieving comments...");
		while (vIt.hasNext()) {
			ArrayList<JSONObject>[] commentTableList = mTestApi.videoCommentTableList(vIt.next(), channelIdSet);
			topLevelCommentTableList.addAll(commentTableList[0]);
			replyTableList.addAll(commentTableList[1]);
		}
		System.out.println("Comments done.");
		System.out.println("topLevelComment created." + "\t-->List size: " + topLevelCommentTableList.size());
		System.out.println("Sample: ==>\n" + topLevelCommentTableList.get(0).toString());
		System.out.println("reply created." + "\t\t\t-->List size: " + replyTableList.size());

		// STEP 2: Category ID based tables:

		ArrayList<JSONObject> videoCategoryTableList = mTestApi.videoCategoryTableList(categoryIdSet);
		System.out.println("videoCategory created." + "\t\t-->List size: " + videoCategoryTableList.size());

		idListController.idSetUpdate(categoryIdSet, categoryIdFilePath);

		// STEP 3: Channel ID based tables:
		// **Divide required**
		ArrayList<JSONObject> channelTableList = new ArrayList<JSONObject>();
		ArrayList<JSONObject> channelStatisticTableList = new ArrayList<JSONObject>();

		// Split:
		ArrayList<LinkedHashSet<String>> channelIdSetArray = new ArrayListSplit<LinkedHashSet<String>>(channelIdSet,
				1000).split(); // split(channelIdSet, 1000);
		Iterator<LinkedHashSet<String>> cIt = channelIdSetArray.iterator();
		count = 0;
		while (cIt.hasNext()) {
			System.out.println("==>channel block#" + ++count + "<==");
			LinkedHashSet<String> channelIdBlock = cIt.next();
			channelTableList.addAll(mTestApi.channelTableList(channelIdBlock));
			channelStatisticTableList.addAll(mTestApi.channelStatisticTableList(channelIdBlock));
		}
		System.out.println("channel created." + "\t\t-->List size: " + channelTableList.size());
		System.out.println("channelStatistic created." + "\t-->List size: " + channelStatisticTableList.size());
		idListController.idSetUpdate(channelIdSet, channelIdFilePath);

		long taskStopTime = System.currentTimeMillis();
		long timeDelta = taskStopTime - taskStartTime;
		System.out.println("Information retrieving time: " + ((Double) (timeDelta / 1000.0)) + " sec");

		taskStartTime = System.currentTimeMillis();
		// NOTICE: order CANNOT be changed.
		insertToDatabase(insertionToDatabase, channelTableList, channelStatisticTableList, videoCategoryTableList,
				videoTableList, videoStatisticTableList, topLevelCommentTableList, replyTableList);

		taskStopTime = System.currentTimeMillis();
		timeDelta = taskStopTime - taskStartTime;
		System.out.println("Database inserting time: " + ((Double) (timeDelta / 1000.0)) + " sec");

		// ---------------------------------------------------------------------------//
		// ---------------------------------------------------------------------------//
		// ---------------------------------------------------------------------------//
	}

	// A subroutine that helps organize the insertion steps.
	private void insertToDatabase(MySQLAccess insertionToDatabase, ArrayList<JSONObject> channelTableList,
			ArrayList<JSONObject> channelStatisticTableList, ArrayList<JSONObject> videoCategoryTableList,
			ArrayList<JSONObject> videoTableList, ArrayList<JSONObject> videoStatisticTableList,
			ArrayList<JSONObject> topLevelCommentTableList, ArrayList<JSONObject> replyTableList) throws Exception {

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
	}
}
