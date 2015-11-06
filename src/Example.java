
public class Example {
	public static void main(String[] argv)throws Exception{
//		String[] scaleArgs = {"-s", "train_scale", "/Users/weililie/Documents/HKUST/COMP5331/project/data_sets/data_type_A/harberman/haberman.data_formatted.txt"};//directory of training file
//		svm_scale.main(scaleArgs);
//        String[] trainArgs = {"-g", "0.0078125", "-c", "8", "train_data.txt"};//directory of training file
//        System.out.println(trainArgs.length);
//        String modelFile = svm_train.main(trainArgs);    
//        String[] testArgs = {"test_data.txt", modelFile, "output.txt"};//directory of test file, model file, result file  
//        Double accuracy = svm_predict.main(testArgs);  
//        System.out.println("SVM Classification is done! The accuracy is " + accuracy); 
        
        //Test for cross validation  
        String[] crossValidationTrainArgs = {"-v","10",/* "-g", "0.0078125", "-c", "8", */"/Users/weililie/Documents/HKUST/COMP5331/project/data_sets/data_type_A/harberman/haberman.data_formatted.txt"};// 10 fold cross validation  
        String modelFile = svm_train.main(crossValidationTrainArgs);  
        System.out.print("Cross validation is done! The modelFile is " + modelFile);  
	}
}