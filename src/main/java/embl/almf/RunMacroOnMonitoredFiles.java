package embl.almf;
//Kota Miura (cmci.embl.de)
//20121122



import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.Border;

import common.ScriptRunner;



import ij.IJ;
import ij.Macro;
import ij.io.OpenDialog;
import ij.macro.MacroRunner;

public class RunMacroOnMonitoredFiles extends AbsMonitorFolderFiles implements ActionListener {
	boolean okpushed, cancelpushed;
	JFrame Dialog;
	JButton bt_open1, bt_open2, bt_ok, bt_cancel;
	JTextField directory, macrotorun, fileEndDialog;
	String watchpath;
	String fileEnd ="lsm";
	String macropath = "";
	int scripttype = 0; //0 or MACRO, 1 for jython
	
	//This contains the full macro
	String macrotext = "";
	int maxrun = Integer.MAX_VALUE;
	String monitor_threadname = "";
	int runcount = 0;
	RunMacroOnMonitoredFiles mff;
    private String latestFileName = "";	
	
	private volatile Thread monitorthread;
	private MonitorFileGUI mgui; // will be !null if this class is called from gui
	
	public RunMacroOnMonitoredFiles(){
		super();
		this.watchpath = "";
	}
	
	public RunMacroOnMonitoredFiles(String macropath){
		super();
		this.macropath = macropath;
		this.watchpath = "C:\\FolderName\\";
		macrotext = IJ.openAsString(this.macropath);
	}
	public boolean checkMacroFileExists(String FOLDER){
		File f = new File(FOLDER);
		return f.exists();
	}


	public void execute() {
		//mff = new RunMacroOnMonitoredFiles();
		if (dialog()){	
			try {
				this.startMonitoring();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			IJ.log("Macro has been cancelled!");
			//IJ.error("Path assignment failed! Check paths again...");
		}
	}	
	
	@Override
	void runOnChangedFile(File file) {
		runOnNewFile(file);
	}

	@Override
	void runOnNewFile(File file) {
		String addedfilepath = "";
		try {
			addedfilepath = file.getCanonicalPath();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		if (mgui != null){
			if (mgui.isVisible()){
				// set message field for currently recognized file
			}
		}
		
		//check file end
		if ( addedfilepath.endsWith(this.fileEnd)) { 
			IJ.log("start script");
			this.setLatestFileName(addedfilepath); //notifies also observer
			switch (this.scripttype) {
				case 0://MACRO
					MacroRunner mr = new MacroRunner(macrotext, addedfilepath);
					break;
				case 1: //JYTHON
					IJ.log("Initiate script");
					HashMap<String, Object> arg = new HashMap<String, Object>();
					arg.put("newImagePath", addedfilepath);
					ScriptRunner.runPY(this.macropath, arg);
					break;
				default:
					IJ.log("scriptype values are 0: MACRO, 1:JYTHON");
					throw new IllegalArgumentException("scriptype values are 0: MACRO, 1:JYTHON");
			}
				
	        monitorthread = Thread.currentThread();
			monitor_threadname = monitorthread.getName();
	        IJ.log("folder monitor thread name:" + monitor_threadname);
	        this.runcount += 1;
        
	        if (this.runcount >= maxrun ){
	        	this.runcount = 1;  		
	        }
		}
		
	}

	@Override
	void runOnFileRemove(File file) {
        try {
            // "file" is the reference to the removed file
            IJ.log("File removed: "
                    + file.getCanonicalPath());
            // "file" does not exists anymore in the location
            IJ.log("File still exists in location: "
                    + file.exists());
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
		
	}
	
	public void setRunMax(int maxrun){
		this.maxrun = maxrun;
	}
	
	public void setMacroFile(String macrofilepath){
		this.macropath = macrofilepath;
	}
	
	public void setFilEnd(String fileEnd){
		this.fileEnd = fileEnd;
	}
	

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == bt_open1) {
			//Do something
			//IJ.log("pushed open1");
			this.watchpath = IJ.getDirectory(""); ;
			setWatchpath(this.watchpath);
			directory.setText(this.watchpath);
		}
		if (e.getSource() == bt_open2) {
			//Do something
			//IJ.log("pushed open1");
			OpenDialog od = new OpenDialog("Choose a .ijm file", null); 
			this.macropath =  od.getDirectory() + od.getFileName();
			this.macrotext = IJ.openAsString(this.macropath);
			//this.watchpath =
			macrotorun.setText(this.macropath);
		}
		if (e.getSource() == bt_ok) {
			//Do something
			//IJ.log("pushed Ok");
			//check for folder and macropath
			this.fileEnd = fileEndDialog.getText();
			IJ.log(" monotor files with ending" + this.fileEnd);
			if (!checkMacroFileExists(macropath)) {
				IJ.log("No such macro file: " + this.macropath);
			}
			if (!checkDirExists(watchpath)){
				IJ.log("No such directory to watch: " + this.watchpath);			
			}
			if (checkMacroFileExists(macropath) && checkDirExists(watchpath)) {
				Dialog.dispose();
				Dialog = null;
				okpushed = true;
			}
				
		}
		if (e.getSource() == bt_cancel) {
			//Do something
			//IJ.log("pushed cancel");
			cancelpushed = true;
			Dialog.dispose();
			Dialog = null;
			//Macro.abort();
		}
		
	}
	
	public boolean dialog(){
		Font font1 = new Font("Default", Font.PLAIN, 12);
		Font font1small = new Font("DefaultSmall", Font.PLAIN, 12);		
		//creates the dialog
		Dialog = new JFrame("Macro Runner Settings");
		directory = new JTextField("",20);
		macrotorun = new JTextField("",20);
		fileEndDialog = new JTextField("lsm", 3);
		JLabel labeldir = new JLabel("Directory");
		JLabel labelmacro = new JLabel("Macro");
		JLabel labelFileEnd = new JLabel("File type");
		JLabel msg = new JLabel("Applies a macro when a new file appears " +
				"in a monitored directory.");
		msg.setFont(font1small);
		labeldir.setFont(font1small);
		labelmacro.setFont(font1small);
		
		//buttons
		bt_open1 = new JButton("Browse");
		bt_open1.setFont(font1);
		bt_open1.addActionListener(this);
		bt_open2 = new JButton("Browse");
		bt_open2.addActionListener(this);
		bt_open2.setFont(font1);
		
		bt_ok = new JButton("OK");
		bt_ok.setFont(font1);
		bt_cancel = new JButton("Cancel");
		bt_cancel.setFont(font1);
		bt_ok.addActionListener(this);
		bt_cancel.addActionListener(this);
		
		//dialog panels
		JPanel topp = new JPanel();
		topp.add(msg);
		
		JPanel upperp = new JPanel();
		upperp.setLayout(new FlowLayout(1,3,0));
		upperp.add(labeldir);
		upperp.add(directory);
		upperp.add(bt_open1);
		
		JPanel middlep = new JPanel();
		middlep.setLayout(new FlowLayout(1,3,0));
		middlep.add(labelmacro);
		middlep.add(macrotorun);
		middlep.add(bt_open2);
		
		JPanel middlep2 = new JPanel();
		middlep2.setLayout(new FlowLayout(1,2,0));
		middlep2.add(labelFileEnd);
		middlep2.add(fileEndDialog);
		
		
		JPanel lowerp = new JPanel();
		lowerp.setLayout(new FlowLayout(1,2,0));
		lowerp.add(bt_ok);
		lowerp.add(bt_cancel);
		
		
		
		JPanel fullp = new JPanel();
		fullp.setLayout(new GridLayout(5,1));
		fullp.add(topp);
		fullp.add(upperp);
		fullp.add(middlep);
		fullp.add(middlep2);
		fullp.add(lowerp);
		
		
		Dialog.getContentPane().add(fullp, BorderLayout.CENTER);
		Dialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		Dialog.setSize(400, 200);
		Dialog.setVisible(true);
		
		//do different things depending what has been pushed on dialog
		if (Dialog == null)
			cancelpushed = true;
		
		while( !cancelpushed && !okpushed ) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if (okpushed) {
			//check a last time
			if (!checkMacroFileExists(this.macropath) || !checkDirExists(this.watchpath))  {
				IJ.error("Path assignment failed! Check paths again...");
				//Macro.abort();
			}
			
			return true;
		}
		
		return false;
		
	}
	
	
	public String getLatestFileName() {
		return latestFileName;
	}

	public void setLatestFileName(String latestFileName) {
		this.latestFileName = latestFileName;
		super.notifyObservers();
	}

/** for debugging
  * 
  * @param args
  */
 public static void main(String[] args) {
	RunMacroOnMonitoredFiles mff = new RunMacroOnMonitoredFiles();
	mff.execute();
 	
 }	

}


