package pt.uminho.ceb.biosystems.merlin.sampler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.uvigo.ei.aibench.core.operation.annotation.Cancel;
import es.uvigo.ei.aibench.core.operation.annotation.Direction;
import es.uvigo.ei.aibench.core.operation.annotation.Operation;
import es.uvigo.ei.aibench.core.operation.annotation.Port;
import es.uvigo.ei.aibench.core.operation.annotation.Progress;
import es.uvigo.ei.aibench.workbench.Workbench;
import pt.uminho.ceb.biosystems.merlin.aibench.datatypes.WorkspaceAIB;
import pt.uminho.ceb.biosystems.merlin.aibench.datatypes.annotation.AnnotationEnzymesAIB;
import pt.uminho.ceb.biosystems.merlin.aibench.gui.CustomGUI;
import pt.uminho.ceb.biosystems.merlin.aibench.gui.EnzymesAnnotationJDialog;
import pt.uminho.ceb.biosystems.merlin.aibench.utilities.AIBenchUtils;
import pt.uminho.ceb.biosystems.merlin.aibench.utilities.TimeLeftProgress;
import pt.uminho.ceb.biosystems.merlin.core.datatypes.WorkspaceDataTable;
import pt.uminho.ceb.biosystems.merlin.core.datatypes.WorkspaceGenericDataTable;
import pt.uminho.ceb.biosystems.merlin.services.annotation.AnnotationEnzymesServices;
import pt.uminho.ceb.biosystems.merlin.utilities.datastructures.map.MapUtils;
import pt.uminho.ceb.biosystems.merlin.utilities.io.FileUtils;

@Operation(name="perform annotation using SamPler", description="generate random sample for enzyme's annotation")
public class AnnotationEnzymesParametersSetting {

	private static final int STARTCOLUMN = 0, FINISHCOLUMN = 1;
	private static final int LOCUS_TAG_COLUMN_NUMBER = 1, EC_NUMBERS_COLUMN_NUMBER = 6, EC_SCORE_COLUMN_NUMBER = 7;
	private AnnotationEnzymesAIB homologyDataContainer;
	private int sampleSize;
	private Map<Integer, String> itemsList;
	private Map<Integer,String> ecTable = new TreeMap<Integer,String>();
	private WorkspaceGenericDataTable mainTableData;
	private WorkspaceAIB workspace;
	private boolean searchFile = true;
	private String blastDatabase;
	private long startTime;
	private String message;
	private TimeLeftProgress progress = new TimeLeftProgress();
	private AtomicBoolean cancel = new AtomicBoolean(false);
	private AtomicInteger querySize;
	private AtomicInteger counter = new AtomicInteger(0);
	
	final static Logger logger = LoggerFactory.getLogger(AnnotationEnzymesParametersSetting.class);

	@Port(direction=Direction.INPUT, name="workspace",description="select workspace",validateMethod="checkWorkspace",order=1)
	public void setNewProject(WorkspaceAIB workspace) {

	}

	@Port(direction=Direction.INPUT, name="homologyDataContainer", order=2)
	public void setEnzymesAnnotationDataInterface(AnnotationEnzymesAIB homologyDataContainer){

		this.homologyDataContainer = homologyDataContainer;

	};

	@Port(direction=Direction.INPUT, name="sampleSize", order=3)
	public void setSampleSize(int sampleSize){

		this.sampleSize = sampleSize;	
	}

	@Port(direction=Direction.INPUT, name="blastDatabase", order=7)
	public void setBlastDatabase(String blastDatabase){
		
		this.startTime = GregorianCalendar.getInstance().getTimeInMillis();

		this.progress.setTime(GregorianCalendar.getInstance().getTimeInMillis() - this.startTime, 0, 2, "generating sample...");

		try {
			this.itemsList = this.homologyDataContainer.getItemsList().get(1);
			
			this.blastDatabase = AnnotationEnzymesServices.getLastestUsedBlastDatabase(this.workspace.getName());
			
			Map <Integer, String> data = this.openFile();

			Map<Integer,String> ecMap = new TreeMap<Integer,String>();

			Map<Integer, String> values = new HashMap<>();

			double userThreshold = homologyDataContainer.getThreshold();
			double userAlpha = homologyDataContainer.getAlpha();

			WorkspaceDataTable randomTable;

			//put the container with the correct settings for selection
			if(!this.cancel.get()){

				if(userThreshold != 0.0)
					homologyDataContainer.setThreshold(0.0);

				if(userAlpha != 0.5)
					homologyDataContainer.setAlpha(0.5);
			}

			this.mainTableData = homologyDataContainer.getAllGenes(this.blastDatabase, false);

			if(data!=null && searchFile == true && !this.cancel.get()){

				randomTable = this.buildTable(data);

				ecMap = this.getEcMap();

				for (int i: data.keySet())
					values.put(Integer.parseInt(ecMap.get(i)), data.get(i));
			}
			else{
				if(!this.cancel.get())
					this.generateRandomSample(sampleSize);

				ecMap = this.getEcMap();

				randomTable = this.buildTable(ecMap);

				for(int i  : ecMap.keySet())
					values.put(Integer.parseInt(ecMap.get(i)), itemsList.get(i));
			}

			//restore user's settings
			if(!this.cancel.get()){

				if(userThreshold != 0.0)
					homologyDataContainer.setThreshold(userThreshold);

				if(userAlpha != 0.5)
					homologyDataContainer.setAlpha(userAlpha);

			}

			if(!this.cancel.get())
				new EnzymesAnnotationJDialog(this.blastDatabase, sampleSize, EC_NUMBERS_COLUMN_NUMBER, EC_SCORE_COLUMN_NUMBER, values, 
						itemsList, LOCUS_TAG_COLUMN_NUMBER, randomTable,  homologyDataContainer, ecMap);
		} 
		catch (Exception e) {
			Workbench.getInstance().error("an error occurred while perfoming the operation");
			e.printStackTrace();
		}


	}

	/**
	 * Method to generate the random sample with a given size.
	 * 
	 * @param newtablesize
	 */
	public void generateRandomSample(int newtablesize) {

		List<Integer> count = IntervalRange(newtablesize);
		List<Integer> tablerows = new ArrayList<Integer>();	

		for (int i=0; i<mainTableData.getRowCount(); i++){

			String score = (String) mainTableData.getValueAt(i, EC_SCORE_COLUMN_NUMBER);

			if (!score.isEmpty() && !score.equals("manual")){
				tablerows.add(i);
			}
		}

		Collections.shuffle(tablerows);

		int i=0;
		int j=0;

		while (sum(count)!=0 && i<tablerows.size() && !this.cancel.get()) {

			String score = (String) mainTableData.getValueAt(tablerows.get(i), EC_SCORE_COLUMN_NUMBER);
			double value = Double.parseDouble(score);
			boolean flag = false;

			if (value >= 0 && value <0.1 && count.get(0) != 0){ count.set(0, count.get(0)-1); flag = true;}
			else if (value >= 0.1 && value <0.2 && count.get(1) != 0){ count.set(1, count.get(1)-1); flag = true;}
			else if (value >= 0.2 && value <0.3 && count.get(2) != 0){ count.set(2, count.get(2)-1); flag = true;}
			else if (value >= 0.3 && value <0.4 && count.get(3) != 0){ count.set(3, count.get(3)-1); flag = true;}
			else if (value >= 0.4 && value <0.5 && count.get(4) != 0){ count.set(4, count.get(4)-1); flag = true;}
			else if (value >= 0.5 && value <0.6 && count.get(5) != 0){ count.set(5, count.get(5)-1); flag = true;}
			else if (value >= 0.6 && value <0.7 && count.get(6) != 0){ count.set(6, count.get(6)-1); flag = true;}
			else if (value >= 0.7 && value <0.8 && count.get(7) != 0){ count.set(7, count.get(7)-1); flag = true;}
			else if (value >= 0.8 && value <0.9 && count.get(8) != 0){ count.set(8, count.get(8)-1); flag = true;}
			else if (value >= 0.9 && value <=1 && count.get(9) != 0){ count.set(9, count.get(9)-1); flag = true;}


			if (flag == true){

				ecTable.put(tablerows.get(i), j+"");
				j++;
			}
			i++;
		}	
	}

	/**
	 * Constructs the table with the random sample generated before.
	 * 
	 * @param ecKey
	 * @return DataTable with two columns containing "gene" and "ec number"
	 */
	public WorkspaceDataTable buildTable(Map<Integer, String> ecKey){

		List<String> columnsNames = Arrays.asList("gene", "EC number");
		WorkspaceDataTable data = new WorkspaceGenericDataTable(columnsNames, "", "");

		int i=0;

		for (Integer key: ecKey.keySet()){

			ArrayList<Object> line = new ArrayList<>();

			String name = (String) mainTableData.getValueAt(key, LOCUS_TAG_COLUMN_NUMBER);
			String[] ecNumber = (String[]) ArrayUtils.addAll(new String[]{"other ec number"},(String[]) mainTableData.getValueAt(key, EC_NUMBERS_COLUMN_NUMBER));	

			line.add(name);
			line.add(ecNumber);

			data.addLine(line);
			this.ecTable.put(key, i+"");
			i++;
		}
		return data;
	}

	/**
	 * Read file with previous selection.
	 * 
	 * @return
	 */
	private Map<Integer, String> openFile() {

		String databaseName = homologyDataContainer.getWorkspace().getName();
		Long taxonomyID = homologyDataContainer.getWorkspace().getTaxonomyID();

		String fileName =  "AutoGeneSelection_" + this.blastDatabase + ".txt";

		if(blastDatabase.isEmpty())
			fileName =  "AutoGeneSelection.txt";

		String path = FileUtils.getWorkspaceTaxonomyFolderPath(databaseName, taxonomyID) + fileName;

		Map<Integer, String> data = null;

		try {
			data = 	MapUtils.readFile(path, STARTCOLUMN, FINISHCOLUMN, "\t");
		}
		catch (IOException e) {
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return data;
	}

	/**
	 * Get the random sample.
	 * 
	 * @return 
	 */
	public Map<Integer,String> getEcMap(){

		return ecTable;
	}

	private List<Integer> IntervalRange(int tableSize){

		int n_intervals = 10;
		List<Integer> intervals = new ArrayList<Integer>();

		int n = tableSize/n_intervals;

		for (int i = 0; i < n_intervals; i++){
			intervals.add(i, n);
		}
		return intervals;
	}	

	private int sum(List<Integer> list){

		int total = 0;
		for (int i=0; i<list.size();i++){
			total = total + list.get(i);
		}
		return total;	
	}

	//////////////////////////ValidateMethods/////////////////////////////
	/**
	 * @param project
	 */
	public void checkWorkspace(WorkspaceAIB workspace) {

		if(workspace == null) {

			throw new IllegalArgumentException("no workspace selected!");
		}
		else {

			this.workspace = workspace;
			this.homologyDataContainer = (AnnotationEnzymesAIB) AIBenchUtils.getEntity(this.workspace.getName(), AnnotationEnzymesAIB.class);
		}
	}

	/**
	 * @return the progress
	 */
	@Progress(progressDialogTitle = "TranSyT annotation", modal = false, workingLabel = "performing TranSyT annotation", preferredWidth = 400, preferredHeight=300)
	public TimeLeftProgress getProgress() {

		return progress;
	}

	/**
	 * @param cancel the cancel to set
	 */
	@Cancel
	public void setCancel() {

		progress.setTime(0, 0, 0);
		this.cancel.set(true);
	}

	/* (non-Javadoc)
	 * @see java.util.Observer#update(java.util.Observable, java.lang.Object)
	 */
	public void update(Observable o, Object arg) {

		this.progress.setTime(GregorianCalendar.getInstance().getTimeInMillis() - this.startTime, this.counter.get(), this.querySize.get(), message);
	}
	/**
	 * @param cancel the cancel to set
	 */
	@Cancel
	public void cancel() {

		String[] options = new String[2];
		options[0] = "yes";
		options[1] = "no";

		int result = CustomGUI.stopQuestion("Cancel confirmation", "Are you sure you want to cancel the operation?", options);

		if(result == 0) {

			this.progress.setTime((GregorianCalendar.getInstance().getTimeInMillis()-GregorianCalendar.getInstance().getTimeInMillis()),1,1);

			this.cancel.set(true);

			Workbench.getInstance().warn("operation canceled!");

		}
	}
}


