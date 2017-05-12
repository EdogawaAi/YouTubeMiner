package data_collector_ver3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;

public class ArrayListSplit<T> extends ArrayList<T> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private LinkedHashSet<String> originSet;
	private int blockSize;

	public ArrayListSplit(LinkedHashSet<String> originSet, int blockSize) {
		this.originSet = originSet;
		this.blockSize = blockSize;
	}

	public ArrayList<LinkedHashSet<String>> split() {
		int count = originSet.size() / blockSize + (originSet.size() % blockSize == 0 ? 0 : 1);
		ArrayList<LinkedHashSet<String>> result = new ArrayList<LinkedHashSet<String>>(count);
		Iterator<String> iter = originSet.iterator();

		for (int i = 0; i < count; i++) {
			LinkedHashSet<String> set = new LinkedHashSet<String>(blockSize);

			for (int j = 0; j < blockSize && iter.hasNext(); j++) {
				set.add(iter.next());
			}

			result.add(set);
		}

		return result;
	}
}
