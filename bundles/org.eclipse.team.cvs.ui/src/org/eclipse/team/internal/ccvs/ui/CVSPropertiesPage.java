/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
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

import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.ui.dialogs.PropertyPage;

public abstract class CVSPropertiesPage extends PropertyPage {

	/**
	 * Return the appropriate Tag label for properties pages
	 * based on the tag type.
	 * @param tag
	 * @return String
	 */
	
	public static String getTagLabel(CVSTag tag) {
	
	if (tag == null) {
		return CVSUIMessages.CVSFilePropertiesPage_none; 
	}
	
	switch (tag.getType()) {
		case CVSTag.HEAD:
			return tag.getName();
		case CVSTag.VERSION:
			return NLS.bind(CVSUIMessages.CVSFilePropertiesPage_version, new String[] { tag.getName() }); 
		case CVSTag.BRANCH:
			return NLS.bind(CVSUIMessages.CVSFilePropertiesPage_branch, new String[] { tag.getName() }); 
		case CVSTag.DATE:
			return NLS.bind(CVSUIMessages.CVSFilePropertiesPage_date, new String[] { tag.getName() }); 
		default :
			return tag.getName();
		}
	}

	/**
	 * Utility method that creates a label instance and sets the default layout data.
	 * 
	 * @param parent the parent for the new label
	 * @param text the text for the new label
	 * @return the new label
	 */
	protected Label createLabel(Composite parent, String text, int span) {
		Label label = new Label(parent, SWT.LEFT);
		label.setText(text);
		GridData data = new GridData();
		data.horizontalSpan = span;
		data.horizontalAlignment = GridData.FILL;
		label.setLayoutData(data);
		return label;
	}

	protected Label createLabel(Composite parent, String text) {
		return createLabel(parent, text, 1);
	}

	/**
	 * Utility method that creates a read-only text field and sets the default layout data.
	 * 
	 * @param parent the parent for the new text field
	 * @param text the text
	 * @return the new read-only text field
	 * @since 4.3
	 */
	protected Text createReadOnlyText(Composite parent, String text) {
		Text textField = new Text(parent, SWT.LEFT | SWT.READ_ONLY);
		textField.setBackground(textField.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
		textField.setText(text);
		GridData data = new GridData();
		data.horizontalAlignment = GridData.FILL;
		textField.setLayoutData(data);
		return textField;
	}

}
