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
package org.eclipse.ant.internal.ui.preferences;


import java.io.File;
import java.io.FilenameFilter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.ant.core.AntCorePlugin;
import org.eclipse.ant.core.AntCorePreferences;
import org.eclipse.ant.core.IAntClasspathEntry;
import org.eclipse.ant.internal.core.AntClasspathEntry;
import org.eclipse.ant.internal.ui.AntUIPlugin;
import org.eclipse.ant.internal.ui.IAntUIHelpContextIds;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.PlatformUI;

/**
 * Sub-page that allows the user to enter custom classpaths
 * to be used when running Ant build files.
 */
public class AntClasspathPage implements IAntBlockContainer {

	private AntClasspathBlock fAntClasspathBlock= new AntClasspathBlock();
	private AntRuntimePreferencePage fPreferencePage;
	private ClasspathModel fModel;
	
	/**
	 * Creates an instance.
	 */
	public AntClasspathPage(AntRuntimePreferencePage preferencePage) {
		fPreferencePage = preferencePage;
	}
	
	/**
	 * Returns the specified user classpath entries
	 * 
	 * @return set of user classpath entries
	 */
	protected IAntClasspathEntry[] getAdditionalEntries() {
		return fModel.getEntries(ClasspathModel.GLOBAL_USER);
	}
	
	/**
	 * Returns the specified ant home classpath entries
	 */
	protected IAntClasspathEntry[] getAntHomeEntries() {
		return fModel.getEntries(ClasspathModel.ANT_HOME);
	}
	
	/**
	 * Returns the contributed classpath entries
	 */
	protected IAntClasspathEntry[] getContributedEntries() {
		return fModel.getEntries(ClasspathModel.CONTRIBUTED);
	}
	
	protected String getAntHome() {
		return fAntClasspathBlock.getAntHome();
	}
	
	/**
	 * Sets the contents of the tables on this page.
	 */
	protected void initialize() {
		
		AntCorePreferences prefs= AntCorePlugin.getPlugin().getPreferences();
		createClasspathModel();
		fAntClasspathBlock.initializeAntHome(prefs.getAntHome());
		fAntClasspathBlock.setInput(fModel);
		
		fPreferencePage.setErrorMessage(null);
		fPreferencePage.setValid(true);
	}
	
	protected void createClasspathModel() {
		fModel= new ClasspathModel();
		AntCorePreferences prefs= AntCorePlugin.getPlugin().getPreferences();
		fModel.setAntHomeEntries(prefs.getAntHomeClasspathEntries());
		fModel.setGlobalEntries(prefs.getAdditionalClasspathEntries());
        fModel.setContributedEntries(prefs.getContributedClasspathEntries());
	}
	
	protected void performDefaults() {
		AntCorePreferences prefs= AntCorePlugin.getPlugin().getPreferences();
		fModel= new ClasspathModel();
		fModel.setAntHomeEntries(prefs.getDefaultAntHomeEntries());
		List additionalEntries= getDefaultAdditionalEntries();
		if (additionalEntries != null) {
			fModel.setGlobalEntries((IAntClasspathEntry[]) additionalEntries.toArray(new IAntClasspathEntry[additionalEntries.size()]));
		} else {
			fModel.setGlobalEntries(new IAntClasspathEntry[0]);
		}
        fModel.setContributedEntries(prefs.getContributedClasspathEntries());
		fAntClasspathBlock.initializeAntHome(prefs.getDefaultAntHome());
		fAntClasspathBlock.setInput(fModel);
		update();
	}
	
	private List getDefaultAdditionalEntries() {
		IAntClasspathEntry toolsJarEntry= AntCorePlugin.getPlugin().getPreferences().getToolsJarEntry();
		//TODO should use AntCorePreferences.getUserLibraries when promoted to API post 3.1
		File libDir= new File(System.getProperty("user.home"), ".ant" + File.separatorChar + "lib"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		URL[] urls= null;
		try {
			urls= getLocationURLs(libDir);
		} catch (MalformedURLException e) {
            AntUIPlugin.log(e);
            return new ArrayList(0);
		}
		
		List entries= new ArrayList(urls.length);
		for (int i = 0; i < urls.length; i++) {
			AntClasspathEntry entry= new AntClasspathEntry(urls[i]);
			entries.add(entry);
		}
		if (toolsJarEntry != null) {
			entries.add(toolsJarEntry);
		}
		return entries;
	}
	
	private URL[] getLocationURLs(File location) throws MalformedURLException {
		 final String extension= ".jar"; //$NON-NLS-1$
		 URL[] urls = new URL[0];
		 
		 if (!location.exists()) {
			 return urls;
		 }
		 
		 if (!location.isDirectory()) {
			 urls = new URL[1];
			 String path = location.getPath();
			 if (path.toLowerCase().endsWith(extension)) {
				 urls[0] = location.toURL();
			 }
			 return urls;
		 }
		 
		 File[] matches = location.listFiles(
			 new FilenameFilter() {
				 public boolean accept(File dir, String name) {
					return name.toLowerCase().endsWith(extension);
				 }
			 });
		 
		 urls = new URL[matches.length];
		 for (int i = 0; i < matches.length; ++i) {
			 urls[i] = matches[i].toURL();
		 }
		 return urls;
	 }
	
	/**
	 * Creates the tab item that contains this sub-page.
	 */
	protected TabItem createTabItem(TabFolder folder) {
		TabItem item = new TabItem(folder, SWT.NONE);
		item.setText(AntPreferencesMessages.AntClasspathPage_title);
		item.setImage(fAntClasspathBlock.getClasspathImage());
		item.setData(this);
		item.setControl(createContents(folder));
		return item;
	}
	
	/**
	 * Creates this page's controls
	 */
	protected Composite createContents(Composite parent) {
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, IAntUIHelpContextIds.ANT_CLASSPATH_PAGE);
		Font font = parent.getFont();
		
		Composite top = new Composite(parent, SWT.NONE);
		top.setFont(font);
		
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = 2;
		layout.marginWidth = 2;
		top.setLayout(layout);

		top.setLayoutData(new GridData(GridData.FILL_BOTH));

		fAntClasspathBlock.setContainer(this);
		fAntClasspathBlock.createContents(top);
		
		return top;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ant.internal.ui.preferences.IAntBlockContainer#update()
	 */
	public void update() {
		if (fAntClasspathBlock.isValidated()){
			return;
		}
		setMessage(null);
		setErrorMessage(null);
		boolean valid= fAntClasspathBlock.validateAntHome();
	
		if (valid) {
			valid= fAntClasspathBlock.validateToolsJAR();
		}
		
		fPreferencePage.setValid(valid);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ant.internal.ui.preferences.IAntBlockContainer#setMessage(java.lang.String)
	 */
	public void setMessage(String message) {
		fPreferencePage.setMessage(message);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ant.internal.ui.preferences.IAntBlockContainer#setErrorMessage(java.lang.String)
	 */
	public void setErrorMessage(String message) {
		fPreferencePage.setErrorMessage(message);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ant.internal.ui.preferences.IAntBlockContainer#createPushButton(org.eclipse.swt.widgets.Composite, java.lang.String)
	 */
	public Button createPushButton(Composite parent, String buttonText) {
		Button button = new Button(parent, SWT.PUSH);
		button.setFont(parent.getFont());
		button.setText(buttonText);
		fPreferencePage.setButtonLayoutData(button);
		return button;
	}
}
