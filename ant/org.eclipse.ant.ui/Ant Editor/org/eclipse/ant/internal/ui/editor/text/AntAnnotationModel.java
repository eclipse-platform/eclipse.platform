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
import java.util.Iterator;
import java.util.List;

import org.eclipse.ant.internal.launching.debug.IAntDebugConstants;
import org.eclipse.ant.internal.ui.AntUIPlugin;
import org.eclipse.ant.internal.ui.editor.outline.AntEditorMarkerUpdater;
import org.eclipse.ant.internal.ui.model.IProblem;
import org.eclipse.ant.internal.ui.model.IProblemRequestor;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.AnnotationModelEvent;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.texteditor.MarkerAnnotation;
import org.eclipse.ui.texteditor.MarkerUtilities;
import org.eclipse.ui.texteditor.ResourceMarkerAnnotationModel;

public class AntAnnotationModel extends ResourceMarkerAnnotationModel implements IProblemRequestor {

	private final List<XMLProblemAnnotation> fGeneratedAnnotations = new ArrayList<>();
	private final List<IProblem> fCollectedProblems = new ArrayList<>();

	public AntAnnotationModel(IFile file) {
		super(file);
	}

	@Override
	protected MarkerAnnotation createMarkerAnnotation(IMarker marker) {
		String markerType = MarkerUtilities.getMarkerType(marker);
		if (AntEditorMarkerUpdater.BUILDFILE_PROBLEM_MARKER.equals(markerType)) {
			// we currently do not show Ant buildfile problem markers in the Ant editor as we have no notion of
			// annotation overlays
			// bug
			return null;
		}
		return new MarkerAnnotation(EditorsUI.getAnnotationTypeLookup().getAnnotationType(marker), marker);
	}

	protected Position createPositionFromProblem(IProblem problem) {
		int start = problem.getOffset();
		if (start >= 0) {
			int length = problem.getLength();

			if (length >= 0)
				return new Position(start, length);
		}

		return null;
	}

	@Override
	public void acceptProblem(IProblem problem) {
		fCollectedProblems.add(problem);
	}

	@Override
	public void beginReporting() {
		// do nothing
	}

	@Override
	public void endReporting() {
		boolean temporaryProblemsChanged = false;

		synchronized (getAnnotationMap()) {

			if (fGeneratedAnnotations.size() > 0) {
				temporaryProblemsChanged = true;
				removeAnnotations(fGeneratedAnnotations, false, true);
				fGeneratedAnnotations.clear();
			}

			if (fCollectedProblems != null && fCollectedProblems.size() > 0) {
				Iterator<IProblem> e = fCollectedProblems.iterator();
				while (e.hasNext()) {

					IProblem problem = e.next();

					Position position = createPositionFromProblem(problem);
					if (position != null) {

						XMLProblemAnnotation annotation = new XMLProblemAnnotation(problem);
						fGeneratedAnnotations.add(annotation);
						try {
							addAnnotation(annotation, position, false);
						}
						catch (BadLocationException ex) {
							AntUIPlugin.log(ex);
						}

						temporaryProblemsChanged = true;
					}
				}

				fCollectedProblems.clear();
			}
		}

		if (temporaryProblemsChanged)
			fireModelChanged(new AnnotationModelEvent(this));
	}

	@Override
	protected boolean isAcceptable(IMarker marker) {
		if (super.isAcceptable(marker)) {
			return !marker.getAttribute(IAntDebugConstants.ANT_RUN_TO_LINE, false);
		}
		return false;
	}
}
