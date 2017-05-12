package data_collector_ver4;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
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
	 * @param args
	 * @throws Exception
	 */
	public static void main(String args[]) throws Exception {

		String mode = args[0];
		YouTubeAuth yAuth = new YouTubeAuth();
		YouTube youtube = yAuth.getYouTube();
		String apiKey = yAuth.getApiKey();
		MySQLAccess dbAccess = new MySQLAccess();

		// Whether or not importing new IDs to the DB.
		if (mode.equals("0")) {
			System.out.println("Mode: 0");
			// skip generation of new video IDs
			// Loop of main process block:
			int count = 1000;
			while (count-- > 0) {
				System.out.println("Process remain: " + count + " times.");
				// Each block read 20 videos:
				LinkedHashSet<String> videoIdSet = dbAccess.readVideoIdList(20);
				System.out.println("--VideoId read.");
				YouTubeAPIProcessThread apiProcessThread = new YouTubeAPIProcessThread(youtube, apiKey, videoIdSet);
				apiProcessThread.run();
				apiProcessThread.join();
			}

		} else if (mode.equals("1")) {
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
			do {
				System.out.print(
						"Please input the seed size of videos, total page number, and maximum result per page (separated by comma): ");
				inputString = input.nextLine().split(",");
			} while (inputString.length != 3);
			seedSize = Integer.valueOf(inputString[0].trim());
			pageNum = Integer.valueOf(inputString[1].trim());
			resultPerPage = Integer.valueOf(inputString[2].trim());
			VideoIdCreator vIdCreator = new VideoIdCreator(youtube, apiKey, "date", seedSize, pageNum,
					resultPerPage, makeFilePath(input));
			// Also input the expanding (i.e., get access to the related videos)
			// times.
			System.out.println(System.in.available());
			System.out.println("Please input the expand time of the seed videos: ");
			int expandTime = input.nextInt();
			System.out.println("Input completed. \n**Notice: Current maximum seed videos are set to 10000");
			LinkedHashSet<String> videoIdSet = vIdCreator.videoIdSetCreate(expandTime);
			input.close();
			System.out.println("In total " + videoIdSet.size() + " IDs are created.");
			System.out.println("Insertion finished.");
		} else {
			System.out.println(
					"Invalid argument. " + "\nValid argument options: \n 0 - Skip the generation of new video IDs"
							+ "\n 1 - Start generating new video IDs and upload them to the DB");
			System.exit(0);
		}

	}

	private static String makeFilePath(Scanner input) throws IOException {
		String dateStr = new SimpleDateFormat("yyyyMMdd").format(new Date());
		String pathname = "videoId" + File.separator + dateStr + "_videoId.txt";
		try {
			File file = new File(pathname);
			if (!file.exists()) {
				System.out.println("filepath: " + pathname);
				file.getParentFile().mkdirs();
				file.createNewFile();
				if (file.exists()) {
					System.out.println("->file created.");
				}
			} else {
				System.out.println("File already exist. Rename(1) or replace(2)?");
				String mode = new String();
				do {
					mode = input.nextLine();
				} while (!(mode.equals("1") || mode.equals("2")));
				if (mode.equals("1")) {
					String time = new SimpleDateFormat("HHmmss").format(new Date());
					pathname = pathname.split("\\.")[0] + "_" + time + ".txt";
					System.out.println("filepath: " + pathname);
					file.getParentFile().mkdirs();
					file.createNewFile();
					if (file.exists()) {
						System.out.println("->file created.");
					}
				} else if (mode.equals("2")) {
					System.out.println("File is deleted: " + file.delete());
					System.out.println("filepath: " + pathname);
					file.getParentFile().mkdirs();
					file.createNewFile();
					if (file.exists()) {
						System.out.println("->file created.");
					}
				} else {
					System.out.println("Should never reach this point.");
				}
			}

		} catch (IOException e) {
			throw e;
		}

		return pathname;
	}

}
