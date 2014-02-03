/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.core;

import java.io.*;
import java.net.URL;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.update.configurator.ConfiguratorUtils;
import org.eclipse.update.configurator.IPlatformConfiguration;
import org.eclipse.update.core.Utilities;


/**
 * Manages the error/recover log file
 */
public class ErrorRecoveryLog {
	private static final String ERROR_RECOVERY_LOG = "error_recovery.log"; //$NON-NLS-1$
	private static final String LOG_ENTRY_KEY = "LogEntry."; //$NON-NLS-1$
	private static final String RETURN_CARRIAGE = "\r\n"; //$NON-NLS-1$
	private static final String END_OF_FILE = "eof=eof"; //$NON-NLS-1$

	//
	public static final String START_INSTALL_LOG = 	"START_INSTALL_LOG"; //$NON-NLS-1$
	public static final String PLUGIN_ENTRY = 		"PLUGIN"; //$NON-NLS-1$
	public static final String FRAGMENT_ENTRY = 		"FRAGMENT";	 //$NON-NLS-1$
	public static final String BUNDLE_MANIFEST_ENTRY = 		"BUNDLE_MANIFEST";	 //$NON-NLS-1$
	public static final String BUNDLE_JAR_ENTRY = 			"BUNDLE";	 //$NON-NLS-1$
	public static final String FEATURE_ENTRY = 		"FEATURE"; //$NON-NLS-1$2
	public static final String ALL_INSTALLED = 		"ALL_FEATURES_INSTALLED"; //$NON-NLS-1$
	public static final String RENAME_ENTRY = 		"RENAME"; //$NON-NLS-1$
	public static final String END_INSTALL_LOG = 	"END_INSTALL_LOG"; //$NON-NLS-1$
	public static final String START_REMOVE_LOG = 	"REMOVE_LOG"; //$NON-NLS-1$
	public static final String END_ABOUT_REMOVE =	"END_ABOUT_TO_REMOVE"; //$NON-NLS-1$
	public static final String DELETE_ENTRY = 		"DELETE"; //$NON-NLS-1$
	public static final String END_REMOVE_LOG = 		"END_REMOVE_LOG"; //$NON-NLS-1$

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
		if (path.endsWith(File.separator) || path.endsWith("/")) //$NON-NLS-1$
			return path;
		File file = new File(path);
		String newName =
			UpdateManagerUtils.getLocalRandomIdentifier(file.getName(), new Date());
		while (new File(file.getParentFile(), newName).exists()) {
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
			ConfiguratorUtils.getCurrentPlatformConfiguration();
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
			UpdateCore.warn("Open nested Error/Recovery log #"+nbOfOpen+":"+logEntry);				 //$NON-NLS-1$ //$NON-NLS-2$
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
			UpdateCore.warn("Start new Error/Recovery log #"+nbOfOpen+":"+logEntry);							 //$NON-NLS-1$ //$NON-NLS-2$
		} catch (IOException e) {
			throw Utilities.newCoreException(
				NLS.bind(Messages.UpdateManagerUtils_UnableToLog, (new Object[] { logFile })),
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
				UpdateCore.warn("Internal Error: The Error/Recovery log is not open:"+logEntry);				 //$NON-NLS-1$
				return;
			}

			StringBuffer buffer = new StringBuffer(LOG_ENTRY_KEY);
			buffer.append(index);
			buffer.append("="); //$NON-NLS-1$
			buffer.append(logEntry);
			buffer.append(RETURN_CARRIAGE);

			out.write(buffer.toString());
			out.flush();
			index++;
		} catch (IOException e) {
			throw Utilities.newCoreException(
				NLS.bind(Messages.UpdateManagerUtils_UnableToLog, (new Object[] { logFile })),
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
		buffer.append(" "); //$NON-NLS-1$
		buffer.append(path);
		append(buffer.toString());
		
		addPath(path);
	}

	/**
	 * Close any open recovery log
	 */
	public void close(String logEntry) throws CoreException {
		
		if (nbOfOpen>0){
			UpdateCore.warn("Close nested Error/Recovery log #"+nbOfOpen+":"+logEntry);			 //$NON-NLS-1$ //$NON-NLS-2$
			nbOfOpen--;			
			return;
		}			
		
		UpdateCore.warn("Close Error/Recovery log #"+nbOfOpen+":"+logEntry); //$NON-NLS-1$ //$NON-NLS-2$
		append(logEntry);
		if (out != null) {
			try {
				out.write(END_OF_FILE);
				out.flush();
				out.close();
			} catch (IOException e) { //eat the exception
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
		//File logFile = getRecoveryLogFile();
		getRecoveryLogFile();
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
	
	/*
	 * creates a Status
	 */
	private IStatus createStatus(int statusSeverity, String msg, Exception e){
		String id =
			UpdateCore.getPlugin().getBundle().getSymbolicName();
	
		StringBuffer completeString = new StringBuffer(""); //$NON-NLS-1$
		if (msg!=null)
			completeString.append(msg);
		if (e!=null){
			completeString.append("\r\n["); //$NON-NLS-1$
			completeString.append(e.toString());
			completeString.append("]\r\n"); //$NON-NLS-1$
		}
		return new Status(statusSeverity, id, IStatus.OK, completeString.toString(), e);
	}	
	
	/**
	 * return a multi status, 
	 * the children are the file that couldn't be removed
	 */
	public IStatus removeFromFileSystem(File file) {
		
		IStatus mainStatus = createStatus(IStatus.OK,"",null); //$NON-NLS-1$
		MultiStatus multi = new MultiStatus(mainStatus.getPlugin(),mainStatus.getCode(),"",null);		 //$NON-NLS-1$
		
		if (!file.exists()){
			multi.add(createStatus(IStatus.ERROR,Messages.ErrorRecoveryLog_noFiletoRemove+file,null)); 
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
			String msg = "Unable to remove file" +file.getAbsolutePath(); //$NON-NLS-1$ 
			multi.add(createStatus(IStatus.ERROR,msg,null));
		}
		return multi;
	}	
	
}
