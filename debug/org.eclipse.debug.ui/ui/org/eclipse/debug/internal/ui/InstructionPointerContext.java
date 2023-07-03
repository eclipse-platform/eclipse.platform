/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui;

import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Represents the context for a single instruction pointer.  This is a convenience class
 * used to store the four objects that comprise an instruction pointer 'context' so it
 * can be stored in collections.
 */
public class InstructionPointerContext {

	/**
	 * The thread this context belongs to.
	 */
	private IThread fThread;

	/**
	 * The debug target this context belongs to.
	 */
	private IDebugTarget fDebugTarget;

	/**
	 * The editor that the annotation is being displayed in
	 */
	private ITextEditor fEditor;

	/**
	 * The vertical ruler annotation for this context.
	 */
	private Annotation fAnnotation;

	public InstructionPointerContext(IDebugTarget target, IThread thread, ITextEditor editor, Annotation annotation) {
		fDebugTarget = target;
		fThread = thread;
		fEditor = editor;
		fAnnotation = annotation;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof InstructionPointerContext) {
			InstructionPointerContext otherContext = (InstructionPointerContext) other;
			if (getAnnotation().equals(otherContext.getAnnotation())){
				return getEditor().equals(otherContext.getEditor());
			}
		}
		return false;
	}

	@Override
	public int hashCode() {
		return getAnnotation().hashCode() + getEditor().hashCode();
	}

	/**
	 * @return the thread
	 */
	public IThread getThread() {
		return fThread;
	}

	/**
	 * @return the debug target
	 */
	public IDebugTarget getDebugTarget() {
		return fDebugTarget;
	}

	/**
	 * @return the editor
	 */
	public ITextEditor getEditor() {
		return fEditor;
	}

	/**
	 * @return the annotation
	 */
	public Annotation getAnnotation() {
		return fAnnotation;
	}

}
