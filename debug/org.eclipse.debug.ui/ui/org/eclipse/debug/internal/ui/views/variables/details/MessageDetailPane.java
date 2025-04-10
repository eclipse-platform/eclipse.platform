/*******************************************************************************
 *  Copyright (c) 2010, 2012 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.internal.ui.views.variables.details;

import org.eclipse.debug.internal.ui.SWTFactory;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.IDetailPane;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbenchPartSite;

/**
 * A detail pane that displays a message in a wrapped label. Not contributed by an extension
 * point - used internally to display messages.
 *
 * @since 3.6
 */
public class MessageDetailPane implements IDetailPane {

	public static final String ID = IDebugUIConstants.PLUGIN_ID + ".detailpanes.message"; //$NON-NLS-1$
	public static final String NAME = DetailMessages.MessageDetailPane_0;
	public static final String DESCRIPTION = DetailMessages.MessageDetailPane_1;

	/**
	 * Composite that contains the label that has margins.
	 */
	private Composite fControlParent;

	/**
	 * Label control
	 */
	private Label fLabel;

	@Override
	public void init(IWorkbenchPartSite partSite) {
	}

	@Override
	public void dispose() {
		fControlParent.dispose();
	}

	@Override
	public Control createControl(Composite parent) {
		fControlParent = SWTFactory.createComposite(parent, 1, 1, GridData.FILL_BOTH);
		fControlParent.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
		fLabel = SWTFactory.createWrapLabel(fControlParent, "", 1); //$NON-NLS-1$
		return fControlParent;
	}

	@Override
	public boolean setFocus() {
		return false;
	}

	@Override
	public void display(IStructuredSelection selection) {
		// re-create controls if the layout has changed
		if (selection != null && selection.size() == 1) {
			Object input = selection.getFirstElement();
			if (input instanceof String) {
				String message = (String) input;
				fLabel.setText(message);
				fControlParent.layout(true);
			}
		} else if (selection == null || selection.isEmpty()) {
			// clear the message
			fLabel.setText(""); //$NON-NLS-1$
			fControlParent.layout(true);
		}
	}

	@Override
	public String getID() {
		return ID;
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public String getDescription() {
		return DESCRIPTION;
	}

}
