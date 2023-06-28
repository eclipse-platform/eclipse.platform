/*******************************************************************************
 * Copyright (c) 2005, 2013 IBM Corporation and others.
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
 *     Bjorn Freeman-Benson - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.examples.ui.pda.editor;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.debug.examples.core.pda.model.PDADebugTarget;
import org.eclipse.debug.examples.core.pda.model.PDAStackFrame;
import org.eclipse.debug.examples.core.pda.model.PDAThread;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextViewer;


/**
 * Produces debug hover for the PDA debugger.
 */
public class TextHover implements ITextHover {

	@Override
	public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {
		String varName = null;
		try {
			varName = textViewer.getDocument().get(hoverRegion.getOffset(), hoverRegion.getLength());
		} catch (BadLocationException e) {
			return null;
		}
		if (varName.startsWith("$") && varName.length() > 1) { //$NON-NLS-1$
			varName = varName.substring(1);
		}

		PDAStackFrame frame = null;
		IAdaptable debugContext = DebugUITools.getDebugContext();
		if (debugContext instanceof PDAStackFrame) {
			frame = (PDAStackFrame) debugContext;
		} else if (debugContext instanceof PDAThread) {
			PDAThread thread = (PDAThread) debugContext;
			try {
				frame = (PDAStackFrame) thread.getTopStackFrame();
			} catch (DebugException e) {
				return null;
			}
		} else if (debugContext instanceof PDADebugTarget) {
			PDADebugTarget target = (PDADebugTarget) debugContext;
			try {
				IThread[] threads = target.getThreads();
				if (threads.length > 0) {
					frame = (PDAStackFrame) threads[0].getTopStackFrame();
				}
			} catch (DebugException e) {
				return null;
			}
		}
		if (frame != null) {
			try {
				IVariable[] variables = frame.getVariables();
				for (int i = 0; i < variables.length; i++) {
					IVariable variable = variables[i];
					if (variable.getName().equals(varName)) {
						return varName + " = " + variable.getValue().getValueString(); //$NON-NLS-1$
					}
				}
			} catch (DebugException e) {
			}
		}
		return null;
	}

	@Override
	public IRegion getHoverRegion(ITextViewer textViewer, int offset) {
		return WordFinder.findWord(textViewer.getDocument(), offset);
	}

}
