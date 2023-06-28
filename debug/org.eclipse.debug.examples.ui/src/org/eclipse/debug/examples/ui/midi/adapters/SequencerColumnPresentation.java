/*******************************************************************************
 * Copyright (c) 2008, 2013 IBM Corporation and others.
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
package org.eclipse.debug.examples.ui.midi.adapters;

import org.eclipse.debug.internal.ui.viewers.provisional.AbstractColumnPresentation;

/**
 * Columns for sequencer in the variables view.
 *
 * @since 1.0
 */
public class SequencerColumnPresentation extends AbstractColumnPresentation {

	/**
	 * Column identifiers
	 */
	public static final String COL_NAME = "NAME"; //$NON-NLS-1$
	public static final String COL_VALUE = "VALUE"; //$NON-NLS-1$

	/**
	 * All columns
	 */
	public static final String[] COLUMN_IDS = new String[]{COL_NAME, COL_VALUE};

	/**
	 * Column presentation ID.
	 */
	public static final String ID = "org.eclipse.debug.examples.ui.midi.columnPresentation"; //$NON-NLS-1$

	@Override
	public String[] getAvailableColumns() {
		return COLUMN_IDS;
	}

	@Override
	public String getHeader(String id) {
		if (COL_NAME.equals(id)) {
			return "Control"; //$NON-NLS-1$
		}
		if (COL_VALUE.equals(id)) {
			return "Value"; //$NON-NLS-1$
		}
		return ""; //$NON-NLS-1$
	}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public String[] getInitialColumns() {
		return COLUMN_IDS;
	}

	@Override
	public boolean isOptional() {
		return false;
	}

}
