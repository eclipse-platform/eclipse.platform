/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
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
/**
 *
 */
package org.eclipse.unittest.internal.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

/**
 * A failure table display
 */
public class FailureTableDisplay implements ITraceDisplay {
	private final Table fTable;

	private final Image fExceptionIcon = Images.createImage("obj16/exc_catch.png"); //$NON-NLS-1$

	private final Image fStackIcon = Images.createImage("obj16/stkfrm_obj.png"); //$NON-NLS-1$

	/**
	 * Constructs a failure table display
	 *
	 * @param table a table object
	 */
	public FailureTableDisplay(Table table) {
		fTable = table;
		fTable.getParent().addDisposeListener(e -> disposeIcons());
	}

	@Override
	public void addTraceLine(int lineType, String label) {
		TableItem tableItem = newTableItem();
		switch (lineType) {
		case TextualTrace.LINE_TYPE_EXCEPTION:
			tableItem.setImage(fExceptionIcon);
			break;
		case TextualTrace.LINE_TYPE_STACKFRAME:
			tableItem.setImage(fStackIcon);
			break;
		case TextualTrace.LINE_TYPE_NORMAL:
		default:
			break;
		}
		tableItem.setText(label);
	}

	/**
	 * Returns an exception icon image
	 *
	 * @return an exception icon image
	 */
	public Image getExceptionIcon() {
		return fExceptionIcon;
	}

	/**
	 * Returns a stack icon image
	 *
	 * @return a stack icon image
	 */
	public Image getStackIcon() {
		return fStackIcon;
	}

	/**
	 * Returns a table object
	 *
	 * @return a table object
	 */
	public Table getTable() {
		return fTable;
	}

	private void disposeIcons() {
		if (fExceptionIcon != null && !fExceptionIcon.isDisposed())
			fExceptionIcon.dispose();
		if (fStackIcon != null && !fStackIcon.isDisposed())
			fStackIcon.dispose();
	}

	/**
	 * Returns a new table item
	 *
	 * @return a table item object instance
	 */
	public TableItem newTableItem() {
		return new TableItem(fTable, SWT.NONE);
	}
}
