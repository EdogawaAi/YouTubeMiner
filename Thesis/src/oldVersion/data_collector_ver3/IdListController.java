package data_collector_ver3;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Iterator;
import java.util.Scanner;

/**
 * This class is for decreasing API usage, by removing the duplicated
 * video/channel/category IDs. The IDs that has been scanned are simply stored
 * in a separate file.
 * 
 * @author tian
 *
 */

public class IdListController {

	/**
	 * Compare a new id set and an already existing id set, and return the
	 * difference. The input file should be a CSV file without \n.
	 * 
	 * @param idSetInput
	 * @param filePath
	 * @return
	 * @throws FileNotFoundException
	 */
	private LinkedHashSet<String> idSetDiff(LinkedHashSet<String> idSetInput, String filePath)
			throws FileNotFoundException {
		LinkedHashSet<String> idSet = new LinkedHashSet<String>();

		Scanner scanner = new Scanner(new File(filePath));
		scanner.useDelimiter(",");
		while (scanner.hasNext()) {
			idSet.add(scanner.next());
		}
		scanner.close();

		idSetInput.removeAll(idSet);

		return idSetInput;
	}

	/**
	 * Update new id set to an existing file.
	 * 
	 * @param idSetUpdate
	 * @param filePath
	 * @throws IOException
	 */
	public void idSetUpdate(LinkedHashSet<String> idSetUpdate, String filePath) throws IOException {
		FileWriter fileWriter = new FileWriter(filePath, true);
		idSetUpdate = idSetDiff(idSetUpdate, filePath);

		Iterator<String> iterator = idSetUpdate.iterator();
		StringBuilder strBuilder = new StringBuilder();
		while (iterator.hasNext()) {
			strBuilder.append(iterator.next() + ",");
		}
		String newIdList = new String();
		BufferedReader br = new BufferedReader(new FileReader(filePath));
		String appendedString = strBuilder.toString().replaceAll(",$", "");
		if (br.readLine() == null) {
			newIdList = appendedString;
		} else {
			if (!appendedString.isEmpty()) {
				newIdList = "," + appendedString;
			} else {
				newIdList = "";
			}
		}
		br.close();
		fileWriter.write(newIdList);
		fileWriter.close();
	}

	public LinkedHashSet<String> getIdSet(String filePath) throws FileNotFoundException {
		LinkedHashSet<String> idSet = new LinkedHashSet<String>();
		
		Scanner scanner = new Scanner(new File(filePath));
		scanner.useDelimiter(",");
		while (scanner.hasNext()) {
			idSet.add(scanner.next());
		}
		scanner.close();
		
		return idSet;
	}
}
