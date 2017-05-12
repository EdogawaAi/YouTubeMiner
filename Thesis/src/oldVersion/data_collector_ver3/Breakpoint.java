package data_collector_ver3;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Stack;

public class Breakpoint {

	/**
	 * Save the break point (last successfully inserted record) to a local file.
	 * 
	 * @param idType
	 * @param breakId
	 * @return
	 * @throws IOException
	 */
	public String saveBreakPoint(String idType, String breakId) throws IOException {
		String idFilePath = "test_ver3" + File.separator + idType + "_BreakPoint.txt";
		File idFile = new File(idFilePath);
		if (!idFile.exists()) {
			idFile.getParentFile().mkdirs();
			idFile.createNewFile();
		}
		if (!breakId.isEmpty()) {
			FileWriter writer = new FileWriter(idFile, false);
			writer.write(breakId);
			writer.close();
		}
		return idFilePath;
	}

	/**
	 * This method is able to return the id set from the last successfully
	 * inserted record.
	 * 
	 * @param updatedIdSet
	 * @param breakPointType
	 * @return
	 * @throws IOException
	 */
	public LinkedHashSet<String> breakPointRestoreSet(LinkedHashSet<String> updatedIdSet, String breakPointType)
			throws IOException {
		LinkedHashSet<String> newIdSet = new LinkedHashSet<String>();
		String breakPointFilePath = "test_ver3" + File.separator + breakPointType + "_BreakPoint.txt";
		File breakPointFile = new File(breakPointFilePath);
		BufferedReader br = new BufferedReader(new FileReader(breakPointFile));
		String breakPointId = br.readLine();
		br.close();
		Stack<String> pendingId = new Stack<String>();

		LinkedList<String> updateIdList = new LinkedList<String>(updatedIdSet);
		Iterator<String> idItr = updateIdList.descendingIterator();
		if (idItr.hasNext()) {
			String currentItr = idItr.next();
			while (idItr.hasNext() && !currentItr.equals(breakPointId)) {
				pendingId.push(currentItr);
				currentItr = idItr.next();
			}
		}

		while (!pendingId.isEmpty()) {
			newIdSet.add(pendingId.pop());
		}

		return newIdSet;
	}
}
