/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.internal.ui.editor.text;

import java.util.Iterator;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

/**
 * Interface of annotations representing markers
 * and problems.
 * 
 * @see org.eclipse.core.resources.IMarker
 * @see org.eclipse.jdt.core.compiler.IProblem
 */
public interface IXMLAnnotation {
	public static final String ERROR_ANNOTATION_TYPE= "org.eclipse.ui.workbench.texteditor.error"; //$NON-NLS-1$
	public static final String WARNING_ANNOTATION_TYPE= "org.eclipse.ui.workbench.texteditor.warning"; //$NON-NLS-1$
	public static final String INFO_ANNOTATION_TYPE= "org.eclipse.ui.workbench.texteditor.info"; //$NON-NLS-1$
	
	/**
	 * Returns the type of this annotation.
	 *
	 * @return the type of the annotation or <code>null</code> if it has none.
	 */
	public String getType();
	
	/**
	 * Returns whether this annotation is temporary rather than persistent.
	 * 
	 * @return <code>true</code> if temporary <code>false</code> otherwise
	 */
	public boolean isTemporary();
	
	/**
	 * Returns the message assocaited with this annotation.
	 * 
	 * @return the message for this annotation
	 */
	public String getMessage();
	
	/**
	 * Returns an image for this annotation.
	 * 
	 * @param display the display for which the image is requested
	 * @return the image for this annotation
	 */
	public Image getImage(Display display);
	
	/**
	 * Returns whether this annotation is relavant.
	 * <p>
	 * If the annotation is overlaid then it is not
	 * relevant. After all overlays have been removed
	 * the annotation might either become relevant again
	 * or stay irrelevant.
	 * </p>
	 * 
	 * @return <code>true</code> if relevant
	 * @see #hasOverlay()
	 */
	public boolean isRelevant();
	
	/**
	 * Returns whether this annotation is overlaid.
	 * 
	 * @return <code>true</code> if overlaid
	 */
	public boolean hasOverlay();
	
	/**
	 * Returns an iterator for iterating over the
 	 * annotation which are overlaid by this annotation.
	 * 
	 * @return an iterator over the overlaid annotaions
	 */
	public Iterator getOverlaidIterator();
	
	/**
	 * Adds the given annotation to the list of
	 * annotations which overlaid by this annotations.
	 *  
	 * @param annotation	the problem annoation
	 */
	public void addOverlaid(IXMLAnnotation annotation);
	
	/**
	 * Removes the given annotation from the list of
	 * annotations which are overlaid by this annotation.
	 *  
	 * @param annotation	the problem annoation
	 */
	public void removeOverlaid(IXMLAnnotation annotation);
	
	/**
	 * Tells whether this annotation is a problem
	 * annotation.
	 * 
	 * @return <code>true</code> if it is a problem annotation
	 */
	public boolean isProblem();
}
