/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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
package org.eclipse.ant.internal.ui.editor.text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.ant.internal.ui.editor.AntEditorMessages;
import org.eclipse.jface.internal.text.html.HTMLPrinter;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationHover;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;

/**
 * Determines all markers for the given line and collects, concatenates, and formulates their messages.
 */
public class XMLAnnotationHover implements IAnnotationHover {

	/**
	 * Returns the distance to the ruler line.
	 */
	private int compareRulerLine(Position position, IDocument document, int line) {

		if (position.getOffset() > -1 && position.getLength() > -1) {
			try {
				int xmlAnnotationLine = document.getLineOfOffset(position.getOffset());
				if (line == xmlAnnotationLine) {
					return 1;
				}
				if (xmlAnnotationLine <= line && line <= document.getLineOfOffset(position.getOffset() + position.getLength())) {
					return 2;
				}
			}
			catch (BadLocationException x) {
				// do nothing
			}
		}

		return 0;
	}

	/**
	 * Returns one marker which includes the ruler's line of activity.
	 */
	private List<Annotation> getXMLAnnotationsForLine(ISourceViewer viewer, int line) {

		IDocument document = viewer.getDocument();
		IAnnotationModel model = viewer.getAnnotationModel();

		if (model == null) {
			return null;
		}

		List<Annotation> exact = new ArrayList<>();

		Iterator<Annotation> e = model.getAnnotationIterator();
		Map<Position, Object> messagesAtPosition = new HashMap<>();
		while (e.hasNext()) {
			Object o = e.next();
			if (o instanceof Annotation a) {
				Position position = model.getPosition(a);
				if (position == null) {
					continue;
				}

				if (isDuplicateXMLAnnotation(messagesAtPosition, position, a.getText())) {
					continue;
				}

				switch (compareRulerLine(position, document, line)) {
					case 1:
						exact.add(a);
						break;
					default:
						break;
				}
			}
		}

		return exact;
	}

	private boolean isDuplicateXMLAnnotation(Map<Position, Object> messagesAtPosition, Position position, String message) {
		if (messagesAtPosition.containsKey(position)) {
			if (message.equals(messagesAtPosition.get(position))) {
				return true;
			}

			if (messagesAtPosition.get(position) instanceof List) {
				@SuppressWarnings("unchecked")
				List<String> messages = ((List<String>) messagesAtPosition.get(position));
				if (messages.contains(message)) {
					return true;
				}
				messages.add(message);
			} else {
				ArrayList<Object> messages = new ArrayList<>();
				messages.add(messagesAtPosition.get(position));
				messages.add(message);
				messagesAtPosition.put(position, messages);
			}
		} else {
			messagesAtPosition.put(position, message);
		}
		return false;
	}

	@Override
	public String getHoverInfo(ISourceViewer sourceViewer, int lineNumber) {
		List<Annotation> xmlAnnotations = getXMLAnnotationsForLine(sourceViewer, lineNumber);
		if (xmlAnnotations != null) {

			if (xmlAnnotations.size() == 1) {

				// optimization
				Annotation xmlAnnotation = xmlAnnotations.get(0);
				String message = xmlAnnotation.getText();
				if (message != null && message.trim().length() > 0) {
					return formatSingleMessage(message);
				}

			} else {

				List<String> messages = new ArrayList<>(xmlAnnotations.size());
				Iterator<Annotation> e = xmlAnnotations.iterator();
				while (e.hasNext()) {
					Annotation xmlAnnotation = e.next();
					String message = xmlAnnotation.getText();
					if (message != null && message.trim().length() > 0) {
						messages.add(message.trim());
					}
				}

				if (messages.size() == 1) {
					return formatSingleMessage(messages.get(0));
				}

				if (messages.size() > 1) {
					return formatMultipleMessages(messages);
				}
			}
		}

		return null;
	}

	/*
	 * Formats a message as HTML text.
	 */
	private String formatSingleMessage(String message) {
		StringBuilder buffer = new StringBuilder();
		HTMLPrinter.addPageProlog(buffer);
		HTMLPrinter.addParagraph(buffer, HTMLPrinter.convertToHTMLContent(message));
		HTMLPrinter.addPageEpilog(buffer);
		return buffer.toString();
	}

	/*
	 * Formats several message as HTML text.
	 */
	private String formatMultipleMessages(List<String> messages) {
		StringBuilder buffer = new StringBuilder();
		HTMLPrinter.addPageProlog(buffer);
		HTMLPrinter.addParagraph(buffer, HTMLPrinter.convertToHTMLContent(AntEditorMessages.getString("AntAnnotationHover.multipleMarkersAtThisLine"))); //$NON-NLS-1$

		HTMLPrinter.startBulletList(buffer);
		Iterator<String> e = messages.iterator();
		while (e.hasNext()) {
			HTMLPrinter.addBullet(buffer, HTMLPrinter.convertToHTMLContent(e.next()));
		}
		HTMLPrinter.endBulletList(buffer);

		HTMLPrinter.addPageEpilog(buffer);
		return buffer.toString();
	}
}
