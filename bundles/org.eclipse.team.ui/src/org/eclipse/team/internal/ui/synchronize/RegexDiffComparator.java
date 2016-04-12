/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.synchronize;

import java.util.regex.Pattern;

import org.eclipse.compare.internal.DocLineComparator;
import org.eclipse.compare.rangedifferencer.RangeDifference;
import org.eclipse.jface.text.*;

/**
 * Compute differences between local and remote contents and checks if all match
 * the given regex pattern. If there is at least one diff whose either left or
 * right side don't match the pattern
 * <code>{@link #compareRangeDifferences(RangeDifference[], IDocument, IDocument)}</code>
 * returns <code>false</code>.
 */
public class RegexDiffComparator extends RangeDifferenceComparator {

	private Pattern pattern;

	public RegexDiffComparator(Pattern pattern, boolean ignoreWhitespace) {
		super(ignoreWhitespace);
		this.pattern = pattern;
	}

	@Override
	protected boolean compareRangeDifferences(RangeDifference[] ranges,
			IDocument lDoc, IDocument rDoc) {
		try {
			for (int i = 0; i < ranges.length; i++) {
				RangeDifference diff = ranges[i];
				if (diff.kind() == RangeDifference.NOCHANGE)
					continue;

				DocLineComparator sleft = new DocLineComparator(lDoc, null,
						shouldIgnoreWhitespace());
				DocLineComparator sright = new DocLineComparator(rDoc, null,
						shouldIgnoreWhitespace());

				IRegion lRegion = lDoc.getLineInformation(diff.leftStart());
				int leftEnd = sleft.getTokenStart(diff.leftStart()
						+ diff.leftLength());
				String left = lDoc.get(lRegion.getOffset(),
						leftEnd - lRegion.getOffset());
				IRegion rRegion = rDoc.getLineInformation(diff.rightStart());
				int rightEnd = sright.getTokenStart(diff.rightStart()
						+ diff.rightLength());
				String right = rDoc.get(rRegion.getOffset(),
						rightEnd - rRegion.getOffset());

				boolean m1 = pattern.matcher(left).matches();
				boolean m2 = pattern.matcher(right).matches();

				if (!m1 && !m2)
					// it's false that all diffs match the pattern
					return false;
			}
		} catch (BadLocationException e) {
			// ignore
		}
		return true;
	}
}