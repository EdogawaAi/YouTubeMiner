package data_collector_ver2;

import java.util.ArrayList;
import java.util.Iterator;

public class Try {
	public static void main(String[] args) {
		String myString = "8OLCy5LZzB0,6SBh04KoBoI,ObIJgfwMqVg,hFeOL-Bbdcc,j6rKVFcFt9I,5Ku06X4ax_Q,jZTEaEsHEDU,BUqGCVf4G90,KNEP29RTVfY,BXV3MThKjiI,x_Sggr9hexU,lic0oCDMfwk,_bj6FirMb8s,Jz949SF12PU,xAJZMgSKGqE,phkW-jLJJ68,uCB_4mt7b4c";

		ArrayList<String> str = csvSplitter(myString);
		for (String s : str) {
			System.out.println(s);
		}

	}

	private static ArrayList<String> csvSplitter(String csvString) {
		ArrayList<String> splittedCSVString = new ArrayList<String>();
		int numberPerChunk = 5;
		String str = new String();
		int position = ordinalIndexOf(csvString, ",", numberPerChunk - 1);
		System.out.println(position);
		while (position != -1) {
			System.out.println("remaining:" + csvString.length());
			str = csvString.substring(0, position);
			csvString = csvString.substring(position + 1);
			splittedCSVString.add(str);
			position = ordinalIndexOf(csvString, ",", numberPerChunk - 1);
		}
		System.out.println("remaining:" + csvString.length());
		splittedCSVString.add(csvString);
		return splittedCSVString;
	}

	private static int ordinalIndexOf(String string, String subString, int index) {
		int position = string.indexOf(subString, 0);
		while (index-- > 0 && position != -1) {
			position = string.indexOf(subString, position + 1);
		}
		return position;
	}

}
