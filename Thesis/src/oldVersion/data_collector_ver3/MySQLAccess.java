package data_collector_ver3;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;

import org.json.JSONException;
import org.json.JSONObject;

public class MySQLAccess {
	private Connection connect = null;
	private Statement statement = null;
	private ResultSet resultSet = null;

	final private String host = "localhost:3306";
	final private String user = "root";
	final private String passwd = "Wezard19901027";

	// Breakpoint handler:

	public void establishConnection() throws SQLException {
		// This will load the MySQL driver, each DB has its own driver
		try {
			Class.forName("com.mysql.jdbc.Driver");
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// Setup the connection with the DB
		connect = DriverManager.getConnection("jdbc:mysql://" + host + "/youtube_data_sample_draft?" + "user=" + user
				+ "&password=" + passwd + "&character_set_server=utf8mb4");

	}

	// channel table insertion.
	public void writeChannelToDataBase(ArrayList<JSONObject> channelTableList) throws Exception {

		// Statements allow to issue SQL queries to the database
		statement = connect.createStatement();
		String lastInsertedChannel = new String();

		for (JSONObject channelTable : channelTableList) {
			String channelId = channelTable.getString("ChannelId");
			Timestamp channelPublishedAt = stringToTimestamp(channelTable.getString("ChannelPublishedAt"));
			String channelTitle = channelTable.getString("ChannelTitle");
			String channelDescription = channelTable.getString("ChannelDescription");

			String query = "INSERT INTO Channel (ChannelId, ChannelPublishedAt, ChannelTitle, ChannelDescription) "
					+ "VALUE (?, ?, ?, ?) ON DUPLICATE KEY UPDATE ChannelId=ChannelId";

			try {
				PreparedStatement preparedStatement = connect.prepareStatement(query);
				preparedStatement.setString(1, channelId);
				preparedStatement.setTimestamp(2, channelPublishedAt);
				preparedStatement.setString(3, channelTitle);
				preparedStatement.setString(4, channelDescription);

				preparedStatement.executeUpdate();
			} catch (java.sql.SQLException e) {
				PreparedStatement preparedStatement = connect.prepareStatement(query);
				if (e.getMessage().contains("ChannelTitle") && !e.getMessage().contains("ChannelDescription")) {
					preparedStatement.setString(1, channelId);
					preparedStatement.setTimestamp(2, channelPublishedAt);
					preparedStatement.setString(3, "");
					preparedStatement.setString(4, channelDescription);
				} else if (!e.getMessage().contains("ChannelTitle") && e.getMessage().contains("ChannelDescription")) {
					preparedStatement.setString(1, channelId);
					preparedStatement.setTimestamp(2, channelPublishedAt);
					preparedStatement.setString(3, channelTitle);
					preparedStatement.setString(4, "");
				} else if (e.getMessage().contains("ChannelTitle") && e.getMessage().contains("ChannelDescription")) {
					preparedStatement.setString(1, channelId);
					preparedStatement.setTimestamp(2, channelPublishedAt);
					preparedStatement.setString(3, "");
					preparedStatement.setString(4, "");
				} else {
					lastInsertedChannel = channelId;
					new Breakpoint().saveBreakPoint("channel", lastInsertedChannel);
					throw e;
				}
				preparedStatement.executeUpdate();
			}

			lastInsertedChannel = channelId;
		}
		new Breakpoint().saveBreakPoint("channel", lastInsertedChannel);
	}

	// video category insertion.
	public void writeVideoCategoryToDatabase(ArrayList<JSONObject> videoCategoryTableList)
			throws SQLException, IOException {
		statement = connect.createStatement();

		String lastInsertedCategory = new String();

		for (JSONObject videoCategoryTable : videoCategoryTableList) {
			String categoryId = videoCategoryTable.getString("CategoryId");
			String CategoryTitle = videoCategoryTable.getString("CategoryTitle");

			String query = "INSERT INTO VideoCategory (CategoryId, CategoryTitle) "
					+ "VALUE (?, ?) ON DUPLICATE KEY UPDATE CategoryId=CategoryId";
			try {
				PreparedStatement preparedStatement = connect.prepareStatement(query);
				preparedStatement.setString(1, categoryId);
				preparedStatement.setString(2, CategoryTitle);

				preparedStatement.executeUpdate();
			} catch (SQLException e) {
				if (e.getMessage().contains("CategoryTitle")) {
					PreparedStatement preparedStatement = connect.prepareStatement(query);
					preparedStatement.setString(1, categoryId);
					preparedStatement.setString(2, "");

					preparedStatement.executeUpdate();
				} else {
					lastInsertedCategory = categoryId;
					new Breakpoint().saveBreakPoint("category", lastInsertedCategory);
				}
			}
			lastInsertedCategory = categoryId;
		}
		new Breakpoint().saveBreakPoint("category", lastInsertedCategory);

	}

	// video table insertion.
	public void writeVideoToDatabase(ArrayList<JSONObject> videoTableList)
			throws SQLException, JSONException, ParseException, IOException {

		// Statements allow to issue SQL queries to the database
		statement = connect.createStatement();
		String lastInsertedVideo = new String();

		for (JSONObject videoTable : videoTableList) {
			String videoId = videoTable.getString("VideoId");
			String CategoryId = videoTable.getString("CategoryId");
			String ChannelId = videoTable.getString("ChannelId");
			Timestamp VideoPublishedAt = stringToTimestamp(videoTable.getString("VideoPublishedAt"));
			String Duration = videoTable.getString("Duration");
			String VideoTitle = videoTable.getString("VideoTitle");
			String VideoDescription = videoTable.getString("VideoDescription");

			String query = "INSERT INTO Video (VideoId, CategoryId, ChannelId, VideoPublishedAt, Duration, VideoTitle, VideoDescription) "
					+ "VALUE (?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE VideoId=VideoId";

			try {
				PreparedStatement preparedStatement = connect.prepareStatement(query);
				preparedStatement.setString(1, videoId);
				preparedStatement.setString(2, CategoryId);
				preparedStatement.setString(3, ChannelId);
				preparedStatement.setTimestamp(4, VideoPublishedAt);
				preparedStatement.setString(5, Duration);
				preparedStatement.setString(6, VideoTitle);
				preparedStatement.setString(7, VideoDescription);

				preparedStatement.executeUpdate();

			} catch (SQLException e) {
				if (e.getMessage().contains("VideoDescription")) {
					PreparedStatement preparedStatement = connect.prepareStatement(query);
					preparedStatement.setString(1, videoId);
					preparedStatement.setString(2, CategoryId);
					preparedStatement.setString(3, ChannelId);
					preparedStatement.setTimestamp(4, VideoPublishedAt);
					preparedStatement.setString(5, Duration);
					preparedStatement.setString(6, VideoTitle);
					preparedStatement.setString(7, "");

					preparedStatement.executeUpdate();
				} else {
					lastInsertedVideo = videoId;
					new Breakpoint().saveBreakPoint("video", lastInsertedVideo);
				}
			}
			lastInsertedVideo = videoId;
		}
		new Breakpoint().saveBreakPoint("video", lastInsertedVideo);
	}

	// video statistic insertion.
	public void writeVideoStatisticToDatabase(ArrayList<JSONObject> videoStatisticTableList)
			throws SQLException, JSONException, ParseException, IOException {

		// Statements allow to issue SQL queries to the database
		statement = connect.createStatement();
		String lastInsertedVideoId = new String();

		for (JSONObject videoStatisticTable : videoStatisticTableList) {
			String videoId = videoStatisticTable.getString("VideoId");
			Date date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
					.parse(videoStatisticTable.getString("VideoTimeStamp"));
			Timestamp videoTimeStamp = new java.sql.Timestamp(date.getTime());
			long videoFavoriteCount = videoStatisticTable.getLong("VideoFavoriteCount");
			long videoViewCount = videoStatisticTable.getLong("VideoViewCount");
			long videoLikeCount = 0;
			try {
				videoLikeCount = videoStatisticTable.getLong("VideoLikeCount");
			} catch (Exception e) {
			}
			long videoDislikeCount = 0;
			try {
				videoDislikeCount = videoStatisticTable.getLong("VideoDislikeCount");
			} catch (Exception e) {
			}
			long videoCommentsCount = 0;
			try {
				videoCommentsCount = videoStatisticTable.getLong("VideoCommentsCount");
			} catch (Exception e) {
			}

			String query = "INSERT INTO VideoStatistic (VideoId, VideoTimeStamp, VideoCommentsCount, VideoDislikeCount, VideoFavoriteCount, VideoLikeCount, VideoViewCount) "
					+ "VALUE (?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE VideoId=VideoId";

			try {
				PreparedStatement preparedStatement = connect.prepareStatement(query);

				preparedStatement.setString(1, videoId);
				preparedStatement.setTimestamp(2, videoTimeStamp);
				preparedStatement.setLong(3, videoCommentsCount);
				preparedStatement.setLong(4, videoDislikeCount);
				preparedStatement.setLong(5, videoFavoriteCount);
				preparedStatement.setLong(6, videoLikeCount);
				preparedStatement.setLong(7, videoViewCount);

				preparedStatement.executeUpdate();

			} catch (SQLException e) {
				lastInsertedVideoId = videoId;
				new Breakpoint().saveBreakPoint("videoStatistic", lastInsertedVideoId);
			}
			lastInsertedVideoId = videoId;
		}
		new Breakpoint().saveBreakPoint("videoStatistic", lastInsertedVideoId);
	}

	// channel statistic insertion.
	public void writeChannelStatisticToDatebase(ArrayList<JSONObject> channelStatisticTableList)
			throws SQLException, JSONException, ParseException, IOException {

		// Statements allow to issue SQL queries to the database
		statement = connect.createStatement();
		String lastInsertedChannelStatistic = new String();

		for (JSONObject channelStatisticTable : channelStatisticTableList) {
			String channelId = channelStatisticTable.getString("ChannelId");
			Date date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
					.parse(channelStatisticTable.getString("ChannelTimeStamp"));
			Timestamp channelTimeStamp = new java.sql.Timestamp(date.getTime());
			long channelCommentCount = channelStatisticTable.getLong("ChannelCommentCount");
			long channelSubscriberCount = channelStatisticTable.getLong("ChannelSubscriberCount");
			long channelVideoCount = channelStatisticTable.getLong("ChannelVideoCount");
			long channelViewCount = channelStatisticTable.getLong("ChannelViewCount");

			String query = "INSERT INTO ChannelStatistic (ChannelId, ChannelTimeStamp, ChannelCommentCount, ChannelSubscriberCount, ChannelVideoCount, ChannelViewCount) "
					+ "VALUE (?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE ChannelId=ChannelId";

			try {
				PreparedStatement preparedStatement = connect.prepareStatement(query);

				preparedStatement.setString(1, channelId);
				preparedStatement.setTimestamp(2, channelTimeStamp);
				preparedStatement.setLong(3, channelCommentCount);
				preparedStatement.setLong(4, channelSubscriberCount);
				preparedStatement.setLong(5, channelVideoCount);
				preparedStatement.setLong(6, channelViewCount);

				preparedStatement.executeUpdate();
			} catch (SQLException e) {
				lastInsertedChannelStatistic = channelId;
				new Breakpoint().saveBreakPoint("channelStatistic", lastInsertedChannelStatistic);
			}
			lastInsertedChannelStatistic = channelId;
		}
		new Breakpoint().saveBreakPoint("channelStatistic", lastInsertedChannelStatistic);
	}

	// top level comment insertion.
	public void writeTopLevelCommentToDatebase(ArrayList<JSONObject> topLevelCommentTableList)
			throws SQLException, JSONException, ParseException, IOException {

		// Statements allow to issue SQL queries to the database
		statement = connect.createStatement();
		String lastInsertedTLCommentVideo = new String();

		for (JSONObject topLevelCommentTable : topLevelCommentTableList) {
			String tlcommentId = topLevelCommentTable.getString("TLCommentId");
			String videoId = topLevelCommentTable.getString("VideoId");
			String channelId = topLevelCommentTable.getString("ChannelId");
			int tlcommentLikeCount = topLevelCommentTable.getInt("TLCommentLikeCount");
			Timestamp TLCommentPublishedAt = stringToTimestamp(topLevelCommentTable.getString("TLCommentPublishedAt"));
			Timestamp TLCommentUpdatedAt = stringToTimestamp(topLevelCommentTable.getString("TLCommentUpdatedAt"));
			String TLCommentTextDisplay = topLevelCommentTable.getString("TLCommentTextDisplay");
			int TotalReplyCount = topLevelCommentTable.getInt("TotalReplyCount");

			String query = "INSERT INTO TopLevelComment (TLCommentId, VideoId, ChannelId, TLCommentLikeCount, TLCommentPublishedAt, TLCommentUpdatedAt, TLCommentTextDisplay, TotalReplyCount) "
					+ "VALUE (?, ?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE TLCOmmentId=TLCOmmentId";

			try {
				PreparedStatement preparedStatement = connect.prepareStatement(query);

				preparedStatement.setString(1, tlcommentId);
				preparedStatement.setString(2, videoId);
				preparedStatement.setString(3, channelId);
				preparedStatement.setInt(4, tlcommentLikeCount);
				preparedStatement.setTimestamp(5, TLCommentPublishedAt);
				preparedStatement.setTimestamp(6, TLCommentUpdatedAt);
				preparedStatement.setString(7, TLCommentTextDisplay);
				preparedStatement.setInt(8, TotalReplyCount);

				preparedStatement.executeUpdate();
			} catch (SQLException e) {
				if (e.getMessage().contains("TLCommentTextDisplay")) {
					PreparedStatement preparedStatement = connect.prepareStatement(query);

					preparedStatement.setString(1, tlcommentId);
					preparedStatement.setString(2, videoId);
					preparedStatement.setString(3, channelId);
					preparedStatement.setInt(4, tlcommentLikeCount);
					preparedStatement.setTimestamp(5, TLCommentPublishedAt);
					preparedStatement.setTimestamp(6, TLCommentUpdatedAt);
					preparedStatement.setString(7, "");
					preparedStatement.setInt(8, TotalReplyCount);

					preparedStatement.executeUpdate();
				} else {
					lastInsertedTLCommentVideo = videoId;
					new Breakpoint().saveBreakPoint("topLevelComment", lastInsertedTLCommentVideo);
				}
			}

			lastInsertedTLCommentVideo = videoId;
		}
		new Breakpoint().saveBreakPoint("topLevelComment", lastInsertedTLCommentVideo);
	}

	public void writeReplyToDatabase(ArrayList<JSONObject> replyTableList)
			throws SQLException, JSONException, ParseException, IOException {
		// Statements allow to issue SQL queries to the database
		statement = connect.createStatement();
		String lastInsertedReplyTLComment = new String();

		for (JSONObject replyTable : replyTableList) {
			String replyId = replyTable.getString("ReplyId");
			String tlcommentId = replyTable.getString("TLCommentId");
			String channelId = replyTable.getString("ChannelId");
			int replyLikeCount = replyTable.getInt("ReplyLikeCount");
			Timestamp replyPublishedAt = stringToTimestamp(replyTable.getString("ReplyPublishedAt"));
			Timestamp replyUpdatedAt = stringToTimestamp(replyTable.getString("ReplyUpdatedAt"));
			String replyTextDisplay = replyTable.getString("ReplyTextDisplay");

			String query = "INSERT INTO Reply (ReplyId, TLCOmmentId, ChannelId, ReplyLikeCount, ReplyPublishedAt, ReplyUpdatedAt, ReplyTextDisplay) "
					+ "VALUE (?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE TLCOmmentId=TLCOmmentId";

			PreparedStatement preparedStatement = connect.prepareStatement(query);
			try {
				preparedStatement.setString(1, replyId);
				preparedStatement.setString(2, tlcommentId);
				preparedStatement.setString(3, channelId);
				preparedStatement.setInt(4, replyLikeCount);
				preparedStatement.setTimestamp(5, replyPublishedAt);
				preparedStatement.setTimestamp(6, replyUpdatedAt);
				preparedStatement.setString(7, replyTextDisplay);

				preparedStatement.executeUpdate();
			} catch (SQLException e) {
				if (e.getMessage().contains("ReplyTextDisplay")) {
					preparedStatement.setString(1, replyId);
					preparedStatement.setString(2, tlcommentId);
					preparedStatement.setString(3, channelId);
					preparedStatement.setInt(4, replyLikeCount);
					preparedStatement.setTimestamp(5, replyPublishedAt);
					preparedStatement.setTimestamp(6, replyUpdatedAt);
					preparedStatement.setString(7, "");

					preparedStatement.executeUpdate();
				} else {
					lastInsertedReplyTLComment = tlcommentId;
					new Breakpoint().saveBreakPoint("replyOfTopLevelComment", lastInsertedReplyTLComment);
				}
			}
			lastInsertedReplyTLComment = tlcommentId;
		}
		new Breakpoint().saveBreakPoint("replyOfTopLevelComment", lastInsertedReplyTLComment);
	}

	// You need to close the resultSet
	public void close() {
		try {
			if (resultSet != null) {
				resultSet.close();
			}

			if (statement != null) {
				statement.close();
			}

			if (connect != null) {
				connect.close();
			}
		} catch (Exception e) {

		}
	}

	private Timestamp stringToTimestamp(String timeString) throws ParseException {
		DateFormat df1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
		Date result = df1.parse(timeString.replace("Z", ""));
		java.sql.Timestamp ts = new java.sql.Timestamp(result.getTime());
		return ts;
	}
	
	public void baseVideoIdList(LinkedHashSet<String> videoIdSet) throws SQLException{
		statement = connect.createStatement();
		
		
		
	}
	

}
