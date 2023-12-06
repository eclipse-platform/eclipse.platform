/*******************************************************************************
 * Copyright (c) 2009, 2011 IBM Corporation and others.
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
package org.eclipse.ant.internal.ui;

import org.eclipse.ant.internal.core.IAntCoreConstants;
import org.eclipse.ant.internal.ui.preferences.AntObjectLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.TableColumn;

/**
 * A sorter that can be attached to a given column in a given viewer. This comparator uses the {@link AntObjectLabelProvider} to get the text to
 * compare
 * 
 * @since 3.5
 */
public abstract class ColumnSorter extends ViewerComparator {

	private ColumnViewer cviewer = null;
	private TableColumn column = null;
	private int direction = SWT.DOWN;
	private int columnidx = 0;

	/**
	 * Constructor
	 */
	public ColumnSorter(ColumnViewer cviewer, TableColumn column) {
		this.cviewer = cviewer;
		this.column = column;
		this.columnidx = getColumnIndex();
		this.column.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (ColumnSorter.this.cviewer.getComparator() != ColumnSorter.this) {
					setDirection(SWT.DOWN);
				} else {
					int tdirection = ColumnSorter.this.column.getParent().getSortDirection();
					if (tdirection == SWT.NONE) {
						setDirection(SWT.DOWN);
					} else {
						setDirection(tdirection == SWT.UP ? SWT.DOWN : SWT.UP);
					}
				}
			}
		});

	}

	/**
	 * Returns the compare text that should be used for the given object coming from the given column index
	 * 
	 * @return the text to compare with
	 */
	public abstract String getCompareText(Object obj, int columnindex);

	/**
	 * Sets the sorting direction for this sorter to use
	 */
	public void setDirection(int direction) {
		this.column.getParent().setSortColumn(this.column);
		this.direction = direction;
		this.column.getParent().setSortDirection(this.direction);
		if (this.cviewer.getComparator() == this) {
			this.cviewer.refresh();
		} else {
			this.cviewer.setComparator(this);
		}
	}

	@Override
	public int compare(Viewer viewer, Object e1, Object e2) {
		String text1 = getCompareText(e1, this.columnidx);
		if (text1 == null) {
			text1 = IAntCoreConstants.EMPTY_STRING;
		}
		String text2 = getCompareText(e2, this.columnidx);
		if (text2 == null) {
			text2 = IAntCoreConstants.EMPTY_STRING;
		}
		return (this.direction == SWT.UP ? -1 : 1) * text1.compareTo(text2);
	}

	/**
	 * Returns the index of the given column in the backing table for this page, or 0
	 * 
	 * @return the index of the column in the backing table for this page or 0
	 */
	private int getColumnIndex() {
		int idx = this.column.getParent().indexOf(this.column);
		return (idx < 0 ? 0 : idx);
	}
}
