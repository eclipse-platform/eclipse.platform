/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.internal.ui.editor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.ant.internal.ui.editor.text.XMLTextHover;
import org.eclipse.ant.internal.ui.model.AntElementNode;
import org.eclipse.ant.internal.ui.model.AntModel;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;

public class OccurrencesFinder {
    
	private AntModel fAntModel;
	private AntEditor fEditor;
	private int fOffset;
	private IDocument fDocument;
	
	public OccurrencesFinder(AntEditor editor, AntModel model, IDocument document, int offset) {
		fAntModel= model;
		fEditor= editor;
		fDocument= document;
		fOffset= offset;
	}
	
	public List perform() {
		if (fOffset == 0 || fAntModel == null) {
			return null;
		}
		
		AntElementNode container= fAntModel.getNode(fOffset, false);
		if (container == null) {
			return null;
		}
		IRegion region= XMLTextHover.getRegion(fEditor.getViewer(), fOffset);
		if (region != null) {
			if (!container.isRegionPotentialReference(region)) {
				return null;
			}
		}
		AntElementNode node;
		if (container.isFromDeclaration(region)) {
			node= container;
		} else {
			Object potentialNode= fEditor.findTarget(region);
			if (!(potentialNode instanceof AntElementNode)) {
				return null;
			} 
			node= (AntElementNode) potentialNode;
		}
		String occurrencesIdentifier= node.getOccurrencesIdentifier();
		if (occurrencesIdentifier == null) {
			return null;
		}
		List nodes= new ArrayList(1);
		nodes.add(fAntModel.getProjectNode());
        List usages= new ArrayList();
        usages.add(node);
		scanNodesForOccurrences(nodes, usages, occurrencesIdentifier);
		String identifier;
		try {
			identifier= fDocument.get(region.getOffset(), region.getLength());
		} catch (BadLocationException e) {
			return null;
		}
        int length= identifier.length();
        if (length == 0) {
            return null;
        }
		List positions= new ArrayList(usages.size());
		for (Iterator each= usages.iterator(); each.hasNext();) {
			AntElementNode currentNode= (AntElementNode)each.next();
			List offsets= currentNode.computeIdentifierOffsets(identifier);
			if (offsets != null) {
				for (int i = 0; i < offsets.size(); i++) {
					positions.add(new Position(((Integer)offsets.get(i)).intValue(), length));
				}
			}
		}
	
		return positions;
	}
	
	private void scanNodesForOccurrences(List nodes, List usages, String identifier) {
		Iterator iter= nodes.iterator();
		while (iter.hasNext()) {
			AntElementNode node = (AntElementNode) iter.next();
			if (!usages.contains(node) && node.containsOccurrence(identifier)) {
				usages.add(node);	
			}
			if (node.hasChildren()) {
				scanNodesForOccurrences(node.getChildNodes(), usages, identifier);
			}
		}
	}
}