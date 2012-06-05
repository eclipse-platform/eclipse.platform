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

package org.eclipse.update.tests.mirror;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.StringTokenizer;

import org.osgi.framework.Bundle;

import org.eclipse.update.configurator.ConfiguratorUtils;
import org.eclipse.update.internal.core.UpdateCore;
import org.eclipse.update.tests.UpdateManagerTestCase;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;


public class MirrorManagerTestCase extends UpdateManagerTestCase{
	public static int exitValue=-1;
	public static StringBuffer errOutput;
	
	public MirrorManagerTestCase(String arg0){
		super(arg0);
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
				// Note: normal test on 2.2Ghz P4 is 2.9s 
				//:. giving 100x leeway to complete process before killing it
				sleep(300000); 
				proc.destroy();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	// structure to hold category definition names and descriptions
	class CategoryDefinition {
		private String name;
		private String desc;
		public void setName(String name) {
			this.name = name;
		}
		public void setDesc(String desc) {
			this.desc = desc;
		}
		public String getName() {
			return name;
		}
		public String getDesc() {
			return desc;
		}
	}
	
	// structure to hold feature IDs and their category names
	public class FeatureCategory{
		private String featureId;
		private ArrayList category;
		
		public FeatureCategory(){
			category = new ArrayList();
		}
		
		public void setFeatureID(String fid){
			featureId = fid;
		}
		
		public void addCategory(String cat){
			category.add(cat);
		}
		
		public String getFeatureID(){
			return featureId;
		}
		
		public String[] getCategories(){
			return (String[])category.toArray(new String[category.size()]);
		}
	}
	
	// StreamConsumer to display output to output files or console
	public static class StreamConsumer extends Thread {
		private BufferedReader bReader;

		public StreamConsumer(InputStream inputStream) {
			super();
			setDaemon(true);
			bReader = new BufferedReader(new InputStreamReader(inputStream));
		}

		public void run() {
			try {
				String line;
				while (null != (line = bReader.readLine())) {
					System.out.println(line);
					errOutput.append(line);
					errOutput.append("\n");
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Returns a string array consisting of commands and their arguments to
	 * be run.
	 * @param remoteLoc - path of remote site (site being mirrored)
	 * @param toLocalSiteUrl - path of local site (new mirror site)
	 * @param fid - feature id of featuring being mirror (optional - can be null)
	 * @param ver - version of feature to be mirrored (optional - can be null)
	 * @param mirUrl - directory location of policy.xml (optional - can be null)
	 * @return
	 */
	public String[] getCommand(
		String remoteLoc,
		String toLocalSiteUrl,
		String fid,
		String ver,
		String mirUrl) {
		final String classpath = getLauncher();
		final String launcher = "org.eclipse.equinox.launcher.Main";
		final String command = "mirror";
		final String application = "org.eclipse.update.core.standaloneUpdate";
		final String FLAG_CP = "-cp";
		final String FLAG_APP = "-application";
		final String FLAG_CMD = "-command";
		final String FLAG_FID = "-featureId";
		final String FLAG_DATA = "-data";
		final String FLAG_VERSION = "-version";
		final String FLAG_FROM = "-from";
		final String FLAG_TO = "-to";
		final String FLAG_URL = "-mirrorURL";
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
				FLAG_FROM,
				(remoteLoc !=null ? remoteLoc : ""),
				FLAG_TO,
				toLocalSiteUrl,
				(fid != null ? FLAG_FID : ""),
				(fid != null ? fid : ""),
				(ver != null ? FLAG_VERSION : ""),
				(ver != null ? ver : ""),
				(mirUrl != null ? FLAG_URL : ""),
				(mirUrl != null ? mirUrl : ""),
				FLAG_SPLASH,
				FLAG_DATA,
				data,
				FLAG_CONSOLELOG};
		return cmd;
	}
	
	private String getLauncher() {
		Bundle launcher = Platform.getBundle("org.eclipse.equinox.launcher");
		// todo
		if (launcher == null)
			return "";
		URL rootEntry = launcher.getEntry("/");
		try {
			rootEntry = FileLocator.resolve(rootEntry);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		if ("file".equals(rootEntry.getProtocol()))
			return new File(rootEntry.getPath()).getAbsolutePath();
		if ("jar".equals(rootEntry.getProtocol())) {
			String path = rootEntry.getPath();	
			// strip off the file: and the !/
			path = path.substring(5, path.length() - 2);
			return new File(path).getAbsolutePath();
		}
		return null;
	}
	
	public boolean checkFeatureInSiteXMLExists(String url, String fid, String ver){
		File site = new File(url + "/site.xml");
		assertTrue(site.exists());
		BufferedReader breader;
		FileReader freader;
		String text = new String();
		String jarName = fid + "_" + ver + ".jar";
		String feature = "<feature url=\"" +
			"features/" + jarName + 
			"\" id=\"" + fid + 
			"\" version=\"" + ver+"\">";
		try {
			freader = new FileReader(site);
			breader = new BufferedReader(freader);
		} catch (FileNotFoundException e) {
			// would have been caught by assert above
			return false;
		}
		try {
			while ((text = breader.readLine()) != null) {
				text = text.trim();
				if (text.equals(feature)){
					breader.close();
					freader.close();
					return true;
				}
			}

			breader.close();
			freader.close();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	public void checkSiteXML(String url) throws Exception {
		File mirror = new File(url);
		File site = new File(url + "/site.xml");
		assertTrue(mirror.isDirectory());
		assertTrue(mirror.exists());
		assertTrue(site.exists());
	}

	public void checkPolicyXML(String url) throws Exception {
		File mirror = new File(url);
		File site = new File(url + "/policy.xml");
		assertTrue(mirror.isDirectory());
		assertTrue(mirror.exists());
		assertTrue(site.exists());
	}
	public boolean checkPolicyURL(String url, String mirrorUrl){
		File site = new File(url + "/policy.xml");
		assertTrue(site.exists());
		BufferedReader breader;
		FileReader freader;
		String text = new String();

		try {
			freader = new FileReader(site);
			breader = new BufferedReader(freader);
		} catch (FileNotFoundException e) {
			// would have been caught by assert above
			return false;
		}

		try {
			while ((text = breader.readLine()) != null) {
				if (text.indexOf(mirrorUrl)!=-1){
					breader.close();
					freader.close();	
					return true;
				}
			}

			breader.close();
			freader.close();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	public boolean doesCategoryDefinitionExist(String url) {
		File site = new File(url + "/site.xml");
		assertTrue(site.exists());
		BufferedReader breader = null;
		String text = new String();

		try {
			breader = new BufferedReader(new FileReader(site));
			while ((text = breader.readLine()) != null) {
				if (text.indexOf("category-def") != -1)
					return true;
			}
			return false;
		} catch (IOException e) {
			return false;
		} finally {
			if (breader != null) {
				try {
					breader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public String getEclipseRoot() {
		return ConfiguratorUtils.getInstallURL().toExternalForm().replaceFirst(
			"file:",
			"");
	}
	
	public FeatureCategory[] getFeatureCategories(String url){
		File site = new File(url + "/site.xml");
		assertTrue(site.exists());
		BufferedReader breader;
		FileReader freader;
		String text = new String();
		ArrayList featureCategories = new ArrayList();
		
		try {
			freader = new FileReader(site);
			breader = new BufferedReader(freader);
		} catch (FileNotFoundException e) {
			// would have been caught by assert above
			return new FeatureCategory[0];
		}

		try {
			FeatureCategory currTok = new FeatureCategory();
			boolean add = false;
			while((text =breader.readLine())!=null){
				StringTokenizer strTok = new StringTokenizer(text);
				while (strTok.hasMoreTokens()){
					String temp = strTok.nextToken();
					if (temp.equals("</feature>")){
						add = true;
					} else if (temp.startsWith("id")){
						currTok.setFeatureID(temp.split("\"")[1]);
					} else if (temp.startsWith("name")){
						currTok.addCategory(temp.split("\"")[1]);
					}
				}
				if (add){
					featureCategories.add(currTok);
					currTok = new FeatureCategory();
					add = false;
				}
			}

			breader.close();
			freader.close();
			return (FeatureCategory[])featureCategories.toArray(new FeatureCategory[featureCategories.size()]);
		} catch (IOException e) {
			System.err.println(e);
			return new FeatureCategory[0];
		}
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
	
	public CategoryDefinition[] getCategoryDefinitions(String url) {
		File site = new File(url + "/site.xml");
		assertTrue(site.exists());
		BufferedReader breader;
		FileReader freader;
		String text = new String();
		ArrayList catDef = new ArrayList();

		try {
			freader = new FileReader(site);
			breader = new BufferedReader(freader);
		} catch (FileNotFoundException e) {
			// would have been caught by assert above
			return new CategoryDefinition[0];
		}

		try {
			boolean add = false;
			boolean isDesc = false;
			CategoryDefinition temp = new CategoryDefinition();
			while ((text = breader.readLine()) != null) {
				if (text.trim().startsWith("<category-def")
					&& text.indexOf("name") != -1) {
					temp.setName(text.split("\"")[1]);
					add = false;
				} else if (text.trim().equals("<description>")) {
					isDesc = true;
				} else if (text.trim().equals("</description>")) {
					isDesc = false;
				} else if (isDesc) {
					temp.setDesc(text.trim());
				} else if (text.trim().equals("</category-def>")) {
					add = true;
				}
				if (add) {
					catDef.add(temp);
					add = false;
					temp = new CategoryDefinition();
				}
			}

			breader.close();
			freader.close();
			return (CategoryDefinition[])catDef.toArray(new CategoryDefinition[catDef.size()]);
		} catch (IOException e) {
			System.err.println(e);
			return new CategoryDefinition[0];
		}

	}
	
	public int performMirror(String[] cmd_mirror) {
		File file = new File(getEclipseRoot());
		try {
			System.out.println("Launching:");
			for(int i=0; i<cmd_mirror.length; i++){
				System.out.print(cmd_mirror[i]+" ");
			}
			System.out.println();
			Process proc =
				Runtime.getRuntime().exec(cmd_mirror, (String[]) null, file);

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

	// ensure exit without problems
	public void testExitValue() throws Exception {
		assertEquals(0, exitValue);
	}
	
	// ensure output string buffer ends with "Mirror command completed
	// successfully."
	// note: output may instead by "Command completed successfully."
	public void testMirrorSuccess() throws Exception {
		StringTokenizer tokenizer =
			new StringTokenizer(
				errOutput.toString(),
				"\n");
		String lastLine = new String();
		while (tokenizer.hasMoreTokens()){
			lastLine = tokenizer.nextToken();
		}
		if (lastLine.equals("Mirror command completed successfully."))
			return;
		assertEquals("Output was: " + errOutput.toString(), "Command completed successfully.", lastLine);
	}
}
