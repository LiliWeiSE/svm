import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.StringTokenizer;

public class undersample {
	public static void main(String[] argv)throws Exception {
		//Counting number of classes
		BufferedReader fp = new BufferedReader(new FileReader("../Formatted_data_sets/car/car.data_formatted.txt"));
		int classcount = 0;
		while (true) {
			String line = fp.readLine();
			if(line == null)
				break;
			StringTokenizer st = new StringTokenizer(line," \t\n\r\f:");
			int classnum = atoi(st.nextToken());
			if (classnum > classcount)
				classcount = classnum;
		}
		fp.close();
		
		//Counting number of each class
		int[] count = new int[classcount];
		for (int i=0;i<classcount;i++)
			count[i] = 0;
		fp = new BufferedReader(new FileReader("../Formatted_data_sets/car/car.data_formatted.txt"));
		while (true) {
			String line = fp.readLine();
			if(line == null)
				break;
			StringTokenizer st = new StringTokenizer(line," \t\n\r\f:");
			count[atoi(st.nextToken())-1]++;
		}
		fp.close();
		
		//Undersampling to get the training set and testing set
		int modelnum = 1; //Number of classifier models or bagged numbers
		double basicrate = 0.7; //The sample rate of the initial training set
		PrintWriter[] fp_train = new PrintWriter[modelnum+1];
		fp_train[0] = new PrintWriter("train.txt", "UTF-8");
		PrintWriter fp_test = new PrintWriter("test.txt", "UTF-8");
		fp = new BufferedReader(new FileReader("../Formatted_data_sets/car/car.data_formatted.txt"));		
		while (true) {
			String line = fp.readLine();
			if(line == null)
				break;
			double random = Math.random();
			if (random<basicrate)
				fp_train[0].println(line);
			else
				fp_test.println(line);
		}
		fp.close();
		fp_train[0].close();
		fp_test.close();
		double mincount = count[0];
		for (int i=1;i<classcount;i++)
			if (count[i]<mincount)
				mincount=count[i];
		double[] samplerate = new double[classcount];
		for (int i=0;i<classcount;i++)
			samplerate[i]=mincount/count[i];
		for (int i=0;i<modelnum;i++) {
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
				if (random<samplerate[atoi(st.nextToken())-1])
					fp_train[i+1].println(line);
			}
			fp.close();
			fp_train[i+1].close();
		}
		
		
		//Training and predicting
		for (int i=0;i<modelnum;i++) {
			StringBuilder str1 = new StringBuilder();
			str1.append("train");
			str1.append(i+1);
			str1.append(".txt");
			String trainfile = str1.toString();
			StringBuilder str2 = new StringBuilder();
			str2.append("output");
			str2.append(i+1);
			str2.append(".txt");
			String outputfile = str2.toString();
			//String[] scaleArgTrain = {"-l", "0", "-s", "scale.txt", trainfile};
			//String[] scaleArgTest = {"-r", "scale.txt", "test.txt"};
			//svm_scale.main(scaleArgTrain);
			//svm_scale.main(scaleArgTest);
			String[] trainArgs = {trainfile};
			String modelFile = svm_train.main(trainArgs);    
			String[] testArgs = {"test.txt", modelFile, outputfile};
			svm_predict.main(testArgs);
		}
		
		//Calculating and printing the result
		BufferedReader fp1 = new BufferedReader(new FileReader("test.txt"));
		BufferedReader[] fp2 = new BufferedReader[modelnum];
		for (int i=0;i<modelnum;i++) {
			StringBuilder str = new StringBuilder();
			str.append("output");
			str.append(i+1);
			str.append(".txt");
			String outputfile = str.toString();
			fp2[i] = new BufferedReader(new FileReader(outputfile));
		}
		int[][] confusionmatrix = new int[classcount][classcount];
		double[] TP = new double[classcount];
		double[] TN = new double[classcount];
		double[] FP = new double[classcount];
		double[] FN = new double[classcount];
		for (int i=0;i<classcount;i++){
			for (int j=0;j<classcount;j++)
				confusionmatrix[i][j]=0;
			TP[i] = 0;
			TN[i] = 0;
			FP[i] = 0;
			FN[i] = 0;
		}
		while (true) {
			String line1 = fp1.readLine();
			String[] line2 = new String[modelnum];
			for (int i=0;i<modelnum;i++)
				line2[i] = fp2[i].readLine();
			if(line1 == null)
				break;
			StringTokenizer st1 = new StringTokenizer(line1," \t\n\r\f:");
			StringTokenizer[] st2 = new StringTokenizer[modelnum];
			for (int i=0;i<modelnum;i++)
				st2[i] = new StringTokenizer(line2[i]," \t\n\r\f:");
			int realclass = atoi(st1.nextToken())-1;
			int[] predictcount = new int[classcount];
			for (int i=0;i<classcount;i++)
				predictcount[i] = 0;
			for (int i=0;i<modelnum;i++)
				predictcount[(int)atof(st2[i].nextToken())-1]++;
			int predictclass = 0;
			for (int i=1;i<classcount;i++)
				if (predictcount[i]>predictcount[predictclass])
					predictclass = i;
			confusionmatrix[realclass][predictclass]++;
			for (int i=0;i<classcount;i++)
				if (realclass == i && predictclass == i)
					TP[i]++;
				else if (realclass == i && predictclass != i)
					FN[i]++;
				else if (realclass != i && predictclass == i)
					FP[i]++;
				else
					TN[i]++;
		}
		fp1.close();
		for (int i=0;i<modelnum;i++)
			fp2[i].close();
		double[] specificity = new double[classcount];
		double[] recall = new double[classcount];
		double[] f2 = new double[classcount];
		for (int i=0;i<classcount;i++) {
			specificity[i] = TN[i]/(TN[i]+FN[i]);
			recall[i] = TP[i]/(TP[i]+FN[i]);
			f2[i] = (1+2*2)*specificity[i]*recall[i]/(2*2*specificity[i]+recall[i]);
		}
		System.out.println("\nClass Numbers : " + classcount);
		for (int i=0;i<classcount;i++)
			System.out.println("Class" + (i+1) + " : " + count[i]);
		System.out.println("SVM Classification is done! The prediction result is:");
		for (int i=0;i<classcount;i++) {
			for (int j=0;j<classcount;j++)
				System.out.format("%8d",confusionmatrix[i][j]);
			System.out.format("       F2-measure : %f", f2[i]);
			System.out.print("\n");
		}
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