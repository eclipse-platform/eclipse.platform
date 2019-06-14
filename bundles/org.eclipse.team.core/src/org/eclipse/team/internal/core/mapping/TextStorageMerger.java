/*******************************************************************************
 * Copyright (c) 2006, 2017 IBM Corporation and others.
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
package org.eclipse.team.internal.core.mapping;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import org.eclipse.compare.rangedifferencer.RangeDifference;
import org.eclipse.compare.rangedifferencer.RangeDifferencer;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.team.core.mapping.IStorageMerger;
import org.eclipse.team.internal.core.Messages;
import org.eclipse.team.internal.core.TeamPlugin;

public class TextStorageMerger implements IStorageMerger {

	@Override
	public IStatus merge(OutputStream output, String outputEncoding,
			IStorage ancestor, IStorage target, IStorage other,
			IProgressMonitor monitor) throws CoreException {

		LineComparator a, t, o;

		try {
			a= LineComparator.create(ancestor, outputEncoding);
			t= LineComparator.create(target, outputEncoding);
			o= LineComparator.create(other,outputEncoding);
		} catch (UnsupportedEncodingException e) {
			throw new CoreException (new Status(IStatus.ERROR, TeamPlugin.ID, UNSUPPORTED_ENCODING, Messages.TextAutoMerge_inputEncodingError, e));
		} catch (IOException e) {
			throw new CoreException (new Status(IStatus.ERROR, TeamPlugin.ID, INTERNAL_ERROR, e.getMessage(), e));
		}

		try {
			boolean firstLine = true;
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
						if (!firstLine)
							output.write(lineSeparator.getBytes(outputEncoding));
						output.write(s.getBytes(outputEncoding));
						firstLine = false;
					}
					break;

				case RangeDifference.LEFT:
					for (int j= rd.leftStart(); j < rd.leftEnd(); j++) {
						String s= t.getLine(j);
						if (!firstLine)
							output.write(lineSeparator.getBytes(outputEncoding));
						output.write(s.getBytes(outputEncoding));
						firstLine = false;
					}
					break;

				case RangeDifference.CONFLICT:
					return new Status(IStatus.WARNING, TeamPlugin.ID, CONFLICT, Messages.TextAutoMerge_conflict, null);

				default:
					break;
				}
			}

		} catch (UnsupportedEncodingException e) {
			throw new CoreException (new Status(IStatus.ERROR, TeamPlugin.ID, UNSUPPORTED_ENCODING, Messages.TextAutoMerge_outputEncodingError, e));
		} catch (IOException e) {
			return new Status(IStatus.ERROR, TeamPlugin.ID, INTERNAL_ERROR, Messages.TextAutoMerge_outputIOError, e);
		}

		return Status.OK_STATUS;
	}

	@Override
	public boolean canMergeWithoutAncestor() {
		return false;
	}

}
