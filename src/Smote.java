import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
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
	
	public static void smote(String inputFilePath, int N, int k, int numAttrs) throws Exception {
		//read file
		String outputFilePath = inputFilePath.substring(0, inputFilePath.lastIndexOf(".")) + "_smote" + inputFilePath.substring(inputFilePath.lastIndexOf("."));
        File inputFile = new File(inputFilePath), outputFile = new File(outputFilePath);

        StringBuilder output = new StringBuilder();
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
        	sample = classToTuples.get(classLabel);
        	int newIndex = 0;
        	for(int i = 0; i < sample.size(); i++) {
        		//get k nearest neighbor
        		ArrayList<Integer> nnArray = getNNArray(i, sample, k, numAttrs);
        		//populate
        		populate(N, i, nnArray, k, numAttrs);
        	}
        }
	}
	
	private static void populate(int N, int idx, List<Integer> nnArray, int k, int numAttrs) {
		Random rand = new Random(2015);
		while(N != 0){
			int nn = rand.nextInt()%k;
			Map<Integer, Double> tuple = sample.get(idx);
			Map<Integer, Double> newSynthetic = new HashMap<>();
			for(int i = 0; i < numAttrs; i++) {
				double dif = sample.get(nnArray.get(nn)).get(i) - tuple.get(i);
				double gap = rand.nextDouble();
				newSynthetic.put(i, tuple.get(i) + gap*dif);
			}
			synthetic.add(newSynthetic);
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
