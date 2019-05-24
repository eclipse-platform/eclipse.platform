/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.ui;

import java.util.Iterator;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.core.history.ITag;

/**
 * Copies the text form the selected element of the given
 * {@link org.eclipse.jface.viewers.TableViewer}.
 */
public class TableViewerAction extends Action {

	/**
	 * Viewer for which the action is to be performed
	 */
	private TableViewer viewer;

	public TableViewerAction(TableViewer viewer) {
		this.viewer = viewer;
	}

	@Override
	public void run() {
		if (viewer.getSelection() instanceof StructuredSelection) {
			StructuredSelection selection = (StructuredSelection) viewer.getSelection();
			if (!selection.isEmpty()) {
				Iterator selectionIter = selection.iterator();
				
				StringBuilder buf = new StringBuilder();
				ITag firstTag = (ITag) selectionIter.next();
				buf.append(firstTag.getName());
				while (selectionIter.hasNext()) {
					String tagName = ((ITag) selectionIter.next()).getName();
					buf.append(System.getProperty("line.separator", "\n")).append(tagName); //$NON-NLS-1$ //$NON-NLS-2$
				}
				
				Clipboard clipboard = new Clipboard(Display.getDefault());
				Object[] data = new Object[] { buf.toString() };
				Transfer[] dataTypes = new Transfer[] {TextTransfer.getInstance()};
				try {
					clipboard.setContents(data, dataTypes);
				} catch (SWTError e) {
					if (e.code != DND.ERROR_CANNOT_SET_CLIPBOARD) {
						throw e;
					}
				} finally {
					clipboard.dispose();
				}
			}
		}
	}
}
