
import java.io.FileNotFoundException;


public class Main {

	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		
		//	create Processor object to handle tweets filtering
		Preprocessor processor = new Preprocessor();
		//	save the json tweets in mongo
		try {
			processor.preprocess();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//	save the annotated dataset in mongo
		processor.processTrainingDataset();
		
		//	create SentimentExtractor object to annotate each tweet in mongo with a sentiment label
		SentimentExtractor se = new SentimentExtractor();
		//	classify tweets according to sentiment
		se.classifyTweetSentiment();
		
		//	create TopicDetector object to perform topic detection in the tweets stored in mongo
		TopicDetector td = new TopicDetector();
		//	detect topics according to keywords
		td.detectTopics();
		
		//	create Statistics object to extract sentiment and topic details
		Statistics stats = new Statistics();
		//	extract sentiment statistics
		stats.overallSentimentStats();
		//	extract topic statistics
		stats.topicStats();
		
	}

}