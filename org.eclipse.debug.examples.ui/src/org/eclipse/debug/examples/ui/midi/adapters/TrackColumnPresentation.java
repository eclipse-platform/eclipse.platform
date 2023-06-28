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
public class TrackColumnPresentation extends AbstractColumnPresentation {

	/**
	 * Column identifiers
	 */
	public static final String COL_TICK = "TICK"; //$NON-NLS-1$
	public static final String COL_BYTES = "BYTES"; //$NON-NLS-1$
	public static final String COL_COMMAND = "COMMAND"; //$NON-NLS-1$
	public static final String COL_CHANNEL = "CHANNEL"; //$NON-NLS-1$

	/**
	 * All columns
	 */
	public static final String[] COLUMN_IDS = new String[]{COL_TICK, COL_BYTES, COL_COMMAND, COL_CHANNEL};

	/**
	 * Initial columns
	 */
	public static final String[] COLUMN_INITIAL = new String[]{COL_TICK, COL_BYTES};

	/**
	 * Column presentation ID.
	 */
	public static final String ID = "org.eclipse.debug.examples.ui.midi.trackColumns"; //$NON-NLS-1$

	@Override
	public String[] getAvailableColumns() {
		return COLUMN_IDS;
	}

	@Override
	public String getHeader(String id) {
		if (COL_TICK.equals(id)) {
			return "Tick"; //$NON-NLS-1$
		}
		if (COL_BYTES.equals(id)) {
			return "Bytes"; //$NON-NLS-1$
		}
		if (COL_CHANNEL.equals(id)) {
			return "Channel"; //$NON-NLS-1$
		}
		if (COL_COMMAND.equals(id)) {
			return "Command"; //$NON-NLS-1$
		}
		return ""; //$NON-NLS-1$
	}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public String[] getInitialColumns() {
		return COLUMN_INITIAL;
	}

	@Override
	public boolean isOptional() {
		return false;
	}

}
