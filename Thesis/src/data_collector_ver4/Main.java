package data_collector_ver4;

import java.util.LinkedHashSet;
import java.util.Scanner;

import com.google.api.services.youtube.YouTube;

import video_id_generator.VideoIdCreator;

public class Main {

	/**
	 * main class that handles the whole process:
	 * <q>1. Select whether or not update new video IDs to the database;
	 * <q>2. Retrieve a subset of video IDs from database.videoIdRecord;
	 * <q>3. Process the API to get the information of the retrieved videos;
	 * <q>4. Insert the informations, and mark the successfully crawled videos
	 * as "crawled".
	 * <q>5. Re-do from step2.
	 * 
	 * This class mainly calls: MySQLAccess to perform database related
	 * functions, VideoIdGenerator to perform video ID collection, and
	 * YouTubeAPIProcess to perform metadata collection.
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String args[]) throws Exception {

		String mode = args[0];
		YouTubeAuth yAuth = new YouTubeAuth();
		YouTube youtube = yAuth.getYouTube();
		String apiKey = yAuth.getApiKey();
		MySQLAccess dbAccess = new MySQLAccess();

		// The configuration argument should be either 0 or 1.
		// If set the argument to 0, we will do the metadata collection part.
		if (mode.equals("0")) {
			System.out.println("Mode: 0");

			int count = 1000;
			// For now we loop 1000 times. It's not a big number but still may
			// cost nearly one week.
			while (count-- > 0) {

				System.out.println("Process remain: " + count + " times.");

				// Each block read 20 videos:
				LinkedHashSet<String> videoIdSet = dbAccess.readVideoIdList(20);
				System.out.println("--VideoId read.");

				YouTubeAPIProcessThread apiProcessThread = new YouTubeAPIProcessThread(youtube, apiKey, videoIdSet);
				apiProcessThread.run(); // At the beginning, the plan was a
										// multithread design. However, it
										// seemed that a distribution framework
										// is more suitable for easily expanding
										// (though it's not implemented yet).
										// However, some old designs are kept
										// since there's no harm...
				apiProcessThread.join();
			}

		}
		// If set the argument to 1, we will process the video ID collection
		// part.
		else if (mode.equals("1")) {
			System.out.println("Mode: 1");
			// upload a list of video IDs to the database.
			// input the size of the video ID.
			int seedSize = 0;
			int pageNum = 0;
			long resultPerPage = 0;
			Scanner input = new Scanner(System.in);
			System.out.println(
					"----------------------------------------------------------------------------------------------");
			System.out.println(
					"Ready to generate a video ID list. At first, a given number of seed videos are collected from \ndifferent categories of YouTube."
							+ "\nThen from each seed video, retrieve its \"related videos\" and treat them as new \"seed videos\"."
							+ "\nWhen retrieving related videos, the API generates a few pages of videos of which maximum \nresult is limited by 100.");
			System.out.println(
					"----------------------------------------------------------------------------------------------");
			String[] inputString;
			// Read values from input.
			// Recommended input: 50, 1, 25.
			do {
				System.out.print(
						"Please input the seed size of videos, total page number, and maximum result per page (separated by comma): ");
				inputString = input.nextLine().split(",");
			} while (inputString.length != 3);
			seedSize = Integer.valueOf(inputString[0].trim());
			pageNum = Integer.valueOf(inputString[1].trim());
			resultPerPage = Integer.valueOf(inputString[2].trim());

			VideoIdCreator vIdCreator = new VideoIdCreator(youtube, apiKey, "date", seedSize, pageNum, resultPerPage);
			// Also input the expanding (i.e., get access to the related videos)
			// times. If it's set to -1, then we'll run an infinite loop.
			System.out.println(System.in.available());
			System.out.println("Please input the expand time of the seed videos: ");
			int expandTime = input.nextInt();
			System.out.println("Input completed. \n**Notice: Current maximum seed videos are set to 10000");
			LinkedHashSet<String> videoIdSet = vIdCreator.videoIdSetCreate(expandTime);
			input.close();
			System.out.println("In total " + videoIdSet.size() + " IDs are created.");
			System.out.println("Insertion finished.");
		} else { // Invalid input, requires re-set the argument.
			System.out.println(
					"Invalid argument. " + "\nValid argument options: \n 0 - Skip the generation of new video IDs"
							+ "\n 1 - Start generating new video IDs and upload them to the DB");
			System.exit(0);
		}

	}

}
