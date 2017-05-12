package addID_to_db;

import java.sql.SQLException;

import data_collector_ver4.DBAccessUpdating;

public class AddID {
	public static void main(String[] args) throws SQLException{
		
		DBAccessUpdating dbAccessUpdating = new DBAccessUpdating();
		dbAccessUpdating.updateRecordID();
		
	}

}
