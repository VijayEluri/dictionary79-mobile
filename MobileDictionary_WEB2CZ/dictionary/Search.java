package dictionary;

import java.io.*;
import java.util.Vector;
import javax.microedition.lcdui.AlertType;

/**
 * Search implemenatation.
 *
 * @author Jakub Trmota | Forrest79
 */
public final class Search {
	/**
	 * English - czech translate.
	 */
	public static final int ENG_CZE = 0;
	/**
	 * Czech - english translate.
	 */
	public static final int CZE_ENG = 1;

	/**
	 * English.
	 */
	public static final String ENG = "eng";

	/**
	 * Czech.
	 */
	public static final String CZE = "cze";

	/**
	 * Max results.
	 */
	private static final int MAX_RESULTS = 100;

	/**
	 * Dictionary midlet.
	 */
	private Dictionary dictionary = null;

	/**
	 * Result words.
	 */
	private Vector results = null;

	/**
	 * Results found.
	 */
	private int resultsCount = 0;

	/**
	 * Translate direction..
	 */
	private int direction;

	/**
	 * Directory search algorithm.
	 */
	private DictionarySearch dictionarySearch = null;

	/**
	 * Czech indexes.
	 */
	private Vector indexesCze = null;

	/**
	 * English indexes.
	 */
	private Vector indexesEng = null;

	/**
	 * Initialize search and read indexes.
	 * 
	 * @param dictionary
	 * @throws IOException
	 */
	public Search(Dictionary dictionary) throws IOException {
		this.dictionary = dictionary;

		results = new Vector();

		indexesCze = new Vector();		
		DataInputStream disIndexesCze = new DataInputStream(this.getClass().getResourceAsStream("/data/cze.index"));
		while (true) {
			try {
				Index index = new Index(disIndexesCze.readUTF(), disIndexesCze.readShort());
				indexesCze.addElement(index);
			} catch (EOFException e) {
				break;
			}
		}
		disIndexesCze.close();

		indexesEng = new Vector();
		DataInputStream disIndexesEng = new DataInputStream(this.getClass().getResourceAsStream("/data/eng.index"));
		while (true) {
			try {
				Index index = new Index(disIndexesEng.readUTF(), disIndexesEng.readShort());
				indexesEng.addElement(index);
			} catch (EOFException e) {
				break;
			}
		}
		disIndexesEng.close();
	}

	/**
	 * Start searching.
	 * 
	 * @param word
	 * @param direction
	 */
	public void search(String word, int direction) {
		if (dictionarySearch != null) {
			dictionarySearch.cancel();
		}

		dictionarySearch = new DictionarySearch(word, direction);
		Thread threadDictionarySearch = new Thread(dictionarySearch);
		threadDictionarySearch.start();

		this.direction = direction;
	}

	public String[] getResults() {
		String allResults[] = new String[results.size()];

		for (int i = 0; i < allResults.length; i++) {
			allResults[i] = (String) results.elementAt(i);
		}

		return allResults;
	}

	public int getResultsCount() {
		return resultsCount;
	}

	public int getDirection() {
		return direction;
	}

	private void startSearch() {
		results.removeAllElements();
		resultsCount = 0;

		dictionary.startSearch();

		dictionary.repaintResults();
	}

	private void stopSearch() {
		dictionary.stopSearch();

		dictionary.repaintResults();
	}

	/**
	 * Dictionary search algorithm.
	 */
	private class DictionarySearch extends Thread {
		/**
		 * Search word.
		 */
		private String searchWord;

		/**
		 * File (cze or eng).
		 */
		private String file;

		/**
		 * First file index.
		 */
		private int fileIndex;

		/**
		 * Cancel search.
		 */
		private boolean cancel = false;

		/**
		 * Initialize algorithm and find first index file.
		 * 
		 * @param searchWord
		 * @param direction
		 */
		private DictionarySearch(String searchWord, int direction) {
			this.searchWord = lowerCase(searchWord);

			if (direction == ENG_CZE) {
				file = ENG;
				fileIndex = getFileIndex(indexesEng, this.searchWord);
			} else if (direction == CZE_ENG) {
				file = CZE;
				fileIndex = getFileIndex(indexesCze, this.searchWord);
			}
		}

		/**
		 * Find index.
		 * 
		 * @param indexes
		 * @param word
		 * @return
		 */
		private int getFileIndex(Vector indexes, String word) {
			for (int i = indexes.size() - 1; i >= 0; i--) {
				Index index = (Index) indexes.elementAt(i);
				if (index.getWord().compareTo(word) < 0) {
					return index.getIndex();
				}
			}

			return 0;
		}

		/**
		 * Cancel search.
		 */
		public void cancel() {
			cancel = true;
		}

		/**
		 * Run search.
		 */
		public void run() {
			try {
				search();
			} catch (IOException e) {
				dictionary.alert("Chyba", e.getMessage(), AlertType.ERROR);
			}
		}

		/**
		 * Search algorithm.
		 * 
		 * @throws IOException
		 */
		private void search() throws IOException {
			startSearch();

			for (int index = fileIndex; index <= fileIndex + 1; index++) {
				if (cancel) {
					break;
				}

				InputStream dataFile = dictionary.getClass().getResourceAsStream("/data/" + file + index + ".dat");
				if (dataFile != null) {
					DataInputStream reader = new DataInputStream(dataFile);

					String word = "";

					while(true) {
						if (resultsCount == MAX_RESULTS) {
							cancel = true;
						}

						if (cancel) {
							break;
						}

						try {
							word = reader.readUTF();

							if (lowerCase(word).startsWith(this.searchWord)) {
								String[] words = split(word, ":");

								results.addElement(words[0]);
								for (int i = 1; i < words.length; i++) {
									results.addElement(words[i]);
								}
								results.addElement("-");
								resultsCount++;
								dictionary.repaintResults();
							}
						} catch (EOFException e) {
							break;
						}
					}

					reader.close();
				}
			}

			stopSearch();
		}

		/**
		 * Lower case string with czech chars.
		 * 
		 * @param str
		 * @return
		 */
		private String lowerCase(String str) {
			if (str == null) {
				return "";
			} else {
				str = str.toLowerCase();
			}

			StringBuffer lower = new StringBuffer(str.length());
			char c, n;
			for (int i = 0; i < str.length(); i++) {
				c = str.charAt(i);
				switch (c) {
					case 193: n = 225; break; // Á
					case 201: n = 233; break; // É
					case 205: n = 237; break; // Í
					case 211: n = 243; break; // Ó
					case 218: n = 250; break; // Ú
					case 221: n = 253; break; // Ý
					case 268: n = 269; break; // Č
					case 270: n = 271; break; // Ď
					case 282: n = 283; break; // Ě
					case 327: n = 328; break; // Ň
					case 344: n = 345; break; // Ř
					case 352: n = 353; break; // Š
					case 356: n = 357; break; // Ť
					case 366: n = 367; break; // Ů
					case 381: n = 382; break; // Ž
					default: n = c;
				}
				lower.append(n);
			}

			return lower.toString();
		}

		/**
		 * Java string split in J2ME.
		 * 
		 * @param original
		 * @param separator
		 * @return
		 */
		private String[] split(String original, String separator) {
			Vector nodes = new Vector();

			int index = original.indexOf(separator);
			while (index >= 0) {
				nodes.addElement(original.substring(0, index));
				original = original.substring(index + separator.length());
				index = original.indexOf(separator);
			}
			nodes.addElement(original);

			String[] result = new String[nodes.size()];
			if (nodes.size() > 0) {
				for (int loop=0; loop < nodes.size(); loop++) {
					result[loop] = (String)nodes.elementAt(loop);
				}
			}

			return result;
		}
	}

	/**
	 * Class for indexes.
	 */
	private final class Index {
		/**
		 * Word.
		 */
		private String word = "";
		/**
		 * And it's index.
		 */
		private short index = 0;

		/**
		 * Index initialization.
		 * 
		 * @param word
		 * @param index
		 */
		public Index(String word, short index) {
			this.word = word;
			this.index = index;
		}

		/**
		 * Get file index.
		 * 
		 * @return
		 */
		public short getIndex() {
			return index;
		}

		/**
		 * Get word.
		 * 
		 * @return
		 */
		public String getWord() {
			return word;
		}
	}
}