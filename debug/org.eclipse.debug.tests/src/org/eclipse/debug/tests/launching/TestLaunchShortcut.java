/*******************************************************************************
 * Copyright (c) 2026 Sergii Zolotarov and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Sergii Zolotarov - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.tests.launching;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.ui.ILaunchShortcut2;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;

/**
 * A launch shortcut for the tests of the "Run As..." dialog. It is enabled for
 * a selection of one {@link Target}, writes down its launches, and gives the
 * configurations in {@link #given}.
 */
public class TestLaunchShortcut implements ILaunchShortcut2 {

	/** An element for which the shortcut is enabled */
	public record Target(String name) {
	}

	static final List<String> LAUNCHES = new ArrayList<>();
	static ILaunchConfiguration[] given = new ILaunchConfiguration[0];

	@Override
	public void launch(ISelection selection, String mode) {
		LAUNCHES.add(mode + " " + selection);
	}

	@Override
	public void launch(IEditorPart editor, String mode) {
		LAUNCHES.add(mode + " editor " + editor.getTitle());
	}

	@Override
	public ILaunchConfiguration[] getLaunchConfigurations(ISelection selection) {
		return given;
	}

	@Override
	public ILaunchConfiguration[] getLaunchConfigurations(IEditorPart editorpart) {
		return given;
	}

	@Override
	public IResource getLaunchableResource(ISelection selection) {
		return null;
	}

	@Override
	public IResource getLaunchableResource(IEditorPart editorpart) {
		return null;
	}
}
