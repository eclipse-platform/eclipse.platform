/*******************************************************************************
 * Copyright (c) 2004, 2013 John-Mason P. Shackelford and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     John-Mason P. Shackelford - initial API and implementation
 *     IBM Corporation - bug fixes
 *******************************************************************************/
package org.eclipse.ant.internal.ui.editor.formatter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.ant.internal.core.IAntCoreConstants;
import org.eclipse.ant.internal.ui.editor.templates.AntContext;
import org.eclipse.ant.internal.ui.editor.text.AntDocumentSetupParticipant;
import org.eclipse.ant.internal.ui.editor.text.AntEditorPartitionScanner;
import org.eclipse.ant.internal.ui.model.AntElementNode;
import org.eclipse.ant.internal.ui.model.AntProjectNode;
import org.eclipse.ant.internal.ui.model.IAntModel;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.DefaultPositionUpdater;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.formatter.MultiPassContentFormatter;
import org.eclipse.jface.text.templates.TemplateBuffer;
import org.eclipse.jface.text.templates.TemplateVariable;

/**
 * Utility class for using the ant code formatter in contexts where an IDocument containing the text to format is not readily available.
 */
public class XmlFormatter {

	private static final String POS_CATEGORY = "tempAntFormatterCategory"; //$NON-NLS-1$

	/**
	 * Format the text using the ant code formatter.
	 *
	 * @param text
	 *            The text to format. Must be a non-null value.
	 * @param prefs
	 *            Preferences to use for this format operation. If null, the preferences currently set in the plug-in's preferences store are used.
	 * @return The formatted text.
	 */
	public static String format(String text, FormattingPreferences prefs) {

		return format(text, prefs, -1);
	}

	private static String format(String text, FormattingPreferences prefs, int indent) {
		Assert.isNotNull(text);

		FormattingPreferences applyPrefs;
		if (prefs == null) {
			applyPrefs = new FormattingPreferences();
		} else {
			applyPrefs = prefs;
		}

		IDocument doc = new Document();
		doc.set(text);
		new AntDocumentSetupParticipant().setup(doc);

		format(applyPrefs, doc, indent);

		return doc.get();
	}

	private static void format(FormattingPreferences prefs, IDocument doc, int indent) {
		MultiPassContentFormatter formatter = new MultiPassContentFormatter(AntDocumentSetupParticipant.ANT_PARTITIONING, IDocument.DEFAULT_CONTENT_TYPE);

		formatter.setMasterStrategy(new XmlDocumentFormattingStrategy(prefs, indent));
		formatter.setSlaveStrategy(new XmlElementFormattingStrategy(prefs), AntEditorPartitionScanner.XML_TAG);
		formatter.format(doc, new Region(0, doc.getLength()));
	}

	/**
	 * Format the text using the ant code formatter using the preferences settings in the plug-in preference store.
	 *
	 * @param text
	 *            The text to format. Must be a non-null value.
	 * @return The formatted text.
	 */
	public static String format(String text) {
		return format(text, null);
	}

	public static void format(TemplateBuffer templateBuffer, AntContext antContext, FormattingPreferences prefs) {
		String templateString = templateBuffer.getString();
		IDocument fullDocument = new Document(antContext.getDocument().get());

		int completionOffset = antContext.getCompletionOffset();
		try {
			// trim any starting whitespace
			IRegion lineRegion = fullDocument.getLineInformationOfOffset(completionOffset);
			String lineString = fullDocument.get(lineRegion.getOffset(), lineRegion.getLength());
			lineString = trimBegin(lineString);
			fullDocument.replace(lineRegion.getOffset(), lineRegion.getLength(), lineString);
		}
		catch (BadLocationException e1) {
			return;
		}
		TemplateVariable[] variables = templateBuffer.getVariables();
		int[] offsets = variablesToOffsets(variables, completionOffset);

		IDocument origTemplateDoc = new Document(fullDocument.get());
		try {
			origTemplateDoc.replace(completionOffset, antContext.getCompletionLength(), templateString);
		}
		catch (BadLocationException e) {
			return; // don't format if the document has changed
		}

		IDocument templateDocument = createDocument(origTemplateDoc.get(), createPositions(offsets));

		String leadingText = getLeadingText(fullDocument, antContext.getAntModel(), completionOffset);
		String newTemplateString = leadingText + templateString;
		int indent = XmlDocumentFormatter.computeIndent(leadingText, prefs.getTabWidth());

		newTemplateString = format(newTemplateString, prefs, indent);

		try {
			templateDocument.replace(completionOffset, templateString.length(), newTemplateString);
		}
		catch (BadLocationException e) {
			return;
		}
		Position[] positions = null;
		try {
			positions = templateDocument.getPositions(POS_CATEGORY);
		}
		catch (BadPositionCategoryException e2) {
			// do nothing
		}
		// offsetsToVariables(offsets, variables, completionOffset);
		positionsToVariables(positions, variables, completionOffset);
		templateBuffer.setContent(newTemplateString, variables);
	}

	private static void positionsToVariables(Position[] positions, TemplateVariable[] variables, int start) {
		for (int i = 0; i != variables.length; i++) {
			TemplateVariable variable = variables[i];

			int[] offsets = new int[variable.getOffsets().length];
			for (int j = 0; j != offsets.length; j++) {
				offsets[j] = positions[j].getOffset() - start;
			}

			variable.setOffsets(offsets);
		}
	}

	private static Document createDocument(String string, Position[] positions) throws IllegalArgumentException {
		Document doc = new Document(string);
		try {
			if (positions != null) {

				doc.addPositionCategory(POS_CATEGORY);
				doc.addPositionUpdater(new DefaultPositionUpdater(POS_CATEGORY) {
					@Override
					protected boolean notDeleted() {
						if (fOffset < fPosition.offset && (fPosition.offset + fPosition.length < fOffset + fLength)) {
							fPosition.offset = fOffset + fLength; // deleted positions: set to end of remove
							return false;
						}
						return true;
					}
				});
				for (Position position : positions) {
					try {
						doc.addPosition(POS_CATEGORY, position);
					}
					catch (BadLocationException e) {
						throw new IllegalArgumentException("Position outside of string. offset: " + position.offset + ", length: " //$NON-NLS-1$//$NON-NLS-2$
								+ position.length + ", string size: " + string.length(), e); //$NON-NLS-1$
					}
				}
			}
		}
		catch (BadPositionCategoryException cannotHappen) {
			// can not happen: category is correctly set up
		}
		return doc;
	}

	public static String trimBegin(String toBeTrimmed) {

		int i = 0;
		while ((i != toBeTrimmed.length()) && Character.isWhitespace(toBeTrimmed.charAt(i))) {
			i++;
		}

		return toBeTrimmed.substring(i);
	}

	private static int[] variablesToOffsets(TemplateVariable[] variables, int start) {
		List<Integer> list = new ArrayList<>();
		for (int i = 0; i != variables.length; i++) {
			int[] offsets = variables[i].getOffsets();
			for (int j = 0; j != offsets.length; j++) {
				list.add(Integer.valueOf(offsets[j]));
			}
		}

		int[] offsets = new int[list.size()];
		for (int i = 0; i != offsets.length; i++) {
			offsets[i] = list.get(i).intValue() + start;
		}

		Arrays.sort(offsets);

		return offsets;
	}

	/**
	 * Returns the indentation level at the position of code completion.
	 */
	private static String getLeadingText(IDocument document, IAntModel model, int completionOffset) {
		AntProjectNode project = model.getProjectNode(false);
		if (project == null) {
			return IAntCoreConstants.EMPTY_STRING;
		}
		AntElementNode node = project.getNode(completionOffset);// - fAccumulatedChange);
		if (node == null) {
			return IAntCoreConstants.EMPTY_STRING;
		}

		StringBuilder buf = new StringBuilder();
		buf.append(XmlDocumentFormatter.getLeadingWhitespace(node.getOffset(), document));
		buf.append(XmlDocumentFormatter.createIndent());
		return buf.toString();
	}

	private static Position[] createPositions(int[] positions) {
		Position[] p = null;

		if (positions != null) {
			p = new Position[positions.length];
			for (int i = 0; i < positions.length; i++) {
				p[i] = new Position(positions[i], 0);
			}
		}
		return p;
	}
}
