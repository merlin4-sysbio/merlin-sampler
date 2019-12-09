package pt.uminho.ceb.biosystems.merlin.sampler;


import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

import es.uvigo.ei.aibench.core.operation.annotation.Cancel;
import es.uvigo.ei.aibench.core.operation.annotation.Direction;
import es.uvigo.ei.aibench.core.operation.annotation.Operation;
import es.uvigo.ei.aibench.core.operation.annotation.Port;
import es.uvigo.ei.aibench.core.operation.annotation.Progress;
import es.uvigo.ei.aibench.workbench.Workbench;
import pt.uminho.ceb.biosystems.merlin.gui.datatypes.annotation.AnnotationEnzymesAIB;
import pt.uminho.ceb.biosystems.merlin.gui.utilities.TimeLeftProgress;
import pt.uminho.ceb.biosystems.merlin.core.datatypes.WorkspaceDataTable;
import pt.uminho.ceb.biosystems.merlin.core.datatypes.WorkspaceGenericDataTable;

@Operation(name="find best alpha", description="find best alpha for enzymes annotation")
public class AnnotationEnzymesBestAlpha {
	
	private AnnotationEnzymesAIB homologyDataContainer;
	private WorkspaceGenericDataTable mainTableData;
	private int ecnumbersColumnNumber, ecScoreColumnNumber, bestAlpha=1;
	private Map<Integer,String> ecMap = new HashMap<>();
	private Map<Integer, String> ecCurated;
	private Map<Integer, Map<Integer, Integer>> counts;
	private Object[][] matrix, confusionMat;
	private double[] accuracy = new double[11];
	private int[] array = IntStream.range(0, 11).toArray();
	private int[] below = new int[11], above = new int[11], total = new int[11];
	private double[] upper = new double[11], lower = new double[11];
	private double[] threshold = {0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9};
	private double[] alpha = {0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0};
	private AtomicBoolean cancel = new AtomicBoolean();
	private TimeLeftProgress progress = new TimeLeftProgress();
	private String blastDatabase;
	
	@Port(direction=Direction.INPUT, name="ecCurated", order=1)
	public void setEcCurated(Map<Integer, String> ecCurated){

		this.ecCurated = ecCurated;
	};
	
	@Port(direction=Direction.INPUT, name="ecMap", order=2)
	public void setEcMap(Map<Integer, String> ecMap){

		this.ecMap = ecMap;
	};
	

	@Port(direction=Direction.INPUT, name="homologyDataContainer", order=3)
	public void setEnzymesAnnotationDataInterface(AnnotationEnzymesAIB homologyDataContainer){

		this.homologyDataContainer = homologyDataContainer;
	};
	
	@Port(direction=Direction.INPUT, name="ecnumbersColumnNumber", order=4)
	public void setEcnumbersColumnNumber(int ecnumbersColumnNumber){

		this.ecnumbersColumnNumber = ecnumbersColumnNumber;
	};
	
	@Port(direction=Direction.INPUT, name="itemsList", order=5)
	public void setBlastDatabase(String blastDatabase){
		
		this.blastDatabase = blastDatabase;	
	}
	
	@Port(direction=Direction.INPUT, name="ecScoreColumnNumber", order=6)
	public void setEcScoreColumnNumber(int ecScoreColumnNumber){

		try {
			this.ecScoreColumnNumber = ecScoreColumnNumber;
			
			if(!this.cancel.get())
				this.bestAlpha();
			
			WorkspaceDataTable data = null;
			
			Object[] xAndY = BestAlphaStatsCalculator.getXAndY(below, above, total, accuracy);
			
			double[] x = (double[]) xAndY[0];
			double[] y = (double[]) xAndY[1];
			
			if(!this.cancel.get())
				data = BestAlphaStatsCalculator.resultsTable(array, lower, upper, y, x, threshold, accuracy, below, above, total);
			
			bestAlpha = BestAlphaStatsCalculator.getBestAlphaIndex(y, accuracy);
			
			if(!this.cancel.get())
				new BestParametersGUI(blastDatabase, bestAlpha, threshold, confusionMat, counts, data, homologyDataContainer);
		} catch (Exception e) {
			Workbench.getInstance().error(e);
			e.printStackTrace();
		}
	};
	
	public void scorer(){
		
		int matrixColumnSize = 23;
		Object[][] matrix = new Object[ecMap.size()][matrixColumnSize];
		int column = 1;
		
		try {
			for (int i = 0; i < alpha.length; i++){
				
				if(!this.cancel.get()){
					homologyDataContainer.setAlpha(alpha[i]);
					mainTableData = homologyDataContainer.getAllGenes(blastDatabase, true);   //compor
					
					int row = 0;
					
					for (Integer key: ecMap.keySet()){
						
						if (alpha[i]<0.1)	
							matrix[row][0] = key;
						String[] ecNumber = (String[]) mainTableData.getValueAt(key, ecnumbersColumnNumber);
						String score = (String) mainTableData.getValueAt(key, ecScoreColumnNumber);
						
						matrix[row][column] = Arrays.asList(ecNumber).get(1);
						matrix[row][column+1] = Double.parseDouble(score.replace("<", ""));
						
						row++;
					}
				}
			column=column+2;
			}
			if(!this.cancel.get())
				homologyDataContainer.setAlpha(0.5);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		this.matrix = matrix;
	}
	
	public void confusionMatrix (){
		
		int key, newKey, confRow = 0;
		int confMatColumnSize = 11, confMatRowSize = 9;
		Object[][] confMat = new Object[confMatRowSize][confMatColumnSize];
		
		
		try {
			
			for (int i = 0; i < threshold.length ; i++ ){
				if(!this.cancel.get()){
					int confCol = 0;
					
					for (int column=1; column<23; column=column+2){
						int tP = 0, tN = 0, fP = 0, fN = 0;
						
						for (int row=0; row<matrix.length; row++){
							key = (int) matrix[row][0];
							newKey = Integer.parseInt(ecMap.get(key));
							
							String[] ecN = ((String) matrix[row][column]).split(", ");
							String[] ecCur = ecCurated.get(newKey).split(", ");

							Set<String> ecNumber = new HashSet<>();
							Set<String> ecNumberCurated = new HashSet<>();
							
							for(int z = 0; z < ecN.length; z++)
								ecNumber.add(ecN[z]);
							
							for(int z = 0; z < ecCur.length; z++)
								ecNumberCurated.add(ecCur[z]);
							
							boolean equals = false;
							
							if(ecNumber.equals(ecNumberCurated))
								equals = true;
							
							if (equals && (double)matrix[row][column+1]>=threshold[i]){tP++;
//							if(column == 9 && i==7)
//								System.out.println(matrix[row][0]);
							}
							else if (ecCurated.get(newKey).equalsIgnoreCase("") && (double)matrix[row][column+1]>=threshold[i]){fP++;}
							else if (ecCurated.get(newKey).equalsIgnoreCase("") && (double)matrix[row][column+1]<threshold[i]){tN++;}
							else if (matrix[row][column].toString().equalsIgnoreCase(ecCurated.get(newKey)) && (double)matrix[row][column+1]<threshold[i]){fN++;}
							else if (!equals && (double)matrix[row][column+1]>=threshold[i]){fP++;}
							else if (!equals && (double)matrix[row][column+1]<threshold[i]){fN++;}
							
						}
						int values[] = {tP, fN, fP, tN};
						confMat[confRow][confCol] = values;
						confCol++;
					}
					confRow++;
				}
			}
		}	
		
		catch (Exception e) {
			e.printStackTrace();
		}
		
//		System.out.println("CONFUSION MATRIX");
//		
//		for (int i=0; i<confMat.length; i++){
//			for (int j=0; j<confMat[0].length; j++){
//				int[] conf = (int[]) confMat[i][j];
//				System.out.print(conf[0] + "(tP)	" +conf[1]+ "(fN)	" +conf[2]+ "(fP)	"+conf[3] + "(tN)    |   ");}
//			System.out.println();
//		}
//		//////////////

		this.confusionMat = confMat;
	}
	
	public void bestAlpha() throws Exception{
		
		double userThreshold = 0.0;
		
		if(!this.cancel.get()){
			userThreshold = homologyDataContainer.getThreshold();
			homologyDataContainer.setThreshold(0.0);
		}
		
		if(!this.cancel.get())
			this.scorer();
		
		if(!this.cancel.get())
			this.confusionMatrix();
		
//		for (int col=0; col<confusionMat[0].length; col++){
//			double total = 0;
//			
//			for (int row=0; row<confusionMat.length;row++){
//				int res[] = (int[]) confusionMat[row][col];
//			
//				total = total + (double) (res[0] + res[3]) / (res[0]+res[1]+res[2]+res[3]);
//			}
//			accuracy[col] = total / 9.0;
//		}
		
		accuracy = BestAlphaStatsCalculator.getAccuracy(confusionMat);
		
//		int index=0;
		
		if(!this.cancel.get()){
			upper = BestAlphaStatsCalculator.getUpperThreshold(confusionMat, threshold, 1.0); 
			lower = BestAlphaStatsCalculator.getLowerThreshold(confusionMat, upper, threshold, 1.0);
		}
		
		
		counts = new HashMap<>();
		
		for (int i = 0; i < alpha.length; i++){
			if(!this.cancel.get()){
//				int upperCounter=0, lowerCounter=0, totalCounter=0;
				homologyDataContainer.setAlpha(alpha[i]);
				mainTableData = homologyDataContainer.getAllGenes(blastDatabase, true);  //compor
				
				Map<Integer, Integer> countPerAlpha = new HashMap<>();
				
				for (int j=0; j<mainTableData.getRowCount(); j++){
					String ecScore = (String) mainTableData.getValueAt(j, ecScoreColumnNumber);	
					
					if (ecScore != ""){
						double score = Double.parseDouble(ecScore);
						
						int key = (int) (score * 10);
						
						if(key == 10)	//1.0 is the limit
							key = 9;
						
						int value = 0;
						
						if(countPerAlpha.containsKey(key))
							value = countPerAlpha.get(key);
						
						value++;
						countPerAlpha.put(key, value);
						
//						if (score>=upper[index]){
//							upperCounter = upperCounter + 1;
//						}
//						else if (score<lower[index]){
//							lowerCounter = lowerCounter + 1;
//						}
//						totalCounter++;
					}
				}
				
				double key = alpha[i] * 10;
				
				counts.put((int) key, countPerAlpha);
				
//				below[index] = lowerCounter;
//				above[index] = upperCounter;
//				total[index] = totalCounter;
//				index++;	
			}
			
		}
		
		if(!this.cancel.get()){
			homologyDataContainer.setAlpha(0.5);
			homologyDataContainer.setThreshold(userThreshold);
		}
		
		above = BestAlphaStatsCalculator.getCountAbove(upper, counts);
		below = BestAlphaStatsCalculator.getCountBelow(lower, counts);
		total = BestAlphaStatsCalculator.getTotal(counts);
		
	}
	
//	public DataTable resultsTable (){
//		List<String> alphaColNames = new ArrayList<>();
//		alphaColNames.add("alpha");
//		for (int i=1; i<array.length-1; i++){
//			alphaColNames.add(""+(i/10.0));
//		}
//		
//		DataTable data = new GenericDataTable(alphaColNames, "", ""); 
//		
//		ArrayList<Object> line = new ArrayList<>();
//		
//		line.add("accuracy");
//		
//		for (int i=1; i<accuracy.length-1;i++){
//			line.add(Math.round(accuracy[i]*1000.0)/1000.0);
//		}
//		
//		data.addLine(line);
//		line.clear();
//		line.add("upper threshold");
//		
//		for (int i=1; i<upper.length-1;i++){
//			line.add(Math.round(upper[i]*1000.0)/1000.0);
//		}
//		
//		data.addLine(line);
//		line.clear();
//		line.add("above UT");
//		
//		for (int i=1; i<above.length-1;i++){
//			line.add(above[i]);
//		}
//		
//		data.addLine(line);
//		line.clear();
//		line.add("lower threshold");
//		
//		for (int i=1; i<lower.length-1;i++){
//			line.add(Math.round(lower[i]*1000.0)/1000.0);
//		}
//		
//		data.addLine(line);
//		line.clear();
//		line.add("below LT");
//		
//		for (int i=1; i<below.length-1;i++){
//			line.add(below[i]);
//		}
//		
//		data.addLine(line);
//		line.clear();
//		line.add("total for curation");
//		
//		for (int i=1; i<below.length-1;i++){
//			line.add(total[i]-below[i]-above[i]);
//		}
//		
//		data.addLine(line);
//		line.clear();
//		line.add("% for curation");
//		
//		for (int i=1; i<x.length-1;i++){
//			line.add(Math.round(x[i]*100.0)+" %");
//		}
//		
//		data.addLine(line);
//		line.clear();
//		line.add("ratio");
//
//		for (int i=1; i<y.length-1;i++){
//			if (y[i]>0)
//				line.add(Math.round(y[i]*100.0)/100.0);
//			else
//				line.add("-");
//		}
//		
//		data.addLine(line);
//		
//		return data;
//	}
	
//	private void getLowerThreshold(){
//		Object[][] npv = new Object[9][11];
//		
//		for (int col=0; col<npv[0].length; col++){			
//			for (int row=0; row<npv.length;row++){
//				int res[] = (int[]) confusionMat[row][col];
//				npv[row][col] = (double) (res[3]) / (res[3]+res[1]);   
//			}	
//		}
//		
////////////////NPV
////	System.out.println();
////	System.out.println("NPV");
////	for (int i=0; i<npv.length; i++){
////		for (int j=0; j<npv[0].length; j++){
////			double prec = (double) npv[i][j];
////			System.out.print(Math.round(prec*1000.0)/1000.0 + "	");}
////		System.out.println();
////	}
////	//////////////
//		
//		for(int col=0; col<npv[0].length; col++ ){		
//			lower[col] = 0.1;
//			double thresh= 0.0;
//			for (int row=0; row<threshold.length; row++){
//				
//				double tScore = (double) npv[row][col];
//				
//				if (tScore >= thresh){
//					lower[col] = threshold[row];
//					thresh=tScore;
//				}
//				
//				if (thresh != 1.0)
//					lower[col] = 0.0;
//			}
//		}
//	}
//	
//	private void getUpperThreshold(){
//		
//		Object[][] precision = new Object[9][11];
//		
//		for (int col=0; col<precision[0].length; col++){			
//			for (int row=0; row<precision.length;row++){
//				
//				int res[] = (int[]) confusionMat[row][col];
//				precision[row][col] = (double) (res[0]) / (res[0]+res[2]);   		
//			}	
//		}
//		
////////////////PRECISION
////		System.out.println();
////		System.out.println("PRECISION");
////		for (int i=0; i<precision.length; i++){
////			for (int j=0; j<precision[0].length; j++){
////				double prec = (double) precision[i][j];
////				System.out.print(Math.round(prec*1000.0)/1000.0 + "	");}
////			System.out.println();
////		}
////	//////////////
//		
//		for(int col=0; col < precision[0].length; col++ ){	
//			upper[col]=0.1;
//			double thresh= 0.0;
//			for (int row=(threshold.length-1); row >= 0; row--){
//				
//				double tScore = (double) precision[row][col];
//				
//				if (tScore >= thresh){
//					upper[col] = threshold[row];
//					thresh=tScore;
//				}
//
//				if (thresh != 1.0)
//					upper[col] = 1.0;
//			}
//		}
//	}
	
	/**
	 * @return the progress
	 */
	@Progress
	public TimeLeftProgress getProgress() {

		return progress;
	}

	/**
	 * @param cancel the cancel to set
	 */
	@Cancel
	public void setCancel() {

		progress.setTime(0, 0, 0);
		Workbench.getInstance().warn("operation canceled!");
		this.cancel.set(true);
	}
}		

