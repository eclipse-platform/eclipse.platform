/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
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
 *     vogella GmbH - Bug 287303 - [patch] Add Word Wrap action to Console View
 *     Paul Pazderski  - Bug 550621 - improved verification of user input
 *******************************************************************************/
package org.eclipse.ui.internal.console;

import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.MultiStringMatcher;
import org.eclipse.jface.text.MultiStringMatcher.Match;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.StyledTextContent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsoleDocumentPartitioner;
import org.eclipse.ui.console.IConsoleDocumentPartitionerExtension;
import org.eclipse.ui.console.IOConsole;
import org.eclipse.ui.console.IScrollLockStateProvider;
import org.eclipse.ui.console.TextConsole;
import org.eclipse.ui.console.TextConsoleViewer;

/**
 * Viewer used to display an {@link IOConsole}.
 *
 * @since 3.1
 */
public class IOConsoleViewer extends TextConsoleViewer {
	/**
	 * Will always scroll with output if value is true.
	 */
	private boolean fAutoScroll = true;

	/**
	 * Listener required for auto scroll.
	 */
	private IDocumentListener fAutoScrollListener;

	/**
	 * Matcher to find line delimiters which are used by current document.
	 * <code>null</code> if no document is set.
	 */
	private MultiStringMatcher lineDelimiterMatcher;

	/**
	 * Constructs a new viewer in the given parent for the specified console.
	 *
	 * @param parent  the containing composite
	 * @param console the IO console
	 */
	public IOConsoleViewer(Composite parent, TextConsole console) {
		super(parent, console);
	}

	/**
	 * Constructs a new viewer in the given parent for the specified console.
	 *
	 * @param parent                  the containing composite
	 * @param console                 the IO console
	 * @param scrollLockStateProvider the scroll lock state provider
	 * @since 3.6
	 */
	public IOConsoleViewer(Composite parent, TextConsole console, IScrollLockStateProvider scrollLockStateProvider) {
		super(parent, console, scrollLockStateProvider);
	}

	public boolean isAutoScroll() {
		return fAutoScroll;
	}

	public void setAutoScroll(boolean scroll) {
		fAutoScroll = scroll;
	}

	public boolean isWordWrap() {
		return getTextWidget().getWordWrap();
	}

	public void setWordWrap(boolean wordwrap) {
		getTextWidget().setWordWrap(wordwrap);
	}

	@Override
	protected void handleVerifyEvent(VerifyEvent e) {
		final IConsoleDocumentPartitioner partitioner = (IConsoleDocumentPartitioner) getDocument()
				.getDocumentPartitioner();
		if (partitioner == null) {
			e.doit = false;
			return;
		}
		final IConsoleDocumentPartitionerExtension partitionerExt = (IConsoleDocumentPartitionerExtension) partitioner;

		final StyledTextContent content = getTextWidget().getContent();
		final String eventText = e.text != null ? e.text : ""; //$NON-NLS-1$
		final Match newlineMatch = lineDelimiterMatcher != null ? lineDelimiterMatcher.indexOf(eventText, 0) : null;
		final IRegion eventRange = event2ModelRange(e);
		final int offset = eventRange.getOffset();
		final int length = eventRange.getLength();

		if (length > 0 && partitionerExt.containsReadOnly(offset, length)) {
			// If user tries to remove or replace text range containing read-only content we
			// modify the change to only remove the writable parts.
			e.doit = false;

			final ITypedRegion[] writableParts = partitionerExt.computeWritablePartitions(offset, length);
			// process text removes in reveres to not bother with changing offsets
			for (int i = writableParts.length - 1; i >= 0; i--) {
				final ITypedRegion writablePart = writableParts[i];
				int replaceOffset = writablePart.getOffset();
				int replaceLength = writablePart.getLength();

				// snap partitions to event range
				final int underflow = offset - writablePart.getOffset();
				if (underflow > 0) {
					replaceOffset += underflow;
					replaceLength -= underflow;
				}
				final int overflow = (replaceOffset + replaceLength) - (offset + length);
				if (overflow > 0) {
					replaceLength -= overflow;
				}

				content.replaceTextRange(replaceOffset, replaceLength, ""); //$NON-NLS-1$
			}

			// now add the users input if any
			if (eventText.length() > 0) {
				getTextWidget().replaceTextRange(offset, 0, eventText);
			}
		} else if (newlineMatch != null && offset != content.getCharCount()) {
			// If newline is entered within a line this viewer will not break that line and
			// instead pretend as if newline was entered at end of document.
			e.doit = false;

			if (newlineMatch.getOffset() > 0) {
				// insert text until newline with further verification
				// and newline plus trailing text without
				getTextWidget().replaceTextRange(offset, length, eventText.substring(0, newlineMatch.getOffset()));
				content.replaceTextRange(content.getCharCount(), 0,
						eventText.substring(newlineMatch.getOffset(), eventText.length()));
			} else {
				// inserted text starts with newline
				content.replaceTextRange(content.getCharCount(), 0, eventText);
			}

			getTextWidget().setCaretOffset(content.getCharCount());
			getTextWidget().showSelection();
		} else if (partitioner.isReadOnly(offset) && partitioner.isReadOnly(offset - 1)) {
			// If input is entered in read-only partition add it to the next writable
			// partition instead
			e.doit = false;

			final int insertOffset = partitionerExt.getNextOffsetByState(offset, true);
			content.replaceTextRange(insertOffset, 0, eventText);

			getTextWidget().setCaretOffset(insertOffset + eventText.length());
			getTextWidget().showSelection();
		} else {
			super.handleVerifyEvent(e);
		}
	}

	/**
	 * Makes the associated text widget uneditable.
	 */
	public void setReadOnly() {
		ConsolePlugin.getStandardDisplay().asyncExec(() -> {
			StyledText text = getTextWidget();
			if (text != null && !text.isDisposed()) {
				text.setEditable(false);
			}
		});
	}

	/**
	 * @return <code>false</code> if text is editable
	 */
	public boolean isReadOnly() {
		return !getTextWidget().getEditable();
	}

	@Override
	public void setDocument(IDocument document) {
		if (getDocument() != null) {
			getDocument().removeDocumentListener(getAutoScrollListener());
		}

		super.setDocument(document);

		lineDelimiterMatcher = null;
		if (document != null) {
			lineDelimiterMatcher = MultiStringMatcher.create(document.getLegalLineDelimiters());
			document.addDocumentListener(getAutoScrollListener());
		}
	}

	/**
	 * Must create listener dynamically since super constructor may call
	 * {@link #setDocument(IDocument)} before field initialization.
	 *
	 * @return document listener to perform auto scroll
	 */
	private IDocumentListener getAutoScrollListener() {
		if (fAutoScrollListener == null) {
			fAutoScrollListener = new IDocumentListener() {
				@Override
				public void documentAboutToBeChanged(DocumentEvent event) {
				}

				@Override
				public void documentChanged(DocumentEvent event) {
					if (fAutoScroll) {
						revealEndOfDocument();
					}
				}
			};
		}
		return fAutoScrollListener;
	}
}
