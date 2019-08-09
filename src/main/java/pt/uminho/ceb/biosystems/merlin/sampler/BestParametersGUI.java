package pt.uminho.ceb.biosystems.merlin.sampler;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.stream.IntStream;

import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import es.uvigo.ei.aibench.workbench.Workbench;
import es.uvigo.ei.aibench.workbench.utilities.Utilities;
import pt.uminho.ceb.biosystems.merlin.aibench.datatypes.annotation.AnnotationEnzymesAIB;
import pt.uminho.ceb.biosystems.merlin.aibench.utilities.CreateImageIcon;
import pt.uminho.ceb.biosystems.merlin.aibench.utilities.ExportToXLS;
import pt.uminho.ceb.biosystems.merlin.aibench.utilities.MerlinUtils;
import pt.uminho.ceb.biosystems.merlin.aibench.utilities.MyJTable;
import pt.uminho.ceb.biosystems.merlin.core.datatypes.WorkspaceDataTable;
import pt.uminho.ceb.biosystems.merlin.database.connector.databaseAPI.HomologyAPI;
import pt.uminho.ceb.biosystems.merlin.database.connector.datatypes.Connection;

public class BestParametersGUI extends javax.swing.JDialog{

	private static final long serialVersionUID = -1L;

	public static final int UPPER_THRESHOLD_ROW = 0;
	public static final int LOWER_THRESHOLD_ROW = 2;
	public static final Double[] PRECISION_OR_NPV = {1.0, 0.95, 0.9, 0.85, 0.8, 0.75};

	private JPanel jPanel1, jPanel2, jPanel3;
	private JScrollPane jScrollPane;
	private JLabel jLabel1;
	private MyJTable jTable;
	private int bestAlphaIndex, thresholdColIndex;
	private WorkspaceDataTable data;
	private ButtonGroup buttonGroupGbk;
	private JRadioButton jRadioButton1;
	private JRadioButton jRadioButton2;
	private JRadioButton jRadioButton3;
	private JRadioButton jRadioButton4;
	private JRadioButton jRadioButton5;
	private JRadioButton jRadioButton6;
	private JRadioButton jRadioButton7;
	private JRadioButton jRadioButton8;
	private JRadioButton jRadioButton9;
	private double bestAlpha;
	private AnnotationEnzymesAIB homologyDataContainer;
	private JComboBox<String> errorMarginUpper;

	private int[] array = IntStream.range(0, 11).toArray();
	//	private int[] below = new int[11], above = new int[11], total = new int[11];
	private double[] threshold = {0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9};
	//	private double[] alpha = {0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0};
	private Object[][] confusionMat;
	private ArrayList<Object> buttons;
	private Map<Integer, Map<Integer, Integer>> counts;
	private JComboBox<String> errorMarginLower;
	private String blastDatabase;

	private JLabel jLabelUpper;

	private JLabel jLabelLower;


	public BestParametersGUI(String blastDatabase, int bestAlphaIndex, double[] threshold,
			Object[][] confusionMat, Map<Integer, Map<Integer, Integer>> counts,  WorkspaceDataTable data, AnnotationEnzymesAIB homologyDataContainer) {

		super(Workbench.getInstance().getMainFrame());

		this.jTable = new MyJTable();
		this.confusionMat = confusionMat;
		this.homologyDataContainer = homologyDataContainer;
		this.bestAlphaIndex=bestAlphaIndex;
		this.thresholdColIndex=bestAlphaIndex;
		this.bestAlpha = Double.parseDouble("0." + bestAlphaIndex);
		this.data=data;
		this.counts=counts;
		this.blastDatabase = blastDatabase;

		initGUI();
		Utilities.centerOnOwner(this);
		this.setVisible(true);		
		this.setAlwaysOnTop(true);
		this.toFront();
	}

	private void initGUI() {

		buttons = new ArrayList<>();

		try {

			GridBagLayout thisLayout = new GridBagLayout();
			thisLayout.columnWeights = new double[] {0.0, 0.1, 0.0};
			thisLayout.columnWidths = new int[] {7, 7, 7};
			thisLayout.rowWeights = new double[] {0.0, 200.0, 0.0, 0.0, 0.0};
			thisLayout.rowHeights = new int[] {7, 50, 7, 3, 7};
			this.setLayout(thisLayout);
			this.setSize(1210, 525);
			{
				this.setTitle("Results");

				jPanel1 = new JPanel();
				GridBagLayout jPanel1Layout = new GridBagLayout();
				jPanel1Layout.rowWeights = new double[] {0.0, 0.1};
				jPanel1Layout.rowHeights = new int[] {7, 7, 7};
				jPanel1Layout.columnWeights = new double[] {0.1, 0.1};
				jPanel1Layout.columnWidths = new int[] {7, 7};
				jPanel1.setLayout(jPanel1Layout);
				this.add(jPanel1, new GridBagConstraints(1, 1, 1, 1, 1.0, 0.5, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));

				jScrollPane = new JScrollPane();
				jPanel1.add(jScrollPane, new GridBagConstraints(0, 0, 2, 2, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
				{
					jPanel2 = new JPanel();
					GridBagLayout jPanel2Layout = new GridBagLayout();
					jPanel1.add(jPanel2, new GridBagConstraints(1, 10, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
					jPanel2.setBounds(1, 41, 376, 79);
					//					jPanel2.setBorder(BorderFactory.createTitledBorder("options"));
					jPanel2Layout.rowWeights = new double[] {0.0, 0.1, 0.0, 0.1, 0.0};
					jPanel2Layout.rowHeights = new int[] {7, 7, 7, 7, 7};
					jPanel2Layout.columnWeights = new double[] {0.1, 0.1, 0.1, 0.1};
					jPanel2Layout.columnWidths = new int[] {7, 20, 7, 7};
					jPanel2.setLayout(jPanel2Layout);
					{
						{
							JButton jButtonCancel = new JButton("cancel");
							jPanel2.add(jButtonCancel, new GridBagConstraints(3, 2, 1, 1, 10.0, 10.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
							jButtonCancel.setToolTipText("close window");
							jButtonCancel.setIcon(new CreateImageIcon(new ImageIcon(getClass().getClassLoader().getResource("icons/Cancel.png")),0.1).resizeImageIcon());
							jButtonCancel.addActionListener(new ActionListener() {

								public void actionPerformed(ActionEvent arg0) {

									simpleFinish();

								}});
						}

						{
							JButton jButtonApply = new JButton("export");
							jPanel2.add(jButtonApply, new GridBagConstraints(2, 2, 1, 1, 10.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
							jButtonApply.setToolTipText("apply parameters");
							jButtonApply.setIcon(new CreateImageIcon(new ImageIcon(getClass().getClassLoader().getResource("icons/Download.png")),0.1).resizeImageIcon());
							jButtonApply.setBounds(1, 1, 40, 20);
							jButtonApply.addActionListener(new ActionListener() {

								public void actionPerformed(ActionEvent arg0) {

									try {

										JFileChooser fc = new JFileChooser();
										fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
										fc.setDialogTitle("Select directory");
										int returnVal = fc.showOpenDialog(new JTextArea());

										if (returnVal == JFileChooser.APPROVE_OPTION) {


											File file = fc.getSelectedFile();
											String filePath = file.getAbsolutePath();
											Calendar cal = new GregorianCalendar();

											// Get the components of the time
											int hour24 = cal.get(Calendar.HOUR_OF_DAY);     // 0..23
											int min = cal.get(Calendar.MINUTE);             // 0..59
											int day = cal.get(Calendar.DAY_OF_YEAR);		//0..365

											filePath += "/"+homologyDataContainer.getWorkspace().getName()+"_"+homologyDataContainer.getWorkspace().getTaxonomyID()+"_BestParameters_"+hour24+"_"+min+"_"+day+".xls";

											ExportToXLS.exportToXLS(filePath, data, jTable);

											Workbench.getInstance().info("Data successfully exported.");
										}
									} catch (Exception e) {

										Workbench.getInstance().error("An error occurred while performing this operation. Error "+e.getMessage());
										e.printStackTrace();
									}

								}});
						}

						{
							JButton jButtonApply = new JButton("apply");
							jPanel2.add(jButtonApply, new GridBagConstraints(0, 2, 1, 1, 10.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
							jButtonApply.setToolTipText("apply parameters");
							jButtonApply.setIcon(new CreateImageIcon(new ImageIcon(getClass().getClassLoader().getResource("icons/Ok.png")),0.1).resizeImageIcon());
							jButtonApply.setBounds(1, 1, 40, 20);
							jButtonApply.addActionListener(new ActionListener() {

								public void actionPerformed(ActionEvent arg0) {

									homologyDataContainer.setThreshold(Double.valueOf(jTable.getValueAt(LOWER_THRESHOLD_ROW, thresholdColIndex)+""));
									homologyDataContainer.setUpperThreshold(Double.valueOf(jTable.getValueAt(UPPER_THRESHOLD_ROW, thresholdColIndex)+""));
									homologyDataContainer.setAlpha(bestAlpha);
									simpleFinish();
									try {
										homologyDataContainer.commitToDatabase(blastDatabase);


										Connection connection = homologyDataContainer.getConnection();
										Statement statement = connection.createStatement();

										HomologyAPI.setBestAlphaFound(statement, blastDatabase);

										if(HomologyAPI.hasCommitedData(statement))
											homologyDataContainer.setHasCommittedData();

										statement.close();
										connection.closeConnection();

									} 
									catch (Exception e) {
										Workbench.getInstance().error(e);
										e.printStackTrace();
									}

									MerlinUtils.updateEnzymesAnnotationView(homologyDataContainer.getWorkspace().getName());
								}});
						}

					}
					JPanel jPanel4 = new JPanel();
					GridBagLayout jPanel4Layout = new GridBagLayout();
					jPanel1.add(jPanel4, new GridBagConstraints(0, 10, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
					jPanel4.setBounds(1, 41, 376, 79);
					jPanel4Layout.rowWeights = new double[] {0.0, 0.1};
					jPanel4Layout.rowHeights = new int[] {7, 7};
					jPanel4Layout.columnWeights = new double[] {0.1, 0.1};
					jPanel4Layout.columnWidths = new int[] {7, 7};
					jPanel4.setLayout(jPanel4Layout);
					//					jPanel4.setBorder(BorderFactory.createTitledBorder(" "));
					{
						Font font = new Font("Courier", Font.BOLD,12);

						{
							jLabelUpper = new JLabel();
							jPanel4.add(jLabelUpper, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
							jLabelUpper.setText("upper threshold");
							jLabelUpper.setFont(font);
						}

						{

							errorMarginUpper = new JComboBox<String>(new DefaultComboBoxModel<>(new String[] {" 100 % precision ", "> 95 % precison" , "> 90 % precison", 
									"> 85 % precison", "> 80 % precison", "> 75 % precison"}));
							jPanel4.add(errorMarginUpper, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
							errorMarginUpper.setSelectedIndex(1);
							errorMarginUpper.setSelectedIndex(0);

							errorMarginUpper.addActionListener(new ActionListener() {

								public void actionPerformed(ActionEvent arg0) {

									includeError(PRECISION_OR_NPV[errorMarginUpper.getSelectedIndex()], 
											PRECISION_OR_NPV[errorMarginLower.getSelectedIndex()]);
									fillList();
								}});
						}
						{
							jLabelLower = new JLabel();
							jPanel4.add(jLabelLower, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
							jLabelLower.setText("lower threshold");
							jLabelLower.setFont(font);
						}
						{
							errorMarginLower = new JComboBox<String>(new DefaultComboBoxModel<>(new String[] {" 100 % negative predictive value", "> 95 % negative predictive value" , "> 90 % negative predictive value" ,
									"> 85 % negative predictive value", "> 80 % negative predictive value", "> 75 % negative predictive value"}));
							jPanel4.add(errorMarginLower, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
							errorMarginLower.setSelectedIndex(1);
							errorMarginLower.setSelectedIndex(0);

							errorMarginLower.addActionListener(new ActionListener() {

								public void actionPerformed(ActionEvent arg0) {

									includeError(PRECISION_OR_NPV[errorMarginUpper.getSelectedIndex()], 
											PRECISION_OR_NPV[errorMarginLower.getSelectedIndex()]);
									fillList();
								}});
						}
					}

					jPanel3 = new JPanel();
					GridBagLayout jPanel3Layout = new GridBagLayout();
					jPanel1.add(jPanel3, new GridBagConstraints(0, 5, 2, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
					jPanel3.setBounds(1, 41, 376, 79);
					jPanel3Layout.columnWeights = new double[] {1, 1, 1, 1, 1, 1, 1, 1, 1, 1};
					jPanel3.setLayout(jPanel3Layout);
					{
						buttonGroupGbk = new ButtonGroup();

						jLabel1 = new JLabel();
						jPanel3.add(jLabel1, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
						jLabel1.setText("       ");

						{
							jRadioButton1 = new JRadioButton();
							jPanel3.add(jRadioButton1, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.CENTER, new Insets(0, 0, 0, 0), 0, 0));
							buttonGroupGbk.add(jRadioButton1);
							buttons.add(jRadioButton1);
							jRadioButton1.addActionListener(new ActionListener() {

								public void actionPerformed(ActionEvent arg0) {

									bestAlpha = 0.1;
									thresholdColIndex = 1;

								}
							});
						}	
						{
							jRadioButton2 = new JRadioButton();
							jPanel3.add(jRadioButton2, new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.CENTER, new Insets(0, 0, 0, 0), 0, 0));
							buttonGroupGbk.add(jRadioButton2);
							buttons.add(jRadioButton2);
							jRadioButton2.addActionListener(new ActionListener() {

								public void actionPerformed(ActionEvent arg0) {

									bestAlpha = 0.2;
									thresholdColIndex = 2;
								}
							});
						}	
						{
							jRadioButton3 = new JRadioButton();
							jPanel3.add(jRadioButton3, new GridBagConstraints(3, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.CENTER, new Insets(0, 0, 0, 0), 0, 0));
							buttonGroupGbk.add(jRadioButton3);
							buttons.add(jRadioButton3);
							jRadioButton3.addActionListener(new ActionListener() {

								public void actionPerformed(ActionEvent arg0) {

									bestAlpha = 0.3;
									thresholdColIndex = 3;
								}
							});
						}	
						{
							jRadioButton4 = new JRadioButton();
							jPanel3.add(jRadioButton4, new GridBagConstraints(4, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.CENTER, new Insets(0, 0, 0, 0), 0, 0));
							buttonGroupGbk.add(jRadioButton4);
							buttons.add(jRadioButton4);
							jRadioButton4.addActionListener(new ActionListener() {

								public void actionPerformed(ActionEvent arg0) {

									bestAlpha = 0.4;
									thresholdColIndex = 4;
								}
							});
						}	
						{
							jRadioButton5 = new JRadioButton();
							jPanel3.add(jRadioButton5, new GridBagConstraints(5, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.CENTER, new Insets(0, 0, 0, 0), 0, 0));
							buttonGroupGbk.add(jRadioButton5);
							buttons.add(jRadioButton5);
							jRadioButton5.addActionListener(new ActionListener() {

								public void actionPerformed(ActionEvent arg0) {

									bestAlpha = 0.5;
									thresholdColIndex = 5;
								}
							});
						}	
						{
							jRadioButton6 = new JRadioButton();
							jPanel3.add(jRadioButton6, new GridBagConstraints(6, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.CENTER, new Insets(0, 0, 0, 0), 0, 0));
							buttonGroupGbk.add(jRadioButton6);
							buttons.add(jRadioButton6);
							jRadioButton6.addActionListener(new ActionListener() {

								public void actionPerformed(ActionEvent arg0) {

									bestAlpha = 0.6;
									thresholdColIndex =6;
								}
							});
						}	
						{
							jRadioButton7 = new JRadioButton();
							jPanel3.add(jRadioButton7, new GridBagConstraints(7, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.CENTER, new Insets(0, 0, 0, 0), 0, 0));
							buttonGroupGbk.add(jRadioButton7);
							buttons.add(jRadioButton7);
							jRadioButton7.addActionListener(new ActionListener() {

								public void actionPerformed(ActionEvent arg0) {

									bestAlpha = 0.7;
									thresholdColIndex = 7;
								}
							});
						}
						{
							jRadioButton8 = new JRadioButton();
							jPanel3.add(jRadioButton8, new GridBagConstraints(8, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.CENTER, new Insets(0, 0, 0, 0), 0, 0));
							buttonGroupGbk.add(jRadioButton8);
							buttons.add(jRadioButton8);
							jRadioButton8.addActionListener(new ActionListener() {

								public void actionPerformed(ActionEvent arg0) {

									bestAlpha = 0.8;
									thresholdColIndex = 8;
								}
							});
						}
						{
							jRadioButton9 = new JRadioButton();
							jPanel3.add(jRadioButton9, new GridBagConstraints(9, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.CENTER, new Insets(0, 0, 0, 0), 0, 0));
							buttonGroupGbk.add(jRadioButton9);
							buttons.add(jRadioButton9);
							jRadioButton9.addActionListener(new ActionListener() {

								public void actionPerformed(ActionEvent arg0) {

									bestAlpha = 0.9;
									thresholdColIndex = 9;
								}
							});
						}


					}
				}

			}

			fillList();
		}
		catch(Exception e){

			e.printStackTrace();
		}
	}

	private void fillList(){

		jTable.setModel(data);
		jTable.setSortableFalse();
		jTable.setRowHeight(40);

		JRadioButton bestParameterRadioButton = (JRadioButton) buttons.get(bestAlphaIndex-1);
		bestParameterRadioButton.setSelected(true);


		TableCellRenderer rendererFromHeader = jTable.getTableHeader().getDefaultRenderer();
		JLabel headerLabel = (JLabel) rendererFromHeader;
		headerLabel.setHorizontalAlignment(JLabel.CENTER);

		DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
		centerRenderer.setHorizontalAlignment(JLabel.CENTER);
		jTable.setDefaultRenderer(String.class, centerRenderer);
		jTable.setDefaultRenderer(Double.class, centerRenderer);
		jTable.setDefaultRenderer(Object.class, centerRenderer);
		jTable.setDefaultRenderer(Long.class, centerRenderer);

		jTable.getTableHeader().setBackground(Color.LIGHT_GRAY);
		//			jTable.getTableHeader().setForeground(Color.WHITE);
		jTable.getTableHeader().setPreferredSize(new Dimension(100, 35));
		jTable.getTableHeader().setDefaultRenderer(centerRenderer);
		jTable.getTableHeader().setReorderingAllowed(false);
		jScrollPane.setViewportView(jTable);


		jTable.updateUI();

		this.setModal(true);

	}

	/**
	 * Calculates the new parameters using error.
	 * 
	 * @param error
	 */
	private void includeError(double errorUpper, double errorLower) {

		double[] upper = BestAlphaStatsCalculator.getUpperThreshold(confusionMat, threshold, errorUpper);
		double[] lower = BestAlphaStatsCalculator.getLowerThreshold(confusionMat, upper, threshold, errorLower);

		double userThreshold = homologyDataContainer.getThreshold();
		homologyDataContainer.setThreshold(0.0);

		double[] accuracy = BestAlphaStatsCalculator.getAccuracy(confusionMat);

		int[] below = BestAlphaStatsCalculator.getCountBelow(lower, counts);
		int[] above = BestAlphaStatsCalculator.getCountAbove(upper, counts);
		int[] total = BestAlphaStatsCalculator.getTotal(counts);

		Object[] xAndY = BestAlphaStatsCalculator.getXAndY(below, above, total, accuracy); 

		double[] x = (double[]) xAndY[0];
		double[] y = (double[]) xAndY[1];

		data = BestAlphaStatsCalculator.resultsTable(array, lower, upper, y, x, threshold, accuracy,
				below, above, total);

		bestAlphaIndex = BestAlphaStatsCalculator.getBestAlphaIndex(y, accuracy);
		this.bestAlpha = Double.parseDouble("0." + bestAlphaIndex);

		thresholdColIndex = bestAlphaIndex;

		homologyDataContainer.setAlpha(0.5);
		homologyDataContainer.setThreshold(userThreshold);

	}

	public void simpleFinish() {

		this.setVisible(false);
		this.dispose();
	}
}
