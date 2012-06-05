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

package org.eclipse.update.tests.standalone;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import org.eclipse.update.configuration.IConfiguredSite;
import org.eclipse.update.configuration.IInstallConfiguration;
import org.eclipse.update.configuration.ILocalSite;
import org.eclipse.update.configurator.ConfiguratorUtils;
import org.eclipse.update.core.ISite;
import org.eclipse.update.core.SiteManager;
import org.eclipse.update.internal.core.InternalSiteManager;
import org.eclipse.update.internal.core.UpdateCore;
import org.eclipse.update.tests.UpdateManagerTestCase;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;

public class StandaloneManagerTestCase extends UpdateManagerTestCase {
	public static StringBuffer errOutput;
	public static Integer exitValue= new Integer(-1);
	public static URL TARGET_FILE_SITE;
	private boolean oldCache = false;
	
	static {
		File targetDir = new File(System.getProperty("java.io.tmpdir"));
		targetDir = new File(targetDir, "standalone");
		targetDir = new File(targetDir, "mytarget");
		if (targetDir.exists())
			deleteDirectory(targetDir);
		try {
			TARGET_FILE_SITE = targetDir.toURL();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}
	
	public StandaloneManagerTestCase (String arg0){
		super(arg0);
	}
	
	/**
	 * Sets up the fixture, for example, open a network connection.
	 * This method is called before a test is executed.
	 */
	protected void umSetUp() {
		// setup cache site to true
		oldCache = InternalSiteManager.globalUseCache;
		InternalSiteManager.globalUseCache = true;
	}
	
	/**
	 * Tears down the fixture, for example, close a network connection.
	 * This method is called after a test is executed.
	 */
	protected void umTearDown() {
		// do nothing.
		InternalSiteManager.globalUseCache = oldCache;
	}
	
	public void checkConfiguredSites(){
		ILocalSite localSite;
		System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
		try {
			localSite = SiteManager.getLocalSite();
			System.out.println("LocalSite: " + localSite.getCurrentConfiguration().getLabel());
		
		//Get site to install to
		IInstallConfiguration currentConfig = localSite.getCurrentConfiguration();
		IConfiguredSite[] sites = currentConfig.getConfiguredSites();
//	
//			// start of config site print
		System.out.println("GETTING CONFIGURED SITES...");
		for (int i = 0; i<sites.length; i++){
			System.out.println("site #" + i + ": " + sites[i].getSite().getURL().getFile());
		}
		System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
		} catch (CoreException e) {
			System.err.println(e);
		}
	}
	
	// get the first configured site found because this is the one we're most likely to 
	// have installed into
	public ISite getConfiguredSite(URL target){
		try {
			ILocalSite local = SiteManager.getLocalSite();
			IInstallConfiguration currentConfig = local.getCurrentConfiguration();
			IConfiguredSite[] sites = currentConfig.getConfiguredSites();
			System.out.println("\nretrieving configured sites...");
			String targetFile = new File(target.getFile()).getAbsolutePath();
			for (int i = 0; i<sites.length ; i++){
				System.out.println("site["+i+"]: " + sites[i].getSite().getURL());
				String siteFile = new File(sites[i].getSite().getURL().getFile()).getAbsolutePath();
				if (targetFile.equals(siteFile))
					return sites[i].getSite();
			}
			if (sites.length == 0)
				return null;
			return sites[0].getSite();
		} catch (CoreException e) {
			System.err.println(e);
			return null;
		}
	}
	//WatchDog thread to kill mirroring process if it hangs (or takes too long)
	public static class Timer extends Thread{
		private Process proc;
		
		public Timer(Process proc){
			super();
			this.setDaemon(true);
			this.proc = proc;
		}
		
		public void run(){
			try {
				// sleep time 5min
				sleep(300000); 
				System.out.println("destroying process");
				proc.destroy();
			} catch (Exception e) {
				System.err.println(e);
				e.printStackTrace();
			}
		}
	}
	
	// StreamConsumer to display output to output files or console
	public static class StreamConsumer extends Thread {
		private BufferedReader bReader;

		public StreamConsumer(InputStream inputStream) {
			super();
			setDaemon(true);
			bReader = new BufferedReader(new InputStreamReader(inputStream));
			errOutput = new StringBuffer();
		}

		public void run() {
			try {
				String line;
				while (null != (line = bReader.readLine())) {
					System.err.println(line);
					errOutput.append(line);
					errOutput.append("\n");
				}
			} catch (IOException e) {
				System.err.println(e);
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * @param command : command argument to run on site/feature 
	 * (i.e. 'install', 'enable', 'disable', 'update') *<i>required</i>*
	 * @param fid : feature id of the feature being installed, enabled, etc.
	 * (optional - can be null)
	 * @param ver : version of feature being installed, enabled, etc. (optional - can be null)
	 * @param config : location of configuration info (i.e. file:D:\temp\.config\)
	 * @param remoteLoc : remote site url (required for installing/searching from remote site)
	 * @param localLoc : target site directory (required unless updating/searching)
	 * @return a string array consisting of commands and their arguments to
	 * be run
	 */
	public String[] getCommand(
		String command,
		String fid,
		String ver,
		String config,
		String remoteLoc,
		String localLoc){
		final String classpath = "startup.jar";
		final String launcher = "org.eclipse.core.launcher.Main";
		final String application = "org.eclipse.update.core.standaloneUpdate";
		final String FLAG_CP = "-cp";
		final String FLAG_APP = "-application";
		final String FLAG_CMD = "-command";
		final String FLAG_FID = "-featureId";
		final String FLAG_DATA = "-data";
		final String FLAG_VERSION = "-version";
		final String FLAG_FROM = "-from";
		final String FLAG_TO = "-to";
		final String FLAG_CONFIG = "-configuration";
//		final String FLAG_URL = "-mirrorURL";
		final String FLAG_SPLASH = "-nosplash";
		final String FLAG_CONSOLELOG = "-consolelog";
		final String data =
			UpdateCore.getPlugin().getStateLocation().toOSString();
		String[] cmd =
			new String[] {
				getJavaVMPath(),
				FLAG_CP,
				classpath,
				launcher,
				FLAG_APP,
				application,
				FLAG_CMD,
				command,
				(remoteLoc !=null ? FLAG_FROM : ""),
				(remoteLoc !=null ? remoteLoc : ""),
				(localLoc !=null ? FLAG_TO : ""),
				(localLoc !=null ?localLoc.replaceFirst("file:", "") : ""),
				(fid != null ? FLAG_FID : ""),
				(fid != null ? fid : ""),
				(ver != null ? FLAG_VERSION : ""),
				(ver != null ? ver : ""),
				(config != null ? FLAG_CONFIG : ""),
				(config != null ? config : ""),
				FLAG_SPLASH,
				FLAG_DATA,
				data,
				FLAG_CONSOLELOG};
		for (int i = 0 ;i<cmd.length; i++){
			System.out.print(cmd[i] + " ");			
		}System.out.println();
		return cmd;
	}
	
	public String getJavaVMPath() {
		// Create command string for launching the process
		String vm = System.getProperty("java.vm.name");
		String executable = "J9".equals(vm) ? "j9" : "java";
		if (org.eclipse.osgi.service.environment.Constants.OS_WIN32.equals(Platform.getOS()))
			executable += "w.exe";

		String javaInstallDir =
			System.getProperty("java.home") + File.separator + "bin";
		return javaInstallDir + File.separator + executable;
	}
	
	public int performAction(String[] cmd) {
		File file = new File(getEclipseRoot());
		try {

			Process proc =
				Runtime.getRuntime().exec(cmd, (String[]) null, file);

			StreamConsumer outputs = new StreamConsumer(proc.getInputStream());
			outputs.start();
			StreamConsumer errors = new StreamConsumer(proc.getErrorStream());
			errors.start();
			Timer timer = new Timer(proc);
			timer.start();

			return proc.waitFor();
		} catch (IOException e) {
			System.err.println(e);
		} catch (InterruptedException e) {
			System.err.println(e);
		} catch (Exception e) {
			System.err.println(e);
		}
		return -1;
	}
	
	public String getEclipseRoot() {
		return ConfiguratorUtils.getInstallURL().toExternalForm().replaceFirst(
			"file:",
			"");
	}
	
	public ArrayList getArrayList(String[] list){
		ArrayList temp = new ArrayList();
		for(int i = 0; i< list.length; i++){
			temp.add(list[i]);
		}
		return temp;
	}
	
	public File getLatestConfigurationFile(File localFile) {
		BufferedReader breader = null;
		try {
			System.out.println("reading from : " + localFile.getAbsolutePath());
			breader = new BufferedReader(new FileReader(localFile));
			while (breader.ready()) {
				breader.readLine();
//					System.out.println(line);
			}
			// read config file
			File parent = new File(localFile.getParent());
			System.out.println("parent: " + parent.getAbsolutePath());
			String[] parList= parent.list();
			int latest= 0;
			for (int i = parList.length - 1; i >= 0; i--) {
				System.out.println("parList[" + i + "]: " + parList[i]);
				if (parList[i].startsWith("platform")) {
					latest= i;
					break;
				}
			}
			File configFile = new File(parent.getAbsolutePath() + File.separator + parList[latest]);
			return configFile;
			// end of config file read
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (breader != null) {
				try {
					breader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return localFile;
	}
	
	private static void deleteDirectory(File dir) {
		File[] list = dir.listFiles();
		if (list == null)
			return;
			
		for (int i=0; i<list.length; i++) {
			if (list[i].isDirectory()) 
				deleteDirectory(list[i]);
			if (!list[i].delete())
				System.out.println("Unable to delete "+list[i].toString());
		}
	}
}
