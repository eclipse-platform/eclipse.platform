/*******************************************************************************
 * Copyright (c) 2003, 2013 IBM Corporation and others.
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

package org.eclipse.ant.internal.ui.editor;

import org.eclipse.ant.internal.core.IAntCoreConstants;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension2;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

public class AntCompletionProposal implements ICompletionProposal, ICompletionProposalExtension2 {

	public static final int TAG_CLOSING_PROPOSAL = 0;
	public static final int TASK_PROPOSAL = 1;
	public static final int PROPERTY_PROPOSAL = 2;

	/** The string to be displayed in the completion proposal popup */
	private final String fDisplayString;
	/** The replacement string */
	private final String fReplacementString;
	/** The replacement offset */
	private final int fReplacementOffset;
	/** The replacement length */
	private int fReplacementLength;
	/** The cursor position after this proposal has been applied */
	private final int fCursorPosition;
	/** The image to be displayed in the completion proposal popup */
	private final Image fImage;
	/** The additional info of this proposal */
	private final String fAdditionalProposalInfo;

	private final int fType;

	/**
	 * Creates a new Ant completion proposal. All fields are initialized based on the provided information.
	 * 
	 * @param replacementString
	 *            the actual string to be inserted into the document
	 * @param replacementOffset
	 *            the offset of the text to be replaced
	 * @param replacementLength
	 *            the length of the text to be replaced
	 * @param cursorPosition
	 *            the position of the cursor following the insert relative to replacementOffset
	 * @param image
	 *            the image to display for this proposal
	 * @param displayString
	 *            the string to be displayed for the proposal
	 * @param additionalProposalInfo
	 *            the additional information associated with this proposal
	 * @param type
	 *            the type of this proposal
	 */
	public AntCompletionProposal(String replacementString, int replacementOffset, int replacementLength, int cursorPosition, Image image, String displayString, String additionalProposalInfo, int type) {
		fReplacementString = replacementString;
		fReplacementOffset = replacementOffset;
		fReplacementLength = replacementLength;
		fCursorPosition = cursorPosition;
		fImage = image;
		fDisplayString = displayString;
		fAdditionalProposalInfo = additionalProposalInfo;
		fType = type;
	}

	@Override
	public void apply(ITextViewer viewer, char trigger, int stateMask, int offset) {
		apply(viewer.getDocument());

	}

	@Override
	public void selected(ITextViewer viewer, boolean smartToggle) {
		// do nothing
	}

	@Override
	public void unselected(ITextViewer viewer) {
		// do nothing
	}

	@Override
	public boolean validate(IDocument document, int offset, DocumentEvent event) {
		String enteredText = IAntCoreConstants.EMPTY_STRING;
		try {
			enteredText = document.get(fReplacementOffset, offset - fReplacementOffset);
		}
		catch (BadLocationException e) {
			// do nothing
		}
		int enteredLength = enteredText.length();
		if (fType == TASK_PROPOSAL && enteredText.startsWith("<")) { //$NON-NLS-1$
			enteredText = enteredText.substring(1);
		} else if (fType == PROPERTY_PROPOSAL) {
			if (enteredText.startsWith("${")) { //$NON-NLS-1$
				enteredText = enteredText.substring(2);
			}
			if (enteredText.startsWith("$")) { //$NON-NLS-1$
				enteredText = enteredText.substring(1);
			}
		} else if (fType == TAG_CLOSING_PROPOSAL) {
			if (enteredText.startsWith("</")) { //$NON-NLS-1$
				enteredText = enteredText.substring(2);
			} else if (enteredText.startsWith("/")) { //$NON-NLS-1$
				try {
					if (document.getChar(fReplacementOffset - 1) == '<') {
						enteredText = enteredText.substring(1);
					}
				}
				catch (BadLocationException e) {
					// do nothing
				}
			} else if (enteredText.startsWith("<")) { //$NON-NLS-1$
				enteredText = enteredText.substring(1);
			}
		}
		boolean valid = fDisplayString.toLowerCase().startsWith(enteredText.toLowerCase());
		if (valid) {
			fReplacementLength = enteredLength;
		}
		return valid;
	}

	@Override
	public void apply(IDocument document) {
		try {
			document.replace(fReplacementOffset, fReplacementLength, fReplacementString);
		}
		catch (BadLocationException x) {
			// ignore
		}
	}

	@Override
	public Point getSelection(IDocument document) {
		return new Point(fReplacementOffset + fCursorPosition, 0);
	}

	@Override
	public String getAdditionalProposalInfo() {
		return fAdditionalProposalInfo;
	}

	@Override
	public String getDisplayString() {
		if (fDisplayString != null) {
			return fDisplayString;
		}
		return fReplacementString;
	}

	@Override
	public Image getImage() {
		return fImage;
	}

	@Override
	public IContextInformation getContextInformation() {
		return null;
	}

	@Override
	public String toString() {
		return getDisplayString();
	}

	/**
	 * @return Returns the type of the completion proposal
	 */
	public int getType() {
		return fType;
	}
}
