/*******************************************************************************
 * Copyright (c) 2004, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.internal.ui.editor.text;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.ant.internal.ui.AntUIPlugin;
import org.eclipse.ant.internal.ui.model.IProblem;
import org.eclipse.ant.internal.ui.model.IProblemRequestor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.AnnotationModel;
import org.eclipse.jface.text.source.AnnotationModelEvent;

public class AntExternalAnnotationModel extends AnnotationModel implements IProblemRequestor {

    private List fGeneratedAnnotations= new ArrayList();
	private List fCollectedProblems= new ArrayList();

    /* (non-Javadoc)
	 * @see org.eclipse.ant.internal.ui.editor.outline.IProblemRequestor#acceptProblem(org.eclipse.ant.internal.ui.editor.outline.IProblem)
	 */
	public void acceptProblem(IProblem problem) {
		fCollectedProblems.add(problem);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ant.internal.ui.editor.outline.IProblemRequestor#acceptProblem(org.eclipse.ant.internal.ui.editor.outline.IProblem)
	 */
	public void endReporting() {
		boolean temporaryProblemsChanged= false;
			
		synchronized (getAnnotationMap()) {
				
			if (fGeneratedAnnotations.size() > 0) {
				temporaryProblemsChanged= true;	
				removeAnnotations(fGeneratedAnnotations, false, true);
				fGeneratedAnnotations.clear();
			}
				
			if (fCollectedProblems != null && fCollectedProblems.size() > 0) {
				Iterator e= fCollectedProblems.iterator();
				while (e.hasNext()) {
						
					IProblem problem= (IProblem) e.next();
						
					Position position= createPositionFromProblem(problem);
					if (position != null) {
							
						XMLProblemAnnotation annotation= new XMLProblemAnnotation(problem);
						fGeneratedAnnotations.add(annotation);
						try {
							addAnnotation(annotation, position, false);
						} catch (BadLocationException ex) {
							AntUIPlugin.log(ex);
						}
							
						temporaryProblemsChanged= true;
					}
				}
					
				fCollectedProblems.clear();
			}
		}
				
		if (temporaryProblemsChanged)
			fireModelChanged(new AnnotationModelEvent(this));
	}
					
	protected Position createPositionFromProblem(IProblem problem) {
		int start= problem.getOffset();
		if (start >= 0) {
			int length= problem.getLength();
				
			if (length >= 0)
				return new Position(start, length);
		}

		return null;
	}

    /* (non-Javadoc)
     * @see org.eclipse.ant.internal.ui.model.IProblemRequestor#beginReporting()
     */
    public void beginReporting() {
    }
}
