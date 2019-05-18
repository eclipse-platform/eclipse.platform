/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.wizards;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Shell;

/**
 * Persists the size of the wizard dialog.
 */
public class ResizableWizard extends Wizard {
	
	private final int DEFAULT_WIDTH;
	private final int DEFAULT_HEIGHT;
	
	private static final String BOUNDS_HEIGHT_KEY = "width"; //$NON-NLS-1$
	private static final String BOUNDS_WIDTH_KEY = "height"; //$NON-NLS-1$
	
	final String fSectionName;
	
	public ResizableWizard(String sectionName, IDialogSettings settings) {
		this(sectionName, settings, 300, 400);
	}
	
	protected ResizableWizard(String sectionName, IDialogSettings settings, int defaultWidth, int defaultHeight) {
		DEFAULT_WIDTH= defaultWidth;
		DEFAULT_HEIGHT= defaultHeight;
		fSectionName= sectionName;
		setDialogSettings(settings);
	}
	
	protected static int open(Shell shell, ResizableWizard wizard) {
		final WizardDialog dialog= new WizardDialog(shell, wizard);
		dialog.setMinimumPageSize(wizard.loadSize());
		return dialog.open();
	}
	
	public void saveSize() {
		final Rectangle bounds= getContainer().getCurrentPage().getControl().getParent().getClientArea();
		final IDialogSettings settings= getDialogSettings();
		if (settings == null)
			return;
		
		IDialogSettings section= settings.getSection(fSectionName); 
		if (section == null)
			section= settings.addNewSection(fSectionName);
		
		section.put(BOUNDS_WIDTH_KEY, bounds.width);
		section.put(BOUNDS_HEIGHT_KEY, bounds.height);
	}
	
	public Point loadSize() {
		final Point size= new Point(DEFAULT_WIDTH, DEFAULT_HEIGHT);
		
		final IDialogSettings settings= getDialogSettings();
		if (settings == null)
			return size;
		
		final IDialogSettings section= settings.getSection(fSectionName);
		if (section == null)
			return size;

		try {
			size.x= section.getInt(BOUNDS_WIDTH_KEY);
			size.y= section.getInt(BOUNDS_HEIGHT_KEY);
		} catch (NumberFormatException e) {
		}
		return size;
	}


	public boolean performFinish() {
		saveSize();
		return true;
	}
}
