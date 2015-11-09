import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

class IdxAndDst implements Comparable {
	private int idx;
	private double dst;
	public IdxAndDst(int idx, double dst) {
		super();
		this.idx = idx;
		this.dst = dst;
	}
	public int getIdx() {
		return idx;
	}
	public void setIdx(int idx) {
		this.idx = idx;
	}
	public double getDst() {
		return dst;
	}
	public void setDst(double dst) {
		this.dst = dst;
	}
	@Override
	public int compareTo(Object o) {
		IdxAndDst iad = (IdxAndDst)o;
		double gap = dst - iad.dst;
		if(gap < 0) {
			return -1;
		} else if(gap == 0) {
			return 0;
		} else {
			return 1;
		}
	}
	
}
public class Smote {
	private static double minProportion = 0.1;
	private static List<Map<Integer, Double>> sample, synthetic = new ArrayList<>();
	private static StringBuilder output = new StringBuilder();
	private static int curClassLabel = 0;
	
	public static void main(String args[]) {
		try {
			smote("/Users/weililie/Documents/HKUST/COMP5331/project/data_sets/data_type_A/car/car.data_formatted.txt", 500, 5, 6);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public static void smote(String inputFilePath, int N, int k, int numAttrs) throws Exception {
		//read file
		String outputFilePath = inputFilePath.substring(0, inputFilePath.lastIndexOf(".")) + "_smote" + inputFilePath.substring(inputFilePath.lastIndexOf("."));
        File inputFile = new File(inputFilePath), outputFile = new File(outputFilePath);

        BufferedReader reader = new BufferedReader(new FileReader(inputFile));
        String line;
        HashMap<Integer, List<Map<Integer, Double>>> classToTuples = new HashMap<>();
        
        N = (int)N/100;
        while((line = reader.readLine()) != null) {
        	//record classes and their corresponding tuples
        	String[] tupleItems = line.split(" ");
        	int className = Integer.parseInt(tupleItems[0]);
        	
        	if(!classToTuples.containsKey(className)) {
        		classToTuples.put(className, new ArrayList<Map<Integer, Double>>());
        	}
        	List<Map<Integer, Double>> tupleList = classToTuples.get(className);
        	Map<Integer, Double> attributeMap = new HashMap<>();
        	
        	//get attributes and save
        	for(int i = 1; i < tupleItems.length; i++) {
        		String[] attribute = tupleItems[i].split(":");
        		int index = Integer.parseInt(attribute[0]);
        		Double attr = Double.parseDouble(attribute[1]);
        		attributeMap.put(index, attr);
        	}
        	tupleList.add(attributeMap);
        }
        reader.close();
        
        int totalTupleNum = 0;
        for(List tuples : classToTuples.values()) {
        	totalTupleNum += tuples.size();
        }
        ArrayList<Integer> smallClasses = new ArrayList<>();
        Iterator<Entry<Integer, List<Map<Integer, Double>>>> classIt = classToTuples.entrySet().iterator();
        while(classIt.hasNext()) {
        	Entry<Integer, List<Map<Integer, Double>>> entry = classIt.next();
        	if((double)entry.getValue().size()/totalTupleNum < minProportion) {
        		smallClasses.add(entry.getKey());
        	}
        }
        
        Iterator<Integer> smallClassIt = smallClasses.iterator();
        while(smallClassIt.hasNext()) {
        	int classLabel = smallClassIt.next();
        	curClassLabel = classLabel;
        	sample = classToTuples.get(classLabel);
        	for(int i = 0; i < sample.size(); i++) {
        		//get k nearest neighbor
        		ArrayList<Integer> nnArray = getNNArray(i, sample, k, numAttrs);
        		//populate
        		populate(N, i, nnArray, k, numAttrs);
        	}
        }
        
        //save to new file
        Path bytes = java.nio.file.Files.copy( 
                inputFile.toPath(), 
                outputFile.toPath(),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                java.nio.file.StandardCopyOption.COPY_ATTRIBUTES,
                java.nio.file.LinkOption.NOFOLLOW_LINKS );
        
        BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile, true));
        writer.write(output.toString());
        writer.flush();
        writer.close();
	}
	
	private static void populate(int N, int idx, List<Integer> nnArray, int k, int numAttrs) {
		Random rand = new Random(2015);
		while(N != 0){
			int nn = Math.abs(rand.nextInt()%k);
			Map<Integer, Double> tuple = sample.get(idx);
			Map<Integer, Double> newSynthetic = new HashMap<>();
			for(int i = 0; i < numAttrs; i++) {
				System.out.println("nn: "+ nn);
				System.out.println("nnArray size: " + nnArray.size());
				System.out.println("sample size: " + sample.size());
				System.out.println("sample idx: " + nnArray.get(nn));
				Double nnI = sample.get(nnArray.get(nn)).get(i), tupleI = tuple.get(i);
				if (nnI == null) {
					nnI = 0d;
				}
				if(tupleI == null) {
					tupleI = 0d;
				}
				double dif = nnI - tupleI;
				double gap = rand.nextDouble();
				newSynthetic.put(i, tupleI + gap*dif);
			}
			synthetic.add(newSynthetic);
			output.append(curClassLabel + " ");
			for(int i = 0; i < numAttrs; i++) {
				output.append(i + ":" + newSynthetic.get(i).intValue() + " ");
			}
			output.append("\n");
			N--;
		}
	}
	
	private static ArrayList<Integer> getNNArray(int tupleIdx, List<Map<Integer, Double>> sample, int k, int numAttrs) {
		ArrayList<Integer> nnArray = new ArrayList<>(k);
		Map<Integer, Double> tuple = sample.get(tupleIdx);
		List<IdxAndDst> neighbors = new ArrayList<>();
		for(int i = 0; i < sample.size(); i++){
			if(i == tupleIdx) {
				continue;
			}
			//calculate the distance
			Map<Integer, Double> neighbor = sample.get(i);
			double distance = 0;
			for(int j = 0; j < numAttrs; j++){
				Double value1 = tuple.get(j), value2 = neighbor.get(j);
				if (value1 == null) {
					value1 = (double)0;
				}
				if (value2 == null) {
					value2 = (double)0;
				}
				distance += (value1 - value2) * (value1 - value2);
			}
			distance = Math.sqrt(distance);
			neighbors.add(new IdxAndDst(i, distance));
		}
		Collections.sort(neighbors);
		for(int i = 0; i < k; i++) {
			nnArray.add(neighbors.get(i).getIdx());
		}
		return nnArray;
	}
}
