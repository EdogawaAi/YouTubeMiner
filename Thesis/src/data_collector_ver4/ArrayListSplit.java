package data_collector_ver4;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;

/**
 * This class is a tool to split a LinkedHashSet into smaller blocks by a given
 * block size. The result sets are stored in an arrayList.
 * 
 * @author tian
 *
 * @param <T>
 */
public class ArrayListSplit<T> {

	private LinkedHashSet<String> originSet;
	private int blockSize;

	// Constructor receives the set that requires to be split, and the block
	// size for each split block.
	public ArrayListSplit(LinkedHashSet<String> originSet, int blockSize) {
		this.originSet = originSet;
		this.blockSize = blockSize;
	}

	// This method executes the split function.
	public ArrayList<LinkedHashSet<String>> split() {

		// Calculate the total number of blocks. If the set size is divisible by
		// the block size then add 0, otherwise add an additional 1.
		int count = originSet.size() / blockSize + (originSet.size() % blockSize == 0 ? 0 : 1);
		// Create the storage for the split sets.
		ArrayList<LinkedHashSet<String>> result = new ArrayList<LinkedHashSet<String>>(count);

		Iterator<String> iter = originSet.iterator();
		for (int i = 0; i < count; i++) {
			// Create a temporary set that stores the elements from the origin
			// set. This set has a fixed size which is set up through the
			// blockSize parameter.
			LinkedHashSet<String> set = new LinkedHashSet<String>(blockSize);
			// Put the elements into the temporary set using a iterator of the
			// original set.
			for (int j = 0; j < blockSize && iter.hasNext(); j++) {
				set.add(iter.next());
			}
			// Add the temporary set into the result arrayList.
			result.add(set);
		}

		return result;
	}
}
