/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.internal.ui.preferences;


import org.eclipse.ant.core.AntCorePlugin;
import org.eclipse.ant.core.AntCorePreferences;
import org.eclipse.ant.core.IAntClasspathEntry;
import org.eclipse.ant.internal.ui.model.IAntUIHelpContextIds;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.help.WorkbenchHelp;

/**
 * Sub-page that allows the user to enter custom classpaths
 * to be used when running Ant build files.
 */
public class AntClasspathPage implements IAntBlockContainer {

	private AntClasspathBlock antClasspathBlock= new AntClasspathBlock();
	private AntRuntimePreferencePage preferencePage;
	private ClasspathModel model;
	
	/**
	 * Creates an instance.
	 */
	public AntClasspathPage(AntRuntimePreferencePage preferencePage) {
		this.preferencePage = preferencePage;
	}
	
	/**
	 * Returns the specified user classpath entries
	 * 
	 * @return set of user classpath entries
	 */
	protected IAntClasspathEntry[] getAdditionalEntries() {
		return model.getEntries(ClasspathModel.GLOBAL_USER);
	}
	
	/**
	 * Returns the specified ant home classpath entries
	 */
	protected IAntClasspathEntry[] getAntHomeEntries() {
		return model.getEntries(ClasspathModel.ANT_HOME);
	}
	
	protected String getAntHome() {
		return antClasspathBlock.getAntHome();
	}
	
	/**
	 * Sets the contents of the tables on this page.
	 */
	protected void initialize() {
		
		AntCorePreferences prefs= AntCorePlugin.getPlugin().getPreferences();
		createClasspathModel();
		antClasspathBlock.initializeAntHome(prefs.getAntHome());
		antClasspathBlock.setInput(model);
		
		preferencePage.setErrorMessage(null);
		preferencePage.setValid(true);
	}
	
	protected void createClasspathModel() {
		model= new ClasspathModel();
		AntCorePreferences prefs= AntCorePlugin.getPlugin().getPreferences();
		model.setAntHomeEntries(prefs.getAntHomeClasspathEntries());
		model.setGlobalEntries(prefs.getAdditionalClasspathEntries());
	}
	
	protected void performDefaults() {
		AntCorePreferences prefs= AntCorePlugin.getPlugin().getPreferences();
		model= new ClasspathModel();
		model.setAntHomeEntries(prefs.getDefaultAntHomeEntries());
		IAntClasspathEntry toolsEntry= prefs.getToolsJarEntry();
		IAntClasspathEntry[] additionalEntries;
		if (toolsEntry == null) {
			additionalEntries= new IAntClasspathEntry[0];
		} else {
			additionalEntries= new IAntClasspathEntry[] {toolsEntry};
		}
		model.setGlobalEntries(additionalEntries);
		antClasspathBlock.initializeAntHome(prefs.getDefaultAntHome());
		antClasspathBlock.setInput(model);
		update();
	}
	
	/**
	 * Creates the tab item that contains this sub-page.
	 */
	protected TabItem createTabItem(TabFolder folder) {
		TabItem item = new TabItem(folder, SWT.NONE);
		item.setText(AntPreferencesMessages.getString("AntClasspathPage.title")); //$NON-NLS-1$;
		item.setImage(antClasspathBlock.getClasspathImage());
		item.setData(this);
		item.setControl(createContents(folder));
		return item;
	}
	
	/**
	 * Creates this page's controls
	 */
	protected Composite createContents(Composite parent) {
		WorkbenchHelp.setHelp(parent, IAntUIHelpContextIds.ANT_CLASSPATH_PAGE);
		Font font = parent.getFont();
		
		Composite top = new Composite(parent, SWT.NONE);
		top.setFont(font);
		
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = 2;
		layout.marginWidth = 2;
		top.setLayout(layout);

		top.setLayoutData(new GridData(GridData.FILL_BOTH));

		antClasspathBlock.setContainer(this);
		antClasspathBlock.createContents(top);
		
		return top;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ant.internal.ui.preferences.IAntBlockContainer#update()
	 */
	public void update() {
		if (antClasspathBlock.isValidated()){
			return;
		}
		setMessage(null);
		setErrorMessage(null);
		boolean valid= antClasspathBlock.validateAntHome();
	
		if (valid) {
			valid= antClasspathBlock.validateToolsJAR();
		}
		
		preferencePage.setValid(valid);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ant.internal.ui.preferences.IAntBlockContainer#setMessage(java.lang.String)
	 */
	public void setMessage(String message) {
		preferencePage.setMessage(message);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ant.internal.ui.preferences.IAntBlockContainer#setErrorMessage(java.lang.String)
	 */
	public void setErrorMessage(String message) {
		preferencePage.setErrorMessage(message);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ant.internal.ui.preferences.IAntBlockContainer#createPushButton(org.eclipse.swt.widgets.Composite, java.lang.String)
	 */
	public Button createPushButton(Composite parent, String buttonText) {
		Button button = new Button(parent, SWT.PUSH);
		button.setFont(parent.getFont());
		button.setText(buttonText);
		preferencePage.setButtonLayoutData(button);
		return button;
	}
}