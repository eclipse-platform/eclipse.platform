/*******************************************************************************
 * Copyright (c) 2010, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.wizards;

import java.util.Vector;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.FrameworkUtil;

public abstract class PsfStore {
	// Most recently used filename is first in the array.
	// Least recently used filename is at the end of the list.
	// When the list overflows, items drop off the end.
	private static final int HISTORY_LENGTH = 10;

	private static final String STORE_SECTION = "ImportPSFDialog"; //$NON-NLS-1$

	private static IDialogSettings _section;

	protected abstract String getPreviousTag();
	protected abstract String getListTag();

	public abstract String getSuggestedDefault();

	protected String getPrevious() {
		IDialogSettings section = getSettingsSection();
		String retval = section.get(getPreviousTag());
		if (retval == null) {
			retval = ""; //$NON-NLS-1$
		}
		return retval;
	}

	public String[] getHistory() {
		IDialogSettings section = getSettingsSection();
		String[] arr = section.getArray(getListTag());
		if (arr == null) {
			arr = new String[0];
		}
		return arr;
	}

	public void remember(String filename) {
		Vector<String> filenames = createVector(getHistory());
		if (filenames.contains(filename)) {
			// The item is in the list. Remove it and add it back at the
			// beginning. If it already was at the beginning this will be a
			// waste of time, but it's not even measurable so I don't care.
			filenames.remove(filename);
		}
		// Most recently used filename goes to the beginning of the list
		filenames.add(0, filename);

		// Forget any overflowing items
		while (filenames.size() > HISTORY_LENGTH) {
			filenames.remove(HISTORY_LENGTH);
		}

		// Make it an array
		String[] arr = filenames.toArray(new String[filenames.size()]);

		IDialogSettings section = getSettingsSection();
		section.put(getListTag(), arr);
		section.put(getPreviousTag(), filename);
	}

	private Vector<String> createVector(String[] arr) {
		Vector<String> v = new Vector<>();
		for (int ix = 0; ix < arr.length; ++ix) {
			v.add(ix, arr[ix]);
		}
		return v;
	}

	private IDialogSettings getSettingsSection() {
		if (_section != null)
			return _section;

		IDialogSettings settings = PlatformUI.getDialogSettingsProvider(FrameworkUtil.getBundle(PsfStore.class)).getDialogSettings();
		_section = settings.getSection(STORE_SECTION);
		if (_section != null)
			return _section;

		_section = settings.addNewSection(STORE_SECTION);
		return _section;
	}
}
