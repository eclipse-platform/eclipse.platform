/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.internal.ccvs.core.ICVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.ui.actions.CVSAction;

public class CopyRepositoryNameAction extends CVSAction {
	public boolean isEnabled() {
		return getSelectedRepositories().length > 0;
	}
	public void execute(IAction action) {
		ICVSRepositoryLocation[] locations = getSelectedRepositories();
		if (locations.length == 0)
			return;

		StringBuilder buffer = new StringBuilder();
		for (int i = 0; i < locations.length; i++) {
			buffer.append(locations[i].getLocation(true));
			if (i < locations.length - 1) buffer.append("\n"); //$NON-NLS-1$
		}
		copyToClipbard(Display.getDefault(), buffer.toString());
	}
	protected ICVSRepositoryLocation[] getSelectedRepositories() {
		ArrayList repositories = null;
		IStructuredSelection selection = getSelection();
		if (!selection.isEmpty()) {
			repositories = new ArrayList();
			Iterator elements = selection.iterator();
			while (elements.hasNext()) {
				Object next = getAdapter(elements.next(), ICVSRepositoryLocation.class);
				if (next instanceof ICVSRepositoryLocation) {
					repositories.add(next);
					continue;
				}
			}
		}
		if (repositories != null && !repositories.isEmpty()) {
			ICVSRepositoryLocation[] result = new ICVSRepositoryLocation[repositories.size()];
			repositories.toArray(result);
			return result;
		}
		return new ICVSRepositoryLocation[0];
	}
	private void copyToClipbard(Display display, String str) {
		Clipboard clipboard = new Clipboard(display);
		clipboard.setContents(new String[] { str },	new Transfer[] { TextTransfer.getInstance()});
		clipboard.dispose();
	}
}
