/*******************************************************************************
 *  Copyright (c) 2000, 2012 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.resources.regression;

import static java.util.function.Predicate.not;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.tests.resources.util.WorkspaceResetExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(WorkspaceResetExtension.class)
public class Bug_006708 {

	@Test
	public void testBug() throws CoreException {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IProject sourceProj = root.getProject("bug_6708");
		assertThat(sourceProj).matches(not(IResource::exists), "does not exists already");
		sourceProj.create(null);
		sourceProj.open(null);
		IFile source = sourceProj.getFile("Source.txt");
		source.create(new ByteArrayInputStream("abcdef".getBytes()), false, null);

		IProject destProj = root.getProject("bug_6708_2");
		assertThat(destProj).matches(not(IResource::exists), "does not exists already");
		destProj.create(null);
		destProj.open(null);
		IFile dest = destProj.getFile("Dest.txt");

		source.copy(dest.getFullPath(), false, null);
		dest.setContents(new ByteArrayInputStream("ghijkl".getBytes()), false, true, null);
	}
}
