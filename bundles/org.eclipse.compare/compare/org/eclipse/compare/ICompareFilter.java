package org.eclipse.compare;

import java.util.HashMap;

import org.eclipse.jface.text.IRegion;

/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

/**
 * A filter that can be applied during the comparison of documents that can be
 * used to customize the detection of text differences via the compareFilter
 * extension point. Filters are exposed as toggle actions in the compare viewer.
 * 
 * @noreference This interface is not intended to be referenced by clients
 * @since 3.6
 */
public interface ICompareFilter {

	/**
	 * Key for the <code>String</code> of the line of text being compared.
	 */
	public static final String THIS_LINE = "THIS_LINE"; //$NON-NLS-1$

	/**
	 * Key for the <code>Character</code> representing contributor of this line.
	 * Value is either 'A' for ancestor, 'L' for left, or 'R' for right.
	 */
	public static final String THIS_CONTRIBUTOR = "THIS_CONTRIBUTOR"; //$NON-NLS-1$

	/**
	 * Key for the <code>String</code> of the line of text this line is being
	 * compared to.
	 */
	public static final String OTHER_LINE = "OTHER_LINE"; //$NON-NLS-1$

	/**
	 * Key for the <code>Character</code> representing contributor of the other
	 * line. Value is either 'A' for ancestor, 'L' for left, or 'R' for right.
	 */
	public static final String OTHER_CONTRIBUTOR = "OTHER_CONTRIBUTOR"; //$NON-NLS-1$

	/**
	 * Forwards the current input objects of the compare
	 * 
	 * @param input
	 *            the merge viewer input
	 * @param ancestor
	 *            input into ancestor viewer
	 * @param left
	 *            input into left viewer
	 * @param right
	 *            input into right viewer
	 */
	public void setInput(Object input, Object ancestor, Object left,
			Object right);

	/**
	 * Identifies the regions of a line of text in a comparison that should be
	 * ignored for comparison purposes.
	 * 
	 * @param lineComparison
	 *            contains values for the keys <CODE>THIS_LINE</CODE>,
	 *            <CODE>THIS_CONTRIBUTOR</CODE>, <CODE>OTHER_LINE</CODE> and
	 *            <CODE>OTHER_CONTRIBUTOR</CODE>
	 * @return Regions of <code>THIS_LINE</code> to be ignored for comparison
	 *         purposes.
	 */
	public IRegion[] getFilteredRegions(HashMap lineComparison);

	/**
	 * Returns whether the filter should be enabled when first initialized
	 * 
	 * @return default enablement
	 */
	public boolean isEnabledInitially();

	/**
	 * Because the comparison routine may compare each line multiple times to
	 * other lines, the ignored regions may need to be calculated multiple times
	 * for the same line during a comparison. If the ignored regions for each
	 * line will be the same regardless of what line it is being compared to,
	 * returning <code>true</code> to this method will cause the ignored region
	 * calculations to be re-used and improve the performance of the comparison.
	 * 
	 * @return ignored region results can be cached
	 */
	public boolean canCacheFilteredRegions();
}
