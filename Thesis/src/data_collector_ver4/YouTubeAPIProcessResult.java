package data_collector_ver4;

import java.util.ArrayList;
import org.json.JSONObject;

/**
 * A simple structure that stores the result of API process. This makes it easy
 * to manage all the results in the main class.
 * 
 * @author tian
 *
 */
public class YouTubeAPIProcessResult {

	// Initiate the attributes of tables that will be inserted to database.
	private ArrayList<JSONObject> videoTableList = new ArrayList<JSONObject>();
	private ArrayList<JSONObject> videoStatisticTableList = new ArrayList<JSONObject>();
	private ArrayList<JSONObject> videoCategoryTableList = new ArrayList<JSONObject>();
	@SuppressWarnings("unchecked")
	private ArrayList<JSONObject>[] videoCommentTableList = new ArrayList[2];
	private ArrayList<JSONObject> channelTableList = new ArrayList<JSONObject>();
	private ArrayList<JSONObject> channelStatisticTableList = new ArrayList<JSONObject>();

	@SuppressWarnings("unchecked")
	public YouTubeAPIProcessResult() {
		videoTableList = new ArrayList<JSONObject>();
		videoStatisticTableList = new ArrayList<JSONObject>();
		videoCategoryTableList = new ArrayList<JSONObject>();
		videoCommentTableList = new ArrayList[2];
		channelTableList = new ArrayList<JSONObject>();
		channelStatisticTableList = new ArrayList<JSONObject>();
	}

	public YouTubeAPIProcessResult(ArrayList<JSONObject> videoTableList, ArrayList<JSONObject> videoStatisticTableList,
			ArrayList<JSONObject> videoCategoryTableList, ArrayList<JSONObject>[] videoCommentTableList,
			ArrayList<JSONObject> channelTableList, ArrayList<JSONObject> channelStatisticTableList) {
		this.videoTableList = videoTableList;
		this.videoStatisticTableList = videoStatisticTableList;
		this.videoCategoryTableList = videoCategoryTableList;
		this.videoCommentTableList = videoCommentTableList;
		this.channelTableList = channelTableList;
		this.channelStatisticTableList = channelStatisticTableList;
	}

	public ArrayList<JSONObject> getVideoTableList() {
		return videoTableList;
	}

	public void setVideoTableList(ArrayList<JSONObject> videoTableList) {
		this.videoTableList = videoTableList;
	}

	public ArrayList<JSONObject> getVideoStatisticTableList() {
		return videoStatisticTableList;
	}

	public void setVideoStatisticTableList(ArrayList<JSONObject> videoStatisticTableList) {
		this.videoStatisticTableList = videoStatisticTableList;
	}

	public ArrayList<JSONObject> getVideoCategoryTableList() {
		return videoCategoryTableList;
	}

	public void setVideoCategoryTableList(ArrayList<JSONObject> videoCategoryTableList) {
		this.videoCategoryTableList = videoCategoryTableList;
	}

	public ArrayList<JSONObject>[] getVideoCommentTableList() {
		return videoCommentTableList;
	}

	public void setVideoCommentTableList(ArrayList<JSONObject>[] videoCommentTableList) {
		this.videoCommentTableList = videoCommentTableList;
	}

	public ArrayList<JSONObject> getChannelTableList() {
		return channelTableList;
	}

	public void setChannelTableList(ArrayList<JSONObject> channelTableList) {
		this.channelTableList = channelTableList;
	}

	public ArrayList<JSONObject> getChannelStatisticTableList() {
		return channelStatisticTableList;
	}

	public void setChannelStatisticTableList(ArrayList<JSONObject> channelStatisticTableList) {
		this.channelStatisticTableList = channelStatisticTableList;
	}

}
