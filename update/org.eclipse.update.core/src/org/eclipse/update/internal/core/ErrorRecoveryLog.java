package org.eclipse.update.internal.core;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.Date;
import java.util.List;

import org.eclipse.core.boot.BootLoader;
import org.eclipse.core.boot.IPlatformConfiguration;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.update.core.Utilities;

/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */

/**
 * singleton pattern.
 * manages the error/recover log file
 */
public class ErrorRecoveryLog {

	public static final boolean RECOVERY_ON = false;

	private static final String ERROR_RECOVERY_LOG = "platform.cfg.log";
	private static final String LOG_ENTRY_KEY = "LogEntry.";
	private static final String RETURN_CARRIAGE = "\r\n";
	private static final String END_OF_FILE = "eof=eof";

	//
	public static final String START_INSTALL_LOG = 	"START_INSTALL_LOG";
	public static final String PLUGIN_ENTRY = 		"PLUGIN";
	public static final String FRAGMENT_ENTRY = 		"FRAGMENT";	
	public static final String FEATURE_ENTRY = 		"FEATURE";
	public static final String ALL_INSTALLED = 		"ALL_FEATURES_INSTALLED";
	public static final String RENAME_ENTRY = 		"RENAME";
	public static final String END_INSTALL_LOG = 	"END_INSTALL_LOG";
	public static final String START_REMOVE_LOG = 	"REMOVE_LOG";
	public static final String END_ABOUT_REMOVE =	"END_ABOUT_TO_REMOVE";
	public static final String DELETE_ENTRY = 		"DELETE";
	public static final String END_REMOVE_LOG = 		"END_REMOVE_LOG";

	public static boolean forceRemove = false;

	private static ErrorRecoveryLog inst;
	private FileWriter out;
	private int index;
	private List paths;
	
	private boolean open = false;
	private int nbOfOpen = 0;
	

	/**
	 * Constructor for ErrorRecoveryLog.
	 */
	private ErrorRecoveryLog() {
		super();
	}

	/**
	 * Singleton
	 */
	public static ErrorRecoveryLog getLog() {
		if (inst == null){
			inst = new ErrorRecoveryLog();
		}
		return inst;
	}

	/**
	 * get a unique identifer for the file, ensure uniqueness up to now
	 */
	public static String getLocalRandomIdentifier(String path) {
		
		if (path==null) return null;
		
		// verify if it will be a directory without creating the file
		// as it doesn't exist yet
		if (path.endsWith(File.separator) || path.endsWith("/"))
			return path;
		File file = new File(path);
		String newName =
			UpdateManagerUtils.getLocalRandomIdentifier(file.getName(), new Date());
		while (new File(newName).exists()) {
			newName =
				UpdateManagerUtils.getLocalRandomIdentifier(file.getName(), new Date());
		}
		File newFile = new File(file.getParentFile(),newName);
		return newFile.getAbsolutePath();
	}

	/**
	 * returns the log file 
	 * We do not check if the file exists
	 */
	public File getRecoveryLogFile() {
		IPlatformConfiguration configuration =
			BootLoader.getCurrentPlatformConfiguration();
		URL location = configuration.getConfigurationLocation();
		String locationString = location.getFile();
		File platformConfiguration = new File(locationString);
		if (!platformConfiguration.isDirectory()) platformConfiguration = platformConfiguration.getParentFile();
		return new File(platformConfiguration, ERROR_RECOVERY_LOG);
	}


	/**
	 * Open the log
	 */
	public void open(String logEntry) throws CoreException {
		if (open) {
			nbOfOpen++;			
			UpdateManagerPlugin.warn("Open nested Error/Recovery log #"+nbOfOpen+":"+logEntry);				
			return;
		}
		
		File logFile = null;		
		try {
			logFile = getRecoveryLogFile();
			out = new FileWriter(logFile);
			index = 0;
			paths=null;
			open=true;
			nbOfOpen=0;
			UpdateManagerPlugin.warn("Start new Error/Recovery log #"+nbOfOpen+":"+logEntry);							
		} catch (IOException e) {
			throw Utilities.newCoreException(
				Policy.bind("UpdateManagerUtils.UnableToLog", new Object[] { logFile }),
				e);
		}
		
		append(logEntry);
	}

	/**
	 * Append the string to the log and flush
	 */
	public void append(String logEntry) throws CoreException {
		File logFile = null;
		try {
			if (!open) {
				UpdateManagerPlugin.warn("Internal Error: The Error/Recovery log is not open:"+logEntry);				
				return;
			}

			StringBuffer buffer = new StringBuffer(LOG_ENTRY_KEY);
			buffer.append(index);
			buffer.append("=");
			buffer.append(logEntry);
			buffer.append(RETURN_CARRIAGE);

			out.write(buffer.toString());
			out.flush();
			index++;
		} catch (IOException e) {
			throw Utilities.newCoreException(
				Policy.bind("UpdateManagerUtils.UnableToLog", new Object[] { logFile }),
				e);
		}
	}

	/**
	 * Append the string to the log and flush
	 */
	public void appendPath(String logEntry, String path) throws CoreException {
		if (path == null)
			return;
		StringBuffer buffer = new StringBuffer(logEntry);
		buffer.append(" ");
		buffer.append(path);
		append(buffer.toString());
		
		addPath(path);
	}

	/**
	 * Close any open recovery log
	 */
	public void close(String logEntry) throws CoreException {
		
		if (nbOfOpen>0){
			UpdateManagerPlugin.warn("Close nested Error/Recovery log #"+nbOfOpen+":"+logEntry);			
			nbOfOpen--;			
			return;
		}			
		
		UpdateManagerPlugin.warn("Close Error/Recovery log #"+nbOfOpen+":"+logEntry);
		append(logEntry);
		if (out != null) {
			try {
				out.write(END_OF_FILE);
				out.flush();
				out.close();
			} catch (Exception e) { //eat the exception
			} finally {
				out = null;
				open=false;
			}
		}
	}

	/**
	 * Delete the file from the file system
	 */
	public void delete() {
		File logFile = getRecoveryLogFile();
		//if (logFile.exists())
			//logFile.delete();	
	}

	/**
	 * 
	 */
	private void addPath(String path){
		if (paths==null) paths = new ArrayList();
		paths.add(path);
	}
	
	/** 
	 * recover an install or remove that didn't finish
	 * Delete file for an unfinished delete
	 * Delete file for an unfinshed install if not all the files were installed
	 * Rename XML files for an install if all the files were installed but not renamed
	 */
	public IStatus recover(){
		
		IStatus mainStatus = createStatus(IStatus.OK,"Recovering status",null);
		MultiStatus multi = new MultiStatus(mainStatus.getPlugin(),mainStatus.getCode(),mainStatus.getMessage(),null);

		//check if recovery is on
		if (!RECOVERY_ON){
			UpdateManagerPlugin.warn("Recovering is turned off. Abort recovery");
			return multi;
		}
		
		File logFile = getRecoveryLogFile();
		if (!logFile.exists()){
			multi.add(createStatus(IStatus.ERROR,"Unable to find log file:"+logFile,null));
			return multi;
		}
		
		InputStream in = null;
		Properties prop = null;
		try {
			in = new FileInputStream(logFile);
			prop = new Properties();
			prop.load(in);
		} catch (IOException e){
			UpdateManagerPlugin.warn("Unable to read:"+logFile,e);
			multi.add(createStatus(IStatus.ERROR,"Unable to access property file:"+logFile,e));
			return multi;
		}
		
		String eof = prop.getProperty("eof");
		if(eof!=null && eof.equals("eof")){
			// all is good
			delete();
			UpdateManagerPlugin.warn("Found log file. Log file contains end-of-file. No need to process");
			multi.add(createStatus(IStatus.OK,null,null));
			return multi;
		}
		
		String recovery = prop.getProperty(LOG_ENTRY_KEY+"0");
		if (recovery==null){
			multi.add(createStatus(IStatus.ERROR,"Unable to read log file. File doesn't contain the log entry:"+logFile,null));
			return multi;			
		}
	
		if(recovery.equalsIgnoreCase(START_INSTALL_LOG)){
			multi.addAll(processRecoverInstall(prop));
			return multi;
		}
		
		if(recovery.equalsIgnoreCase(START_REMOVE_LOG)){
			multi.addAll(processRecoverRemove(prop));
			return multi;
		}

		multi.add(createStatus(IStatus.ERROR,"Unable to read log file. Unable to determine recovery to execute based on the first line of :"+logFile,null));
		return multi;	
	}
	
	/*
	 * creates a Status
	 */
	private IStatus createStatus(int statusSeverity, String msg, Exception e){
		String id =
			UpdateManagerPlugin.getPlugin().getDescriptor().getUniqueIdentifier();
	
		StringBuffer completeString = new StringBuffer("");
		if (msg!=null)
			completeString.append(msg);
		if (e!=null){
			completeString.append("\r\n[");
			completeString.append(e.toString());
			completeString.append("]\r\n");
		}
		return new Status(statusSeverity, id, IStatus.OK, completeString.toString(), e);
	}	
	
	/*
	 * 
	 */
	 private IStatus processRecoverInstall(Properties prop){
	 	
		IStatus mainStatus = createStatus(IStatus.OK,"",null);
		MultiStatus multi = new MultiStatus(mainStatus.getPlugin(),mainStatus.getCode(),"",null);
	 	
	 	Collection values = prop.values();
	 	
	 	if(values.contains(END_INSTALL_LOG)){
			// all is good
			delete();
			UpdateManagerPlugin.warn("Found log file. Log file contains END_INSTALL_LOG. No need to process rename");
			multi.add(createStatus(IStatus.OK,null,null));
			return multi;
	 	}
	 	
	 	if (values.contains(ALL_INSTALLED) && !forceRemove){
	 		// finish install by renaming
	 		int index = 0;
	 		boolean found = false;
	 		String val = prop.getProperty(LOG_ENTRY_KEY+index);
	 		while(val!=null && !found){
	 			if(val.equalsIgnoreCase(ALL_INSTALLED)) found = true;
	 			IStatus renameStatus = processRename(val);
	 			UpdateManagerPlugin.log(renameStatus);
	 			if(renameStatus.getSeverity()!=IStatus.OK){
	 				multi.add(renameStatus);
	 			}
	 			index++;
	 			val = prop.getProperty(LOG_ENTRY_KEY+index);	 			
	 		}
	 		if (val==null){
	 			UpdateManagerPlugin.warn("Unable to find value for :"+LOG_ENTRY_KEY+index);
	 			multi.add(createStatus(IStatus.ERROR,"Wrong log file. Unable to find entry for:"+LOG_ENTRY_KEY+index,null));
				return multi;
	 		}
	 		// process recovery finished
	 		delete();
			UpdateManagerPlugin.warn("Found log file. Successfully recovered by renaming. Feature is installed.");
			multi.add(createStatus(IStatus.OK,null,null));
	 	} else {
	 		// remove all because install did not lay out all the files
	 		// or recovery is not allowed
	 		int index = 0;
	 		String val = prop.getProperty(LOG_ENTRY_KEY+index);
	 		while(val!=null){
	 			IStatus removeStatus = processRemove(val);
	 			UpdateManagerPlugin.log(removeStatus);
	 			if(removeStatus.getSeverity()!=IStatus.OK){
	 				multi.addAll(removeStatus);
	 			}
	 			index++;
	 			val = prop.getProperty(LOG_ENTRY_KEY+index);	 			
	 		}
	 		// process recovery finished
	 		delete();
			UpdateManagerPlugin.warn("Found log file. Successfully recovered by removing. Feature is removed.");
			multi.add(createStatus(IStatus.OK,null,null));
	 	}
	 	return multi;
	 }
	 
	 /*
	  * 
	  */
	  private IStatus processRename(String val){
	  	
		// get the path
		int index = -1;
		String newFileName = null;
	  	if (val.startsWith(PLUGIN_ENTRY)){
	  		index = PLUGIN_ENTRY.length();
	  		newFileName= "plugin.xml";
	  	}
	  	if (val.startsWith(FRAGMENT_ENTRY)){
	  		index = FRAGMENT_ENTRY.length();
	  		newFileName= "fragment.xml";
	  	}
	  	if (val.startsWith(FEATURE_ENTRY)){
	  		index = FEATURE_ENTRY.length();
	  		newFileName= "feature.xml";
	  	}
	  	
	  	if (index==-1){
	  		return createStatus(IStatus.ERROR,"Unable to determine what action was taken by parsing"+val,null);
	  	}
	  	
	  	String oldName = val.substring(index+1);
	  	File oldFile = new File(oldName);
	  	File parentFile = oldFile.getParentFile();
	  	File newFile = new File(parentFile,newFileName);
	  	if (!oldFile.exists()){
	  		if (newFile.exists()){
	  			// ok the file has been renamed apparently
			  	return createStatus(IStatus.OK,"File already renamed into:"+newFile,null);	  				
	  		} else {
	  			// the file doesn't exist, log as problem, and force the removal of the feature
		  		return createStatus(IStatus.ERROR,"Unable to find file:"+oldFile,null);	  			
	  		}
	  	} 	
	  	
		boolean sucess = false;
		if (newFile.exists()) {
			UpdateManagerUtils.removeFromFileSystem(newFile);
			UpdateManagerPlugin.warn("Removing already existing file:"+newFile);
		}
		sucess = oldFile.renameTo(newFile);
			
		if(!sucess){
			String msg =("Unable to rename old in new:"+oldFile+newFile);
			return createStatus(IStatus.ERROR,msg,null);
		}
		return createStatus(IStatus.OK,"Sucessfully renamed:"+oldFile+" to:"+newFile,null);
	  }
	  
	 /*
	  * 
	  */
	  private IStatus processRemove(String val){
	  	
		IStatus mainStatus = createStatus(IStatus.OK,"",null);
		MultiStatus multi = new MultiStatus(mainStatus.getPlugin(),mainStatus.getCode(),"",null);	  	
	  	
		// get the path
		int index = -1;
	  	if (val.startsWith(PLUGIN_ENTRY)){
	  		index = PLUGIN_ENTRY.length();
	  	}
	  	if (val.startsWith(FRAGMENT_ENTRY)){
	  		index = FRAGMENT_ENTRY.length();
	  	}
	  	if (val.startsWith(FEATURE_ENTRY)){
	  		index = FEATURE_ENTRY.length();
	  	}
	  	
	  	if (index==-1){
	  		return createStatus(IStatus.ERROR,"Unable to determine what action was taken by parsing"+val,null);
	  	}
	  	
	  	String oldName = val.substring(index+1);
	  	File oldFile = new File(oldName);
	  	File parentFile = oldFile.getParentFile();
	  	if (!parentFile.exists()){
  			// the directory doesn't exist, log as problem, and force the removal of the feature
	  		multi.add(createStatus(IStatus.ERROR,"Unable to find file:"+oldFile,null));	  			
	  		return multi;
	  	} 	
	  	
		multi.addAll(removeFromFileSystem(parentFile));
		return multi;
	  }	
	  
	/**
	 * return a multi status, 
	 * the children are the file that couldn't be removed
	 */
	public IStatus removeFromFileSystem(File file) {
		
		IStatus mainStatus = createStatus(IStatus.OK,"",null);
		MultiStatus multi = new MultiStatus(mainStatus.getPlugin(),mainStatus.getCode(),"",null);		
		
		if (!file.exists()){
			multi.add(createStatus(IStatus.ERROR,"Unable to find file to remove:"+file,null));
			return multi;
		}
			
		if (file.isDirectory()) {
			String[] files = file.list();
			if (files != null) // be careful since file.list() can return null
				for (int i = 0; i < files.length; ++i){
					multi.addAll(removeFromFileSystem(new File(file, files[i])));
				}
		}
		
		if (!file.delete()) {
			String msg = "Unable to remove file" +file.getAbsolutePath();
			//$NON-NLS-1$ 
			multi.add(createStatus(IStatus.ERROR,msg,null));
		}
		return multi;
	}	
	
	/*
	 * 
	 */
	 private IStatus processRecoverRemove(Properties prop){
	 	
		IStatus mainStatus = createStatus(IStatus.OK,"",null);
		MultiStatus multi = new MultiStatus(mainStatus.getPlugin(),mainStatus.getCode(),"",null);
	 	
	 	Collection values = prop.values();
	 	
	 	if(values.contains(END_REMOVE_LOG)){
			// all is good
			delete();
			UpdateManagerPlugin.warn("Found log file. Log file contains END_REMOVE_LOG. No need to process rename");
			multi.add(createStatus(IStatus.OK,null,null));
			return multi;
	 	}
	 	
	 	if (!values.contains(END_ABOUT_REMOVE)){
	 		// finish install by renaming
 			multi.add(createStatus(IStatus.ERROR,"The remove process didn't start. Please remove the disable feature from the program.",null));
				return multi;
	 	} else {
	 		// finish install by renaming
	 		int index = 0;
	 		boolean found = false;
	 		String val = prop.getProperty(LOG_ENTRY_KEY+index);
	 		while(val!=null && !found){
	 			if(val.equalsIgnoreCase(END_ABOUT_REMOVE)) found = true;
	 			IStatus renameStatus = processRemove(val);
	 			UpdateManagerPlugin.log(renameStatus);
	 			if(renameStatus.getSeverity()!=IStatus.OK){
	 				multi.add(renameStatus);
	 			}
	 			index++;
	 			val = prop.getProperty(LOG_ENTRY_KEY+index);	 			
	 		}
	 		if (val==null){
	 			UpdateManagerPlugin.warn("Unable to find value for :"+LOG_ENTRY_KEY+index);
	 			multi.add(createStatus(IStatus.ERROR,"Wrong log file. Unable to find entry for:"+LOG_ENTRY_KEY+index,null));
				return multi;
	 		}
	 		// process recovery finished
	 		delete();
			UpdateManagerPlugin.warn("Found log file. Successfully recovered by deleting. Feature is removed.");
			multi.add(createStatus(IStatus.OK,null,null));
	 	}
	 	return multi;
	 }	    
}