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

package org.eclipse.ant.internal.ui.editor.text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.ant.internal.ui.editor.AntEditor;
import org.eclipse.ant.internal.ui.model.AntElementNode;
import org.eclipse.ant.internal.ui.model.AntModel;
import org.eclipse.ant.internal.ui.model.AntProjectNode;
import org.eclipse.ant.internal.ui.model.IAntElement;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.projection.ProjectionAnnotation;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;

public class AntFoldingStructureProvider {

	private final AntEditor fEditor;
	private IDocument fDocument;

	/**
	 * A mapping of the foldable position to the <code>AntElementNode</code> that represent that region
	 */
	private Map<Position, IAntElement> fPositionToElement = new HashMap<>();

	public AntFoldingStructureProvider(AntEditor editor) {
		fEditor = editor;
	}

	private void updateFoldingRegions(ProjectionAnnotationModel model, Set<Position> currentRegions) {
		Annotation[] deletions = computeDifferences(model, currentRegions);

		Map<Annotation, Position> additionsMap = new HashMap<>();
		for (Position position : currentRegions) {
			IAntElement node = fPositionToElement.get(position);
			additionsMap.put(new ProjectionAnnotation(node.collapseProjection()), position);
		}

		if ((deletions.length != 0 || additionsMap.size() != 0)) {
			model.modifyAnnotations(deletions, additionsMap, new Annotation[] {});
		}
	}

	private Annotation[] computeDifferences(ProjectionAnnotationModel model, Set<Position> additions) {
		List<Annotation> deletions = new ArrayList<>();
		for (Iterator<Annotation> iter = model.getAnnotationIterator(); iter.hasNext();) {
			Object annotation = iter.next();
			if (annotation instanceof ProjectionAnnotation) {
				Annotation annot = (Annotation) annotation;
				Position position = model.getPosition(annot);
				if (additions.contains(position)) {
					additions.remove(position);
				} else {
					deletions.add(annot);
				}
			}
		}
		return deletions.toArray(new Annotation[deletions.size()]);
	}

	public void updateFoldingRegions(AntModel antModel) {
		fPositionToElement = new HashMap<>();
		try {
			ProjectionAnnotationModel model = fEditor.getAdapter(ProjectionAnnotationModel.class);
			if (model == null) {
				return;
			}

			Set<Position> currentRegions = new HashSet<>();
			List<IAntElement> root = new ArrayList<>();
			AntProjectNode node = antModel.getProjectNode();
			if (node != null && node.getOffset() != -1) {
				root.add(node);
				List<AntElementNode> nodes = antModel.getNonStructuralNodes();
				root.addAll(nodes);
			}
			addFoldingRegions(currentRegions, root);
			updateFoldingRegions(model, currentRegions);
		}
		catch (BadLocationException be) {
			// ignore as document has changed
		}
	}

	private void addFoldingRegions(Set<Position> regions, List<IAntElement> children) throws BadLocationException {
		// add a Position to 'regions' for each foldable region
		for (IAntElement element : children) {
			if (element.getImportNode() != null || element.isExternal()) {
				continue; // elements are not really in this document and therefore are not foldable
			}
			int startLine = fDocument.getLineOfOffset(element.getOffset());
			int endLine = fDocument.getLineOfOffset(element.getOffset() + element.getLength());
			if (startLine < endLine) {
				int start = fDocument.getLineOffset(startLine);
				int end = fDocument.getLineOffset(endLine) + fDocument.getLineLength(endLine);
				Position position = new Position(start, end - start);
				regions.add(position);
				fPositionToElement.put(position, element);
			}

			List<IAntElement> childNodes = element.getChildNodes();
			if (childNodes != null) {
				addFoldingRegions(regions, childNodes);
			}
		}
	}

	public void setDocument(IDocument document) {
		fDocument = document;
	}
}
