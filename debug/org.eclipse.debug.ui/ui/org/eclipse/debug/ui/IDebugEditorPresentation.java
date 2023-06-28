/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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
package org.eclipse.debug.ui;

import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.ui.IEditorPart;

/**
 * A debug model presentation may implement this interface to override
 * standard editor positioning and annotations associated with
 * source code display for stack frames.
 *
 * @since 3.0
 */
public interface IDebugEditorPresentation {
	/**
	 * Positions and adds annotations to the given editor for the specified
	 * stack frame and returns whether any annotations were added. When
	 * <code>true</code> is returned, a call will be made to remove annotations
	 * when the source selection is cleared for the stack frame. When
	 * <code>false</code> is returned, the debugger will position and add
	 * standard annotations to the editor, and a corresponding call to remove
	 * annotations will not be made. This method is called when the debugger is
	 * has opened an editor to display source for the given stack frame.
	 *
	 * @param editorPart the editor the debugger has opened
	 * @param frame the stack frame for which the debugger is displaying
	 *  source
	 * @return <code>true</code> if annotations were added to the given editor part <code>false</code> otherwise
	 */
	boolean addAnnotations(IEditorPart editorPart, IStackFrame frame);

	/**
	 * Removes any debug related annotations from the given editor.
	 * This method is called when the debugger clears the source selection
	 * in an editor opened by the debugger. For example, when a debug
	 * session is resumed or terminated.
	 *
	 * @param editorPart an editor that annotations were added to for
	 *  a stack frame
	 * @param thread the thread for which stack frame annotations were
	 *  added to the editor
	 */
	void removeAnnotations(IEditorPart editorPart, IThread thread);
}
