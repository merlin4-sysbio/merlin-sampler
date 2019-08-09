package pt.uminho.ceb.biosystems.merlin.sampler;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.EventObject;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableColumnModel;

import org.apache.commons.lang.ArrayUtils;
import org.apache.jcs.access.exception.InvalidArgumentException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import es.uvigo.ei.aibench.core.Core;
import es.uvigo.ei.aibench.core.ParamSpec;
import es.uvigo.ei.aibench.core.operation.OperationDefinition;
import es.uvigo.ei.aibench.workbench.Workbench;
import es.uvigo.ei.aibench.workbench.utilities.Utilities;
import pt.uminho.ceb.biosystems.merlin.aibench.datatypes.annotation.AnnotationEnzymesAIB;
import pt.uminho.ceb.biosystems.merlin.aibench.gui.CustomGUI;
import pt.uminho.ceb.biosystems.merlin.aibench.utilities.ComboBoxColumn;
import pt.uminho.ceb.biosystems.merlin.aibench.utilities.CreateImageIcon;
import pt.uminho.ceb.biosystems.merlin.aibench.utilities.LinkOut;
import pt.uminho.ceb.biosystems.merlin.aibench.utilities.MyJTable;
import pt.uminho.ceb.biosystems.merlin.aibench.utilities.TimeLeftProgress;
import pt.uminho.ceb.biosystems.merlin.core.datatypes.WorkspaceDataTable;
import pt.uminho.ceb.biosystems.merlin.core.datatypes.WorkspaceGenericDataTable;
import pt.uminho.ceb.biosystems.merlin.utilities.datastructures.map.MapUtils;
import pt.uminho.ceb.biosystems.merlin.utilities.io.FileUtils;

public class EnzymesAnnotationJDialog extends javax.swing.JDialog {

	private static final int STARTCOLUMN = 0, FINISHCOLUMN = 1;
	private static final int LOCUS_TAG_COLUMN_NUMBER = 1, EC_NUMBERS_COLUMN_NUMBER = 6, EC_SCORE_COLUMN_NUMBER = 7;
	private static final long serialVersionUID = -1L;
	private JPanel jPanel1, jPanel2;
	private JScrollPane jScrollPane;
	private JTextField jSampleSizeTextField;
	private JLabel jLabel1, jLabel2;
	private MouseListener enzymesMouseAdapter;
	private ItemListener enzymesItemListener;
	private PopupMenuListener enzymesPopupMenuListener;
	private Map<Integer, String> itemsList;
	private Map<Integer, String> values = new HashMap<>();
	private MyJTable newjTable = new MyJTable();
	private AnnotationEnzymesAIB homologyDataContainer;
	private Map<Integer,String> ecMap = new TreeMap<Integer,String>();
	private ComboBoxColumn ecList;
	private int sampleSize;
	private MouseAdapter tableMouseAdapator;
	private TableModelListener tableModelListener;
	@SuppressWarnings("unused")
	private int selectedModelRow;
	private WorkspaceDataTable randomTable;
	private String blastDatabase;
	private TimeLeftProgress progress = new TimeLeftProgress();
	private AtomicBoolean cancel = new AtomicBoolean(false);
	private AtomicInteger querySize;
	private AtomicInteger counter = new AtomicInteger(0);
	private WorkspaceDataTable mainTableData;
	private boolean searchFile = true;
	private Map<Integer,String> ecTable = new TreeMap<Integer,String>();
	private long startTime;
	private Integer totalOfMetabolicGenes;


	/**
	 * @param sampleSize
	 * @param ecnumbersColumnNumber
	 * @param ecScoreColumnNumber
	 * @param values
	 * @param itemsList
	 * @param locus_tagColumnNumber
	 * @param data
	 * @param homologyDataContainer
	 */
	public EnzymesAnnotationJDialog(String blastDatabase, int sampleSize, 
			AnnotationEnzymesAIB homologyDataContainer, boolean searchFile, Integer totalOfMetabolicGenes) {

		super(Workbench.getInstance().getMainFrame());
		this.homologyDataContainer = homologyDataContainer;
		this.sampleSize = sampleSize;
		this.blastDatabase = blastDatabase;
		this.totalOfMetabolicGenes = totalOfMetabolicGenes;

		generateTable(searchFile);

		this.addMouseListener();
		this.addTableModelListener();
		if(this.enzymesItemListener==null)
			this.enzymesItemListener = this.getComboBoxEnzymesItemListener();
		if(this.enzymesMouseAdapter==null)
			this.enzymesMouseAdapter = this.getComboBoxEnzymesMouseListener();
		if(this.enzymesPopupMenuListener==null)
			this.enzymesPopupMenuListener = this.getComboBoxEnzymesPopupMenuListener();

		initGUI();
		Utilities.centerOnOwner(this);
		this.setVisible(true);		
		//		this.setAlwaysOnTop(true);
		this.toFront();
	}

	private void initGUI() {

		try {

			GridBagLayout thisLayout = new GridBagLayout();
			thisLayout.columnWeights = new double[] {0.0, 0.1, 0.0};
			thisLayout.columnWidths = new int[] {7, 7, 7};
			thisLayout.rowWeights = new double[] {0.0, 200.0, 0.0, 0.0, 0.0};
			thisLayout.rowHeights = new int[] {7, 50, 7, 3, 7};
			this.setLayout(thisLayout);
			this.setPreferredSize(new Dimension(875, 585));
			this.setSize(550, 800);
			{
				this.setTitle("sample selection window");

				jPanel1 = new JPanel();
				GridBagLayout jPanel1Layout = new GridBagLayout();
				jPanel1Layout.rowWeights = new double[] {0.1};
				jPanel1Layout.rowHeights = new int[] {7};
				jPanel1Layout.columnWeights = new double[] {0.1};
				jPanel1Layout.columnWidths = new int[] {7};
				jPanel1.setLayout(jPanel1Layout);
				this.add(jPanel1, new GridBagConstraints(1, 1, 1, 1, 1.0, 0.5, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));

				jScrollPane = new JScrollPane();
				jPanel1.add(jScrollPane, new GridBagConstraints(0, 0, 1, 2, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
				jScrollPane.setPreferredSize(new java.awt.Dimension(700, 700));
				jScrollPane.setSize(900, 420);
				{
					jPanel2 = new JPanel();
					GridBagLayout jPanel2Layout = new GridBagLayout();
					jPanel1.add(jPanel2, new GridBagConstraints(0, 10, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
					jPanel2.setBounds(1, 41, 376, 79);
					jPanel2.setBorder(BorderFactory.createTitledBorder("options"));
					jPanel2Layout.rowWeights = new double[] {0.0, 0.1, 0.0, 0.1, 0.0, 0.1};
					jPanel2Layout.rowHeights = new int[] {7, 7, 7, 7, 7, 7};
					jPanel2Layout.columnWeights = new double[] {0.1, 0.1, 0.1, 0.1};
					jPanel2Layout.columnWidths = new int[] {7, 20, 7, 7};
					jPanel2.setLayout(jPanel2Layout);
					{

						jLabel1 = new JLabel();
						jPanel2.add(jLabel1, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
						jLabel1.setText("new sample size (%):");
						jLabel1.setLabelFor(jSampleSizeTextField);

						jLabel2 = new JLabel();
						jPanel2.add(jLabel2, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

						jSampleSizeTextField = new JTextField("", 3);
						jPanel2.add(jSampleSizeTextField, new GridBagConstraints(1, 1, 2, 1, 2.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
						jSampleSizeTextField.setToolTipText("enter the size(%) of the new sample (" + AnnotationEnzymesParametersSetting.DEFAULT_RATIO + " by default)");
						jSampleSizeTextField.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(BevelBorder.LOWERED),null));
						jSampleSizeTextField.setBounds(164, 57, 36, 20);

						JButton jButtonBestAlpha = new JButton();
						jPanel2.add(jButtonBestAlpha, new GridBagConstraints(4, 2, 2, 1, 50.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
						jButtonBestAlpha.setText("find best parameters");
						jButtonBestAlpha.setToolTipText("press to find best parameters.");
						jButtonBestAlpha.setIcon(new CreateImageIcon(new ImageIcon(getClass().getClassLoader().getResource("icons/Find.png")),0.1).resizeImageIcon());
						jButtonBestAlpha.setBounds(1, 1, 40, 20);
						jButtonBestAlpha.addActionListener(new ActionListener() {

							public void actionPerformed(ActionEvent arg0) {

								Map<Integer, String> ecCurated = ecList.getValues();

								try {

									writeNewFile();

									ParamSpec[] paramsSpec = new ParamSpec[]{
											new ParamSpec("ecCurated", Map.class, ecCurated, null),
											new ParamSpec("ecMap", Map.class, ecMap, null),
											new ParamSpec("homologyDataContainer", AnnotationEnzymesAIB.class, homologyDataContainer, null),
											new ParamSpec("ecnumbersColumnNumber", Integer.class, EC_NUMBERS_COLUMN_NUMBER, null),
											new ParamSpec("blastDatabase", String.class, blastDatabase, null),
											new ParamSpec("ecScoreColumnNumber", Integer.class, EC_SCORE_COLUMN_NUMBER, null)
									};

									for (@SuppressWarnings("rawtypes") OperationDefinition def : Core.getInstance().getOperations()){
										if (def.getID().equals("operations.AnnotationEnzymesBestAlpha.ID")){

											Workbench.getInstance().executeOperation(def, paramsSpec);
										}
									}

								}
								catch (Exception e) {
									e.printStackTrace();
									Workbench.getInstance().error("an error occurred while calculating best parameters");

								}

								simpleFinish();
							}});
					}
					{
						JButton jButtonNewSample = new JButton();
						jPanel2.add(jButtonNewSample, new GridBagConstraints(4, 1, 1, 1, 50.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
						jButtonNewSample.setText("new sample");
						jButtonNewSample.setToolTipText("generate new sample");
						jButtonNewSample.setIcon(new CreateImageIcon(new ImageIcon(getClass().getClassLoader().getResource("icons/Synchronize.png")),0.1).resizeImageIcon());
						jButtonNewSample.setBounds(1, 1, 40, 20);
						jButtonNewSample.addActionListener(new ActionListener() {

							public void actionPerformed(ActionEvent arg0) {

								if (isInteger(jSampleSizeTextField.getText()) == true){
									int size = Integer.parseInt(jSampleSizeTextField.getText());
									if (size > 0 && size < 100){
										
										try {
											sampleSize = (int)(totalOfMetabolicGenes * (size/100.0));
											
											renderWaitMessage();

											boolean generateNew = resetTable();
											
//											System.out.println(sampleSize);

											if(generateNew) {
												new EnzymesAnnotationJDialog(blastDatabase, sampleSize, 
														homologyDataContainer, false, totalOfMetabolicGenes);
												simpleFinish();
											}
											else
												jScrollPane.setViewportView(newjTable);

										} catch (Exception e) {
											Workbench.getInstance().error(e);
											e.printStackTrace();
										}
					

										//										generateTable(false);

										

										//										jLabel2.setText("sample retrieved: " + getTableSize());
									}
									else{
										Workbench.getInstance().error("value must be > 0 and < 100!");
										jSampleSizeTextField.setText("");
//										JOptionPane.showMessageDialog(rootPane, "value must be > 0 and < 100!");
									}
								}
								else{
									Workbench.getInstance().error("please insert an integer");
									jSampleSizeTextField.setText("");
//									JOptionPane.showMessageDialog(rootPane, "please insert an integer");
								}
							}});
					}

					{
						JButton jButtonExport = new JButton();
						jPanel2.add(jButtonExport, new GridBagConstraints(5, 1, 1, 1, 50.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
						jButtonExport.setText("export file");
						jButtonExport.setToolTipText("export to excel file (xlsx)");
						jButtonExport.setIcon(new CreateImageIcon(new ImageIcon(getClass().getClassLoader().getResource("icons/Download.png")),0.1).resizeImageIcon());
						jButtonExport.setBounds(1, 1, 40, 20);
						jButtonExport.addActionListener(new ActionListener() {

							public void actionPerformed(ActionEvent arg0) {

								try {

									JFileChooser fc = new JFileChooser();
									fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
									fc.setDialogTitle("Select directory");
									int returnVal = fc.showOpenDialog(new JTextArea());

									if (returnVal == JFileChooser.APPROVE_OPTION) {

										File file = fc.getSelectedFile();
										String excelFileName = file.getAbsolutePath();
										Calendar cal = new GregorianCalendar();

										// Get the components of the time
										int hour24 = cal.get(Calendar.HOUR_OF_DAY);     // 0..23
										int min = cal.get(Calendar.MINUTE);             // 0..59
										int day = cal.get(Calendar.DAY_OF_YEAR);		//0..365

										String name = homologyDataContainer.getWorkspace().getName();

										excelFileName += "/"+name+"_"+hour24+"_"+min+"_"+day+".xlsx";

										String sheetName = "annotation";

										Workbook wb = new XSSFWorkbook();
										Sheet sheet = wb.createSheet(sheetName) ;

										WorkspaceDataTable table = createTableToExport();

										Row row = sheet.createRow(0);

										TableColumnModel tc = newjTable.getColumnModel();

										int i = 0;

										while (i < tc.getColumnCount()) {

											row.createCell(i).setCellValue(tc.getColumn(i).getHeaderValue().toString());
											i++;
										}


										for (int r=0;r < table.getRowCount(); r++ )
										{
											row = sheet.createRow(r+2);

											//iterating c number of columns
											for (int c=0;c < table.getColumnCount(); c++ )
											{
												Cell cell = row.createCell(c);

												cell.setCellValue(table.getValueAt(r, c).toString());
											}
										}

										FileOutputStream fileOut = new FileOutputStream(excelFileName);

										//write this workbook to an Outputstream.
										wb.write(fileOut);
										fileOut.flush();
										wb.close();
										fileOut.close();

										Workbench.getInstance().info("data successfully exported.");
									}
								} catch (Exception e) {

									Workbench.getInstance().error("an error occurred while performing this operation. Error "+e.getMessage());
									e.printStackTrace();
								}
							}});
					}

					{
						JButton jButtonSave = new JButton();
						jPanel2.add(jButtonSave, new GridBagConstraints(6, 1, 1, 1, 50.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
						jButtonSave.setText("save");
						jButtonSave.setToolTipText("save data");
						jButtonSave.setIcon(new CreateImageIcon(new ImageIcon(getClass().getClassLoader().getResource("icons/Save.png")),0.1).resizeImageIcon());
						jButtonSave.setBounds(1, 1, 40, 20);
						jButtonSave.addActionListener(new ActionListener() {

							public void actionPerformed(ActionEvent arg0) {

								try {
									writeNewFile();
									JOptionPane.showMessageDialog(rootPane, "data saved successfully");
								} catch (IOException e) {

									e.printStackTrace();
									JOptionPane.showMessageDialog(rootPane, "error - data not saved!");
								}
							}});
					}
					{
						JButton jButtonClose = new JButton();
						jPanel2.add(jButtonClose, new GridBagConstraints(6, 2, 1, 1, 50.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
						jButtonClose.setText("close");
						jButtonClose.setToolTipText("close window");
						jButtonClose.setIcon(new CreateImageIcon(new ImageIcon(getClass().getClassLoader().getResource("icons/Cancel.png")),0.1).resizeImageIcon());
						jButtonClose.setBounds(1, 1, 40, 20);
						jButtonClose.addActionListener(new ActionListener() {

							public void actionPerformed(ActionEvent arg0) {

								simpleFinish();
							}});
					}
				}
			}

			newjTable.setModel(randomTable);
			newjTable.setSortableFalse();
			ecList = new ComboBoxColumn(newjTable, 1, values , this.enzymesItemListener, this.enzymesMouseAdapter, this.enzymesPopupMenuListener);

			newjTable.setRowHeight(20);

			jScrollPane.setViewportView(newjTable);

			jLabel2.setText("sample retrieved: " + getTableSize());

			//			this.setModal(true);
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}

	private int getSampleSize(){
		return sampleSize;
	}

	private boolean isInteger(String str){
		try {
			Integer.parseInt(str);
			return true;
		} 
		catch (NumberFormatException nfe) {
			return false;
		}
	}
	private int getTableSize(){
		return ecMap.size();
	}

	private void generateTable(boolean searchFile){ 		

		try {
			this.startTime = GregorianCalendar.getInstance().getTimeInMillis();

			this.progress.setTime(GregorianCalendar.getInstance().getTimeInMillis() - this.startTime, 0, 2, "generating random sample...");

			this.itemsList = this.homologyDataContainer.getItemsList().get(1);

			double userThreshold = homologyDataContainer.getThreshold();
			double userAlpha = homologyDataContainer.getAlpha();

			Map <Integer, String> data = this.openFile();

			this.ecMap = new TreeMap<Integer,String>();

			this.values = new HashMap<>();

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
		} 
		catch (Exception e) {
			Workbench.getInstance().error(e);
			e.printStackTrace();
		}
	}

	private void renderWaitMessage() {

		jScrollPane.setViewportView(new RenderingMessageComponent("Generating new sample, please wait..."));

		jPanel1.add(jScrollPane, new GridBagConstraints(0, 0, 1, 2, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));

		jScrollPane.revalidate();
		jScrollPane.repaint();



	}
	private ItemListener getComboBoxEnzymesItemListener() {

		return new ItemListener() {

			@Override
			public void itemStateChanged(ItemEvent e) {

				processEnzymesComboBoxChange(e);
			}
		};
	}

	private MouseAdapter getComboBoxEnzymesMouseListener() {

		return new MouseAdapter() {

			public void mouseClicked(MouseEvent e) {

				Point p = e.getPoint();

				int  columnNumber = newjTable.columnAtPoint(p);
				newjTable.setColumnSelectionInterval(columnNumber, columnNumber);

				int selectedModelRow = newjTable.getSelectedRow();
				homologyDataContainer.setSelectedRow(selectedModelRow);

				int myRow = newjTable.getSelectedRow();

				if(myRow>-1 && newjTable.getRowCount()>0 && newjTable.getRowCount()> myRow) {

					newjTable.setRowSelectionInterval(myRow, myRow);
					scrollToVisible(newjTable.getCellRect(myRow, -1, true));
				}

				processEnzymesComboBoxChange(e);
			}
		};		
	}

	private PopupMenuListener getComboBoxEnzymesPopupMenuListener() {

		return new PopupMenuListener() {

			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {

				processEnzymesComboBoxChange(e);
			}

			@Override
			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {

			}

			@Override
			public void popupMenuCanceled(PopupMenuEvent e) {

			}
		};
	}

	@SuppressWarnings("unchecked")
	private void processEnzymesComboBoxChange(EventObject e) {

		boolean go = false;
		JComboBox<String> comboBox = null;

		if(e.getClass()==MouseEvent.class) {

			Object obj = ((MouseEvent) e).getSource();

			if(obj instanceof JComboBox)
				comboBox = (JComboBox<String>) obj;

			ListSelectionModel model = newjTable.getSelectionModel();
			model.setSelectionInterval( ecList.getSelectIndex(comboBox), ecList.getSelectIndex(comboBox));

			if(((MouseEvent) e).getButton()==MouseEvent.BUTTON3 ) {

				List<Integer> dbs = new ArrayList<Integer>();

				String text=null;

				dbs.add(1);
				dbs.add(3);
				text=comboBox.getSelectedItem().toString();

				if(text!=null) 
					new LinkOut(dbs, text).show(((MouseEvent) e).getComponent(),((MouseEvent) e).getX(), ((MouseEvent) e).getY());
			}
		}

		else if((e.getClass()==ItemEvent.class && ((ItemEvent) e).getStateChange() == ItemEvent.SELECTED) ) {

			Object obj = ((ItemEvent) e).getSource();

			if(obj instanceof JComboBox) 
				comboBox = (JComboBox<String>) obj;

			if(comboBox != null) 
				go = true;
		}

		else if(e.getClass() == PopupMenuEvent.class) {

			Object obj = ((PopupMenuEvent) e).getSource();

			if(obj instanceof JComboBox) 
				comboBox = (JComboBox<String>) obj;

			if(comboBox != null) 
				go = true;
		}

		if(go) {
			int row = ecList.getSelectIndex(comboBox);
			comboBox.setSelectedIndex(comboBox.getSelectedIndex());
			ecList.getValues().put(row, comboBox.getSelectedItem().toString());
			ecList.setComboBox(ecList.getSelectIndex(comboBox), comboBox);
		}
	}

	private void scrollToVisible(final Rectangle visible) {

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				newjTable.scrollRectToVisible(visible);
			}
		});
	}

	private void addMouseListener() {

		if(this.tableMouseAdapator==null)
			this.tableMouseAdapator = this.getTableMouseAdapator();

		newjTable.addMouseListener(this.tableMouseAdapator);
	}

	private MouseAdapter getTableMouseAdapator() {

		MouseAdapter mouseAdapter = new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent arg0) {

				selectedModelRow=newjTable.getSelectedRow();

				if(newjTable.getSelectedRow()>-1 && newjTable.getRowCount()>0 && newjTable.getRowCount()> newjTable.getSelectedRow()) {

					newjTable.setRowSelectionInterval(newjTable.getSelectedRow(), newjTable.getSelectedRow());
					scrollToVisible(newjTable.getCellRect(newjTable.getSelectedRow(), -1, true));
				}

				{
					Point p = arg0.getPoint();
					int  columnNumber = newjTable.columnAtPoint(p);
					newjTable.setColumnSelectionInterval(columnNumber, columnNumber);
				}
			}
		};
		return mouseAdapter;
	}

	private void addTableModelListener() {

		if(this.tableModelListener == null)
			this.tableModelListener = this.getTableModelListener();

		newjTable.getModel().addTableModelListener(this.tableModelListener);

	}

	private TableModelListener getTableModelListener() {

		TableModelListener tableModelListener = new TableModelListener() {

			@Override
			public void tableChanged(TableModelEvent e) {

				if(newjTable.getSelectedRow()>-1) {

					selectedModelRow=newjTable.getSelectedRow();
				}
			}};
			return tableModelListener;
	}

	public void simpleFinish() {

		this.setVisible(false);
		this.dispose();
	}

	private void writeNewFile() throws IOException{

		try {
			String databaseName = homologyDataContainer.getWorkspace().getName();

			Long taxonomyID = homologyDataContainer.getWorkspace().getTaxonomyID();

			String fileName =  "AutoGeneSelection_" + this.blastDatabase + ".txt";

			if(blastDatabase.isEmpty())
				fileName =  "AutoGeneSelection.txt";

			String path = FileUtils.getWorkspaceTaxonomyFolderPath(databaseName, taxonomyID) + fileName;
			File file = new File(path);
			PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(file)));

			for (int i: ecMap.keySet())
				out.write(i + "\t" + ecList.getValues().get(Integer.parseInt(ecMap.get(i))) + "\n");

			out.close();
		}
		catch (IOException e) {

			System.out.println("FILE NOT FOUND!!");

			//			e.printStackTrace();
		}
	}

	/**
	 * Method to generate a table containing the information to be exported.
	 * @return DataTable
	 */
	private WorkspaceDataTable createTableToExport(){

		List<String> columnsNames = Arrays.asList("gene", "EC number");

		WorkspaceDataTable data = new WorkspaceGenericDataTable(columnsNames, "", "");  

		for (int i = 0; i < newjTable.getRowCount(); i++){

			ArrayList<Object> line = new ArrayList<>();

			line.add(newjTable.getValueAt(i, 0));
			line.add(ecList.getSelectItem(i));

			data.addLine(line);
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
			double value = Double.parseDouble(score.replace("<", ""));
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
		
		if(n == 0)
			n = 1;

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

	/**
	 * @return
	 */
	private boolean resetTable() {

		int i = CustomGUI.stopQuestion("Continue", 
				"this action will discard all your annotations in this sample. continue?",
				new String[]{"Yes", "No"});


		switch (i)
		{
		case 0:
		{
			return true;
		}
		default:
		{
			return false;
		}
		}
	}

	class RenderingMessageComponent extends JPanel{
		private static final long serialVersionUID = 1L;

		public RenderingMessageComponent(String message){
			this.setLayout(new BorderLayout());
			JLabel label = new JLabel(message);
			label.setHorizontalAlignment(JLabel.CENTER);
			this.add(label, BorderLayout.CENTER);
		}
	}
}



