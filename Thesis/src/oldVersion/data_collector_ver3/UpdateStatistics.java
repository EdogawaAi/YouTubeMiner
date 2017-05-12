package data_collector_ver3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;

import org.json.JSONObject;

import com.google.api.services.youtube.YouTube;

public class UpdateStatistics extends Thread {

	private YouTube youtube;
	private String apiKey;

	public UpdateStatistics(YouTube youtube, String apiKey) {
		// TODO Auto-generated constructor stub
		this.youtube = youtube;
		this.apiKey = apiKey;
	}

	@Override
	public void run() {

		try {
			System.out.println("********************Statistics update task**********************");

			Breakpoint breakpoint = new Breakpoint();
			FileCreator fileCreator = new FileCreator();
			// ***Block required.***
			String videoIdFilePath = fileCreator.createFile("video");
			String channelIdFilePath = fileCreator.createFile("channel");

			IdListController idListController = new IdListController();
			LinkedHashSet<String> videoIdSet = idListController.getIdSet(videoIdFilePath);
			LinkedHashSet<String> channelIdSet = idListController.getIdSet(channelIdFilePath);

			TestApi mTestApi = new TestApi(youtube, apiKey);

			// _______________________
			MySQLAccess insertionToDatabase = new MySQLAccess();

			long taskStartTime = System.currentTimeMillis();

			// **Block the video IDs.
			ArrayList<LinkedHashSet<String>> videoIdSetArray = new ArrayListSplit<LinkedHashSet<String>>(videoIdSet, 10)
					.split();
			Iterator<LinkedHashSet<String>> vIt = videoIdSetArray.iterator();
			ArrayList<JSONObject> videoStatisticTableList = new ArrayList<JSONObject>();
			ArrayList<JSONObject> topLevelCommentTableList = new ArrayList<JSONObject>();
			ArrayList<JSONObject> replyTableList = new ArrayList<JSONObject>();

			int count = 1;
			while (vIt.hasNext()) {
				System.out.println("==>video block #" + count++ + "<==");
				LinkedHashSet<String> videoIdBlock = vIt.next();
				// --Video Statistic table:
				videoStatisticTableList = mTestApi.videoStatisticTableList(videoIdBlock);
				System.out.println("videoStatistic created." + "\t\t-->List size: " + videoStatisticTableList.size());

				// --Comment tables:
				ArrayList<JSONObject>[] commentTableList = mTestApi.videoCommentTableList(videoIdBlock, channelIdSet);
				topLevelCommentTableList = commentTableList[0];
				System.out.println("topLevelComment created." + "\t-->List size: " + topLevelCommentTableList.size());
				replyTableList = commentTableList[1];
				System.out.println("reply created." + "\t\t\t-->List size: " + replyTableList.size());
			}
			// --Channel Statistics table:
			// **Block the channel IDs.
			idListController.idSetUpdate(channelIdSet, channelIdFilePath);
			channelIdSet = idListController.getIdSet(channelIdFilePath);
			LinkedHashSet<String> channelIdSetDiff = breakpoint.breakPointRestoreSet(channelIdSet, "channel");

			// Update the different channels.
			ArrayList<LinkedHashSet<String>> channelIdSetDiffArray = new ArrayListSplit<LinkedHashSet<String>>(
					channelIdSetDiff, 1000).split();
			Iterator<LinkedHashSet<String>> cDIt = channelIdSetDiffArray.iterator();
			ArrayList<JSONObject> channelTableList = new ArrayList<JSONObject>();
			while (cDIt.hasNext()) {
				channelTableList = mTestApi.channelTableList(cDIt.next());
			}
			System.out.println("channel created." + "\t\t-->List size: " + channelTableList.size());

			// Update all the channel statistics.
			ArrayList<LinkedHashSet<String>> channelIdSetArray = new ArrayListSplit<LinkedHashSet<String>>(channelIdSet,
					1000).split();
			Iterator<LinkedHashSet<String>> cIt = channelIdSetArray.iterator();
			ArrayList<JSONObject> channelStatisticTableList = new ArrayList<JSONObject>();

			int count2 = 1;
			while (cIt.hasNext()) {
				System.out.println("==>channel block #" + count2++ + "<==");
				channelStatisticTableList = mTestApi.channelStatisticTableList(cIt.next());
			}
			System.out.println("channelStatistic created." + "\t-->List size: " + channelStatisticTableList.size());

			long taskStopTime = System.currentTimeMillis();
			long timeDelta = taskStopTime - taskStartTime;
			System.out.println("Information retrieving time: " + ((Double) (timeDelta / 1000.0)) + " sec");

			taskStartTime = System.currentTimeMillis();
			// NOTICE: order CANNOT be changed.
			insertionToDatabase.establishConnection();
			insertionToDatabase.writeChannelToDataBase(channelTableList);
			insertionToDatabase.writeChannelStatisticToDatebase(channelStatisticTableList);
			insertionToDatabase.writeVideoStatisticToDatabase(videoStatisticTableList);
			insertionToDatabase.writeTopLevelCommentToDatebase(topLevelCommentTableList);
			insertionToDatabase.writeReplyToDatabase(replyTableList);
			insertionToDatabase.close();

			taskStopTime = System.currentTimeMillis();
			timeDelta = taskStopTime - taskStartTime;
			System.out.println("Database inserting time: " + ((Double) (timeDelta / 1000.0)) + " sec");
			// ---------------------------------------------------------------------------//
			// ---------------------------------------------------------------------------//
			// ---------------------------------------------------------------------------//

		} catch (Exception e) {
			// TODO: handle exception
		}
	}

}
