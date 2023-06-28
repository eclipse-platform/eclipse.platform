/*******************************************************************************
 * Copyright (c) 2005, 2011 IBM Corporation and others.
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
 *     Pawel Piech (Wind River) - Bug fixing
 *******************************************************************************/
package org.eclipse.debug.internal.ui.viewers.update;

import org.eclipse.debug.core.model.IDebugElement;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelSelectionPolicy;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;

/**
 * Default selection policy for the debug view.
 *
 * @since 3.2
 */
public class DefaultSelectionPolicy implements IModelSelectionPolicy {

	private IDebugElement fDebugElement;

	/**
	 * Constructs a new selection policy for the given debug
	 * element.
	 *
	 * @param element the backing debug element
	 */
	public DefaultSelectionPolicy(IDebugElement element) {
		fDebugElement = element;
	}

	@Override
	public boolean contains(ISelection selection, IPresentationContext context) {
		if (IDebugUIConstants.ID_DEBUG_VIEW.equals(context.getId())) {
			if (selection instanceof IStructuredSelection) {
				IStructuredSelection ss = (IStructuredSelection) selection;
				Object element = ss.getFirstElement();
				if (element instanceof IDebugElement) {
					IDebugElement debugElement = (IDebugElement) element;
					return fDebugElement.getDebugTarget().equals(debugElement.getDebugTarget());
				}
			}
		}
		return false;
	}

	@Override
	public boolean overrides(ISelection existing, ISelection candidate, IPresentationContext context) {
		if (IDebugUIConstants.ID_DEBUG_VIEW.equals(context.getId())) {
			if (existing instanceof IStructuredSelection && candidate instanceof IStructuredSelection) {
				IStructuredSelection ssExisting = (IStructuredSelection) existing;
				IStructuredSelection ssCandidate = (IStructuredSelection) candidate;
				return overrides(ssExisting.getFirstElement(), ssCandidate.getFirstElement());
			}
		}
		return true;
	}

	protected boolean overrides(Object existing, Object candidate) {
		if (existing == null) {
			return true;
		}
		if (existing.equals(candidate)) {
			return true;
		}
		if (existing instanceof IStackFrame && candidate instanceof IStackFrame) {
			IStackFrame curr = (IStackFrame) existing;
			IStackFrame next = (IStackFrame) candidate;
			return curr.getThread().equals(next.getThread()) || !isSticky(existing);
		}
		return !isSticky(existing);
	}

	@Override
	public boolean isSticky(ISelection selection, IPresentationContext context) {
		if (IDebugUIConstants.ID_DEBUG_VIEW.equals(context.getId())) {
			if (selection instanceof IStructuredSelection) {
				return isSticky(((IStructuredSelection)selection).getFirstElement());
			}
		}
		return false;
	}

	/**
	 * Returns if the selection should remain on the given selection
	 * @param element the element to check
	 * @return <code>true</code> if the selection should remain on the given element <code>false</code> otherwise
	 */
	protected boolean isSticky(Object element) {
		if (element instanceof IStackFrame) {
			IStackFrame frame = (IStackFrame) element;
			return frame.isSuspended() || frame.isStepping();
		}
		return false;
	}

	/**
	 * If an attempt is made to select an invalid element, it usually indicates that the
	 * currently selected element was removed from the model.  Instead of leaving the
	 * selection empty, attempt to select the parent element instead.
	 *
	 * @param selection the selection to replace
	 * @param newSelection the selection to use if the given selection is not an {@link ITreeSelection}
	 * @return the replaced selection or <code>newSelection</code> if the given selection is not an {@link ITreeSelection}
	 *
	 * @see org.eclipse.debug.internal.ui.viewers.model.provisional.IModelSelectionPolicy#replaceInvalidSelection(ISelection, ISelection)
	 */
	@Override
	public ISelection replaceInvalidSelection(ISelection selection, ISelection newSelection) {
		if (selection instanceof ITreeSelection) {
			TreePath[] paths = ((ITreeSelection)selection).getPaths();
			if (paths.length > 0 && paths[0].getSegmentCount() > 1) {
				return new TreeSelection(paths[0].getParentPath());
			}
		}
		return newSelection;
	}
}
