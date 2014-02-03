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
package org.eclipse.ant.internal.ui.views.actions;


import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.program.Program;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

/**
 * Copy of org.eclipse.ui.internal.misc.ExternalProgramImageDescriptor for
 * use in the AntViewOpenWithMenu
 */
public class EditorImageDescriptor extends ImageDescriptor {

	public Program program;

	/**
	 * Creates a new ImageDescriptor. The image is loaded 
	 * from a file with the given name <code>name</code>.
	 */
	public EditorImageDescriptor(Program program) {
		this.program = program;
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object o) {
		if (!(o instanceof EditorImageDescriptor)) {
			return false;
		}
		EditorImageDescriptor other = (EditorImageDescriptor) o;

		//See if there is a name - compare it if so and compare the programs if not
		String otherName = other.program.getName();
		if (otherName == null) {
			return other.program.equals(program);
		} 
		return otherName.equals(program.getName());
	}
	/**
	 * Returns an SWT Image that is described by the information
	 * in this descriptor.  Each call returns a new Image.
	 */
	public Image getImage() {
		return createImage();
	}
	
	/**
	 * @see org.eclipse.jface.resource.ImageDescriptor#getImageData()
	 */
	public ImageData getImageData() {
		
		ImageData defaultImage = PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_OBJ_FILE).getImageData();
		if (defaultImage == null) {
			return null;
		}
		ImageData data = null;
		if (program == null || ((data = program.getImageData()) == null)) {
			return defaultImage;
		}

		//The images in GNOME are too big. Scaling them does not give nice result so return defaultImage;
		if (data.height > defaultImage.height || data.width > defaultImage.width) {
			return defaultImage;
		}

		return data;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		String programName = program.getName();
		if (programName == null) {
			return program.hashCode();
		} 
		return programName.hashCode();
	}
}
