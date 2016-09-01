import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.bson.Document;

import com.mongodb.Block;
import com.mongodb.client.FindIterable;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.bayes.NaiveBayesUpdateable;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffLoader;

public class Vectorizer extends WebMiner { // class to create vectors according
											// to the selected feature for data
											// representation

	public Vectorizer() {
		super();
	}

	// Creates a corpus of terms, consisting of the terms found in the training set.
	public HashMap<String, Integer> getTerms() {
		HashMap<String, Integer> terms = new HashMap<>();
		FindIterable<Document> iterable = mongo.retrieveTrainingCollection();
		iterable.forEach(new Block<Document>() {
			@Override
			public void apply(final Document document) {
				String str = document.get("tokens").toString();
				String[] tokens = str.replaceAll("[^a-zA-Z ]", " ").trim().split("\\s+");
				for (int i = 0; i < tokens.length; i++) {
					if (!terms.containsKey(tokens[i])) {
						int id = terms.size();
						terms.put(tokens[i], id);
					}
				}
			}
		});
		return terms;
	}

	// Given a corpus with the terms-features to be used and a tweet from the database, returns the vector
	// representation of that tweet for the specific features.
	public HashMap<Integer, String> getTermVector(HashMap<String, Integer> terms, boolean frequency, boolean labeled,
			Document doc) {

		HashMap<Integer, String> vector = new HashMap<>();
		if (labeled) {
			vector.put(-1, doc.get("sentiment").toString());
		}
		String str = doc.get("tokens").toString();
		String[] tokens = str.replaceAll("[^a-zA-Z ]", " ").trim().split("\\s+");

		for (int i = 0; i < tokens.length; i++) {
			int id;
			if (terms.containsKey(tokens[i])) {
				id = terms.get(tokens[i]);
				if (vector.containsKey(id) && frequency) {
					int freq = Integer.parseInt(vector.get(id));
					freq += 1;
					vector.put(id, Integer.toString(freq));
				} else {
					vector.put(id, "1");
				}
			}
		}
		return vector;
	}

	// Creates a corpus, consisting of the bigrams found in the training set.
	public HashMap<String, Integer> getBigrams() {
		HashMap<String, Integer> bigrams = new HashMap<>();
		FindIterable<Document> iterable = mongo.retrieveTrainingCollection();
		iterable.forEach(new Block<Document>() {
			@Override
			public void apply(final Document document) {
				String str = document.get("tokens").toString();
				String[] tokens = str.replaceAll("[^a-zA-Z ]", " ").trim().split("\\s+");
				for (int i = 0; i < tokens.length - 1; i++) {
					String bigram = tokens[i] + " " + tokens[i + 1];
					if (!bigrams.containsKey(bigram)) {
						int id = bigrams.size();
						bigrams.put(bigram, id);
					}
				}
			}
		});
		return bigrams;
	}

	// Given a corpus with the bigrams-features to be used and a tweet from the database, returns the vector
	// representation of that tweet for the specific features.
	public HashMap<Integer, String> getBigramVector(HashMap<String, Integer> bigrams, boolean frequency,
			boolean labeled, Document doc) {

		HashMap<Integer, String> vector = new HashMap<>();
		if (labeled) {
			vector.put(-1, doc.get("sentiment").toString());
		}
		String str = doc.get("tokens").toString();
		String[] tokens = str.replaceAll("[^a-zA-Z ]", " ").trim().split("\\s+");

		for (int i = 0; i < tokens.length - 1; i++) {
			String bigram = tokens[i] + " " + tokens[i + 1];
			if (bigrams.containsKey(bigram)) {
				int id = bigrams.get(bigram);
				if (vector.containsKey(id) && frequency) {
					int freq = Integer.parseInt(vector.get(id));
					freq += 1;
					vector.put(id, Integer.toString(freq));
				} else {
					vector.put(id, "1");
				}
			}
		}
		return vector;
	}

	// Creates a corpus consisting of emoticons found in the input file.
	public HashMap<String, Integer> getEmoticons() throws IOException {
		HashMap<String, Integer> emoticons = new HashMap<>();
		File file = new File(
				"C:\\Users\\John\\Documents\\Πανεπιστήμιο\\2ο Εξάμηνο\\Εξόρυξη & Ανάκτηση Πληροφορίας στον Παγκόσμιο Ιστό\\Project\\emoji_new.txt");

		BufferedReader br = new BufferedReader(new FileReader(file));
		String line = br.readLine();
		int id = 0;
		while (line != null) {
			emoticons.put(line, id);
			id++;
			line = br.readLine();
		}
		br.close();
		return emoticons;
	}

	// Given a corpus with the emoticons-features to be used and a tweet from the database, returns the vector
	// representation of that tweet for the specific features.
	public HashMap<Integer, String> getEmoticonVector(HashMap<String, Integer> emoticons, boolean labeled,
			Document doc) {
		HashMap<Integer, String> vector = new HashMap<>();
		if (labeled) {
			vector.put(-1, doc.get("sentiment").toString());
		}

		ArrayList<String> tweetEmoticons = (ArrayList<String>) doc.get("emoticons");

		for (String emoticon : tweetEmoticons) {
			vector.put(emoticons.get(emoticon), "1");
		}

		return vector;
	}

	// Creates a corpus consisting ! and ?.
	public HashMap<String, Integer> getExMarks() {
		HashMap<String, Integer> exMarks = new HashMap<>();
		exMarks.put("!", 0);
		exMarks.put("?", 1);
		return exMarks;
	}
	
	// Given a corpus with the exclamation marks-features to be used and a tweet from the database, returns the vector
	// representation of that tweet for the specific features.
	public HashMap<Integer, String> getExMarksVector(boolean train, Document doc) {
		HashMap<Integer, String> vector = new HashMap<>();
		if (train) {
			vector.put(-1, doc.get("sentiment").toString());
		}

		String text = doc.get("text_clean").toString();
		if (text.contains("!"))
			vector.put(0, "1");
		if (text.contains("?"))
			vector.put(1, "1");

		return vector;
	}

	// Creates a corpus consisting of sentiwords found in the input file.
	public HashMap<String, Integer> getSentiwords() throws IOException {
		HashMap<String, Integer> sentiwords = new HashMap<>();
		File file = new File(
				"C:\\Users\\John\\Documents\\Πανεπιστήμιο\\2ο Εξάμηνο\\Εξόρυξη & Ανάκτηση Πληροφορίας στον Παγκόσμιο Ιστό\\Project\\sentiwords.txt");

		BufferedReader br = new BufferedReader(new FileReader(file));
		String line = br.readLine();
		int id = 0;

		while (line != null) {
			sentiwords.put(line, id);
			id++;
			line = br.readLine();
		}
		br.close();
		return sentiwords;
	}

	// Given a corpus with the sentiwords-features to be used and a tweet from the database, returns the vector
	// representation of that tweet for the specific features.
	public HashMap<Integer, String> getSentiwordsVector(HashMap<String, Integer> sentiwords, boolean labeled,
			Document doc) {
		HashMap<Integer, String> vector = new HashMap<>();
		if (labeled) {
			vector.put(-1, doc.get("sentiment").toString());
		}

		ArrayList<String> tweetSentiwords = (ArrayList<String>) doc.get("sentiwords");

		for (String sentiword : tweetSentiwords) {
			vector.put(sentiwords.get(sentiword), "1");
		}

		return vector;
	}

	// Creates a corpus consisting of sentiwords, emoticons and exclamation marks found.
	public HashMap<String, Integer> getCombinedLexicon() throws IOException {
		HashMap<String, Integer> combinedLexicon = new HashMap<>();
		HashMap<String, Integer> sentiwords = getSentiwords();
		HashMap<String, Integer> emoticons = getEmoticons();
		HashMap<String, Integer> exMarks = getExMarks();

		for (Entry<String, Integer> entry : sentiwords.entrySet()) {
			combinedLexicon.put(entry.getKey(), entry.getValue());
		}
		for (Entry<String, Integer> entry : emoticons.entrySet()) {
			combinedLexicon.put(entry.getKey(), entry.getValue() + sentiwords.size());
		}
		for (Entry<String, Integer> entry : exMarks.entrySet()) {
			combinedLexicon.put(entry.getKey(), entry.getValue() + sentiwords.size() + emoticons.size());
		}

		return combinedLexicon;
	}

	// Given a corpus with the combined features of sentiwords, emoticons and ex. marks, and a tweet from the database,
	// returns the vector representation of that tweet for the specific features.
	public HashMap<Integer, String> getCombinedVector(HashMap<String, Integer> combinedLexicon, boolean labeled,
			Document doc) {
		HashMap<Integer, String> vector = new HashMap<>();
		if (labeled) {
			vector.put(-1, doc.get("sentiment").toString());
		}

		String text = doc.get("text_clean").toString();
		ArrayList<String> tweetSentiwords = (ArrayList<String>) doc.get("sentiwords");
		ArrayList<String> tweetEmoticons = (ArrayList<String>) doc.get("emoticons");
		
		if (text.contains("!")) {
			vector.put(combinedLexicon.get("!"), "1");
		}
		
		if (text.contains("?")) {
			vector.put(combinedLexicon.get("?"), "1");
		}
		
		for (String sentiword : tweetSentiwords) {
			vector.put(combinedLexicon.get(sentiword), "1");
		}
		
		for (String emoticon : tweetEmoticons) {
			vector.put(combinedLexicon.get(emoticon), "1");
		}

		return vector;
	}

}
