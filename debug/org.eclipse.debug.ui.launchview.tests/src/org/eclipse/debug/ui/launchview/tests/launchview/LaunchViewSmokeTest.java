/*******************************************************************************
 * Copyright (c) 2021 SSI Schaefer IT Solutions GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SSI Schaefer IT Solutions GmbH
 *******************************************************************************/
package org.eclipse.debug.ui.launchview.tests.launchview;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import org.eclipse.debug.ui.launchview.tests.AbstractLaunchViewTest;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.junit.jupiter.api.Test;

public class LaunchViewSmokeTest extends AbstractLaunchViewTest {

	@Test
	public void testOpenView() {
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		assertNotNull(page, "The active workbench page should not be null");
		try {
			page.showView("org.eclipse.debug.ui.launchView"); //$NON-NLS-1$
		} catch (PartInitException exception) {
			fail("Failed to open launch configuration view");
		}

	}

}
