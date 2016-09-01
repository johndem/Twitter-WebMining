import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;

import org.bson.Document;

import com.mongodb.Block;
import com.mongodb.client.FindIterable;


public class Statistics extends WebMiner {
	
	public Statistics() {
		super();
	}
	
	//	initialize and return a hashmap containing each one of the 14 sentiment's frequency
	public HashMap<String, Integer> getSentimentMap() {
		HashMap<String, Integer> sentimentStats = new HashMap<String, Integer>();
		sentimentStats.put("anger", 0);
		sentimentStats.put("disgust", 0);
		sentimentStats.put("fear", 0);
		sentimentStats.put("joy", 0);
		sentimentStats.put("sadness", 0);
		sentimentStats.put("surprise", 0);
		sentimentStats.put("anxiety", 0);
		sentimentStats.put("calm", 0);
		sentimentStats.put("enthusiasm", 0);
		sentimentStats.put("interested", 0);
		sentimentStats.put("nervous", 0);
		sentimentStats.put("rejection", 0);
		sentimentStats.put("shame", 0);
		sentimentStats.put("neutral", 0);
		
		return sentimentStats;
	}
	
	//	return a file containing each sentiment's frequency in the tweets collection/dataset
	public void overallSentimentStats() throws IOException {
		HashMap<String, Integer> sentimentStats = getSentimentMap();

		//	iterate through every document/tweet in mongo and for each sentiment found, update its respective count number
		FindIterable<Document> iterable = mongo.retrieveTweetsCollection();
		iterable.forEach(new Block<Document>() {
			@Override
			public void apply(final Document document) {
				String sentiment = document.get("sentiment").toString();	//	get this tweet's sentiment label
				sentimentStats.put(sentiment, sentimentStats.get(sentiment) + 1);	//	increment the sentiment's overall count number
			}
		});
		
		//	write results in overallSentimentStats.txt
		BufferedWriter bw = new BufferedWriter(new FileWriter("overallSentimentStats.txt"));
		for (Entry<String, Integer> entry : sentimentStats.entrySet()) {
			bw.write(entry.getKey() + " : " + entry.getValue() + "\n");
		}
		bw.close();
	}
	
	//	return a file containing all unique topics detected and the frequency for each sentiment in said topics
	public void topicStats() throws IOException {
		HashMap<String, HashMap<String, Integer>> topicStats = new HashMap<String, HashMap<String, Integer>>();
	
		//	iterate through every document/tweet in mongo and for each topic found, update its respective sentiment's count number
		FindIterable<Document> iterable = mongo.retrieveTweetsCollection();
		iterable.forEach(new Block<Document>() {
			@Override
			public void apply(final Document document) {
				String topic = document.get("topic").toString();	//	get this tweet's topic
				String sentiment = document.get("sentiment").toString();	//	get this tweet's sentiment label
				
				if (!topic.equals("")) {	//	if it's a valid topic
					if (!topicStats.containsKey(topic)) {	//	if inserting topic for the first time, create a sentiment hashmap for it
						topicStats.put(topic, getSentimentMap());
					}
					topicStats.get(topic).put(sentiment, topicStats.get(topic).get(sentiment) + 1);	//	increment the topic's respective sentiment's count number
				}
			}
		});
		
		//	write results in overallTopicStats.txt
		BufferedWriter bw = new BufferedWriter(new FileWriter("overallTopicStats.txt"));
		for (Entry<String, HashMap<String, Integer>> topic : topicStats.entrySet()) {
			bw.write(topic.getKey() + "\n");
			for (Entry<String, Integer> sentiment : topic.getValue().entrySet()) {
				bw.write(sentiment.getKey() + " : " + sentiment.getValue()  + "\n");
			}
			bw.write("\n");
		}
		bw.close();

	}

}
