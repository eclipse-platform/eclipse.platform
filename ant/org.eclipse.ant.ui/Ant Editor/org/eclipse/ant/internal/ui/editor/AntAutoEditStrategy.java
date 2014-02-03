/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
 
package org.eclipse.ant.internal.ui.editor;
import org.eclipse.ant.internal.ui.AntUIPlugin;
import org.eclipse.ant.internal.ui.editor.formatter.XmlDocumentFormatter;
import org.eclipse.ant.internal.ui.model.AntElementNode;
import org.eclipse.ant.internal.ui.model.AntModel;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DefaultIndentLineAutoEditStrategy;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;

/**
 * Auto edit strategy for Ant build files
 * Current does special indenting.
 */
public class AntAutoEditStrategy extends DefaultIndentLineAutoEditStrategy {
	
	private AntModel fModel;
	private int fAccumulatedChange= 0;
	
	public AntAutoEditStrategy(AntModel model) {
		fModel= model;
	}
	
	/**
	 * Sets the indentation based on the Ant element node that contains the offset
	 * of the document command.
	 *
	 * @param d the document to work on
	 * @param c the command to deal with
	 */
	private synchronized void autoIndentAfterNewLine(IDocument d, DocumentCommand c) {
		
		if (c.offset == -1 || d.getLength() == 0 || fModel.getProjectNode(false) == null) {
			return;
		}
		
		int position= (c.offset == d.getLength() ? c.offset  - 1 : c.offset);
		AntElementNode node= fModel.getProjectNode(false).getNode(position - fAccumulatedChange);
		if (node == null) {
			return;
		}
		
		try {
			StringBuffer correct= XmlDocumentFormatter.getLeadingWhitespace(node.getOffset(), d);
			if (!nextNodeIsEndTag(c.offset, d)) {
				correct.append(XmlDocumentFormatter.createIndent());
			}
			StringBuffer buf= new StringBuffer(c.text);
			buf.append(correct);
			fAccumulatedChange+= buf.length();
			
			int line= d.getLineOfOffset(position);
			IRegion reg= d.getLineInformation(line);
			int lineEnd= reg.getOffset() + reg.getLength();
			int contentStart= findEndOfWhiteSpace(d, c.offset, lineEnd);
			
			c.length=  Math.max(contentStart - c.offset, 0);
			c.caretOffset= c.offset + buf.length();
			c.shiftsCaret= false;
			c.text= buf.toString();
	
		} catch (BadLocationException e) {
			AntUIPlugin.log(e);
		}
	}
	
	private boolean nextNodeIsEndTag(int offset, IDocument document) {
		if (offset + 1 > document.getLength()) {
			return false;
		}
		try {
			IRegion lineRegion= document.getLineInformationOfOffset(offset);
			offset= findEndOfWhiteSpace(document, offset, lineRegion.getOffset() + lineRegion.getLength());
			String nextChars= document.get(offset, 2).trim();
			if ("</".equals(nextChars) || "/>".equals(nextChars)) { //$NON-NLS-1$ //$NON-NLS-2$
				return true;
			}
		} catch (BadLocationException e) {
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.IAutoEditStrategy#customizeDocumentCommand(org.eclipse.jface.text.IDocument, org.eclipse.jface.text.DocumentCommand)
	 */
	public void customizeDocumentCommand(IDocument d, DocumentCommand c) {
		
		if (c.length == 0 && c.text != null && isLineDelimiter(d, c.text)) {
			autoIndentAfterNewLine(d, c);
		} else if (c.text.length() > 1) {
			smartPaste(d, c);
		}
	}
	
	 private boolean isLineDelimiter(IDocument document, String text) {
		String[] delimiters= document.getLegalLineDelimiters();
		if (delimiters != null)
			return TextUtilities.equals(delimiters, text) > -1;
		return false;
	}
	
	public synchronized void reconciled() {
		fAccumulatedChange= 0;
	}
	
	private void smartPaste(IDocument document, DocumentCommand command) {
		try {
			if (command.offset == -1 || document.getLength() == 0 || fModel.getProjectNode(false) == null) {
				return;
			}
			String origChange= command.text;
			int position= (command.offset == document.getLength() ? command.offset  - 1 : command.offset);
			AntElementNode node= fModel.getProjectNode(false).getNode(position - fAccumulatedChange);
			if (node == null) {
				return;
			}
			
			// eat any WS before the insertion to the beginning of the line
			int firstLine= 1; // don't format the first line if it has other content before it
			IRegion line= document.getLineInformationOfOffset(command.offset);
			String notSelected= document.get(line.getOffset(), command.offset - line.getOffset());
			if (notSelected.trim().length() == 0) {
				command.length += notSelected.length();
				command.offset= line.getOffset();
				firstLine= 0;
			}
			
			// handle the indentation computation inside a temporary document
			Document temp= new Document(command.text);
			
			// indent the first and second line
			// compute the relative indentation difference from the second line
			// (as the first might be partially selected) and use the value to
			// indent all other lines.
			boolean isIndentDetected= false;
			StringBuffer addition= new StringBuffer();
			int insertLength= 0;
			int lines= temp.getNumberOfLines();
			for (int l= firstLine; l < lines; l++) { // we don't change the number of lines while adding indents
				
				IRegion r= temp.getLineInformation(l);
				int lineOffset= r.getOffset();
				int lineLength= r.getLength();
				
				if (lineLength == 0) { // don't modify empty lines
					continue;
				}
				
				if (!isIndentDetected){
					
					// indent the first pasted line
					StringBuffer current= XmlDocumentFormatter.getLeadingWhitespace(lineOffset, temp);
					StringBuffer correct= XmlDocumentFormatter.getLeadingWhitespace(node.getOffset(), document);
					correct.append(XmlDocumentFormatter.createIndent());
					
					insertLength= subtractIndent(correct, current, addition);
					isIndentDetected= true;
				}
				
				// relatively indent all pasted lines 
				if (insertLength > 0) {
					addIndent(temp, l, addition);
				} else if (insertLength < 0) {
					cutIndent(temp, l, -insertLength);
				}	
			}
			
			// modify the command
			if (!origChange.equals(temp.get())) {
				fAccumulatedChange+=  temp.getLength();
				command.text= temp.get();
			}
			
		} catch (BadLocationException e) {
			AntUIPlugin.log(e);
		}
	}
	
	/**
	 * Indents line <code>line</code> in <code>document</code> with <code>indent</code>.
	 * Leaves leading comment signs alone.
	 * 
	 * @param document the document
	 * @param line the line
	 * @param indent the indentation to insert
	 * @throws BadLocationException on concurrent document modification
	 */
	private void addIndent(Document document, int line, CharSequence indent) throws BadLocationException {
		IRegion region= document.getLineInformation(line);
		int insert= region.getOffset();
		
		// insert indent
		document.replace(insert, 0, indent.toString());
	}
	
	/**
	 * Cuts the visual equivalent of <code>toDelete</code> characters out of the
	 * indentation of line <code>line</code> in <code>document</code>.
	 * 
	 * @param document the document
	 * @param line the line
	 * @param toDelete the number of space equivalents to delete.
	 * @throws BadLocationException on concurrent document modification
	 */
	private void cutIndent(Document document, int line, int toDelete) throws BadLocationException {
		IRegion region= document.getLineInformation(line);
		int from= region.getOffset();
		int endOffset= region.getOffset() + region.getLength();
		
		int to= from;
		while (toDelete > 0 && to < endOffset) {
			char ch= document.getChar(to);
			if (!Character.isWhitespace(ch))
				break;
			toDelete -= computeVisualLength(ch);
			if (toDelete >= 0) {
				to++;
			} else {
				break;
			}
		}
		
		document.replace(from, to - from, null);
	}
	
	/**
	 * Returns the visual length of a given character taking into
	 * account the visual tabulator length.
	 * 
	 * @param ch the character to measure
	 * @return the visual length of <code>ch</code>
	 */
	private int computeVisualLength(char ch) {
		if (ch == '\t') {
			return getVisualTabLengthPreference();
		} 
			
		return 1;
	}
	
	/**
	 * Returns the visual length of a given <code>CharSequence</code> taking into
	 * account the visual tabulator length.
	 * 
	 * @param seq the string to measure
	 * @return the visual length of <code>seq</code>
	 */
	private int computeVisualLength(CharSequence seq) {
		int size= 0;
		int tablen= getVisualTabLengthPreference();
		
		for (int i= 0; i < seq.length(); i++) {
			char ch= seq.charAt(i);
			if (ch == '\t') {
				size += tablen - size % tablen;
			} else {
				size++;
			}
		}
		return size;
	}
	
	/**
	 * Computes the difference of two indentations and returns the difference in
	 * length of current and correct. If the return value is positive, <code>addition</code>
	 * is initialized with a substring of that length of <code>correct</code>.
	 * 
	 * @param correct the correct indentation
	 * @param current the current indentation (might contain non-whitespace)
	 * @param difference a string buffer - if the return value is positive, it will be cleared and set to the substring of <code>current</code> of that length
	 * @return the difference in length of <code>correct</code> and <code>current</code> 
	 */
	private int subtractIndent(CharSequence correct, CharSequence current, StringBuffer difference) {
		int c1= computeVisualLength(correct);
		int c2= computeVisualLength(current);
		int diff= c1 - c2;
		if (diff <= 0) {
			return diff;
		}
		
		difference.setLength(0);
		int len= 0, i= 0;
		while (len < diff) {
			char c= correct.charAt(i++);
			difference.append(c);
			len += computeVisualLength(c);
		}
		
		return diff;
	}
	
	/**
	 * The preference setting for the visual tabulator display.
	 *  
	 * @return the number of spaces displayed for a tabulator in the editor
	 */
	private int getVisualTabLengthPreference() {
		return AntUIPlugin.getDefault().getCombinedPreferenceStore().getInt(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_TAB_WIDTH);
	}
}
