/*******************************************************************************
 * Copyright (c) 2015, 2018 CWI. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * Davy Landman (CWI) - [475267][api] Initial definition of interface
 *******************************************************************************/
package org.eclipse.terminal.control;

import org.eclipse.terminal.model.ITerminalTextDataReadOnly;

/**
 * Terminal specific version of {@link org.eclipse.swt.events.MouseListener}
 */
public interface ITerminalMouseListener {

	/**
	 * Invoked when a double-click has happend inside the terminal control.<br>
	 * <br>
	 * <strong>Important:</strong> the event fires for every click, even outside the text region.
	 * @param terminalText a read-only view of the current terminal text
	 * @param button see {@link org.eclipse.swt.events.MouseEvent#button} for the meaning of the button values
	 * @param stateMask see {@link org.eclipse.swt.events.MouseEvent#stateMask} for the meaning of the values
	 */
	default void mouseDoubleClick(ITerminalTextDataReadOnly terminalText, int line, int column, int button,
			int stateMask) {
		// do nothing by default so that implementors only need to implement methods they care about
	}

	/**
	 * Invoked when a mouse button is pushed down inside the terminal control.<br>
	 * <br>
	 * <strong>Important:</strong> the event fires for every mouse down, even outside the text region.
	 * @param terminalText a read-only view of the current terminal text
	 * @param button see {@link org.eclipse.swt.events.MouseEvent#button} for the meaning of the button values
	 * @param stateMask see {@link org.eclipse.swt.events.MouseEvent#stateMask} for the meaning of the values
	 */
	default void mouseDown(ITerminalTextDataReadOnly terminalText, int line, int column, int button, int stateMask) {
		// do nothing by default so that implementors only need to implement methods they care about
	}

	/**
	 * Invoked when a mouse button is released inside the terminal control.<br>
	 * <br>
	 * <strong>Important:</strong> the event fires for every mouse up, even outside the text region.
	 * @param terminalText a read-only view of the current terminal text
	 * @param button see {@link org.eclipse.swt.events.MouseEvent#button} for the meaning of the button values
	 * @param stateMask see {@link org.eclipse.swt.events.MouseEvent#stateMask} for the meaning of the values
	 */
	default void mouseUp(ITerminalTextDataReadOnly terminalText, int line, int column, int button, int stateMask) {
		// do nothing by default so that implementors only need to implement methods they care about
	}

}
