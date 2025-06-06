/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others.
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

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.IAnnotationModelFactory;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.text.source.IAnnotationModel;

public class AnnotationModelFactory implements IAnnotationModelFactory {

	@Override
	public IAnnotationModel createAnnotationModel(IPath location) {
		IFile file = FileBuffers.getWorkspaceFileAtLocation(location);
		if (file == null) {
			return new AntExternalAnnotationModel();
		}
		return new AntAnnotationModel(file);
	}
}
