/*******************************************************************************
 * Copyright (c) 2006, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.graphics.Image;

/**
 * A label provider that provides the label and image for the left, right and 
 * ancestor sides for a compare input being shown in compare/merge viewers.
 * <p>
 * This interface may be implemented by clients.
 * 
 * @since 3.3
 */
public interface ICompareInputLabelProvider extends ILabelProvider {

	/**
	 * Returns the label for the ancestor side of compare/merge viewers.
	 * This label is typically shown in the title of the ancestor area in a compare viewer.
	 *
	 * @param input the input object of a compare/merge viewer or <code>null</code>
	 * @return the label for the ancestor side or <code>null</code>
	 */
	String getAncestorLabel(Object input);
	
	/**
	 * Returns the image for the ancestor side of compare/merge viewers.
	 * This image is typically shown in the title of the ancestor area in a compare viewer.
	 *
	 * @param input the input object of a compare/merge viewer or <code>null</code>
	 * @return the image for the ancestor side or <code>null</code>
	 */	
	Image getAncestorImage(Object input);
	
	/**
	 * Returns the label for the left hand side of compare/merge viewers.
	 * This label is typically shown in the title of the left side of a compare viewer.
	 *
	 * @param input the input object of a compare/merge viewer or <code>null</code>
	 * @return the label for the left hand side or <code>null</code>
	 */
	String getLeftLabel(Object input);
	
	/**
	 * Returns the image for the left hand side of compare/merge viewers.
	 * This image is typically shown in the title of the left side of a compare viewer.
	 *
	 * @param input the input object of a compare/merge viewer or <code>null</code>
	 * @return the image for the left hand side or <code>null</code>
	 */	
	Image getLeftImage(Object input);
	
	/**
	 * Returns the label for the right hand side of compare/merge viewers.
	 * This label is typically shown in the title of the right side of a compare viewer.
	 *
	 * @param input the input object of a compare/merge viewer or <code>null</code>
	 * @return the label for the right hand side or <code>null</code>
	 */
	String getRightLabel(Object input);
	
	/**
	 * Returns the image for the right hand side of compare/merge viewers.
	 * This image is typically shown in the title of the right side of a compare viewer.
	 *
	 * @param input the input object of a compare/merge viewer or <code>null</code>
	 * @return the image for the right hand side or <code>null</code>
	 */
	Image getRightImage(Object input);
}
