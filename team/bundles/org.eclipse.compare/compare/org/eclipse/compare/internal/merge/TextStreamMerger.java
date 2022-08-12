/*******************************************************************************
 * Copyright (c) 2003, 2011 IBM Corporation and others.
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
package org.eclipse.compare.internal.merge;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import org.eclipse.compare.CompareUI;
import org.eclipse.compare.IStreamMerger;
import org.eclipse.compare.rangedifferencer.RangeDifference;
import org.eclipse.compare.rangedifferencer.RangeDifferencer;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

/**
 * A simple merger for streams containing text lines.
 */
public class TextStreamMerger implements IStreamMerger {

	@Override
	public IStatus merge(OutputStream output, String outputEncoding, InputStream ancestor, String ancestorEncoding, InputStream target, String targetEncoding, InputStream other, String otherEncoding, IProgressMonitor monitor) {

		LineComparator a, t, o;

		try {
			a= new LineComparator(ancestor, ancestorEncoding);
			t= new LineComparator(target, targetEncoding);
			o= new LineComparator(other, otherEncoding);
		} catch (UnsupportedEncodingException e) {
			return new Status(IStatus.ERROR, CompareUI.PLUGIN_ID, 1, MergeMessages.TextAutoMerge_inputEncodingError, e);
		} catch (IOException e) {
			return new Status(IStatus.ERROR, CompareUI.PLUGIN_ID, 1, e.getMessage(), e);
		}

		try {
			String lineSeparator= System.getProperty("line.separator"); //$NON-NLS-1$
			if (lineSeparator == null)
				lineSeparator= "\n"; //$NON-NLS-1$

			RangeDifference[] diffs= RangeDifferencer.findRanges(monitor, a, t, o);

			for (RangeDifference rd : diffs) {
				switch (rd.kind()) {
				case RangeDifference.ANCESTOR: // pseudo conflict
				case RangeDifference.NOCHANGE:
				case RangeDifference.RIGHT:
					for (int j= rd.rightStart(); j < rd.rightEnd(); j++) {
						String s= o.getLine(j);
						output.write(s.getBytes(outputEncoding));
						output.write(lineSeparator.getBytes(outputEncoding));
					}
					break;

				case RangeDifference.LEFT:
					for (int j= rd.leftStart(); j < rd.leftEnd(); j++) {
						String s= t.getLine(j);
						output.write(s.getBytes(outputEncoding));
						output.write(lineSeparator.getBytes(outputEncoding));
					}
					break;

				case RangeDifference.CONFLICT:
					return new Status(IStatus.ERROR, CompareUI.PLUGIN_ID, CONFLICT, MergeMessages.TextAutoMerge_conflict, null);

				default:
					break;
				}
			}

		} catch (UnsupportedEncodingException e) {
			return new Status(IStatus.ERROR, CompareUI.PLUGIN_ID, 1, MergeMessages.TextAutoMerge_outputEncodingError, e);
		} catch (IOException e) {
			return new Status(IStatus.ERROR, CompareUI.PLUGIN_ID, 1, MergeMessages.TextAutoMerge_outputIOError, e);
		}

		return Status.OK_STATUS;
	}
}
