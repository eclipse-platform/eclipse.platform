/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.synchronize;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.compare.internal.DocLineComparator;
import org.eclipse.compare.internal.Utilities;
import org.eclipse.compare.rangedifferencer.RangeDifference;
import org.eclipse.compare.rangedifferencer.RangeDifferencer;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Region;
import org.eclipse.team.internal.core.subscribers.AbstractContentComparator;

/**
 * Compare differences between local and remote contents.
 * <p>
 * Subclass to specify a criterion for comparison.
 */
public abstract class RangeDifferenceComparator extends
		AbstractContentComparator {

	public RangeDifferenceComparator(boolean ignoreWhitespace) {
		super(ignoreWhitespace);
	}

	/**
	 * Return <code>true</code> if the provided differences match a criterion.
	 *
	 * @param ranges the differences found
	 * @param lDoc the left document
	 * @param rDoc the right document
	 * @return <code>true</code> if all differences match a criterion
	 */
	abstract protected boolean compareRangeDifferences(RangeDifference[] ranges,
			IDocument lDoc, IDocument rDoc);

	@Override
	protected boolean contentsEqual(IProgressMonitor monitor, InputStream is1,
			InputStream is2, boolean ignoreWhitespace) {
		try {
			final String left = Utilities.readString(is1, ResourcesPlugin.getEncoding());
			final String right = Utilities.readString(is2, ResourcesPlugin.getEncoding());
			return compareStrings(left, right, monitor);
		} catch (IOException e) {
			// ignore
		}
		return false;
	}

	private boolean compareStrings(String left, String right,
			IProgressMonitor monitor) {
		IDocument lDoc = new Document(left);
		IDocument rDoc = new Document(right);
		DocLineComparator sleft = new DocLineComparator(lDoc, new Region(0,
				lDoc.getLength()), shouldIgnoreWhitespace());
		DocLineComparator sright = new DocLineComparator(rDoc, new Region(0,
				rDoc.getLength()), shouldIgnoreWhitespace());
		final DocLineComparator sl = sleft, sr = sright;
		RangeDifference[] ranges = RangeDifferencer.findRanges(monitor, sl, sr);
		return compareRangeDifferences(ranges, lDoc, rDoc);
	}
}
