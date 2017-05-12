package data_crawler_test1;

import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONObject;

public class Try {
	public static void main(String[] args) throws ParseException, SQLException {

		// java.util.Date date = new java.util.Date();
		// System.out.println(new java.sql.Timestamp(date.getTime()));

		JSONObject object = new JSONObject().put("key", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
		System.out.println(object.getString("key"));

		Date date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(object.getString("key"));
		java.sql.Timestamp timestamp = new java.sql.Timestamp(date.getTime());
		System.out.println(timestamp.toString());
	}
}
