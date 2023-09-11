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

import java.util.Iterator;
import java.util.StringTokenizer;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.AbstractFileSet;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.PatternSet;
import org.eclipse.ant.internal.core.IAntCoreConstants;
import org.eclipse.ant.internal.launching.debug.model.AntProperty;
import org.eclipse.ant.internal.launching.debug.model.AntStackFrame;
import org.eclipse.ant.internal.launching.debug.model.AntValue;
import org.eclipse.ant.internal.ui.editor.AntEditor;
import org.eclipse.ant.internal.ui.editor.AntEditorSourceViewerConfiguration;
import org.eclipse.ant.internal.ui.model.AntElementNode;
import org.eclipse.ant.internal.ui.model.AntModel;
import org.eclipse.ant.internal.ui.model.AntPropertyNode;
import org.eclipse.ant.internal.ui.model.IAntModel;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.internal.text.html.HTMLPrinter;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextHoverExtension;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.information.IInformationProviderExtension2;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.editors.text.EditorsUI;

public class XMLTextHover implements ITextHover, ITextHoverExtension, IInformationProviderExtension2 {

	private final AntEditor fEditor;

	public XMLTextHover(AntEditor editor) {
		super();
		fEditor = editor;
	}

	/*
	 * Formats a message as HTML text. Expects the message to already be properly escaped
	 */
	private String formatMessage(String message) {
		StringBuilder buffer = new StringBuilder();
		HTMLPrinter.addPageProlog(buffer);
		HTMLPrinter.addParagraph(buffer, message);
		HTMLPrinter.addPageEpilog(buffer);
		return buffer.toString();
	}

	/*
	 * Formats a message as HTML text.
	 */
	private String formatPathMessage(String[] list) {
		StringBuilder buffer = new StringBuilder();
		HTMLPrinter.addPageProlog(buffer);
		HTMLPrinter.addSmallHeader(buffer, AntEditorTextMessages.XMLTextHover_4);
		HTMLPrinter.startBulletList(buffer);
		for (String element : list) {
			HTMLPrinter.addBullet(buffer, element);
		}
		HTMLPrinter.endBulletList(buffer);
		HTMLPrinter.addPageEpilog(buffer);
		return buffer.toString();
	}

	@Override
	public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {

		if (!(textViewer instanceof ISourceViewer)) {
			return null;
		}

		ISourceViewer sourceViewer = (ISourceViewer) textViewer;
		IAnnotationModel model = sourceViewer.getAnnotationModel();

		if (model != null) {
			String message = getAnnotationModelHoverMessage(model, hoverRegion);
			if (message != null) {
				return message;
			}
		}

		AntModel antModel = fEditor.getAntModel();
		if (antModel == null) { // the ant model has not been created yet
			return null;
		}

		return getAntModelHoverMessage(antModel, hoverRegion, textViewer);

	}

	private String getAntModelHoverMessage(AntModel antModel, IRegion hoverRegion, ITextViewer textViewer) {
		try {
			IDocument document = textViewer.getDocument();
			int offset = hoverRegion.getOffset();
			int length = hoverRegion.getLength();
			String text = document.get(offset, length);
			String value;
			AntElementNode node = antModel.getNode(offset, false);
			if (document.get(offset - 2, 2).equals("${") || node instanceof AntPropertyNode) { //$NON-NLS-1$
				AntStackFrame frame = getFrame();
				if (frame != null) {// active Ant debug session
					AntProperty property = frame.findProperty(text);
					if (property != null) {
						return ((AntValue) property.getValue()).getValueString();
					}
				}
				value = antModel.getPropertyValue(text);
				if (value != null) {
					return formatMessage(HTMLPrinter.convertToHTMLContent(value));
				}
			}
			value = antModel.getTargetDescription(text);
			if (value != null) {
				return formatMessage(HTMLPrinter.convertToHTMLContent(value));
			}
			Object referencedObject = antModel.getReferenceObject(text);
			if (referencedObject != null) {
				if (referencedObject instanceof Path) {
					return formatPathMessage(((Path) referencedObject).list());
				} else if (referencedObject instanceof PatternSet) {
					return formatPatternSetMessage((PatternSet) referencedObject);
				} else if (referencedObject instanceof AbstractFileSet) {
					return formatFileSetMessage((AbstractFileSet) referencedObject);
				}
			}
		}
		catch (BadLocationException e) {
			// do nothing
		}
		catch (BuildException be) {
			return be.getMessage();
		}

		return null;
	}

	private String getAnnotationModelHoverMessage(IAnnotationModel model, IRegion hoverRegion) {
		Iterator<Annotation> e = model.getAnnotationIterator();
		while (e.hasNext()) {
			Annotation a = e.next();
			if (a instanceof XMLProblemAnnotation) {
				Position p = model.getPosition(a);
				if (p.overlapsWith(hoverRegion.getOffset(), hoverRegion.getLength())) {
					String msg = a.getText();
					if (msg != null && msg.trim().length() > 0) {
						return formatMessage(msg);
					}
				}
			}
		}
		return null;
	}

	private String formatFileSetMessage(AbstractFileSet set) {
		FileScanner fileScanner = new FileScanner();
		IAntModel antModel = fEditor.getAntModel();
		Project project = antModel.getProjectNode().getProject();
		set.setupDirectoryScanner(fileScanner, project);
		String[] excludedPatterns = fileScanner.getExcludesPatterns();
		String[] includesPatterns = fileScanner.getIncludePatterns();
		return formatSetMessage(includesPatterns, excludedPatterns);
	}

	private String formatPatternSetMessage(PatternSet set) {
		IAntModel antModel = fEditor.getAntModel();
		Project project = antModel.getProjectNode().getProject();
		String[] includes = set.getIncludePatterns(project);
		String[] excludes = set.getExcludePatterns(project);
		return formatSetMessage(includes, excludes);
	}

	private String formatSetMessage(String[] includes, String[] excludes) {
		StringBuilder buffer = new StringBuilder();
		HTMLPrinter.addPageProlog(buffer);
		if (includes != null && includes.length > 0) {
			HTMLPrinter.addSmallHeader(buffer, AntEditorTextMessages.XMLTextHover_5);
			for (String include : includes) {
				HTMLPrinter.addBullet(buffer, include);
			}
		}
		HTMLPrinter.addParagraph(buffer, IAntCoreConstants.EMPTY_STRING);
		HTMLPrinter.addParagraph(buffer, IAntCoreConstants.EMPTY_STRING);
		if (excludes != null && excludes.length > 0) {
			HTMLPrinter.addSmallHeader(buffer, AntEditorTextMessages.XMLTextHover_6);
			for (String exclude : excludes) {
				HTMLPrinter.addBullet(buffer, exclude);
			}
		}
		HTMLPrinter.addPageEpilog(buffer);
		return buffer.toString();
	}

	@Override
	public IRegion getHoverRegion(ITextViewer textViewer, int offset) {
		if (textViewer != null) {
			return getRegion(textViewer, offset);
		}
		return null;
	}

	public static IRegion getRegion(ITextViewer textViewer, int offset) {
		IDocument document = textViewer.getDocument();

		int start = -1;
		int end = -1;
		IRegion region = null;
		try {
			int pos = offset;
			char c;

			if (document.getChar(pos) == '"') {
				pos--;
			}
			while (pos >= 0) {
				c = document.getChar(pos);
				if (c != '.' && c != '-' && c != '/' && c != '\\' && c != ' ' && c != ')' && c != '(' && c != ':'
						&& !Character.isJavaIdentifierPart(c) && pos != offset)
					break;
				--pos;
			}

			start = pos;

			pos = offset;
			int length = document.getLength();

			while (pos < length) {
				c = document.getChar(pos);
				if (c != '.' && c != '-' && c != '/' && c != '\\' && c != ' ' && c != ')' && c != '(' && c != ':'
						&& !Character.isJavaIdentifierPart(c))
					break;
				if (c == '/' && (document.getLength() - 1) > (pos + 1) && document.getChar(pos + 1) == '>') {
					// e.g. <name/>
					break;
				}
				++pos;
			}

			end = pos;

		}
		catch (BadLocationException x) {
			// do nothing
		}

		if (start > -1 && end > -1) {
			if (start == offset && end == offset) {
				return new Region(offset, 0);
			} else if (start == offset) {
				return new Region(start, end - start);
			} else {
				try { // correct for spaces at beginning or end
					while (document.getChar(start + 1) == ' ') {
						start++;
					}
					while (document.getChar(end - 1) == ' ') {
						end--;
					}
				}
				catch (BadLocationException e) {
					// do nothing
				}
				region = new Region(start + 1, end - start - 1);
			}
		}

		if (region != null) {
			try {
				char c = document.getChar(region.getOffset() - 1);
				if (c == '"') {
					if (document.get(offset, region.getLength()).indexOf(',') != -1) {
						region = cleanRegionForNonProperty(offset, document, region);
					}
				} else if (c != '{') {
					region = cleanRegionForNonProperty(offset, document, region);
				}
			}
			catch (BadLocationException e) {
				// do nothing
			}
		}

		return region;
	}

	private static IRegion cleanRegionForNonProperty(int offset, IDocument document, IRegion region) throws BadLocationException {
		// do not allow spaces in region that is not a property
		IRegion r = region;
		String text = document.get(r.getOffset(), r.getLength());
		if (text.startsWith("/")) { //$NON-NLS-1$
			text = text.substring(1);
			r = new Region(r.getOffset() + 1, r.getLength() - 1);
		}
		StringTokenizer tokenizer = new StringTokenizer(text, " "); //$NON-NLS-1$
		if (tokenizer.countTokens() != 1) {
			while (tokenizer.hasMoreTokens()) {
				String token = tokenizer.nextToken();
				int index = text.indexOf(token);
				if (r.getOffset() + index <= offset && r.getOffset() + index + token.length() >= offset) {
					r = new Region(r.getOffset() + index, token.length());
					break;
				}
			}
		}

		return r;
	}

	@Override
	public IInformationControlCreator getHoverControlCreator() {
		return parent -> new DefaultInformationControl(parent, EditorsUI.getTooltipAffordanceString());
	}

	/**
	 * Returns the stack frame in which to search for properties, or <code>null</code> if none.
	 * 
	 * @return the stack frame in which to search for properties, or <code>null</code> if none
	 */
	private AntStackFrame getFrame() {
		IAdaptable adaptable = DebugUITools.getDebugContext();
		if (adaptable != null) {
			return adaptable.getAdapter(AntStackFrame.class);
		}
		return null;
	}

	/**
	 * @since 3.3
	 */
	@Override
	public IInformationControlCreator getInformationPresenterControlCreator() {
		return AntEditorSourceViewerConfiguration.getInformationPresenterControlCreator();
	}
}