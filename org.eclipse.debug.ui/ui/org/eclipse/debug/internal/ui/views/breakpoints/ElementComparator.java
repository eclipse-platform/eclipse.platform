/*****************************************************************
 * Copyright (c) 2009, 2016 Texas Instruments and others
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Patrick Chuong (Texas Instruments) - Initial API and implementation (Bug 238956)
 *     Pawel Piech (Windriver) - Ongoing bug fixes and enhancements (Bug 311457)
 *     IBM Corporation - bug fixing
 *****************************************************************/
package org.eclipse.debug.internal.ui.views.breakpoints;

import java.util.Comparator;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.ILineBreakpoint;
import org.eclipse.debug.internal.core.IInternalDebugCoreConstants;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.internal.ui.breakpoints.provisional.IBreakpointContainer;
import org.eclipse.debug.internal.ui.breakpoints.provisional.OtherBreakpointCategory;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;
import org.eclipse.debug.internal.ui.views.DebugModelPresentationContext;

/**
 * Breakpoint element comparator.
 *
 * @since 3.6
 */
public class ElementComparator implements Comparator<Object> {
	final private static String SPACE = " "; //$NON-NLS-1$

	protected DebugModelPresentationContext fContext;

	public ElementComparator(IPresentationContext context) {
		if (context instanceof DebugModelPresentationContext) {
			fContext = (DebugModelPresentationContext) context;
		}
	}

	@Override
	public int compare(Object arg0, Object arg1) {
		IBreakpoint bp0 = (IBreakpoint)DebugPlugin.getAdapter(arg0, IBreakpoint.class);
		IBreakpoint bp1 = (IBreakpoint)DebugPlugin.getAdapter(arg1, IBreakpoint.class);
		if (bp0 != null && bp1 != null) {
			return doCompare(bp0, bp1);
		} else if (arg0 instanceof IBreakpointContainer && arg1 instanceof IBreakpointContainer) {
			return doCompare((IBreakpointContainer) arg0, (IBreakpointContainer) arg1);
		} else {
			return -1; // just return -1 if the two objects are not IBreakpoint type
		}
	}

	/**
	 * Compares two breakpoint containers.
	 *
	 * @param c1
	 * @param c2
	 * @return
	 */
	private int doCompare(IBreakpointContainer c1, IBreakpointContainer c2) {
		// The "Other" breakpoint category should be listed last.
		// (Bug 311457).
		if (c1.getCategory() instanceof OtherBreakpointCategory) {
			if (c2.getCategory() instanceof OtherBreakpointCategory) {
				return 0;
			}
			return 1;
		} else if (c2.getCategory() instanceof OtherBreakpointCategory) {
			return -1;
		}

		// Rest of categories should be listed alphabetically.
		if (fContext != null) {
			String name1 = fContext.getModelPresentation().getText(c1);
			String name2 = fContext.getModelPresentation().getText(c2);

			return name1.compareTo(name2);
		}

		return -1;
	}

	/**
	 * Compares two breakpoints.
	 *
	 * @param b1
	 * @param b2
	 * @return
	 */
	private int doCompare(IBreakpoint b1, IBreakpoint b2) {
		int sortingOrder = DebugUIPlugin.getDefault().getPreferenceStore().getInt(IInternalDebugUIConstants.PREF_BREAKPOINT_SORTING_ORDER);
		IMarker marker1 = b1.getMarker();
		IMarker marker2 = b2.getMarker();
		if (sortingOrder == IInternalDebugUIConstants.BREAKPOINT_SORTING_ORDER_CREATION_TIME) {
			// The most relevant/ new ones to be shown on top
			try {
				long b1CreationTime = marker1.getCreationTime();
				long b2CreationTime = marker2.getCreationTime();
				if (b1CreationTime > b2CreationTime) {
					return -1;
				} else if (b1CreationTime < b2CreationTime) {
					return 1;
				}
			} catch (CoreException e) {
				e.printStackTrace();
			}

		}
		String text1 = IInternalDebugCoreConstants.EMPTY_STRING;
		String text2 = IInternalDebugCoreConstants.EMPTY_STRING;

		text1 += b1.getModelIdentifier();
		text2 += b2.getModelIdentifier();


		try {
			if (marker1.exists() && marker2.exists()) {
				text1 += SPACE + marker1.getType();
				text2 += SPACE + marker2.getType();
			}
		} catch (CoreException e) {
			DebugUIPlugin.log(e);
		}

		int result = text1.compareTo(text2);
		if (result != 0) {
			return result;
		}

		// model and type are the same
		if (fContext != null) {
			String name1 = fContext.getModelPresentation().getText(b1);
			String name2 = fContext.getModelPresentation().getText(b2);

			boolean lineBreakpoint = false;
			try {
				lineBreakpoint = marker1.isSubtypeOf(IBreakpoint.LINE_BREAKPOINT_MARKER);
			} catch (CoreException ce) {
			}
			if (lineBreakpoint) {
				return compareLineBreakpoints(b1, b2, name1,name2);
			}

			return name1.compareTo(name2);
		}

		return result;
	}

	/**
	 * Compares two line breakpoints.
	 *
	 * @param b1
	 * @param b2
	 * @param name1
	 * @param name2
	 * @return
	 */
	private int compareLineBreakpoints(IBreakpoint b1, IBreakpoint b2, String name1, String name2) {
		int colon1 = name1.indexOf(':');
		if (colon1 != -1) {
			int colon2 = name2.indexOf(':');
			if (colon2 != -1) {
				String upToColon1 = name1.substring(0, colon1);
				if (name2.startsWith(upToColon1)) {
					int l1 = 0;
					int l2 = 0;
					try {
						l1 = ((ILineBreakpoint)b1).getLineNumber();
					} catch (CoreException e) {
						DebugUIPlugin.log(e);
					}
					try {
						l2 = ((ILineBreakpoint)b2).getLineNumber();
					} catch (CoreException e) {
						DebugUIPlugin.log(e);
					}
					return l1 - l2;
				}
			}
		}
		return name1.compareTo(name2);
	}

}
