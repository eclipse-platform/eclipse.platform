/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Matt McCutchen (hashproduct+eclipse@gmail.com) - Bug 35390 Three-way compare cannot select (mis-selects) )ancestor resource
 *     Aleksandra Wozniak (aleksandra.k.wozniak@gmail.com) - Bug 239959
 *******************************************************************************/
package org.eclipse.compare.internal;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareUI;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;


/*
 * The "Compare with each other" action
 */
public class CompareAction extends BaseCompareAction implements IObjectActionDelegate {

	protected ResourceCompareInput fInput;
	protected IWorkbenchPage fWorkbenchPage;
	protected boolean showSelectAncestorDialog = true;

	public void run(ISelection selection) {
		if (fInput != null) {
			// Pass the shell so setSelection can prompt the user for which
			// resource should be the ancestor
			boolean ok = fInput.setSelection(selection, fWorkbenchPage
					.getWorkbenchWindow().getShell(), showSelectAncestorDialog);
			if (!ok) return;
			fInput.initializeCompareConfiguration();
			CompareUI.openCompareEditorOnPage(fInput, fWorkbenchPage);
			fInput= null;	// don't reuse this input!
		}
	}

	protected boolean isEnabled(ISelection selection) {
		if (fInput == null) {
			CompareConfiguration cc= new CompareConfiguration();
			// buffered merge mode: don't ask for confirmation
			// when switching between modified resources
			cc.setProperty(CompareEditor.CONFIRM_SAVE_PROPERTY, new Boolean(false));
			
			// uncomment following line to have separate outline view
			//cc.setProperty(CompareConfiguration.USE_OUTLINE_VIEW, new Boolean(true));
						
			fInput= new ResourceCompareInput(cc);
		}
		return fInput.isEnabled(selection);
	}

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		fWorkbenchPage= targetPart.getSite().getPage();
	}
}
