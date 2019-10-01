package pt.uminho.ceb.biosystems.merlin.sampler;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import es.uvigo.ei.aibench.core.operation.annotation.Cancel;
import es.uvigo.ei.aibench.core.operation.annotation.Direction;
import es.uvigo.ei.aibench.core.operation.annotation.Operation;
import es.uvigo.ei.aibench.core.operation.annotation.Port;
import es.uvigo.ei.aibench.core.operation.annotation.Progress;
import es.uvigo.ei.aibench.workbench.Workbench;
import pt.uminho.ceb.biosystems.merlin.aibench.datatypes.WorkspaceAIB;
import pt.uminho.ceb.biosystems.merlin.aibench.datatypes.annotation.AnnotationEnzymesAIB;
import pt.uminho.ceb.biosystems.merlin.aibench.gui.CustomGUI;
import pt.uminho.ceb.biosystems.merlin.aibench.utilities.AIBenchUtils;
import pt.uminho.ceb.biosystems.merlin.aibench.utilities.TimeLeftProgress;
import pt.uminho.ceb.biosystems.merlin.core.datatypes.WorkspaceGenericDataTable;
import pt.uminho.ceb.biosystems.merlin.database.connector.databaseAPI.HomologyAPI;
import pt.uminho.ceb.biosystems.merlin.database.connector.datatypes.Connection;
import pt.uminho.ceb.biosystems.merlin.database.connector.datatypes.DatabaseAccess;
import pt.uminho.ceb.biosystems.merlin.services.annotation.AnnotationEnzymesServices;
import pt.uminho.ceb.biosystems.merlin.services.model.ModelGenesServices;

@Operation(name="perform annotation using SamPler", description="generate random sample for enzyme's annotation")
public class AnnotationEnzymesParametersSetting {

	public static final int DEFAULT_RATIO = 5;
	private AnnotationEnzymesAIB homologyDataContainer;
	private int sampleSize = 50;
	private Map<Integer, String> itemsList;
	private WorkspaceGenericDataTable mainTableData;
	private WorkspaceAIB workspace;
	private String blastDatabase;
	private long startTime;
	private String message;
	private TimeLeftProgress progress = new TimeLeftProgress();
	private AtomicBoolean cancel = new AtomicBoolean(false);
	private AtomicInteger querySize;
	private AtomicInteger counter = new AtomicInteger(0);
	
	@Port(direction=Direction.INPUT, name="workspace", description="select workspace",validateMethod="checkWorkspace",order=1)
	public void setNewProject(WorkspaceAIB workspace) {

		this.startTime = GregorianCalendar.getInstance().getTimeInMillis();

		this.progress.setTime(GregorianCalendar.getInstance().getTimeInMillis() - this.startTime, 0, 2, "");

		try {
			
			DatabaseAccess dbAcess = this.workspace.getDatabase().getDatabaseAccess();
			Connection conn = new Connection(dbAcess);
			Statement stmt = conn.createStatement();
			
			this.blastDatabase = AnnotationEnzymesServices.getLastestUsedBlastDatabase(this.workspace.getName());
			
			if(resetScorer(blastDatabase)) {
				
				this.progress.setTime(GregorianCalendar.getInstance().getTimeInMillis() - this.startTime, 0, 2, "deleting previous annotations...");
				
				HomologyAPI.deleteHomologyData(blastDatabase, stmt);
				
				this.progress.setTime(GregorianCalendar.getInstance().getTimeInMillis() - this.startTime, 0, 2, "reseting parameters...");
				
				updateSettings(true);
				
//					updateTableUI();
				AIBenchUtils.updateView(this.workspace.getName(), AnnotationEnzymesAIB.class);
				
//				Workbench.getInstance().info("parameters successfully reset!");
			}
			else {
				
				stmt.close();
				conn.closeConnection();
				
				throw new Exception("cannot continue without reseting parameters...");
			}
			
			Integer total = ModelGenesServices.countInitialMetabolicGenes(this.workspace.getName());
			
			stmt.close();
			conn.closeConnection();
			
			if(total != null)
				sampleSize = (int) (total * (DEFAULT_RATIO/100.0));
			
//			System.out.println(sampleSize);
//			System.out.println(total);
//			System.out.println(DEFAULT_RATIO);
			
			if(!this.cancel.get())
				new EnzymesAnnotationJDialog(this.blastDatabase, sampleSize, 
						homologyDataContainer, true, total);
		} 
		catch (Exception e) {
			Workbench.getInstance().error("an error occurred while perfoming the operation");
			e.printStackTrace();
		}


	}
	
//	/**
//	 * 
//	 * Method to decide when the buttons alpha, threshold and auto select should be available
//	 * @throws SQLException 
//	 */
//	private boolean areParametersAlreadySet(Statement statement){
//
//
//		ArrayList<String> databases = new ArrayList<>();
//
//		try {
//
//			if(statement == null) {
//				Connection connection = homologyDataContainer.getConnection();
//				statement = connection.createStatement();
//			}
//
//			databases = HomologyAPI.bestAlphasFound(statement);
//
//		} 
//		catch (SQLException e) {
//			e.printStackTrace();
//		}
//
//		if(!databases.contains("") && this.blastDatabase.equalsIgnoreCase("") && databases.size() > 0) {
//
//			return true;
//
//		}
//		else if(!databases.contains("") && this.blastDatabase.equalsIgnoreCase("") && databases.size() == 0) {
//
//			return true;
//
//		}
//		else if(databases.contains("") && !this.blastDatabase.equalsIgnoreCase("")) {
//
//			return true;
//
//		}
//		else if(databases.contains(this.blastDatabase)) {
//
//			return true;
//
//		}
//		else {
//
//			return false;
//
//		}
//		
//	}
	
	
	

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
			
			if(homologyDataContainer == null)
				throw new IllegalArgumentException("please open the enzymes annotation view before generating a new sample!");
		}
	}
	
	/**
	 * Method to upate the user commited settings for a specific database
	 */
	private void updateSettings(boolean restore) {
		
		if (restore) {

			this.homologyDataContainer.setThreshold(AnnotationEnzymesAIB.THRESHOLD);
			this.homologyDataContainer.setUpperThreshold(AnnotationEnzymesAIB.UPPER_THRESHOLD);
//			this.homologyDataContainer.setBlastHmmerWeight(EnzymesAnnotationDataInterface.BLAST_HMMER_WEIGHT);
			this.homologyDataContainer.setBeta(AnnotationEnzymesAIB.BETA);
			this.homologyDataContainer.setAlpha(AnnotationEnzymesAIB.ALPHA);
			this.homologyDataContainer
			.setMinimumNumberofHits(AnnotationEnzymesAIB.MINIMUM_NUMBER_OF_HITS);
		}
		else {

			this.homologyDataContainer.setThreshold(this.homologyDataContainer.getCommittedThreshold());
			this.homologyDataContainer.setUpperThreshold(this.homologyDataContainer.getCommittedUpperThreshold());
//			this.homologyDataContainer.setBlastHmmerWeight(this.homologyDataContainer.getCommittedBalanceBH());
			this.homologyDataContainer.setBeta(this.homologyDataContainer.getCommittedBeta());
			this.homologyDataContainer.setAlpha(this.homologyDataContainer.getCommittedAlpha());
			this.homologyDataContainer
			.setMinimumNumberofHits(this.homologyDataContainer.getCommittedMinHomologies());

		}
		
	}
	
	/**
	 * @return
	 * @throws Exception 
	 */
	private boolean resetScorer(String blastDatabase) throws Exception {

		int i;

		if(blastDatabase.equals("")) {
			i =CustomGUI.stopQuestion("Continue", 
					"This operation will discard any previously set parameters and annotations for all blast databases. Continue?",
					new String[]{"Yes", "No"});

		}
		else {
			i =CustomGUI.stopQuestion("Continue", 
					"This operation will discard any previously set parameters and annotations for blast database " + blastDatabase + ". Continue?",
					new String[]{"Yes", "No"});
		}
		
		try {
			Connection connection = homologyDataContainer.getConnection();
			Statement statement = connection.createStatement();
			
			switch (i)
			{
			case 0:
			{
				if(blastDatabase.equals(""))
					AnnotationEnzymesServices.resetAllScorers(this.workspace.getName());
				else
					AnnotationEnzymesServices.resetDatabaseScorer(this.workspace.getName(), blastDatabase);

				return true;
			}
			default:
			{
				return false;
			}
			}
		} 
		catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	
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

