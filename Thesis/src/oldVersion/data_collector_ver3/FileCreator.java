package data_collector_ver3;

import java.io.File;
import java.io.IOException;

public class FileCreator {

	/**
	 * File creator based on the id type, and returns the file path.
	 * 
	 * @param idKind
	 * @return
	 * @throws IOException
	 */
	public String createFile(String idKind) throws IOException {
		String idFilePath = "test_ver3" + File.separator + idKind + "IdList.txt";
		File idFile = new File(idFilePath);
		if (!idFile.exists()) {
			idFile.getParentFile().mkdirs();
			idFile.createNewFile();
		}

		return idFilePath;
	}
}
