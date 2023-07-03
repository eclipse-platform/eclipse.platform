/*******************************************************************************
 * Copyright (c) 2004, 2015 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.views.memory;

import java.math.BigInteger;
import java.util.ArrayList;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IMemoryBlockManager;
import org.eclipse.debug.core.model.IDebugElement;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IMemoryBlockExtension;
import org.eclipse.debug.core.model.IMemoryBlockRetrieval;
import org.eclipse.debug.internal.core.IInternalDebugCoreConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.UIJob;

/**
 * Util class for Memory View
 *
 * @since 3.0
 */
public class MemoryViewUtil {

	public static final int[] ignoreKeyEvents = {
			SWT.ARROW_UP, SWT.ARROW_DOWN, SWT.ARROW_LEFT, SWT.ARROW_RIGHT,
			SWT.PAGE_UP, SWT.PAGE_DOWN, SWT.HOME, SWT.END, SWT.INSERT, SWT.F1,
			SWT.F2, SWT.F3, SWT.F4, SWT.F5, SWT.F6, SWT.F7, SWT.F8, SWT.F9,
			SWT.F10, SWT.F11, SWT.F12, SWT.F13, SWT.F14, SWT.F15, SWT.HELP,
			SWT.CAPS_LOCK, SWT.NUM_LOCK, SWT.SCROLL_LOCK, SWT.PAUSE, SWT.BREAK,
			SWT.PRINT_SCREEN, SWT.ESC, SWT.CTRL, SWT.ALT, SWT.SHIFT };

	public static ArrayList<String> MEMORY_BLOCKS_HISTORY = new ArrayList<>();

	/**
	 * @param selection
	 * @return true if the given selection is valid for creating a memory block
	 */
	static public boolean isValidSelection(ISelection selection) {

		if (!(selection instanceof IStructuredSelection)) {
			return false;
		}

		// only single selection is allowed for this action
		if (selection.isEmpty() || ((IStructuredSelection) selection).size() > 1) {
			return false;
		}

		Object elem = ((IStructuredSelection) selection).getFirstElement();

		return isValidContext(elem);
	}

	/**
	 * @param elem
	 * @return
	 */
	public static boolean isValidContext(Object elem) {
		// if not debug element
		if (!(elem instanceof IAdaptable)) {
			return false;
		}

		IMemoryBlockRetrieval memRetrieval = ((IAdaptable) elem).getAdapter(IMemoryBlockRetrieval.class);

		IDebugTarget debugTarget = null;
		if (elem instanceof IDebugElement) {
			debugTarget = ((IDebugElement) elem).getDebugTarget();
		} else {
			debugTarget = ((IAdaptable) elem).getAdapter(IDebugTarget.class);
		}

		if (memRetrieval == null) {
			// if debug element returns null from getAdapter, assume its debug
			// target is going to retrieve memory blocks
			memRetrieval = debugTarget;
		}

		// not valid if the debug target is already terminated
		if (debugTarget != null && (debugTarget.isTerminated() || debugTarget.isDisconnected())) {
			return false;
		}

		if (memRetrieval != null && memRetrieval.supportsStorageRetrieval()) {
			return true;
		}

		return false;
	}

	/**
	 * Helper function to open an error dialog.
	 *
	 * @param title
	 * @param message
	 * @param e
	 */
	static public void openError(final String title, final String message, final Exception e) {
		UIJob uiJob = new UIJob("open error") { //$NON-NLS-1$

			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {
				// open error for the exception
				String detail = IInternalDebugCoreConstants.EMPTY_STRING;
				if (e != null) {
					detail = e.getMessage();
				}

				Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();

				MessageDialog.openError(shell, title, message + "\n" + detail); //$NON-NLS-1$
				return Status.OK_STATUS;
			}
		};
		uiJob.setSystem(true);
		uiJob.schedule();
	}

	static IMemoryBlockManager getMemoryBlockManager() {
		return DebugPlugin.getDefault().getMemoryBlockManager();
	}

	static public boolean isLinuxGTK() {
		String ws = Platform.getWS();
		return ws.equals(Platform.WS_GTK);
	}

	/**
	 * Checks to see if the event is valid for activating cell editing in a view
	 * tab
	 *
	 * @param event
	 * @return true if the edit event is valid for activating the cell editor
	 */
	public static boolean isValidEditEvent(int event) {
		for (int ignoredKeyKevent : MemoryViewUtil.ignoreKeyEvents) {
			if (event == ignoredKeyKevent) {
				return false;
			}
		}
		return true;
	}

	public static BigInteger alignToBoundary(BigInteger integer, int numberOfUnitsPerLine) {
		BigInteger[] result = integer.divideAndRemainder(BigInteger.valueOf(numberOfUnitsPerLine));
		return integer.subtract(result[1]);
	}

	public static void addHistory(String expression) {
		if (!MEMORY_BLOCKS_HISTORY.contains(expression)) {
			MEMORY_BLOCKS_HISTORY.add(0, expression);
		}

		if (MEMORY_BLOCKS_HISTORY.size() > 5) {
			MEMORY_BLOCKS_HISTORY.remove(MEMORY_BLOCKS_HISTORY.size() - 1);
		}
	}

	public static String[] getHistory() {
		return MEMORY_BLOCKS_HISTORY.toArray(new String[MEMORY_BLOCKS_HISTORY.size()]);
	}

	/**
	 * Return the memory block retrieval of the given object
	 *
	 * @param object
	 * @return the memory block retrieval of the given object or
	 *         <code>null</code> if no memory block retrieval can be found
	 *
	 *         Returning null for the memory block retrieval will result in
	 *         errors in operations that follow.
	 *
	 *         Non-standard debug models must provide a memory block retrieval
	 *         via <code>getAdapter(IMemoryBlockRetrieval.class</code>
	 */
	public static IMemoryBlockRetrieval getMemoryBlockRetrieval(Object object) {
		IMemoryBlockRetrieval retrieval = null;

		// if memory block extension, retrieval must be queired throught its
		// interface
		if (object instanceof IMemoryBlockExtension) {
			return ((IMemoryBlockExtension) object).getMemoryBlockRetrieval();
		}

		// check if the object can adapt to a memory block retrieval
		if (object instanceof IAdaptable) {
			IAdaptable adaptable = (IAdaptable) object;
			retrieval = adaptable.getAdapter(IMemoryBlockRetrieval.class);
		}

		// if cannot adapt and the object itself is already an
		// IMemoryBlockRetrieval, return that.
		if (retrieval == null && object instanceof IMemoryBlockRetrieval) {
			retrieval = (IMemoryBlockRetrieval) object;
		}

		// otherewise, default back to the debug target
		if (retrieval == null && object instanceof IDebugElement) {
			IDebugElement de = (IDebugElement) object;
			retrieval = de.getDebugTarget();
		}

		return retrieval;
	}

	/**
	 * Returns the hash code for this object, as an Integer.
	 * */
	public static Integer getHashCode(Object o) {
		return Integer.valueOf(o.hashCode());
	}
}
