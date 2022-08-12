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
package org.eclipse.compare.structuremergeviewer;

import org.eclipse.compare.ITypedElement;
import org.eclipse.swt.graphics.Image;

/**
 * Interface for objects used as input to a two-way or three-way compare viewer.
 * It defines API for accessing the three sides for the compare,
 * and a name and image which is used when displaying the three way input
 * in the UI, for example, in a title bar.
 * <p>
 * Note: at most two sides of an {@code ICompareInput} can be {@code null},
 * (as it is normal for additions or deletions) but not all three.
 * <p>
 * {@code ICompareInput} provides methods for registering
 * {@link ICompareInputChangeListener}s
 * that get informed if one (or more)
 * of the three sides of an {@code ICompareInput} object changes its value.
 * <p>
 * For example when accepting an incoming addition
 * the (non-{@code null}) left side of an {@code ICompareInput}
 * is copied to the right side by means of method {@code copy}.
 * This should trigger a call to {@link ICompareInputChangeListener#compareInputChanged} of registered
 * {@link ICompareInputChangeListener}s.
 * <p>
 * Clients can implement this interface, or use the convenience implementation
 * {@link DiffNode}.
 *
 * @see StructureDiffViewer
 * @see org.eclipse.compare.contentmergeviewer.ContentMergeViewer
 * @see DiffNode
 */
public interface ICompareInput {
	/**
	 * Returns name of input.
	 * This name is displayed when this input is shown in a viewer.
	 * In many cases this name is the name of one of the non-{@code null} sides or a combination
	 * thereof.
	 *
	 * @return name of input
	 */
	String getName();

	/**
	 * Returns an image representing this input.
	 * This image is typically displayed when this input is shown in a viewer.
	 * In many cases this image is the image of one of the non-{@code null} sides.
	 *
	 * @return image representing this input, or {@code null} if no icon should be shown
	 */
	Image getImage();

	/**
	 * Returns the kind of difference between the three sides ancestor, left and right.
	 * This field is only meaningful if the {@code ICompareInput} is the result of another
	 * compare. In this case it is used together with {@link #getImage()} to compose an icon
	 * which reflects the kind of difference between the two or three elements.
	 *
	 * @return kind of difference (see {@link Differencer})
	 */
	int getKind();

	/**
	 * Returns the ancestor side of this input.
	 * Returns {@code null} if this input has no ancestor
	 * or in the two-way compare case.
	 *
	 * @return the ancestor of this input, or {@code null}
	 */
	ITypedElement getAncestor();

	/**
	 * Returns the left side of this input.
	 * Returns {@code null} if there is no left side (deletion or addition).
	 *
	 * @return the left side of this input, or {@code null}
	 */
	ITypedElement getLeft();

	/**
	 * Returns the right side of this input.
	 * Returns {@code null} if there is no right side (deletion or addition).
	 *
	 * @return the right side of this input, or {@code null}
	 */
	ITypedElement getRight();

	/**
	 * Registers the given listener for notification.
	 * If the identical listener is already registered the method has no effect.
	 *
	 * @param listener the listener to register for changes of this input
	 */
	void addCompareInputChangeListener(ICompareInputChangeListener listener);

	/**
	 * Unregisters the given listener.
	 * If the identical listener is not registered the method has no effect.
	 *
	 * @param listener the listener to unregister
	 */
	void removeCompareInputChangeListener(ICompareInputChangeListener listener);

	/**
	 * Copy one side (source) to the other side (destination) depending on the
	 * value of {@code leftToRight}. This method is called from
	 * a merge viewer if a corresponding action ("take left" or "take right")
	 * has been pressed.
	 * <p>
	 * The implementation should handle the following cases:
	 * <ul>
	 * <li>
	 * if the source side is {@code null} the destination must be deleted,
	 * <li>
	 * if the destination is {@code null} the destination must be created
	 * and filled with the contents from the source,
	 * <li>
	 * if both sides are non-{@code null} the contents of source must be copied to destination.
	 * </ul>
	 * In addition the implementation should send out notification to the registered
	 * {@link ICompareInputChangeListener}.
	 *
	 * @param leftToRight if {@code true} the left side is copied to the right side.
	 * If {@code false} the right side is copied to the left side
	 */
	void copy(boolean leftToRight);
}

