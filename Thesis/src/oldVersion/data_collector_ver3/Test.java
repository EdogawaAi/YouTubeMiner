package data_collector_ver3;

import com.google.api.services.youtube.YouTube;

public class Test {

	private static YouTube youtube;

	public static void main(String[] args) throws Exception {
		YouTubeAuth youTubeAuth = new YouTubeAuth();

		youtube = youTubeAuth.getYouTube();
		final String apiKey = youTubeAuth.getApiKey();

		InitialThread initial = new InitialThread(youtube, apiKey);
		initial.run();
		initial.join();

		// ScheduledExecutorService taskExecutor =
		// Executors.newScheduledThreadPool(1);
		// taskExecutor.scheduleAtFixedRate(update, 0, 120, TimeUnit.MINUTES);

	}
}
