/*******************************************************************************
 * Copyright (c) 2004, 2013 IBM Corporation and others.
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

package org.eclipse.ant.internal.ui.model;

import org.eclipse.ant.internal.ui.AntUIPlugin;
import org.eclipse.ant.internal.ui.preferences.AntEditorPreferenceConstants;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.Region;

public class AntModelProblem extends Region implements IProblem {

	public static final int NO_PROBLEM = -1;
	public static final int SEVERITY_WARNING = 0;
	public static final int SEVERITY_ERROR = 1;
	public static final int SEVERITY_FATAL_ERROR = 2;

	private final String fMessage;
	private final String fEscapedMessage;
	private final int fSeverity;
	private int fAdjustedLength = -1;
	private int fLineNumber = -1;

	public AntModelProblem(String message, int severity, int offset, int length, int lineNumber) {
		super(offset, length);
		fMessage = message;
		fEscapedMessage = getEscaped(message);
		fSeverity = severity;
		fLineNumber = lineNumber;
	}

	@Override
	public String getMessage() {
		return fEscapedMessage;
	}

	@Override
	public boolean isError() {
		return fSeverity == SEVERITY_ERROR || fSeverity == SEVERITY_FATAL_ERROR;
	}

	@Override
	public boolean isWarning() {
		return fSeverity == SEVERITY_WARNING;
	}

	@Override
	public int getLength() {
		if (fAdjustedLength != -1) {
			return fAdjustedLength;
		}
		return super.getLength();
	}

	/**
	 * Sets the length for this problem.
	 */
	public void setLength(int adjustedLength) {
		fAdjustedLength = adjustedLength;
	}

	@Override
	public int getLineNumber() {
		return fLineNumber;
	}

	private void appendEscapedChar(StringBuffer buffer, char c) {
		String replacement = getReplacement(c);
		if (replacement != null) {
			buffer.append(replacement);
		} else {
			buffer.append(c);
		}
	}

	private String getEscaped(String s) {
		StringBuffer result = new StringBuffer(s.length() + 10);
		for (int i = 0; i < s.length(); ++i) {
			appendEscapedChar(result, s.charAt(i));
		}
		return result.toString();
	}

	private String getReplacement(char c) {
		// Encode special characters into the equivalent character references.
		// Ensures that error messages that include special characters do not get
		// incorrectly represented as HTML in the text hover (bug 56258)
		switch (c) {
			case '<':
				return "&lt;"; //$NON-NLS-1$
			case '>':
				return "&gt;"; //$NON-NLS-1$
			case '"':
				return "&quot;"; //$NON-NLS-1$
			case '&':
				return "&amp;"; //$NON-NLS-1$
			default:
				break;
		}
		return null;
	}

	@Override
	public String getUnmodifiedMessage() {
		return fMessage;
	}

	public static int getSeverity(String preferenceKey) {
		IPreferenceStore store = AntUIPlugin.getDefault().getPreferenceStore();
		String severityLevel = store.getString(preferenceKey);
		if (severityLevel.length() == 0 || severityLevel.equals(AntEditorPreferenceConstants.BUILDFILE_ERROR)) {
			return SEVERITY_ERROR;
		} else if (severityLevel.equals(AntEditorPreferenceConstants.BUILDFILE_WARNING)) {
			return SEVERITY_WARNING;
		} else {
			return NO_PROBLEM;
		}
	}

	@Override
	public boolean equals(Object o) {
		boolean equal = super.equals(o);
		if (equal) {
			return ((AntModelProblem) o).getUnmodifiedMessage().equals(getUnmodifiedMessage());
		}
		return false;
	}
}
