/*******************************************************************************
 * Copyright (c) 2004, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
	
	public static final int NO_PROBLEM= -1;
	public static final int SEVERITY_WARNING= 0;
	public static final int SEVERITY_ERROR= 1;
	public static final int SEVERITY_FATAL_ERROR= 2;
	
	private String fMessage;
	private String fEscapedMessage;
	private int fSeverity;
	private int fAdjustedLength= -1;
	private int fLineNumber= -1;
	
	public AntModelProblem(String message, int severity, int offset, int length, int lineNumber) {
		super(offset, length);
		fMessage= message;
		fEscapedMessage= getEscaped(message);
		fSeverity= severity;
		fLineNumber= lineNumber;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ant.internal.ui.editor.outline.IProblem#getMessage()
	 */
	public String getMessage() {
		return fEscapedMessage;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ant.internal.ui.editor.outline.IProblem#isError()
	 */
	public boolean isError() {
		return fSeverity == SEVERITY_ERROR || fSeverity == SEVERITY_FATAL_ERROR;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ant.internal.ui.editor.outline.IProblem#isWarning()
	 */
	public boolean isWarning() {
		return fSeverity == SEVERITY_WARNING;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.IRegion#getLength()
	 */
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
		fAdjustedLength= adjustedLength;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ant.internal.ui.editor.outline.IProblem#getLineNumber()
	 */
	public int getLineNumber() {
		return fLineNumber;
	}
	
	private void appendEscapedChar(StringBuffer buffer, char c) {
		String replacement= getReplacement(c);
		if (replacement != null) {
			buffer.append(replacement);
		} else {
			buffer.append(c);
		}
	}
	
	private String getEscaped(String s) {
		StringBuffer result= new StringBuffer(s.length() + 10);
		for (int i= 0; i < s.length(); ++i) {
			appendEscapedChar(result, s.charAt(i));
		}
		return result.toString();
	}
	
	private String getReplacement(char c) {
		// Encode special characters into the equivalent character references.
		// Ensures that error messages that include special characters do not get
		//incorrectly represented as HTML in the text hover (bug 56258)
		switch (c) {
			case '<' :
				return "&lt;"; //$NON-NLS-1$
			case '>' :
				return "&gt;"; //$NON-NLS-1$
			case '"' :
				return "&quot;"; //$NON-NLS-1$
			case '&' :
				return "&amp;"; //$NON-NLS-1$
		}
		return null;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ant.internal.ui.editor.outline.IProblem#getUnmodifiedMessage()
	 */
	public String getUnmodifiedMessage() {
		return fMessage;
	}
	
	public static int getSeverity(String preferenceKey) {
		IPreferenceStore store= AntUIPlugin.getDefault().getPreferenceStore();
		String severityLevel= store.getString(preferenceKey);
		if (severityLevel.length() == 0 || severityLevel.equals(AntEditorPreferenceConstants.BUILDFILE_ERROR)) {
			return SEVERITY_ERROR;
		} else if (severityLevel.equals(AntEditorPreferenceConstants.BUILDFILE_WARNING)) {
			return SEVERITY_WARNING;
		} else {
			return NO_PROBLEM;
		}
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object o) {
		boolean equal= super.equals(o);
		if (equal) {
			return ((AntModelProblem)o).getUnmodifiedMessage().equals(getUnmodifiedMessage());
		}
		return false;
	}
}
