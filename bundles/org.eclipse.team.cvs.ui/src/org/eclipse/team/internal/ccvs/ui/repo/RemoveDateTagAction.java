/*******************************************************************************
 * Copyright (c) 2003, 2006 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.ui.repo;

import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.ui.CVSUIMessages;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.ccvs.ui.model.CVSTagElement;
import org.eclipse.team.internal.ui.actions.TeamAction;
import org.eclipse.ui.actions.SelectionListenerAction;


public class RemoveDateTagAction extends SelectionListenerAction {
	private IStructuredSelection selection;
	
	public RemoveDateTagAction() {
		super(CVSUIMessages.RemoveDateTagAction_0); 
	}

	public void run() {
		CVSTagElement[] elements = getSelectedCVSTagElements();
		if (elements.length == 0) return;
		for (CVSTagElement element : elements) {
			RepositoryManager mgr = CVSUIPlugin.getPlugin().getRepositoryManager();
			CVSTag tag = element.getTag();
			if (tag.getType() == CVSTag.DATE) {
				mgr.removeDateTag(element.getRoot(), tag);
			}				
		}
	}

	protected boolean updateSelection(IStructuredSelection selection) {
		this.selection = selection;
		boolean b = containsDataTag();
		setEnabled(b);
		return b;
	}
	
	private boolean containsDataTag(){
		CVSTagElement[] elements = getSelectedCVSTagElements();
		if (elements.length > 0){ 		
			for (CVSTagElement element : elements) {
				CVSTag tag = element.getTag();
				if(tag.getType() == CVSTag.DATE){
					return true;
				}				
			}
		}
		return false;
	}
	
	/**
	 * Returns the selected CVS date tag elements
	 */
	private CVSTagElement[] getSelectedCVSTagElements() {
		ArrayList cvsTagElements = null;
		if (selection!=null && !selection.isEmpty()) {
			cvsTagElements = new ArrayList();
			Iterator elements = selection.iterator();
			while (elements.hasNext()) {
				Object next = TeamAction.getAdapter(elements.next(), CVSTagElement.class);
				if (next instanceof CVSTagElement) {
					cvsTagElements.add(next);
				}
			}
		}
		if (cvsTagElements != null && !cvsTagElements.isEmpty()) {
			CVSTagElement[] result = new CVSTagElement[cvsTagElements.size()];
			cvsTagElements.toArray(result);
			return result;
		}
		return new CVSTagElement[0];
	}
}
