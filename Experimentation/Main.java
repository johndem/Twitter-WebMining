
public class Main {

	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		
		boolean[][] svmArgs = { { false, true }, { false, false }, { true, true }, { true, false } };	//	different arguments for SVM to experiment (normalize or not, shrink or not)
		double[][] repTree = { { 2, 3 }, { 2, 5 }, { 2, 8 }, { 2, 3 }, { 5, 3 }, { 8, 3 } };	//	different arguments for REPTree to experiment(values for minNum and number of folds)
		
		switch (args[2]) {
		case "1" :	//	call Naive Bayes classifier
			Trainer.trainNaiveBayesClassifier(args[0], args[1]);
			break;
		case "2" :	//	call SVM classifier
			for (int i = 0; i < 4; i++) {
				Trainer.trainLibSVMClassifier(args[0], i + args[1], svmArgs[i][0], svmArgs[i][1]);
			}
			break;
		case "3" :	//	call REPTree classifier
			for (int i = 0; i < 6; i++) {
				Trainer.trainREPTreeClassifier(args[0], i + args[1], repTree[i][0], (int)repTree[i][1]);
			}
			break;
		default :
			break;
		}

	}

}