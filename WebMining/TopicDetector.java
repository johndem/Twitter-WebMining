import java.awt.Frame;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.bson.Document;

import com.mongodb.Block;
import com.mongodb.client.FindIterable;

public class TopicDetector extends WebMiner {

	// duration of a timeframe set to 30 minutes
	private static final long interval = 1800000;

	// the timestamp of the earliest tweet in the database
	private long startTime;
	// the timestamp of the latest tweet in the database
	private long finishTime;
	// number of total timeframes
	private int k;
	// hashmap containing the events found for each timeframe
	private HashMap<Date, ArrayList<String>> events;
	// // hashmap containing the number of tweets during each timeframe
	private HashMap<Date, Integer> tweetsPerTimeframe;

	// Class constructor, initializing class fields.
	public TopicDetector() {
		super();
		events = new HashMap<Date, ArrayList<String>>();
		tweetsPerTimeframe = new HashMap<Date, Integer>();

		FindIterable<Document> iterable = mongo.getMaxTimestampTweet();
		iterable.forEach(new Block<Document>() {
			@Override
			public void apply(final Document document) {
				finishTime = (Long) document.get("timestamp_ms");
			}
		});

		FindIterable<Document> iterable2 = mongo.getMinTimestampTweet();
		iterable2.forEach(new Block<Document>() {
			@Override
			public void apply(final Document document) {
				startTime = (Long) document.get("timestamp_ms");
			}
		});

		k = (int) ((finishTime - startTime) / interval + 1);
		

	}
	
	public long getStartTime() {
		return startTime;
	}

	public void detectTopics() throws IOException {
		// ArrayList containing the normalised usage of each term found during each timeframe.
		ArrayList<HashMap<String, Double>> hist = new ArrayList<HashMap<String, Double>>();

		for (int i = 0; i < k; i++) {
			// Hashmap containing the user ids of users that posted a specific term, for each one of the
			// terms posted during the current timeframe.
			HashMap<String, HashSet<String>> timeframeTerms = new HashMap<String, HashSet<String>>();
			// Normalised usage of each term during this timeframe.
			HashMap<String, Double> timeframeHist = new HashMap<String, Double>();
			// HashSet containing words that were determined to be bursty during this frame.
			HashSet<String> frameKeywords = new HashSet<String>();
			// HashMap containing the terms of the tweets posted during this frame.
			// Key is the id and value the terms.
			HashMap<String, String> frameTweets = new HashMap<String, String>();
			// HashSet containing the users that made a post during this timeframe.
			HashSet<String> users = new HashSet<String>();

			long rangeStart = getStartTime() + i * interval;
			long rangeFinish = rangeStart + interval;

			// For each one of the tweets posted during this timeframe...
			FindIterable<Document> iterable = mongo.getTweetsInRange(rangeStart, rangeFinish);
			iterable.forEach(new Block<Document>() {
				@Override
				public void apply(final Document document) {
					ArrayList<String> tokens = new ArrayList<String>();
					ArrayList<String> hashtags = new ArrayList<String>();

					StringBuilder b = new StringBuilder();
					Document user_doc = (Document) document.get("user");
					users.add(user_doc.get("user_id").toString());
					
					// Puts the tokens and hashtags to the timeframeTerms hashmap
					// along with the id of the user that posted them.
					tokens = (ArrayList<String>) document.get("tokens");
					for (String term : tokens) {
						if (!timeframeTerms.containsKey(term)) {
							timeframeTerms.put(term, new HashSet<String>());
						}
						timeframeTerms.get(term).add(user_doc.get("user_id").toString());
						b.append(term + " ");
					}
					hashtags = (ArrayList<String>) document.get("hashtags");
					for (String term : hashtags) {
						if (!timeframeTerms.containsKey(term)) {
							timeframeTerms.put(term, new HashSet<String>());
						}
						timeframeTerms.get(term).add(user_doc.get("user_id").toString());
						b.append(term + " ");
					}
					// Also adds the tweet's id and terms to the dedicated map.
					frameTweets.put(document.get("_id").toString(), b.toString().trim());
				}
			});

			// For each one of the terms encountered during this timeframe...
			for (Entry<String, HashSet<String>> entry : timeframeTerms.entrySet()) {

				// The normalized usage of this term during the current timeframe is calculated. 
				double normalizedUsage = (double) entry.getValue().size() / users.size();

				// The usage is added to the dedicated map.
				timeframeHist.put(entry.getKey(), normalizedUsage);

				// The mean average normalised usage of this term during the previous
				// timeframes is calculated.
				double mean = 0.0;
				for (HashMap<String, Double> map : hist) {
					if (map.containsKey(entry.getKey())) {
						mean += map.get(entry.getKey());
					}
				}
				if (i != 0) {
					mean /= i;
				}

				// Same for standard deviation.
				double stDeviation = 0.0;
				for (HashMap<String, Double> map : hist) {
					if (map.containsKey(entry.getKey())) {
						stDeviation += Math.pow(mean - map.get(entry.getKey()), 2);
					} else {
						stDeviation += Math.pow(mean, 2);
					}
				}
				if (i != 0) {
					stDeviation /= i;
				}
				stDeviation = Math.sqrt(stDeviation);

				// If stanard deviation is found to be zero, set it to a value
				// that will result to the term being bursty.
				if (stDeviation == 0.0) {
					stDeviation = normalizedUsage / 2;
				}

				// Burst degree, which is actually z-score, of this term is calculated.
				double b_degree = (normalizedUsage - mean) / stDeviation;
				// If the burst degree is 2 or greater, at least 10% of the users that
				// posted during this frame have used this term and the total number of
				// users was greater than 500, then add the term to the list of the bursty ones.
				if (b_degree >= 2 && normalizedUsage > 0.1 && users.size() > 500) {
					frameKeywords.add(entry.getKey());
				}

			}
			// The usage of terms in this timeframe is added to the history list.
			hist.add(timeframeHist);

			// Puts the number of tweets during this frame to the dedicated map.
			tweetsPerTimeframe.put((new Date(rangeStart)), frameTweets.size());

			// For each one of the bursty words found, create a topic containing
			// as unique keyword that specific bursty word.
			ArrayList<Topic> topics = new ArrayList<Topic>();
			HashMap<String, HashSet<String>> keywordLists = new HashMap<String, HashSet<String>>();
			for (String keyword : frameKeywords) {
				keywordLists.put(keyword, new HashSet<String>());

				for (Entry<String, String> doc : frameTweets.entrySet()) {
					if (doc.getValue().contains(keyword)) {
						keywordLists.get(keyword).add(doc.getKey());
					}
				}
				topics.add(new Topic(keyword, new HashSet<String>(keywordLists.get(keyword))));
			}

			// For each one of the current topics, check if you can
			// merge it with another.
			for (int j = 0; j < topics.size(); j++) {
				for (int l = 0; l < j; l++) {
					if (topics.get(l).getId().equals("")) {
						continue;
					}
					HashSet<String> intersected = new HashSet<String>(topics.get(j).getDocs());
					intersected.retainAll(topics.get(l).getDocs());
					if (intersected.size() >= 8) {
						topics.get(j).setDocs(intersected);
						topics.get(j).setId(topics.get(j).getId() + " " + topics.get(l).getId());
						topics.get(l).setId("");
						topics.get(l).getDocs().clear();
					}
				}

			}

			// For each of the tweets posted during this timeframe,
			// if it contains any bursty word, match it to the most
			// relevant topic and update the topic field in the database.
			for (Entry<String, String> doc : frameTweets.entrySet()) {

				float percentage = 0.0f;
				int index = -1;
				
				for (int j = 0; j < topics.size(); j++) {
					if (topics.get(j).getId().equals("")) {
						continue;
					}
					String[] tokens = topics.get(j).getId().split("\\s+");
					float counter = 0.0f;
					for (int l = 0; l < tokens.length; l++) {
						if (doc.getValue().contains(tokens[l])) {
							counter += 1;
						}
					}
					counter /=  tokens.length;
					if (counter > percentage) {
						percentage = counter;
						index = j;
					}
				}
				if (percentage > 0)
					mongo.insertTweetTopic(doc.getKey(), topics.get(index).getId());
				
			}
			
			// Add the topics found during this timeframe
			// to the dedicated structure.
			ArrayList<String> frameTopics = new ArrayList<>();
			for (Topic x : topics) {
				if (!x.getId().equals("")) {
					frameTopics.add(x.getId());
				}
			}
			events.put(new Date(rangeStart), frameTopics);
		}

		// For each timeframe, write to a file the topics that were
		// detected during its course.
		BufferedWriter bw = new BufferedWriter(new FileWriter("topics.txt"));
		SortedSet<Date> dates = new TreeSet<>(events.keySet());
		for (Date date : dates) {
			if (events.get(date).size() == 0) {
				continue;
			}
			bw.write(date.toString() + "\n");
			for (String topic : events.get(date)) {
				bw.write(topic + "\n");
			}
			bw.write("\n");
		}
		bw.close();
		
		// For each timeframe, write to a file the number of tweets
		// that were posted during its course.
		bw = new BufferedWriter(new FileWriter("tweetsPerFrame.txt"));
		SortedSet<Date> keys = new TreeSet<>(tweetsPerTimeframe.keySet());
		for (Date date : keys) {
			bw.write(date.toString() + ": " + tweetsPerTimeframe.get(date) + "\n");
		}
		bw.close();

	}

}
