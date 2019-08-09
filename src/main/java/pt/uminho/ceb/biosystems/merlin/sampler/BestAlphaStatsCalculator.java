package pt.uminho.ceb.biosystems.merlin.sampler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import pt.uminho.ceb.biosystems.merlin.core.datatypes.WorkspaceDataTable;
import pt.uminho.ceb.biosystems.merlin.core.datatypes.WorkspaceGenericDataTable;

public class BestAlphaStatsCalculator {
	
	
	/**
	 * Get total of genes calculated.
	 * 
	 * @param counts
	 * @return
	 */
	public static int[] getTotal(Map<Integer, Map<Integer, Integer>> counts){
		
		int[] total = new int[11];
		
		for(int i = 0; i < counts.size(); i++){
			
			Map<Integer, Integer> countPerAlpha = counts.get(i);
			
//			System.out.println(countPerAlpha);
			
			for(int j = 0; j < 10; j++){
//				System.out.print(total[i]+ "   ");
				if(countPerAlpha.containsKey(j))
					total[i] = total[i] + countPerAlpha.get(j);
			}
//			System.out.println(total[i]);
			
		}
		
		return total;
	}
	
	/**
	 * Counts how many genes above the upper threshold for each alpha.
	 * 
	 * @param upperThreshold
	 * @param counts
	 * @return
	 */
	public static int[] getCountAbove(double[] upperThreshold, Map<Integer, Map<Integer, Integer>> counts){
		
		int[] upper = new int[11];
		
		for(int i = 0; i < counts.size(); i++){
			
			Map<Integer, Integer> countPerAlpha = counts.get(i);
			
			int threshold = (int) (upperThreshold[i] * 10);
			
			for(int j = threshold; j < 10; j++){
				if(countPerAlpha.containsKey(j))
					upper[i] = upper[i] + countPerAlpha.get(j);
			}
		}
		
		return upper;
	}
	
	/**
	 * Counts how many genes below the lower threshold for each alpha.
	 * 
	 * @param lowerThreshold
	 * @param counts
	 * @return
	 */
	public static int[] getCountBelow(double[] lowerThreshold, Map<Integer, Map<Integer, Integer>> counts){
		
		int[] below = new int[11];
		
		for(int i = 0; i < counts.size(); i++){
			
			Map<Integer, Integer> countPerAlpha = counts.get(i);
			
			int threshold = (int) (lowerThreshold[i] * 10);
			
			for(int j = 0; j < threshold; j++){
				if(countPerAlpha.containsKey(j))
					below[i] = below[i] + countPerAlpha.get(j);
			}
		}
		
		return below;
	}
	
	/**
	 * Method to calculate y.
	 * 
	 * @param x
	 * @param below
	 * @param above
	 * @param total
	 * @param accuracy
	 * @return
	 */
	public static Object[] getXAndY(int[] below, int[] above, int total[], double[] accuracy){
		
		double[] y = new double[11];
		double[] x = new double[11];
		
		Object[] xAndY = new Object[2];
		
		for (int i=0; i<x.length; i++){
			double percentage = (double) ((total[i]-above[i]-below[i])/(double) total[i]);
			double res = (double) accuracy[i]/percentage;
			x[i]=percentage;
			if(percentage>0)
				y[i]=res;
			else
				y[i]=-1;
		}
		
		xAndY[0] = x;
		xAndY[1] = y;
		
		return xAndY;
	}
	
	/**
	 * Get the index of the best alpha
	 * 
	 * @param y
	 * @return
	 */
	public static int getBestAlphaIndex(double[] y, double[] accuracy){

		int bestAlpha = -1;

		double max = -10;

		for (int i=1; i<y.length-1; i++){		// alpha 1 is ignored
			
			if(y[i] < 0){				//in case there's cases of 0% for annotation
				
				if (accuracy[i]>max){
					max=accuracy[i];
					bestAlpha = i;
				}
			}
		}

		if (bestAlpha < 0){				// in case there's not
			
			bestAlpha = 1;
		
			for (int i=1; i<y.length-1; i++){		// alpha 1 is ignored
	
				if (y[i]>max){
					max=y[i];
	
					bestAlpha = i;
				}
			}
		}
		
		return bestAlpha;
	}
	

	/**
	 * Get accuracy for each alpha.
	 * 
	 * @param confusionMat
	 * @return
	 */
	public static double[] getAccuracy(Object[][] confusionMat){
		
		double[] accuracy = new double[11];
		
		for (int col=0; col<confusionMat[0].length; col++){
			double total = 0;
			
			for (int row=0; row<confusionMat.length;row++){
				int res[] = (int[]) confusionMat[row][col];
			
				total = total + (double) (res[0] + res[3]) / (res[0]+res[1]+res[2]+res[3]);
			}
			accuracy[col] = total / 9.0;
		}
		
		return accuracy;
	}
	
	
	/**
	 * Calculates lower threshold for each alpha.
	 * 
	 * @param confusionMat
	 * @param threshold
	 * @param lower
	 * @return
	 */
	public static double[] getLowerThreshold(Object[][] confusionMat,  double[] upperThreshold, double[] threshold, double thresh){
		
		double[] lower = new double[11];
		
		Object[][] npv = new Object[9][11];
		
		for (int col=0; col<npv[0].length; col++){			
			for (int row=0; row<npv.length;row++){
				int res[] = (int[]) confusionMat[row][col];
				npv[row][col] = (double) (res[3]) / (res[3]+res[1]);   
			}	
		}
		
	//////////////NPV
//		System.out.println();
//		System.out.println("NPV");
//		for (int i=0; i<npv.length; i++){
//			for (int j=0; j<npv[0].length; j++){
//				double prec = (double) npv[i][j];
//				System.out.print(Math.round(prec*1000.0)/1000.0 + "	");}
//			System.out.println();
//		}
	//	//////////////
		
		for(int col=0; col<npv[0].length; col++ ){		
			
			lower[col] = -1.0;
			
//			double thresh= 0.0;
			for (int row=0; row<threshold.length; row++){
				
				double tScore = (double) npv[row][col];
				
//				if (tScore >= thresh){
//					lower[col] = threshold[row];
//					thresh=tScore;
//				}
//				
//				if (thresh != 1.0)
//					lower[col] = 0.0;
				
				if(tScore >= thresh)
					lower[col] = threshold[row];
				
				if(lower[col] > upperThreshold[col])
					lower[col] = upperThreshold[col];
					
			}
			
			if(lower[col] == -1.0){
				lower[col] = 0.0;
			}
		}
		
		return lower;
	}
	
	/**
	 * Calculates upper threshold for each alpha
	 * 
	 * @param confusionMat
	 * @param threshold
	 * @param upper
	 * @return
	 */
	public static double[] getUpperThreshold(Object[][] confusionMat, double[] threshold, double error){
		
		double[] upper = new double[11];
		
		Object[][] precision = new Object[9][11];
		
		for (int col=0; col<precision[0].length; col++){			
			for (int row=0; row<precision.length;row++){
				
				int res[] = (int[]) confusionMat[row][col];
				precision[row][col] = (double) (res[0]) / (res[0]+res[2]);   		
			}	
		}
		
	//////////////PRECISION
//			System.out.println();
//			System.out.println("PRECISION");
//			for (int i=0; i<precision.length; i++){
//				for (int j=0; j<precision[0].length; j++){
//					double prec = (double) precision[i][j];
//					System.out.print(Math.round(prec*1000.0)/1000.0 + "	");}
//				System.out.println();
//			}
	//	//////////////
		
		for(int col=0; col < precision[0].length; col++ ){	
			
			upper[col] = -1.0;
//			double thresh= 0.0;
			for (int row=(threshold.length-1); row >= 0; row--){
				
				double tScore = (double) precision[row][col];
				
//				if (tScore >= thresh){
//					upper[col] = threshold[row];
//					thresh=tScore;
//				}
//
//				if (thresh != 1.0)
//					upper[col] = 1.0;
				
				if(tScore >= error)
					upper[col] = threshold[row];
					
					
				
				
			}
			
			if(upper[col] == -1.0){
				upper[col] = 1.0;
			}
		}
		
		return upper;
	}

	
	/**
	 * Method to perform final stats and return DataTable to display in results GUI.
	 * 
	 * @param array
	 * @param below
	 * @param above
	 * @param total
	 * @param lower
	 * @param upper
	 * @param y
	 * @param x
	 * @param threshold
	 * @param accuracy
	 * @return
	 */
	public static WorkspaceDataTable resultsTable (int[] array,  double[] lower, double[] upper,  double[] y,  double[] x,  
			double[] threshold,  double[] accuracy, int[] below, int[] above,  int[] total){
		
		List<String> alphaColNames = new ArrayList<>();
		alphaColNames.add("alpha");
		for (int i=1; i<array.length-1; i++){
			alphaColNames.add(""+(i/10.0));
		}
		
		WorkspaceDataTable data = new WorkspaceGenericDataTable(alphaColNames, "", ""); 
		
		ArrayList<Object> line = new ArrayList<>();
		
		line.add("upper threshold");
		
		for (int i=1; i<upper.length-1;i++){
			line.add((Math.round(upper[i]*1000.0)/1000.0)+"");
		}
		
		data.addLine(line);
		line.clear();
		line.add("above UT");
		
		for (int i=1; i<above.length-1;i++){
			line.add(above[i]+"");
		}
		
		data.addLine(line);
		line.clear();
		line.add("lower threshold");
		
		for (int i=1; i<lower.length-1;i++){
			line.add((Math.round(lower[i]*1000.0)/1000.0)+"");
		}
		
		data.addLine(line);
		line.clear();
		line.add("below LT");
		
		for (int i=1; i<below.length-1;i++){
			line.add(below[i]+"");
		}
		
		data.addLine(line);
		line.clear();
		line.add("total for curation");
		
		for (int i=1; i<below.length-1;i++){
			line.add((total[i]-below[i]-above[i])+"");
		}
		
		data.addLine(line);
		line.clear();
		line.add("% for curation");
		
		for (int i=1; i<x.length-1;i++){
			line.add(Math.round(x[i]*100.0)+" %");
		}
		
		data.addLine(line);
		line.clear();
		
		line.add("accuracy");
		
		for (int i=1; i<accuracy.length-1;i++){
			line.add((Math.round(accuracy[i]*1000.0)/1000.0)+"");
		}
		data.addLine(line);
			
		line.clear();
		line.add("curation ratio score");

		for (int i=1; i<y.length-1;i++){
			if (y[i]>0)
				line.add((Math.round(y[i]*100.0)/100.0)+"");
			else
				line.add("-");
		}
		
		data.addLine(line);
		
		return data;
	}
	
}
