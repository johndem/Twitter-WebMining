import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.xml.sax.InputSource;
import org.bson.Document;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;


public class Preprocessor extends WebMiner{

	HashSet<String> stopwords;	//	stopwords to be removed from tweet terms
	HashSet<String> emoticons;	//	lexicon of available emoticons
	HashSet<String> sentiwords;	//	lexicon of sentiwords
	
	//	path to folder containing the streamed dataset/tweets collected
	private static final String filePath = "Tweets";
	
	public Preprocessor() {
		stopwords = new HashSet<>();
		
		//	fill hashset with stopwords
		try (BufferedReader br = new BufferedReader(new FileReader("stopwords.txt"))) {
		    String word;
		    while ((word = br.readLine()) != null) {
		       stopwords.add(word);
		    }
		    br.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		emoticons = new HashSet<>();
		
		//	fill hashset with emoticons
		try (BufferedReader br2 = new BufferedReader(new FileReader(new File("emoticons.txt")))) {
			String line;
			line = br2.readLine();
			while (line != null) {
				emoticons.add(line);
				line = br2.readLine();
			}
			br2.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		sentiwords = new HashSet<>();
		
		//	fill hashset with sentiwords
		try (BufferedReader br3 = new BufferedReader(new FileReader(new File("sentiwords.txt")))) {
			String line;
			line = br3.readLine();
			while (line != null) {
				sentiwords.add(line);
				line = br3.readLine();
			}
			br3.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	//	give a shortened url and get the full/expanded url
	public static String expandUrl(String shortenedUrl) throws IOException {
		URL url = new URL(shortenedUrl);    
        // open connection
        HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection(Proxy.NO_PROXY); 
        
        // stop following browser redirect
        httpURLConnection.setInstanceFollowRedirects(false);
         
        // extract location header containing the actual destination URL
        String expandedURL = httpURLConnection.getHeaderField("Location");
        httpURLConnection.disconnect();
        
        return expandedURL;
    }
	
	//	return a list of person, location, organization entities detected in tweet text by the Stanford NLP library
	public ArrayList<ArrayList<String>> findEntities(String text) throws ClassCastException, ClassNotFoundException, IOException {
		ArrayList<ArrayList<String>> entities = new ArrayList<ArrayList<String>>(2);
		ArrayList<String> persons = new ArrayList<>();
		ArrayList<String> locations = new ArrayList<>();
		ArrayList<String> organizations = new ArrayList<>();
		
		String serializedClassifier = "english.all.3class.distsim.crf.ser.gz";

	    AbstractSequenceClassifier<CoreLabel> classifier = CRFClassifier.getClassifier(serializedClassifier);
	    
	    String result = classifier.classifyWithInlineXML(text);
	    
	    DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
	    domFactory.setNamespaceAware(true);
        try {
        	
        	String xml = "<XML>" + result + "</XML>";
            DocumentBuilder builder = domFactory.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(xml));
            org.w3c.dom.Document dDoc = builder.parse(is);
            
            for (int i = 0; i < dDoc.getElementsByTagName("PERSON").getLength(); i++)
            	persons.add(dDoc.getElementsByTagName("PERSON").item(i).getTextContent());	//	add person entity to respective list
            for (int i = 0; i < dDoc.getElementsByTagName("LOCATION").getLength(); i++)
            	locations.add(dDoc.getElementsByTagName("LOCATION").item(i).getTextContent());	//	add location entity to respective list
            for (int i = 0; i < dDoc.getElementsByTagName("ORGANIZATION").getLength(); i++)
            	organizations.add(dDoc.getElementsByTagName("ORGANIZATION").item(i).getTextContent());	//	add organization entity to respective list
            

        } catch (Exception e) {
            e.printStackTrace();
        }
        
        entities.add(persons);
        entities.add(locations);
        entities.add(organizations);

		return entities;
	}
	
	//	return text annotated with POS tags
	public String POStagger(String text) {
		// Initialize the tagger 
		MaxentTagger tagger = new MaxentTagger("english-left3words-distsim.tagger");
		
		// The tagged string
		String tagged = tagger.tagString(text);
		
		return tagged;
	}
	
	// read the annotated/training dataset and save each tweet's text and label in mongo in the appropriate collection
	public void processTrainingDataset() {
		
		//	path to annotated/training dataset
		File file = new File("C:\\Users\\John\\Documents\\Πανεπιστήμιο\\2ο Εξάμηνο\\Εξόρυξη & Ανάκτηση Πληροφορίας στον Παγκόσμιο Ιστό\\Project\\AnnotatedDataset.txt");
		try (BufferedReader br = new BufferedReader(new FileReader(file))) {
			
			/* ---------------------------------------- training tweet processing --------------------------------------------- */
			
			String line;
			//	for each line in the training dataset file
			while ((line = br.readLine()) != null) {
				String[] tokens = line.split("\\t+");	//	tokenize line to get text and label
				
				if (!tokens[2].equals("interestingness") && !tokens[2].equals("unknown")) {	//	ignore tweets with a label of interestingness and unknown
					
					//	remove quotation marks from start and end of text
					String tweetText = tokens[1];
					if (tweetText.startsWith("\"")) {
						tweetText = tweetText.substring(1);
					}
					if (tweetText.endsWith("\"")) {
						tweetText = tweetText.substring(0, tweetText.length()-1);
					}
					//	remove dots from text
					tweetText = tweetText.replaceAll("\\.", "");
					
					//	get tweet essence/text without other entities and tokens
					ArrayList<String> tokenList = new ArrayList<>();
					ArrayList<String> tokenListTemp = new ArrayList<>();
					ArrayList<String> sentiwordList = new ArrayList<String>();
					String s;
					String[] textTokens2 = tweetText.toLowerCase().trim().split("\\s+");
					//	for each token/term in tweet text only add to token list those that are not hashtags, mentions, urls or RTs
					//	and remove blanks and turn to lower case
					for (String token : textTokens2) {
						if (!token.startsWith("@") && !token.startsWith("#") && !token.startsWith("http") && !token.equals("rt")) {	//	ignore other entities
							tokenListTemp.add(token);
							s = token.replaceAll("[^a-zA-Z ]", " ").trim();	//	replace unwanted characters and trim
							if (sentiwords.contains(s)) {	//	if token is a sentiword add to respective list
								sentiwordList.add(s);
							}
							if (!stopwords.contains(s)) {	//	get rid of other blanks and unnecessary characters
								if (s.contains(" ")) {
									int spaceIndex = s.indexOf(" ");
									s = s.substring(0, spaceIndex);
								}
								if (!s.equals("") && !stopwords.contains(s)) {
									tokenList.add(s);	//	add token to final token list
								}
							}
						}
						
					}
					
					//	tweet essence/text without entities
					String tweetEssence = "";
					for (String item : tokenListTemp) {
						tweetEssence = tweetEssence + " " + item;
					}
					tweetEssence = tweetEssence.toLowerCase().trim();
					
					ArrayList<String> emoticonList = new ArrayList<String>();
					//	tweet emoticons
					for (String emoticon : emoticons) {
						if (tweetText.contains(emoticon)) {
							emoticonList.add(emoticon);
						}
					}
					
					//	tweet text POS tagged
					String tweetPOStagged = ""; //POStagger(tweetEssence);
					
					
					/* ---------------------------------------- document creation for training tweet --------------------------------------------- */
					
					Document tweet = new Document();
					tweet.put("text", tweetText);
					tweet.put("text_clean", tweetEssence);
					tweet.put("tokens", tokenList);
					
					ArrayList<String> persons;
					ArrayList<String> locations;
					ArrayList<String> organizations;
					Document name_entities = new Document();
					name_entities.put("persons", null);
					name_entities.put("locations", null);
					name_entities.put("organizations", null);
					
					tweet.put("name_entities", name_entities);
					
					tweet.put("POStagged", tweetPOStagged);
					tweet.put("created_at", "");
					tweet.put("timestamp_ms", "");
					tweet.put("urls", null);
					tweet.put("hashtags", null);
					tweet.put("mentions", null);
					tweet.put("sentiwords", sentiwordList);
					tweet.put("emoticons", emoticonList);
					
					Document user = new Document();
					user.put("screen_name", "");
					user.put("friends_count", "");
					user.put("followers_count", "");
					user.put("lists_count", "");
					
					tweet.put("user", user);
					
					Document retweet = new Document();
					retweet.put("text", "");
					retweet.put("created_at", "");
					
					Document retweetUser = new Document();
					retweetUser.put("screen_name", "");
					retweetUser.put("friends_count", "");
					retweetUser.put("followers_count", "");
					retweetUser.put("lists_count", "");
					retweet.put("user", retweetUser);
					
					tweet.put("retweet", retweet);
					tweet.put("topic", "");
					tweet.put("sentiment", tokens[2]);
					
					/* ---------------------------------------- Training tweet information display --------------------------------------------- */
					
					/*
					System.out.println("Tweet : " + tweetText);
					System.out.println("Tweet cleaned : " + tweetEssence);
					System.out.println("Tweet text tokens : ");
					for (String item : tokenList) {
						System.out.println("-" + item + "-");
					}
					
					System.out.println();
					System.out.println();
					System.out.println();
					
					System.out.println("Tweet POS tagged : " + tweetPOStagged);
					System.out.println("Sentiment : " + tokens[2]);
					
					System.out.println();
					System.out.println();
					System.out.println();
					*/
					
					
					mongo.insertTraining(tweet);	//	add training tweet to the training mongo collection
					
				}
				
				
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	//	readd tweets in json format and add them in the appropriate mongo collection
	public void preprocess() throws FileNotFoundException {
		
		File[] files = new File(filePath).listFiles();
		
		//	iterate through each json file/tweet
		for (File file : files) {
			
			FileReader reader = new FileReader(filePath + "\\" + file.getName());
			JSONParser jsonParser = new JSONParser();
			
			try {
				
				/* ---------------------------------------- Tweet JSON preprocessing --------------------------------------------- */
				
				
				//	parse JSON tweet information
				JSONObject jsonObject = (JSONObject) jsonParser.parse(reader);
				
				//	tweet language
				String tweetLang = jsonObject.get("lang").toString();
				
				if (tweetLang.equals("en")) {	//	only keep tweets whose language is english
					
					//	tweet text
					String tweetText = jsonObject.get("text").toString();
					
					//	get tweet essence/text without other entities and tokens
					ArrayList<String> tokenList = new ArrayList<>();
					ArrayList<String> tokenListTemp = new ArrayList<>();
					ArrayList<String> sentiwordList = new ArrayList<String>();
					String s;
					String[] textTokens2 = tweetText.trim().toLowerCase().split("\\s+");	//	get tweet text tokens
					//	for each token/term in tweet text only add to token list those that are not hashtags, mentions, urls or RTs
					//	and remove blanks and turn to lower case
					for (String token : textTokens2) {
						if (!token.startsWith("@") && !token.startsWith("#") && !token.startsWith("http") && !token.equals("rt")) {	//	ignore other entities
							tokenListTemp.add(token);
							s = token.replaceAll("[^a-zA-Z ]", " ").trim();	//	replace unwanted characters and trim
							if (sentiwords.contains(s)) {	//	if token is a sentiword add to respective list
								sentiwordList.add(s);
							}
							if (!stopwords.contains(s)) {	//	get rid of other blanks and unnecessary characters
								if (s.contains(" ")) {
									int spaceIndex = s.indexOf(" ");
									s = s.substring(0, spaceIndex);
								}
								if (!s.equals("") && !stopwords.contains(s)) {
									tokenList.add(s);	//	add token to final token list
								}
							}
						}
						
					}
					
					//	tweet essence/text without entities
					String tweetEssence = "";
					for (String item : tokenListTemp) {
						tweetEssence = tweetEssence + " " + item;
					}
					tweetEssence = tweetEssence.toLowerCase().trim();
					
					ArrayList<String> emoticonList = new ArrayList<String>();
					//	tweet emoticons
					for (String emoticon : emoticons) {
						if (tweetText.contains(emoticon)) {
							emoticonList.add(emoticon);
						}
					}
					
					//	tweet name(person, location, organization) entities
					/*
					ArrayList<ArrayList<String>> entities = new ArrayList<ArrayList<String>>();
					entities = findEntities(tweetEssence);
					ArrayList<String> persons = entities.get(0);
					ArrayList<String> locations = entities.get(1);
					ArrayList<String> organizations = entities.get(2);
					*/
					
					//	tweet text POS tagged
					String tweetPOStagged = ""; //	POStagger(tweetEssence);
					
					JSONObject entitiesObject = (JSONObject) jsonObject.get("entities");
							
					//	store tweet hashtags
					ArrayList<String> hashtagList = new ArrayList<>();
					JSONArray hashtags = (JSONArray) entitiesObject.get("hashtags");
					for (int i = 0; i < hashtags.size(); i++) {
						JSONObject hashtagObject = (JSONObject) hashtags.get(i);
						String tweetHashtag = hashtagObject.get("text").toString();
						hashtagList.add("#" + tweetHashtag.toLowerCase());
					}
					
					//	store tweet mentions
					ArrayList<String> mentionList = new ArrayList<>();
					JSONArray mentions = (JSONArray) entitiesObject.get("user_mentions");
					for (int i = 0; i < mentions.size(); i++) {
						JSONObject mentionObject = (JSONObject) mentions.get(i);
						String tweetMention = mentionObject.get("screen_name").toString();
						mentionList.add(tweetMention);
					}
					
					//	tweet creation time
					String tweetTime = jsonObject.get("created_at").toString();
					
					//	tweet timestamp
					long tweetTimestamp = Long.parseLong(jsonObject.get("timestamp_ms").toString());
					
					JSONObject userObject = (JSONObject) jsonObject.get("user");
					
					//	tweet user's friends count
					String userName = userObject.get("id_str").toString();
					
					//	tweet's user friends count
					String userFriends = userObject.get("friends_count").toString();
					
					//	tweet user's lists count
					String userLists = userObject.get("listed_count").toString();
					
					//	tweet user's followers count
					String userFollowers = userObject.get("followers_count").toString();
					
					String retweetText = "", retweetTime = "", retweetUserName = "", retweetUserFriends = "", retweetUserFollowers = "", retweetUserLists = "";
					if (jsonObject.get("retweeted_status") != null) {
						//	retweet content
						JSONObject retweetObject = (JSONObject) jsonObject.get("retweeted_status");
						//	retweet text
						retweetText = retweetObject.get("text").toString();
						
						//	retweeted tweet's time of creation
						retweetTime = retweetObject.get("created_at").toString();
						
						JSONObject retweetUserObject = (JSONObject) retweetObject.get("user");
						
						//	retweet user's screen name
						retweetUserName = retweetUserObject.get("screen_name").toString();
						
						//	retweet user's friends count
						retweetUserFriends = retweetUserObject.get("friends_count").toString();
						
						//	retweet user's followers count
						retweetUserFollowers = retweetUserObject.get("followers_count").toString();
						
						//	retweet user's list count
						retweetUserLists = retweetUserObject.get("listed_count").toString();
					}
					
					//	tweet urls
					ArrayList<String> urlList = new ArrayList<>();
					JSONArray urls = (JSONArray) entitiesObject.get("urls");
					//	store tweet urls appearing in tweet text
					for (int i = 0; i < urls.size(); i++) {
						JSONObject urlObject = (JSONObject) urls.get(i);
						String tweetUrl = urlObject.get("url").toString();
						urlList.add(expandUrl(tweetUrl));
					}
					//	store tweet urls appearing in media entities
					if (jsonObject.get("extended_entities") != null) {
						// tweet extended entities
						JSONObject extendedEntitiesObject = (JSONObject) jsonObject.get("extended_entities");
						JSONArray mediaUrls = (JSONArray) extendedEntitiesObject.get("media");
						for (int i = 0; i < mediaUrls.size(); i++) {
							JSONObject mediaUrlObject = (JSONObject) mediaUrls.get(i);
							String tweetMediaUrl = mediaUrlObject.get("url").toString();
							urlList.add(expandUrl(tweetMediaUrl));
						}
					}	
					
					
					/* ---------------------------------------- tweet information display --------------------------------------------- */
					
					/*
					
					System.out.println("Tweet language : " + tweetLang);
					System.out.println("Tweet time created : " + tweetTime);
					System.out.println("Tweet timestamp_ms : " + tweetTimestamp);
					System.out.println("Tweet : " + tweetText);
					System.out.println("Tweet cleaned : " + tweetEssence);
					System.out.println("Tweet text tokens : ");
					for (String item : tokenList) {
						System.out.println(item);
					}
					System.out.println("Tweet text person entities detected : ");
					for (String item : persons) {
						System.out.println(item);
					}
					System.out.println("Tweet text location entities detected : ");
					for (String item : locations) {
						System.out.println(item);
					}
					System.out.println("Tweet text organization entities detected : ");
					for (String item : organizations) {
						System.out.println(item);
					}
					System.out.println("Tweet POS tagged : " + tweetPOStagged);
					System.out.println("URLs : ");
					for (String item : urlList) {
						System.out.println(item);
					}
					System.out.println("Tweet hashtags # : ");
					for (String item : hashtagList) {
						System.out.println(item);
					}
					System.out.println("Tweet mentions @ : ");
					for (String item : mentionList) {
						System.out.println(item);
					}
					System.out.println("Tweet poster : " + userName);
					System.out.println("Tweet poster number of friends : " + userFriends);
					System.out.println("Tweet poster number of lists : " + userLists);
					System.out.println("Tweet poster number of followers : " + userFollowers);
					
					System.out.println("RT Retweet text : " + retweetText);
					System.out.println("RT Retweet time created : " + retweetTime);
					System.out.println("RT Retweet user screen name : " + retweetUserName);
					System.out.println("RT Retweet user friends : " + retweetUserFriends);
					System.out.println("RT Retweet user followers : " + retweetUserFollowers);
					System.out.println("RT Retweet user lists : " + retweetUserLists);
					
					*/
					
					/* ---------------------------------------- tweet document creation --------------------------------------------- */
				    
					
					Document tweet = new Document();
					tweet.put("text", tweetText);
					tweet.put("text_clean", tweetEssence);
					tweet.put("tokens", tokenList);
					
					Document name_entities = new Document();
					name_entities.put("persons", null);
					name_entities.put("locations", null);
					name_entities.put("organizations", null);
					
					tweet.put("name_entities", name_entities);
					
					tweet.put("POStagged", tweetPOStagged);
					tweet.put("created_at", tweetTime);
					tweet.put("timestamp_ms", tweetTimestamp);
					tweet.put("urls", urlList);
					tweet.put("hashtags", hashtagList);
					tweet.put("mentions", mentionList);
					tweet.put("sentiwords", sentiwordList);
					tweet.put("emoticons", emoticonList);
					
					Document user = new Document();
					user.put("user_id", userName);
					user.put("friends_count", userFriends);
					user.put("followers_count", userFollowers);
					user.put("lists_count", userLists);
					
					tweet.put("user", user);
					
					Document retweet = new Document();
					retweet.put("text", retweetText);
					retweet.put("created_at", retweetTime);
					
					Document retweetUser = new Document();
					retweetUser.put("screen_name", retweetUserName);
					retweetUser.put("friends_count", retweetUserFriends);
					retweetUser.put("followers_count", retweetUserFollowers);
					retweetUser.put("lists_count", retweetUserLists);
					retweet.put("user", retweetUser);
					
					tweet.put("retweet", retweet);
					tweet.put("topic", "");
					tweet.put("sentiment", "");
					
					
					mongo.insertTweet(tweet);	//	add tweet to the tweets mongo collection
					
				}
				
				
			} catch (IOException | org.json.simple.parser.ParseException | ClassCastException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		
	}
	
}