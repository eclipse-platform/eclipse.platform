/*******************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.synchronize;

import org.eclipse.jface.preference.*;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.team.internal.ui.IHelpContextIds;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.ui.*;

public class StartupPreferencePage extends FieldEditorPreferencePage implements
		IWorkbenchPreferencePage {

	public static final String PROP_STARTUP_ACTION = "startupAction"; //$NON-NLS-1$
	public static final String STARTUP_ACTION_NONE = "none"; //$NON-NLS-1$
	public static final String STARTUP_ACTION_POPULATE = "populate"; //$NON-NLS-1$
	public static final String STARTUP_ACTION_SYNCHRONIZE = "synchronize"; //$NON-NLS-1$
	
	// Property used to make preferences available in the page configuration
	public static final String STARTUP_PREFERENCES = "org.eclipse.team.ui.startupPreferences"; //$NON-NLS-1$
	
	public StartupPreferencePage(IPreferenceStore store) {
		super(GRID);
		setTitle(TeamUIMessages.StartupPreferencePage_0); 
		setDescription(TeamUIMessages.StartupPreferencePage_1); 
		setPreferenceStore(store);
	}

	@Override
	public void init(IWorkbench workbench) {
		// Nothing to do
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createControl(Composite parent) {
		super.createControl(parent);
        // set F1 help
        PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IHelpContextIds.SYNC_STARTUP_PREFERENCE_PAGE);
	}

	@Override
	protected void createFieldEditors() {
	    addField(new RadioGroupFieldEditor(PROP_STARTUP_ACTION, 
	            TeamUIMessages.StartupPreferencePage_2, 1,  
	            new String[][] {
	            	{TeamUIMessages.StartupPreferencePage_3, STARTUP_ACTION_POPULATE}, 
	            	{TeamUIMessages.StartupPreferencePage_4, STARTUP_ACTION_SYNCHRONIZE},
	            	{TeamUIMessages.StartupPreferencePage_5, STARTUP_ACTION_NONE}
	    		}, 
	    		getFieldEditorParent(), true /* use a group */));
	}

}
