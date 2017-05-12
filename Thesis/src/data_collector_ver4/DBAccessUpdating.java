package data_collector_ver4;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DBAccessUpdating extends MySQLAccess {

	public void updateRecordID() throws SQLException {

		super.establishConnection();
		String query = "SELECT COUNT(*) FROM videoidrecord";
		PreparedStatement pStatement = super.connect.prepareStatement(query);
		super.resultSet = pStatement.executeQuery();

		long ID = 0;
		while (super.resultSet.next()) {
			ID = resultSet.getLong("COUNT(*)");
			System.out.println(ID);
		}

		String query2 = "UPDATE videoidrecord SET ID=? WHERE ID=NULL";
		PreparedStatement pStatement2 = super.connect.prepareStatement(query2);
		for (long l = 1; l < ID; l++) {
			pStatement2.setString(1, String.valueOf(l));
			pStatement2.executeUpdate();
		}

		super.close();
	}

}
