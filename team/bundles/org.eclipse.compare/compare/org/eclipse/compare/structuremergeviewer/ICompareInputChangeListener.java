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

/**
 * Listener that gets informed if one (or more)
 * of the three sides of an {@link ICompareInput} object changes its value.
 * <p>
 * For example when accepting an incoming addition
 * the (non-{@code null}) left side of an {@link ICompareInput}
 * is copied to the right side (which was {@code null}).
 * This triggers a call to {@link #compareInputChanged} of registered
 * {@code ICompareInputChangeListener}.
 * <p>
 * Note however, that listener are not informed if the content of one of the sides changes.
 * <p>
 * Clients may implement this interface. It is also implemented by viewers that take
 * an {@link ICompareInput} as input.
 * </p>
 */
public interface ICompareInputChangeListener {
	/**
	 * Called whenever the value (not the content) of one or more of the three sides
	 * of a {@link ICompareInput} has changed.
	 *
	 * @param source the {@link ICompareInput} that has changed
	 */
	void compareInputChanged(ICompareInput source);
}
