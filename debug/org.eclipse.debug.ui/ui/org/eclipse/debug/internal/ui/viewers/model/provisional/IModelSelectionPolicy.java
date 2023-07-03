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
 *******************************************************************************/
package org.eclipse.debug.internal.ui.viewers.model.provisional;

import org.eclipse.jface.viewers.ISelection;

/**
 * Resolves selection conflicts within a model. When
 * a selection exists in a viewer, and a client asks
 * to set another selection, the selection policy for
 * that model is asked to determine whether the new
 * selection should override the previous selection.
 * When a selection is from a different model, the
 * original selection is maintained if its model selection
 * policy specifies the selection to be 'sticky'.
 * <p>
 * A selection policy is obtained by querying for the selection
 * policy factory for a given element.  The selection policy factory
 * will then be asked to create a model selection policy adapter.
 * The adapter represents selections from an instance of a model.
 * </p>
 *
 * @see IModelSelectionPolicyFactory
 * @since 3.2
 */
public interface IModelSelectionPolicy {

	/**
	 * Returns whether the given selection is contained in this model.
	 *
	 * @param selection Selection to check
	 * @param context Presentation context of the viewer.
	 * @return true if selection is contained in model.
	 */
	boolean contains(ISelection selection, IPresentationContext context);

	/**
	 * Returns whether the candidate selection overrides the
	 * existing selection. The policy is only asked about selections
	 * that it contains.
	 *
	 * @param existing Existing selection to check.
	 * @param candidate New proposed selection.
	 * @param context Presentation context of viewer.
	 * @return true if candidate selection should be set to viewer
	 */
	boolean overrides(ISelection existing, ISelection candidate, IPresentationContext context);

	/**
	 * Returns whether the given selection should be maintained in the
	 * face of a selection attempt from a different model.
	 *
	 * @param selection selection to test
	 * @param context Presentation context of viewer.
	 * @return true if selection is sticky
	 */
	boolean isSticky(ISelection selection, IPresentationContext context);

	/**
	 * Replaces an invalid selection.
	 * <p>
	 * This method is called if a model change picked up by a viewer
	 * results in an invalid selection. For instance if an element contained in
	 * the selection has been removed from the viewer, the selection policy associated
	 * with the invalid selection is free to specify a new selection. The default
	 * selection policy returns the <code>newSelection</code> as is (with missing
	 * elements removed). Model selection policies may implement a different strategy
	 * for picking a new selection when the old selection becomes invalid.
	 * </p>
	 *
	 * @param invalidSelection
	 *            the selection before the viewer was updated
	 * @param newSelection
	 *            the selection after the update, or <code>null</code> if none
	 * @return new selection or <code>null</code> if none
	 */
	ISelection replaceInvalidSelection(ISelection invalidSelection, ISelection newSelection);
}
