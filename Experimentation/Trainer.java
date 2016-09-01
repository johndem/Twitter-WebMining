import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.Random;

import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.functions.LibSVM;
import weka.classifiers.trees.REPTree;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;

public class Trainer {	//	class to experiment on training different classifiers when provided with an .arff file containing the training dataset in vector form
	
	public Trainer() {
		
	}
	
	//	training of Naive Bayes
	public static void trainNaiveBayesClassifier(String inputPath, String outputPath) throws Exception {
		
		NaiveBayes nb = new NaiveBayes();	//	create Naive Bayes classifier
		
		//	get input for the classifier
		DataSource source = new DataSource(inputPath);
        Instances data = source.getDataSet();
        
        //	set number of different classes/labels for supervised training
		data.setClassIndex(data.numAttributes() - 1);
		
		//	perform training via cross validation
		Evaluation eval = new Evaluation(data);
		eval.crossValidateModel(nb, data, 10, new Random(1));
		
		//	write results and metrics in file
		BufferedWriter bw = new BufferedWriter(new FileWriter(outputPath));
		
		bw.write("NAIVE BAYES\n");
		
		bw.write("Accuracy: " + eval.correct() / data.numInstances() + "\n");
		bw.write("----------------------------------------------\n");
		
		for (int i = 0; i < data.numClasses(); i++) {
			bw.write(data.classAttribute().value(i) + ":\n");
			bw.write("Precision: " + eval.precision(i) + "\n");
			bw.write("Recall: " + eval.recall(i) + "\n");
			bw.write(("FMeasure: " + eval.fMeasure(i)) + "\n");
			bw.write("----------------------------------------------\n");	
		}
		
		bw.close();
		
	}
	
	//	training of SVM
	public static void trainLibSVMClassifier(String inputPath, String outputPath, boolean normalize, boolean shrinking) throws Exception {
		
		LibSVM svm = new LibSVM();	//	create SVM classifier and set parameters
		svm.setNormalize(normalize);
		svm.setShrinking(shrinking);
		
		//	get input for the classifier
		DataSource source = new DataSource(inputPath);
        Instances data = source.getDataSet();
        
        //	set number of different classes/labels for supervised training
		data.setClassIndex(data.numAttributes() - 1);
		 
		//	perform training via cross validation
		Evaluation eval = new Evaluation(data);
		eval.crossValidateModel(svm, data, 10, new Random(1));
		
		//	write results and metrics in file
		BufferedWriter bw = new BufferedWriter(new FileWriter(outputPath));
		
		bw.write("SVM, Normalize: " + normalize + ", Shrinking: " + shrinking + "\n");
		
		bw.write("Accuracy: " + eval.correct() / data.numInstances() + "\n");
		bw.write("----------------------------------------------\n");
		
		for (int i = 0; i < data.numClasses(); i++) {
			bw.write(data.classAttribute().value(i) + ":\n");
			bw.write("Precision: " + eval.precision(i) + "\n");
			bw.write("Recall: " + eval.recall(i) + "\n");
			bw.write(("FMeasure: " + eval.fMeasure(i)) + "\n");
			bw.write("----------------------------------------------\n");	
		}
		
		bw.close();
		
	}
	
	//	training of REPTree
	public static void trainREPTreeClassifier(String inputPath, String outputPath, double minNum, int numFolds) throws Exception {
		
		REPTree repTree = new REPTree();	//	create REPTree classifier and set parameters
		repTree.setMinNum(minNum);
		repTree.setNumFolds(numFolds);
		
		//	get input for the classifier
		DataSource source = new DataSource(inputPath);
        Instances data = source.getDataSet();
        
        //	set number of different classes/labels for supervised training
		data.setClassIndex(data.numAttributes() - 1);

		//	perform training via cross validation
		Evaluation eval = new Evaluation(data);
		eval.crossValidateModel(repTree, data, 10, new Random(1));
		
		//	write results and metrics in file
		BufferedWriter bw = new BufferedWriter(new FileWriter(outputPath));
		
		bw.write("RETree, MinNum: " + minNum + ", NumFolds: " + numFolds + "\n");
		
		bw.write("Accuracy: " + eval.correct() / data.numInstances() + "\n");
		bw.write("----------------------------------------------\n");
		
		for (int i = 0; i < data.numClasses(); i++) {
			bw.write(data.classAttribute().value(i) + ":\n");
			bw.write("Precision: " + eval.precision(i) + "\n");
			bw.write("Recall: " + eval.recall(i) + "\n");
			bw.write(("FMeasure: " + eval.fMeasure(i)) + "\n");
			bw.write("----------------------------------------------\n");	
		}
		
		bw.close();
		
	}

}
