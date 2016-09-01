import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.bson.Document;

import com.mongodb.Block;
import com.mongodb.client.FindIterable;

import weka.classifiers.bayes.NaiveBayes;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

public class SentimentExtractor extends WebMiner {

	Vectorizer vec;	//	object of class Vectorizer to produce corpus and create vectors
	FastVector fvClassVal;	//	vector of classes upon which training and classification will be performed

	public SentimentExtractor() {
		super();
		vec = new Vectorizer();
		//	set the 14 sentiment labels
		fvClassVal = new FastVector(14);
		fvClassVal.addElement("anger");
		fvClassVal.addElement("disgust");
		fvClassVal.addElement("fear");
		fvClassVal.addElement("joy");
		fvClassVal.addElement("sadness");
		fvClassVal.addElement("surprise");
		fvClassVal.addElement("anxiety");
		fvClassVal.addElement("calm");
		fvClassVal.addElement("enthusiasm");
		fvClassVal.addElement("interested");
		fvClassVal.addElement("nervous");
		fvClassVal.addElement("rejection");
		fvClassVal.addElement("shame");
		fvClassVal.addElement("neutral");
	}

	//	train the Naive Bayes classifier and then iterate over all tweets of database and assign a sentiment label to them via classification
	public void classifyTweetSentiment() throws Exception {
		
		//	get combined lexicon(i.e. the corpus of sentiwords, exclamation marks and emoticons) upon which a vector for each tweet will be created
		HashMap<String, Integer> features = vec.getCombinedLexicon();
		ArrayList<HashMap<Integer, String>> featureVectorsTrain = new ArrayList<HashMap<Integer, String>>();
		
		//	for each tweet in the training collection
		FindIterable<Document> iterable = mongo.retrieveTrainingCollection();
		iterable.forEach(new Block<Document>() {
			@Override
			public void apply(final Document document) {	//	create vector based on the feature selected(here combination of sentiwords, exclamation marks and emoticons)
				featureVectorsTrain.add(vec.getCombinedVector(features, true, document));	//	add vector to featureVectorsTrain list
			}
		});

		//	get training dataset in the appropriate weka shape to pass to the classifier by sending the featureVectorsTrain list(containing all training tweets)
		Instances train = createWekaSet(features, featureVectorsTrain, true, false);
		
		NaiveBayes nb = new NaiveBayes();	//	initialize naive bayes classifier
		nb.buildClassifier(train);	//	train classifier with the annotated dataset
		
		//	for each tweet in the tweets collection
		FindIterable<Document> iterable2 = mongo.retrieveTweetsCollection();
		iterable2.forEach(new Block<Document>() {
			@Override
			public void apply(final Document document) {
				ArrayList<HashMap<Integer, String>> featureVectorsTest = new ArrayList<HashMap<Integer, String>>();
				
				//	create vector based on the feature selected(here combination of sentiwords, exclamation marks and emoticons)
				featureVectorsTest.add(vec.getCombinedVector(features, false, document));	//	add vector to featureVectorsTest list
				
				//	transform and get tweet in the appropriate weka shape to pass to the classifier by sending the featureVectorsTest list(containing a single tweet every time)
				Instances test = createWekaSet(features, featureVectorsTest, false, false);
				
				double pred;
				String id = document.get("_id").toString();	//	get tweet/document id
				
				try {
					pred = nb.classifyInstance(test.instance(0));	//	classify tweet via naive bayes
					mongo.insertTweetSentiment(id, fvClassVal.elementAt((int) pred).toString());	//	insert extracted sentiment label in the same tweet/document in mongo
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
		
	}

	//	create and return a weka Instances object to be handed as input to the classifier structured by the corpus(features)
	//	and based on the content of the list of vector(s) passed as parameter(featureVectors)
	public Instances createWekaSet(HashMap<String, Integer> features, ArrayList<HashMap<Integer, String>> featureVectors, boolean labeled, boolean exportToArff) {
		
		FastVector atts;
		
		//	set up attributes
		atts = new FastVector();

		//	declare the class attribute along with its values
		Attribute ClassAttribute = new Attribute("theClass", fvClassVal);

		HashMap<Integer, String> myNewHashMap = new HashMap<>();
		for (HashMap.Entry<String, Integer> entry : features.entrySet()) {
			myNewHashMap.put(entry.getValue(), entry.getKey());
		}

		//	create a new attribute/dimension for each vecter/item of corpus(combined lexicon of sentiwords, exclamation marks and emoticons)
		for (int i = 0; i < features.size(); i++) {
			atts.addElement(new Attribute(myNewHashMap.get(i)));
		}
		
		//	declare the feature vector
		atts.addElement(ClassAttribute);

		//	create empty Instances object with the appropriate dimensionality based on the corpus
		Instances data = new Instances("MyRelation", atts, 0);

		//	create data/weka vector for each feature vector passed in this function
		for (HashMap<Integer, String> map : featureVectors) {

			//	to be filled with data
			double[] vals = new double[data.numAttributes()];

			if (labeled) {	//	if training vector add its sentiment label
				vals[data.numAttributes() - 1] = fvClassVal.indexOf(map.get(-1));
			} else {
				vals[data.numAttributes() - 1] = Instance.missingValue();
			}
			for (int i = 0; i < data.numAttributes() - 1; i++) { // -1
				if (map.containsKey(i)) {
					vals[i] = Double.parseDouble(map.get(i));	//	populate with vector values
				}
			}
			
			data.add(new Instance(1.0, vals));
		}
		
		if (exportToArff) {	//	choose to export weka set as .arff file
			try {
				BufferedWriter bw = new BufferedWriter(new FileWriter("features.arff"));
				bw.write(data.toString());
				bw.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		data.setClassIndex(data.numAttributes() - 1);
		
		return data;
	}
}
