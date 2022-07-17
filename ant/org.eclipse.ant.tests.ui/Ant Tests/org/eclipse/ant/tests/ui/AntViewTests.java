/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
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

package org.eclipse.ant.tests.ui;

import java.lang.reflect.Method;
import java.util.Collections;

import org.eclipse.ant.internal.ui.AntUtil;
import org.eclipse.ant.internal.ui.preferences.FileFilter;
import org.eclipse.ant.internal.ui.views.actions.AddBuildFilesAction;
import org.eclipse.ant.tests.ui.testplugin.AbstractAntUITest;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PlatformUI;

@SuppressWarnings("restriction")
public class AntViewTests extends AbstractAntUITest {

	public AntViewTests(String name) {
		super(name);
	}

	public void testAddBuildFilesAction() throws CoreException {
		// Ensure that AddBuildFilesAction is present!
		String viewId = "org.eclipse.ant.ui.views.AntView"; //$NON-NLS-1$
		IViewPart view = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(viewId);
		assertNotNull("Failed to obtain the AntView", view); //$NON-NLS-1$
		IViewSite viewSite = view.getViewSite();
		assertNotNull("Failed to obtain view site", viewSite); //$NON-NLS-1$
		IToolBarManager toolBarMgr = viewSite.getActionBars().getToolBarManager();
		assertNotNull("Failed to obtain the AntView ToolBar", toolBarMgr); //$NON-NLS-1$
		AddBuildFilesAction action = getAddBuildFilesAction(toolBarMgr);
		assertNotNull("Failed to obtain the AddBuildFilesAction", action); //$NON-NLS-1$
	}

	private AddBuildFilesAction getAddBuildFilesAction(IToolBarManager toolBarMgr) {
		IContributionItem[] actions = toolBarMgr.getItems();
		if (actions != null && actions.length > 0) {
			for (IContributionItem action : actions) {
				if (action instanceof ActionContributionItem) {
					ActionContributionItem actionItem = (ActionContributionItem) action;
					if (actionItem.getAction() instanceof AddBuildFilesAction) {
						return (AddBuildFilesAction) actionItem.getAction();
					}
				}
			}
		}
		return null;
	}

	public void testAntBuildFilesExtensionFilter() {
		// Ensure coverage for the extension filter used by AddBuildFilesAction
		// Create blocks to scope the vars to catch typos!

		{// Accept only a single extension
			String extnFilter1 = "xml"; //$NON-NLS-1$
			FileFilterProxy ff1 = new FileFilterProxy(extnFilter1);
			assertTrue("xml is not accepted as a build file extension", ff1.canAccept("xml")); //$NON-NLS-1$ //$NON-NLS-2$
			assertFalse("ent is accepted as a build file extension", ff1.canAccept("ent")); //$NON-NLS-1$ //$NON-NLS-2$
		}

		{// Accept multiple extensions
			String extnFilter2 = AntUtil.getKnownBuildFileExtensionsAsPattern();
			FileFilterProxy ff2 = new FileFilterProxy(extnFilter2);
			assertTrue("xml is not accepted as a build file extension", ff2.canAccept("xml")); //$NON-NLS-1$ //$NON-NLS-2$
			assertTrue("ant is not accepted as a build file extension", ff2.canAccept("ant")); //$NON-NLS-1$ //$NON-NLS-2$
			assertTrue("ent is not accepted as a build file extension", ff2.canAccept("ent")); //$NON-NLS-1$ //$NON-NLS-2$
			assertFalse("def is accepted as a build file extension", ff2.canAccept("def")); //$NON-NLS-1$ //$NON-NLS-2$
			assertTrue("macrodef is not accepted as a build file extension", ff2.canAccept("macrodef")); //$NON-NLS-1$ //$NON-NLS-2$
			assertTrue("XML is not accepted as a build file extension", ff2.canAccept("XML")); //$NON-NLS-1$ //$NON-NLS-2$
			assertFalse("macro is accepted as a build file extension", ff2.canAccept("macro")); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	private static class FileFilterProxy extends TypeProxy {

		Method canAcceptMethod = null;

		FileFilterProxy(String extnFilter) {
			super(new FileFilter(Collections.EMPTY_LIST, extnFilter));
		}

		boolean canAccept(String extn) {
			if (canAcceptMethod == null) {
				canAcceptMethod = get("canAccept", new Class[] { String.class }); //$NON-NLS-1$
			}
			Object result = invoke(canAcceptMethod, new String[] { extn });
			assertNotNull("Failed to invoke 'canAccept()'", result); //$NON-NLS-1$
			return ((Boolean) result).booleanValue();
		}
	}

}
