/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
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
 *     Freescale Semiconductor - Bug 293618, Breakpoints view sorts up to first colon only
 *******************************************************************************/
package org.eclipse.debug.internal.ui.views.breakpoints;


import java.text.DecimalFormat;
import java.text.ParsePosition;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.ILineBreakpoint;
import org.eclipse.debug.internal.core.IInternalDebugCoreConstants;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.jface.viewers.IBasicPropertyConstants;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;

/**
 * @since 3.3
 */
public class BreakpointsComparator extends ViewerComparator {
	@Override
	public boolean isSorterProperty(Object element,String propertyId) {
		return propertyId.equals(IBasicPropertyConstants.P_TEXT);
	}

	/**
	 * Returns a negative, zero, or positive number depending on whether
	 * the first element is less than, equal to, or greater than
	 * the second element.
	 * <p>
	 * Group breakpoints by debug model
	 * 	within debug model, group breakpoints by type
	 * 		within type groups, sort by line number (if applicable) and then
	 * 		alphabetically by label
	 *
	 * @param viewer the viewer
	 * @param e1 the first element
	 * @param e2 the second element
	 * @return a negative number if the first element is less than the
	 *  second element; the value <code>0</code> if the first element is
	 *  equal to the second element; and a positive number if the first
	 *  element is greater than the second element
	 */
	@Override
	public int compare(Viewer viewer, Object e1, Object e2) {
		if (!(e1 instanceof IBreakpoint)) {
			return super.compare(viewer, e1, e2);
		}

		IBreakpoint b1= (IBreakpoint)e1;
		IBreakpoint b2= (IBreakpoint)e2;
		String modelId1= b1.getModelIdentifier();
		String modelId2= b2.getModelIdentifier();
		int result= modelId1.compareTo(modelId2);
		if (result != 0) {
			return result;
		}
		String type1= IInternalDebugCoreConstants.EMPTY_STRING;
		String type2= IInternalDebugCoreConstants.EMPTY_STRING;
		IMarker marker1= b1.getMarker();
		if (!marker1.exists()) {
			return 0;
		}
		try {
			type1= marker1.getType();
		} catch (CoreException ce) {
			DebugUIPlugin.log(ce);
		}
		try {
			IMarker marker2= b2.getMarker();
			if (!marker2.exists()) {
				return 0;
			}
			type2= marker2.getType();
		} catch (CoreException e) {
			DebugUIPlugin.log(e);
		}

		result= type1.compareTo(type2);
		if (result != 0) {
			return result;
		}

		// model and type are the same
		ILabelProvider lprov = (ILabelProvider) ((StructuredViewer)viewer).getLabelProvider();
		String name1= lprov.getText(e1);
		String name2= lprov.getText(e2);

		result = numericalStringCompare(name1, name2);

		if (result != 0) {
			return result;
		}

		// Compare the line number for debug models which do not encode the line number into the label text.
		int l1 = 0;
		int l2 = 0;
		// Note: intentionally using line 0 if not a line breakpoint or if ILineBreakpoint.getLineNumber throws.
		if (b1 instanceof ILineBreakpoint) {
			try {
				l1 = ((ILineBreakpoint)b1).getLineNumber();
			} catch (CoreException e) {
				DebugUIPlugin.log(e);
			}
		}
		if (b2 instanceof ILineBreakpoint) {
			try {
				l2 = ((ILineBreakpoint)b2).getLineNumber();
			} catch (CoreException e) {
				DebugUIPlugin.log(e);
			}
		}
		if (l1 != l2) {
			result = l1 - l2;
		}
		return result;
	}

	/**
	 * Utility routine to order strings with respect to numerical values.
	 *
	 * E.g.
	 * <p><code>
	 * "0", "1", "9", "11"
	 * <p></code>
	 *
	 * Note that String.compareTo orders "11" before "9".
	 *
	 * The function also supports mixed numbers and values. It uses the string comparison except when both strings differ by a number only,
	 * in this case the numerical value is compared.
	 * E.g.
	 * <p><code>
	 * stringNumberCompareTo("a_01", "a_1") returns 0.
	 * <p></code>
	 * Note: For now no additional elements (spaces) are considered, for numbers only base 10 numbers are supported.
	 *
	 * @param n1 the first string to compare
	 * @param n2 the second string to compare
	 * @return
	 * 			< 0, negative - if n1 < n2
	 * 			== 0, zero - if n1 == n2 (with a a special comparison, not identical or equals)
	 * 			> 0, negative - if n1 > n2
	 */
	int numericalStringCompare(String n1, String n2) {
		int index1 = 0;
		int index2 = 0;
		int digitLen = 0; // Number of identical digits prior to the current index position.
		for (; index1 < n1.length() && index2 < n2.length(); ) {
			char c1 = n1.charAt(index1);
			char c2 = n2.charAt(index2);

			if (c1 != c2) {
				// Strings are different starting at index.
				// If both strings have a number at this location, compare it.
				boolean isDig1 = Character.isDigit(c1);
				boolean isDig2 = Character.isDigit(c2);

				if (isDig1 && isDig2 || digitLen > 0 && (isDig1 || isDig2)) {
					// Have 2 numbers if either the different characters are both digits or if there are common digits before the difference.
					DecimalFormat format = new DecimalFormat();
					ParsePosition p1 = new ParsePosition(index1 - digitLen);
					Number num1 = format.parse(n1, p1);
					ParsePosition p2 = new ParsePosition(index2 - digitLen);
					Number num2 = format.parse(n2, p2);
					if (num1 == null || num2 == null) {
						// Failed to parse number. Should not happen
						return c1 - c2;
					}
					int cmp;
					if (num1 instanceof Long && num2 instanceof Long) {
						cmp = ((Long)num1).compareTo((Long)num2);
					} else {
						cmp = Double.compare(num1.doubleValue(), num2.doubleValue());
					}
					if (cmp != 0) {
						return cmp;
					}
					// Parsed the same number, compare the remaining of the string
					index1 = p1.getIndex();
					index2 = p2.getIndex();
					digitLen = 0;
					continue;
				} else {
					return c1 - c2;
				}
			}
			if (Character.isDigit(c1)) {
				digitLen++;
			} else {
				digitLen = 0;
			}
			index1++;
			index2++;
		}
		// Same characters up to index1/index2. Return < 0 if remaining in n1 is shorter than remaining in n2.
		return (n1.length() - index1) - (n2.length() - index2);
	}
}