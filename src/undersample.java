import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.StringTokenizer;

public class Undersample {
	public static void main(String[] argv) {
		int modelNum = 11; //Number of classifier models or bagged numbers
		double  basicRate = 0.7; //The sample rate of the initial training set
		String fileName = "../Formatted_data_sets/car/car.data_formatted.txt";
		try {
			undersample(modelNum, basicRate, fileName);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void undersample(int modelNum, double basicRate, String fileName) throws Exception {
		//Counting number of classes
		int classCount = classSum(fileName);
		
		//Counting number of each class
		int[] count = new int[classCount];
		for (int i=0;i<classCount;i++)
			count[i] = 0;
		classCounter(count, fileName);
		
		//Undersampling to get the training set and testing set
		baggedSetGenerator(classCount, count, modelNum, basicRate, fileName);
		
		//Training and predicting
		baggedTrain(modelNum);
		
		//Calculating and printing the result
		report(classCount, modelNum);
	}
	
	private static void baggedSetGenerator(int classCount, int count[], int modelNum, double basicRate, String fileName) throws Exception {
		PrintWriter[] fp_train = new PrintWriter[modelNum+1];
		fp_train[0] = new PrintWriter("train.txt", "UTF-8");
		PrintWriter fp_test = new PrintWriter("test.txt", "UTF-8");
		BufferedReader fp = new BufferedReader(new FileReader(fileName));		
		while (true) {
			String line = fp.readLine();
			if(line == null)
				break;
			double random = Math.random();
			if (random<basicRate)
				fp_train[0].println(line);
			else
				fp_test.println(line);
		}
		fp.close();
		fp_train[0].close();
		fp_test.close();
		double minCount = count[0];
		for (int i=1;i<classCount;i++)
			if (count[i]<minCount)
				minCount=count[i];
		double[] sampleRate = new double[classCount];
		for (int i=0;i<classCount;i++)
			sampleRate[i]=minCount/count[i];
		for (int i=0;i<modelNum;i++) {
			StringBuilder str = new StringBuilder();
			str.append("train");
			str.append(i+1);
			str.append(".txt");
			String trainfile = str.toString();
			fp_train[i+1] = new PrintWriter(trainfile, "UTF-8");
			fp = new BufferedReader(new FileReader("train.txt"));		
			while (true) {
				String line = fp.readLine();
				if(line == null)
					break;
				StringTokenizer st = new StringTokenizer(line," \t\n\r\f:");
				double random = Math.random();
				if (random<sampleRate[atoi(st.nextToken())-1])
					fp_train[i+1].println(line);
			}
			fp.close();
			fp_train[i+1].close();
		}
	}
	
	private static int classSum(String fileName) throws Exception {
		int classCount = 0;
		BufferedReader fp = new BufferedReader(new FileReader(fileName));
		while (true) {
			String line = fp.readLine();
			if(line == null)
				break;
			StringTokenizer st = new StringTokenizer(line," \t\n\r\f:");
			int classNum = atoi(st.nextToken());
			if (classNum > classCount)
				classCount = classNum;
		}
		fp.close();
		return classCount;
	}
	
	private static void classCounter(int count[], String fileName) throws Exception {
		BufferedReader fp = new BufferedReader(new FileReader(fileName));
		while (true) {
			String line = fp.readLine();
			if(line == null)
				break;
			StringTokenizer st = new StringTokenizer(line," \t\n\r\f:");
			count[atoi(st.nextToken())-1]++;
		}
		fp.close();
	}
	
	private static void baggedTrain(int modelNum) throws Exception {
		// With undersampling
		for (int i=0;i<modelNum;i++) {
			StringBuilder str1 = new StringBuilder();
			str1.append("train");
			str1.append(i+1);
			str1.append(".txt");
			String trainfile = str1.toString();
			StringBuilder str2 = new StringBuilder();
			str2.append("output");
			str2.append(i+1);
			str2.append(".txt");
			String outputFile = str2.toString();
			//String[] scaleArgTrain = {"-l", "0", "-s", "scale.txt", trainfile};
			//String[] scaleArgTest = {"-r", "scale.txt", "test.txt"};
			//svm_scale.main(scaleArgTrain);
			//svm_scale.main(scaleArgTest);
			String[] trainArgs = {trainfile};
			String modelFile = svm_train.main(trainArgs);    
			String[] testArgs = {"test.txt", modelFile, outputFile};
			svm_predict.main(testArgs);
		}
		// Without undersampling
		String[] trainArgs = {"train.txt"};
		String modelFile = svm_train.main(trainArgs);    
		String[] testArgs = {"test.txt", modelFile, "output.txt"};
		svm_predict.main(testArgs);
	}
	
	private static void report(int classCount, int modelNum) throws Exception {
		BufferedReader fp1 = new BufferedReader(new FileReader("test.txt"));
		BufferedReader[] fp2 = new BufferedReader[modelNum];
		for (int i=0;i<modelNum;i++) {
			StringBuilder str = new StringBuilder();
			str.append("output");
			str.append(i+1);
			str.append(".txt");
			String outputFile = str.toString();
			fp2[i] = new BufferedReader(new FileReader(outputFile));
		}
		int[][] confusionMatrix = new int[classCount][classCount];
		for (int i=0;i<classCount;i++)
			for (int j=0;j<classCount;j++)
				confusionMatrix[i][j]=0;
		while (true) {
			String line1 = fp1.readLine();
			String[] line2 = new String[modelNum];
			for (int i=0;i<modelNum;i++)
				line2[i] = fp2[i].readLine();
			if(line1 == null)
				break;
			StringTokenizer st1 = new StringTokenizer(line1," \t\n\r\f:");
			StringTokenizer[] st2 = new StringTokenizer[modelNum];
			for (int i=0;i<modelNum;i++)
				st2[i] = new StringTokenizer(line2[i]," \t\n\r\f:");
			int realClass = atoi(st1.nextToken())-1;
			int[] predictcount = new int[classCount];
			for (int i=0;i<classCount;i++)
				predictcount[i] = 0;
			for (int i=0;i<modelNum;i++)
				predictcount[(int)atof(st2[i].nextToken())-1]++;
			int predictClass = 0;
			for (int i=1;i<classCount;i++)
				if (predictcount[i]>predictcount[predictClass])
					predictClass = i;
			confusionMatrix[realClass][predictClass]++;
		}
		fp1.close();
		for (int i=0;i<modelNum;i++)
			fp2[i].close();
		double[] f2_average = new double[2]; // micro_average_f2 and macro_average_f2
		f2_average[0] = 0; f2_average[1] = 0;
		f2Calculator(confusionMatrix, classCount, f2_average);
		System.out.println("\nClass Number : " + classCount);
		System.out.println("Classifier Number : " + modelNum);
		originalReport(classCount);
		System.out.println("----------SVM with Undersampling----------");
		System.out.println("Confusion Matrix :");
		for (int i=0;i<classCount;i++) {
			for (int j=0;j<classCount;j++)
				System.out.format("%8d",confusionMatrix[i][j]);
			System.out.print("\n");
		}
		System.out.format("The micro-averaged F2-measure : %f \n", f2_average[0]);
		System.out.format("The macro-averaged F2-measure : %f \n", f2_average[1]);
	}
	
	private static void originalReport(int classCount) throws IOException{
		BufferedReader fp1 = new BufferedReader(new FileReader("test.txt"));
		BufferedReader fp2 = new BufferedReader(new FileReader("output.txt"));
		int[][] confusionMatrix = new int[classCount][classCount];
		for (int i=0;i<classCount;i++)
			for (int j=0;j<classCount;j++)
				confusionMatrix[i][j]=0;
		while (true) {
			String line1 = fp1.readLine();
			String line2 = fp2.readLine();
			if(line1 == null)
				break;
			StringTokenizer st1 = new StringTokenizer(line1," \t\n\r\f:");
			StringTokenizer st2 = new StringTokenizer(line2," \t\n\r\f:");
			int realClass = atoi(st1.nextToken())-1;
			int predictClass = (int)atof(st2.nextToken())-1;
			confusionMatrix[realClass][predictClass]++;
		}
		fp1.close();
		fp2.close();
		double[] f2_average = new double[2]; // micro_average_f2 and macro_average_f2
		f2_average[0] = 0; f2_average[1] = 0;
		f2Calculator(confusionMatrix, classCount, f2_average);
		System.out.println("----------Baseline SVM----------");
		System.out.println("Confusion Matrix :");
		for (int i=0;i<classCount;i++) {
			for (int j=0;j<classCount;j++)
				System.out.format("%8d",confusionMatrix[i][j]);
			System.out.print("\n");
		}
		System.out.format("The micro-averaged F2-measure : %f \n", f2_average[0]);
		System.out.format("The macro-averaged F2-measure : %f \n", f2_average[1]);
	}
	
	private static void f2Calculator(int[][] confusionMatrix, int classCount, double[] f2_average){
		double[] TP = new double[classCount];
		double[] TN = new double[classCount];
		double[] FP = new double[classCount];
		double[] FN = new double[classCount];
		for (int i=0;i<classCount;i++){
			TP[i] = 0;
			TN[i] = 0;
			FP[i] = 0;
			FN[i] = 0;
		}
		for (int rClass=0;rClass<classCount;rClass++)
			for (int pClass=0;pClass<classCount;pClass++)
				for (int i=0;i<classCount;i++){
					if (rClass == i && pClass == i)
						TP[i] += confusionMatrix[rClass][pClass];
					else if (rClass == i && pClass != i)
						FN[i] += confusionMatrix[rClass][pClass];
					else if (rClass != i && pClass == i)
						FP[i] += confusionMatrix[rClass][pClass];
					else
						TN[i] += confusionMatrix[rClass][pClass];
				}
		//Calculating F-measure: 
		//Page6 of http://citeseerx.ist.psu.edu/viewdoc/download?doi=10.1.1.104.8244&rep=rep1&type=pdf
		double specificity, recall, TNsum=0, TNFN=0, TPsum=0, TPFN=0;
		double[] f2 = new double[classCount];
		for (int i=0;i<classCount;i++) {
			TNsum += TN[i];
			TNFN += (TN[i]+FN[i]);
			TPsum += TP[i];
			TPFN += (TP[i]+FN[i]);
			f2[i] = (1+2*2)*(TN[i]/(TN[i]+FN[i]))*(TP[i]/(TP[i]+FN[i]))/(2*2*(TN[i]/(TN[i]+FN[i]))+(TP[i]/(TP[i]+FN[i])));
			f2_average[1] += f2[i];
		}
		f2_average[1] = f2_average[1]/classCount;
		specificity = TNsum/TNFN;
		recall = TPsum/TPFN;
		f2_average[0] = (1+2*2)*specificity*recall/(2*2*specificity+recall);
	}
	
	private static int atoi(String s)
	{
		return Integer.parseInt(s);
	}
	
	private static double atof(String s)
	{
		double d = Double.valueOf(s).doubleValue();
		if (Double.isNaN(d) || Double.isInfinite(d))
		{
			System.err.print("NaN or Infinity in input\n");
			System.exit(1);
		}
		return(d);
	}
}