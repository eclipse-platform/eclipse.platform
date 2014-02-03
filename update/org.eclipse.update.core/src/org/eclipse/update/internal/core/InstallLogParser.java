/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
import java.net.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.*;
import org.eclipse.update.configuration.*;
import org.eclipse.update.configurator.*;
import org.eclipse.update.core.*;
import org.eclipse.update.internal.operations.*;


/**
 * Parses the installation log and creates installation configuration objects
 */
public class InstallLogParser {
	private IPath logPath;
	private BufferedReader buffRead;
	private InstallConfiguration currentConfiguration;
	private HashMap installConfigMap;
	private Comparator comparator;
	
	private static final String FEATURE_INSTALL = "feature-install"; //$NON-NLS-1$
	private static final String FEATURE_REMOVE = "feature-remove"; //$NON-NLS-1$
	private static final String SITE_INSTALL = "site-install"; //$NON-NLS-1$
	private static final String SITE_REMOVE = "site-remove"; //$NON-NLS-1$
	private static final String UNCONFIGURE = "feature-disable"; //$NON-NLS-1$
	private static final String CONFIGURE = "feature-enable"; //$NON-NLS-1$
	private static final String REVERT = "revert"; //$NON-NLS-1$
	private static final String RECONCILIATION = "reconciliation"; //$NON-NLS-1$
	private static final String PRESERVED = "preserve-configuration"; //$NON-NLS-1$	
	
	private static final String ACTIVITY = "!ACTIVITY"; //$NON-NLS-1$
	
	public static final String SUCCESS = "success"; //$NON-NLS-1$
	public static final String FAILURE = "failure"; //$NON-NLS-1$

	
	public InstallLogParser(){
		String loc = ConfiguratorUtils.getCurrentPlatformConfiguration().getConfigurationLocation().getFile();
		logPath = new Path(loc).removeLastSegments(1).append("install.log");  //$NON-NLS-1$
		installConfigMap = new HashMap();
		try {
			InstallConfiguration[] configs = (InstallConfiguration[])SiteManager.getLocalSite().getConfigurationHistory();
			for (int i=0;i<configs.length; i++){
				if (!configs[i].isCurrent())
					installConfigMap.put(new Long(configs[i].getCreationDate().getTime()), configs[i]);
			}
			// Need to make a copy of the current config instead
			InstallConfiguration config = getConfigCopy((InstallConfiguration)SiteManager.getLocalSite().getCurrentConfiguration());
			installConfigMap.put(new Long(config.getCreationDate().getTime()), config);
			
		} catch (CoreException e) {
			UpdateCore.log(e);
		} catch (MalformedURLException e){
			UpdateCore.log(e);
		}
		comparator = new Comparator(){
			public int compare(Object e1, Object e2) {
				Date date1 = ((InstallConfiguration)e1).getCreationDate();
				Date date2 = ((InstallConfiguration)e2).getCreationDate();
				return date1.before(date2) ? 1 : -1;
			}
		};
	}
	private InstallConfiguration getConfigCopy(InstallConfiguration origConfig) throws CoreException, MalformedURLException{
		InstallConfiguration config = new InstallConfiguration(origConfig, origConfig.getURL(), origConfig.getLabel() );
		config.setCreationDate(origConfig.getCreationDate());
		return config;
	}
	public void parseInstallationLog(){
		try {
			openLog();
			parseLog();
		} catch (CoreException e) {
			UpdateUtils.logException(e);
		} finally {
			closeLog();
		}
	}
	
	private void openLog() throws CoreException {
		try {
		    // throws FileNotFoundException, IOException
		    InputStream is = new FileInputStream(logPath.toOSString());
		    // throws UnsupportedEncodingException
		    InputStreamReader isr = new InputStreamReader(is,"UTF-8"); //$NON-NLS-1$
		    buffRead = new BufferedReader(isr);
		} catch (Exception e) {
			throwCoreException(e);
		}
	}
	
	private void throwCoreException(Throwable e) throws CoreException {
		throw new CoreException(
			new Status(
				IStatus.ERROR,
				UpdateUtils.getPluginId(),
				IStatus.ERROR,
				Messages.InstallLogParser_errors, 
				e));
	}
	
	private void parseLog() throws CoreException {
		// 		.install-log template
		//		!CONFIGURATION <configuration-date>
		//		!ACTIVITY <date> <target> <action> <status>

		try {
			String type, status, action;
			StringTokenizer htmlCode;

			while (buffRead.ready()) {

				htmlCode = new StringTokenizer(buffRead.readLine());
				while (!(htmlCode.hasMoreElements())) {
					if (!buffRead.ready())
						return;
					htmlCode = new StringTokenizer(buffRead.readLine());
				}

				type = htmlCode.nextToken().trim();

				if (type.equals(ACTIVITY)) {
					String time = htmlCode.nextToken();
					String date;
					StringBuffer target = new StringBuffer();
					date = htmlCode.nextToken(".");  //$NON-NLS-1$
					htmlCode.nextToken(" ");  //$NON-NLS-1$
					while (htmlCode.countTokens() > 2){
						target.append(" "); //$NON-NLS-1$
						target.append(htmlCode.nextToken());
					}
					
					action = htmlCode.nextToken();
					status = htmlCode.nextToken();
					createActivity(action, time, date, status, target.toString(), currentConfiguration);
				}  else {
					String time = htmlCode.nextToken();
					StringBuffer date;
					date = new StringBuffer();
					while (htmlCode.countTokens() > 0){
						if (date.length() != 0)
							date.append(" "); //$NON-NLS-1$
						date.append(htmlCode.nextToken());
					}
					currentConfiguration = (InstallConfiguration)installConfigMap.get(new Long(time));
				}
			}
		} catch (Exception e) {
			throwCoreException(e);
		}
	}
	
	private void closeLog() {
		try {
			if (buffRead != null)
				buffRead.close();
		} catch (IOException e) {
		} finally {
			buffRead = null;
		}
	}
	private IActivity createActivity(String action, String time, String date, String status, String target, InstallConfiguration config){
		ConfigurationActivity a = new ConfigurationActivity();

		int code = 0;
		if (FEATURE_INSTALL.equals(action))
			code = IActivity.ACTION_FEATURE_INSTALL;
		else if (FEATURE_REMOVE.equals(action))
			code = IActivity.ACTION_FEATURE_REMOVE;
		else if (SITE_INSTALL.equals(action))
			code = IActivity.ACTION_SITE_INSTALL;
		else if (SITE_REMOVE.equals(action))
			code = IActivity.ACTION_SITE_REMOVE;
		else if (UNCONFIGURE.equals(action))
			code = IActivity.ACTION_UNCONFIGURE;
		else if (CONFIGURE.equals(action))
			code = IActivity.ACTION_CONFIGURE;
		else if (REVERT.equals(action))
			code = IActivity.ACTION_REVERT;
		else if (RECONCILIATION.equals(action))
			code = IActivity.ACTION_RECONCILIATION;
		else if (PRESERVED.equals(action))
			code = IActivity.ACTION_ADD_PRESERVED;
		
		a.setAction(code);
		try {
			long activityTime = Long.parseLong(time);
			a.setDate(new Date(activityTime));
		} catch (NumberFormatException e) {
			//PAL foundation
			//a.setDate(new Date(date));
			try {
				a.setDate(new SimpleDateFormat().parse(date));
			} catch (ParseException e1) {
				//ignore
			}
		}
		a.setStatus(SUCCESS.equals(status) ? IActivity.STATUS_OK : IActivity.STATUS_NOK);
		a.setLabel(target);
		a.setInstallConfigurationModel(config);
		
		if (config != null && !configContainsActivity(config, a)){
			config.addActivity(a);
		}
		
		return a;
	}
	
	private boolean configContainsActivity(InstallConfiguration c, IActivity a){
		IActivity[] activities = c.getActivities();
		for (int i = 0 ; i<activities.length; i++){
			if (a.equals(activities[i]))
				return true;
		}
		return false;
	}

	public InstallConfiguration[] getConfigurations(){
		Collection configSet = installConfigMap.values();
		InstallConfiguration[] configs = (InstallConfiguration[]) configSet.toArray(new InstallConfiguration[configSet.size()]);
		Arrays.sort(configs, comparator);
		return configs;
	}
}
