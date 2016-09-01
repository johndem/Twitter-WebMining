import org.bson.Document;
import org.bson.types.ObjectId;

import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public class MongoDB {
	
	private MongoClient mongoClient;
	private MongoDatabase database;
	private MongoCollection<Document> tweetsCol;	//	collection for the streamed tweets dataset
	private MongoCollection<Document> trainingCol;	//	collection for the annotated/training dataset
	
	public MongoDB() {
		mongoClient = new MongoClient("localhost");
		database = mongoClient.getDatabase("twitter");
		tweetsCol = database.getCollection("tweets");
		trainingCol = database.getCollection("training");
	}
	
	//	insert sentiment for the doc/tweet given its id
	public void insertTweetSentiment(String id, String sentiment) {
		database.getCollection("tweets").updateOne(new Document("_id", new ObjectId(id)), new Document("$set", new Document("sentiment", sentiment)));
	}
	
	//	insert doc/tweet's topic given its id
	public void insertTweetTopic(String id, String topic) {
		database.getCollection("tweets").updateOne(new Document("_id", new ObjectId(id)), new Document("$set", new Document("topic", topic)));
	}
	
	//	insert tweet as document in tweets collection
	public void insertTweet(Document doc) {
		tweetsCol.insertOne(doc);
	}
	
	//	insert tweet from the annotated/training dataset as document in collection training
	public void insertTraining(Document doc) {
		trainingCol.insertOne(doc);
	}
	
	//	return document/tweet from mongo with maximum timestamp/time of creation
	public FindIterable<Document> getMaxTimestampTweet() {
		FindIterable<Document> iterable = database.getCollection("tweets").find().sort(new Document("timestamp_ms", -1)).limit(1);
		
		return iterable;
	}
	
	//	return document/tweet from mongo with minimum timestamp/time of creation
	public FindIterable<Document> getMinTimestampTweet() {
		FindIterable<Document> iterable = database.getCollection("tweets").find().sort(new Document("timestamp_ms", 1)).limit(1);
		
		return iterable;
	}
	
	//	return documents/tweets created in a given timestamp range
	public FindIterable<Document> getTweetsInRange(long start, long finish) {
		FindIterable<Document> iterable = database.getCollection("tweets").find(new Document("timestamp_ms", new Document("$gte", start).append("$lt", finish)));
		
		return iterable;
	}
	
	//	return all documents/tweets of the tweets collection
	public FindIterable<Document> retrieveTweetsCollection() {
		FindIterable<Document> iterable = database.getCollection("tweets").find();
		
		return iterable;
	}
	
	//	return all documents/tweets of the training dataset collection
	public FindIterable<Document> retrieveTrainingCollection() {
		FindIterable<Document> iterable = database.getCollection("training").find();
		
		return iterable;
	}

}