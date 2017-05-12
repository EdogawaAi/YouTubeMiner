package video_id_generator;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Scanner;

import com.google.api.services.youtube.YouTube;

import data_collector_ver4.YouTubeAuth;

public class Test {

	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		YouTubeAuth yAuth = new YouTubeAuth();
		YouTube youtube = yAuth.getYouTube();
		String apiKey = yAuth.getApiKey();

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
				Scanner input = new Scanner(System.in);
				String mode = new String();
				do {
					mode = input.nextLine();
				} while (!(mode.equals("1") || mode.equals("2")));
				input.close();
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

		VideoIdCreator vIdSet = new VideoIdCreator(youtube, apiKey, "date", 50, 1, 20, pathname);
		LinkedHashSet<String> result = vIdSet.videoIdSetCreate(7); // Expand 7
																	// times.
		System.out.println(result.size());

	}

}
