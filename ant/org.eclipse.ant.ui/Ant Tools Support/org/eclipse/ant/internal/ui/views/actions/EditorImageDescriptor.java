/*******************************************************************************
 * Copyright (c) 2000, 2025 IBM Corporation and others.
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
package org.eclipse.ant.internal.ui.views.actions;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.program.Program;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

/**
 * Copy of org.eclipse.ui.internal.misc.ExternalProgramImageDescriptor for use in the AntViewOpenWithMenu
 */
public class EditorImageDescriptor extends ImageDescriptor {

	public Program program;

	/**
	 * Creates a new ImageDescriptor. The image is loaded from a file with the given name <code>name</code>.
	 */
	public EditorImageDescriptor(Program program) {
		this.program = program;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof EditorImageDescriptor other)) {
			return false;
		}
		// See if there is a name - compare it if so and compare the programs if not
		String otherName = other.program.getName();
		if (otherName == null) {
			return other.program.equals(program);
		}
		return otherName.equals(program.getName());
	}

	@Override
	public ImageData getImageData(int zoom) {
		ImageData defaultImage = PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_OBJ_FILE).getImageData(zoom);
		if (defaultImage == null) {
			return null;
		}
		ImageData data = null;
		if (program == null || ((data = program.getImageData(zoom)) == null)) {
			return defaultImage;
		}

		// The images in GNOME are too big. Scaling them does not give nice result so return defaultImage;
		if (data.height > defaultImage.height || data.width > defaultImage.width) {
			return defaultImage;
		}

		return data;
	}

	@Override
	public int hashCode() {
		String programName = program.getName();
		if (programName == null) {
			return program.hashCode();
		}
		return programName.hashCode();
	}
}
