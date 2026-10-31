/*******************************************************************************
 * Copyright (c) 2012-2022 Mihai Nita and others
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.ui.internal.console.ansi.participants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.DefaultPositionUpdater;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IPositionUpdater;
import org.eclipse.jface.text.Position;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.LineStyleEvent;
import org.eclipse.swt.custom.LineStyleListener;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GlyphMetrics;
import org.eclipse.ui.internal.console.ansi.AnsiConsoleUtils;
import org.eclipse.ui.internal.console.ansi.preferences.AnsiConsolePreferenceUtils;
import org.eclipse.ui.internal.console.StyleRangeUtil;
import org.eclipse.ui.internal.console.ansi.utils.AnsiConsoleAttributes;
import org.eclipse.ui.internal.console.ansi.utils.AnsiConsoleColorPalette;

public class AnsiConsoleStyleListener implements LineStyleListener, IPositionUpdater {
	private static final Font MONO_FONT = new Font(null, "Monospaced", 6, SWT.NORMAL); //$NON-NLS-1$

	private final DefaultPositionUpdater defaultPositionUpdater = new DefaultPositionUpdater(AnsiPosition.POSITION_NAME);
	private final HashMap<Integer, List<StyleRange>> offsetToStyleRangeCache = new HashMap<>();
	private IDocument document;
	private boolean documentEverScanned = false;
	private boolean isCdtBuildConsole = false;
	private int oldEventTime;

	private AnsiConsoleAttributes lastVisibleAttribute = new AnsiConsoleAttributes();

	public AnsiConsoleStyleListener(IDocument document) {
		setDocument(document);
		AnsiConsoleColorPalette.setPalette(AnsiConsolePreferenceUtils.getPreferredPalette());
	}

	private void setDocument(IDocument newDocument) {
		offsetToStyleRangeCache.clear();
		documentEverScanned = false;
		document = newDocument;
		isCdtBuildConsole = document.getClass().getSimpleName().equals("BuildConsoleDocument"); //$NON-NLS-1$
		document.addPositionCategory(AnsiPosition.POSITION_NAME);
		if (!hasPositionUpdater(this)) {
			document.addPositionUpdater(this);
		}
	}

	public boolean hasPositionUpdater(IPositionUpdater updater) {
		for (IPositionUpdater posUpdater : document.getPositionUpdaters()) {
			if (posUpdater == updater) {
				return true;
			}
		}
		return false;
	}

	private static void addRange(List<StyleRange> ranges, int start, int length,
			AnsiConsoleAttributes attributes, Color foreground, boolean isCode) {

		final StyleRange range = new StyleRange(start, length, foreground, null);
		AnsiConsoleAttributes.updateRangeStyle(range, attributes);
		if (isCode) {
			if (AnsiConsolePreferenceUtils.showAnsiEscapes()) {
				range.font = MONO_FONT; // Show the codes in small, monospaced font
			} else {
				range.metrics = new GlyphMetrics(0, 0, 0); // Hide the codes
			}
		}
		ranges.add(range);
	}

	@Override
	public void lineGetStyle(LineStyleEvent event) {
		if (event == null || event.lineText == null || event.lineText.length() == 0) {
			return;
		}
		if (document == null) {
			return;
		}
		if (!AnsiConsolePreferenceUtils.isAnsiConsoleEnabled()) {
			return;
		}

		/* If user selected another C/C++ project (for new versions of Eclipse) the console is reused, but not the document.
		 * Since AnsiConsolePageParticipant.getDocument iterates the listeners, it might be time consuming to do on each line.
		 * So we try to avoid the execution of this "if" as much as possible.
		 * When you "Build All" all events from several C++ projects have the same time-stamp, but still change the document.
		 * We want to still execute the if in that case, hence the test on isCdtBuildConsole.
		 * Might make it a bit slower, but at least it is only for the CDT BuildConsole.
		 */
		if ((oldEventTime != event.time || isCdtBuildConsole) && event.getSource() instanceof StyledText) {
			oldEventTime = event.time;
			StyledText text = (StyledText) event.getSource();
			IDocument newDocument = AnsiConsolePageParticipant.getDocument(text);
			if (newDocument != null && !newDocument.equals(document)) {
				setDocument(newDocument);
			}
		}

		final int eventOffset = event.lineOffset;
		final int eventLength = event.lineText.length();
		if (event.styles == null) { // It looks that in some cases this comes in as null
			event.styles = new StyleRange[0];
		}

		if (!documentEverScanned) {
			calculateDocumentAnsiPositions(document, 0, 0, null);
		}

		Position[] positions;
		try {
			positions = document.getPositions(AnsiPosition.POSITION_NAME);
		} catch (BadPositionCategoryException e) {
			return;
		}

		if (positions.length == 0) {
			return;
		}

		List<StyleRange> ranges = offsetToStyleRangeCache.get(eventOffset);
		if (ranges == null) {
			Color foregroundColor = AnsiConsolePreferenceUtils.getDebugConsoleFgColor();
			if (AnsiConsolePreferenceUtils.tryPreservingStdErrColor()) {
				Color errorColor = AnsiConsolePreferenceUtils.getDebugConsoleErrorColor();
				for (StyleRange range : event.styles) {
					if (errorColor.equals(range.foreground)) {
						foregroundColor = errorColor;
						break;
					}
				}
			}
			ranges = calculateAnsiRanges(positions, eventOffset, eventLength, foregroundColor);
			if (!ranges.isEmpty()) {
				offsetToStyleRangeCache.put(eventOffset, ranges);
			}
		}

		if (!ranges.isEmpty()) {
			// Copy the links that might already exist.
			List<StyleRange> merged = new ArrayList<>(ranges.size());
			for (StyleRange range : ranges) {
				merged.add((StyleRange) range.clone());
			}
			for (StyleRange range : event.styles) {
				if (range.foreground != null) {
					if (range.foreground.equals(AnsiConsolePreferenceUtils.getHyperlinkColor())) {
						StyleRangeUtil.overrideStyleRange(merged, range);
					}
					// We preserve the C/C++ build console colors (all except regular output)
					if (isCdtBuildConsole && !range.foreground.equals(AnsiConsolePreferenceUtils.getCdtOutputStreamColor())) {
						StyleRangeUtil.overrideStyleRange(merged, range);
					}
				}
			}
			// A hyperlink (or other overriding) range may span escape sequences that
			// are part of the matched text. Re-apply the zero-width metrics so those
			// escape sequences stay hidden instead of showing up as raw text.
			for (StyleRange range : ranges) {
				if (range.metrics != null) {
					StyleRangeUtil.overrideStyleRange(merged, (StyleRange) range.clone());
				}
			}
			event.styles = merged.toArray(new StyleRange[0]);
		}
	}

	private List<StyleRange> calculateAnsiRanges(Position[] positions, int eventOffset, int eventLength, Color foregroundColor) {
		final List<StyleRange> ranges = new ArrayList<>();
		AnsiConsoleAttributes prevAttr = lastVisibleAttribute;
		int prevPos = eventOffset;

		for (Position position : positions) {
			AnsiPosition apos = (AnsiPosition) position;
			if (apos.getOffset() > eventOffset + eventLength) { // we passed the end of line, stop searching
				break;
			}
			if (apos.overlapsWith(eventOffset, eventLength)) {
				if (apos.offset != prevPos) {
					addRange(ranges, prevPos, apos.offset - prevPos, prevAttr, foregroundColor, false);
				}
				addRange(ranges, apos.offset, apos.length, apos.attributes, foregroundColor, true);
				prevPos = apos.offset + apos.length;
			}
			if (apos.attributes != null) {
				// Attributes can be null for non \e[..m escapes, for example \e[K
				// Those kind of escape sequences don't affect the attributes.
				prevAttr = apos.attributes;
			}
		}
		addRange(ranges, prevPos, eventOffset + eventLength - prevPos, prevAttr, foregroundColor, false);
		return ranges;
	}

	private static List<AnsiPosition> findPositions(int offset, String currentText) {
		final List<AnsiPosition> result = new ArrayList<>();
		final Matcher matcher = AnsiConsoleUtils.ESCAPE_SEQUENCE_REGEX_TXT.matcher(currentText);
		while (matcher.find()) {
			int start = matcher.start();
			String group = matcher.group();
			result.add(new AnsiPosition(start + offset, group));
		}
		return result;
	}

	/**
	 * We scan newly appended text, or the full document the first time.
	 *
	 * Although in a general an IDocument can also replace text, or insert text in the middle (or beginning)
	 * that would complicate things a lot.
	 * But this is a console output, so I don't expect such a thing to happen.
	 * We only expect to append (at the end), or remove from the beginning (when the doc size > console buffer size)
	 *   offset: 0, length: 0, "something" => Appended at beginning of doc (the doc was empty)
	 *   offset:42, length: 0, "something" => Appended at end of doc. 42 was doc size before append.
	 *   offset:0, length: 100, ""         => Removed the first 100 characters (replace with "" == remove)
	 *
	 * @param eventDocument The document
	 * @param offset        Where was the text added. 0 => might mean remove, or the first content added
	 * @param length        The length of the text replaced. 0 if append, != if removed (from the beginning)
	 * @param text          For insert / append, this is the new text. For delete, empty string.
	 */
	private void calculateDocumentAnsiPositions(IDocument eventDocument, int offset, int length, String text) {
		String txt = (text != null) ? text : eventDocument.get();

		if (length != 0) { // This is the length of the text replaced. If not zero then this is not append, is replace.
			return;
		}
		// First time or the appended text is at the end (so it is not inserted text).
		if (documentEverScanned && offset + txt.length() != eventDocument.getLength()) {
			return;
		}
		documentEverScanned = true;
		try {
			List<AnsiPosition> newPos = findPositions(offset, txt);
			for (AnsiPosition apos : newPos) {
				eventDocument.addPosition(AnsiPosition.POSITION_NAME, apos);
			}
		} catch (BadPositionCategoryException | BadLocationException e) {
			// ignored
		}
	}

	@Override
	public void update(DocumentEvent event) {
		// Make sure we don't do anything if disabled
		if (!AnsiConsolePreferenceUtils.isAnsiConsoleEnabled()) {
			return;
		}

		final IDocument eventDocument = event.getDocument();

		final int offset = event.getOffset();
		final int length = event.getLength();
		final String text = event.getText();
		try {
			if (offset == 0 && length != 0) { // This removes the beginning. We scan to find and save the last style.
				for (Position pos : eventDocument.getPositions(AnsiPosition.POSITION_NAME)) {
					if (pos.offset >= length) {
						break;
					}
					AnsiConsoleAttributes attributes = ((AnsiPosition) pos).attributes;
					if (attributes != null) {
						lastVisibleAttribute = attributes;
					}
				}
			}
			defaultPositionUpdater.update(event);
			// This will only do something on new text (appended)
			calculateDocumentAnsiPositions(eventDocument, offset, length, text);
			// Would probably be possible to re-calculate things. But let's measure first (premature optimization and all that :-)
			offsetToStyleRangeCache.clear();
		} catch (BadPositionCategoryException e) {
			// ignored
		}
	}
}
