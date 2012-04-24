/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

public class AntViewTests extends AbstractAntUITest {

	public AntViewTests(String name) {
		super(name);
	}
	
	public void testAddBuildFilesAction() throws CoreException {
		// Ensure that AddBuildFilesAction is present!
		String viewId = "org.eclipse.ant.ui.views.AntView";
		IViewPart view = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow().getActivePage().showView(viewId);
		assertNotNull("Failed to obtain the AntView", view);
		IViewSite viewSite = view.getViewSite();
		assertNotNull("Failed to obtain view site", viewSite);
		IToolBarManager toolBarMgr= viewSite.getActionBars().getToolBarManager();
		assertNotNull("Failed to obtain the AntView ToolBar", toolBarMgr);
		AddBuildFilesAction action = getAddBuildFilesAction(toolBarMgr);
		assertNotNull("Failed to obtain the AddBuildFilesAction", action);
	}

	private AddBuildFilesAction getAddBuildFilesAction(IToolBarManager toolBarMgr) {
		IContributionItem[] actions = toolBarMgr.getItems();
		if (actions != null && actions.length > 0) {
			for (int i = 0; i < actions.length; i++) {
				if (actions[i] instanceof ActionContributionItem) {
					ActionContributionItem actionItem = (ActionContributionItem) actions[i];
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
			String extnFilter1 = "xml";
			FileFilterProxy ff1 = new FileFilterProxy(extnFilter1);
			assertTrue("xml is not accepted as a build file extension", ff1.canAccept("xml"));
			assertFalse("ent is accepted as a build file extension", ff1.canAccept("ent"));
		}
		
		{// Accept multiple extensions
			String extnFilter2 = AntUtil.getKnownBuildFileExtensionsAsPattern();
			FileFilterProxy ff2 = new FileFilterProxy(extnFilter2);
			assertTrue("xml is not accepted as a build file extension", ff2.canAccept("xml"));
			assertTrue("ant is not accepted as a build file extension", ff2.canAccept("ant"));
			assertTrue("ent is not accepted as a build file extension", ff2.canAccept("ent"));
			assertFalse("def is accepted as a build file extension", ff2.canAccept("def"));
			assertTrue("macrodef is not accepted as a build file extension", ff2.canAccept("macrodef"));
			assertTrue("XML is not accepted as a build file extension", ff2.canAccept("XML"));
			assertFalse("macro is accepted as a build file extension", ff2.canAccept("macro"));
		}
	}
	
	private static class FileFilterProxy extends TypeProxy {
		
		Method canAcceptMethod = null;
		
		FileFilterProxy(String extnFilter) {
			super(new FileFilter(Collections.EMPTY_LIST, extnFilter));
		}
		
		boolean canAccept(String extn) {
			if (canAcceptMethod == null) {
				canAcceptMethod = get("canAccept", new Class[] { String.class });
			}
			Object result = invoke(canAcceptMethod, new String[] {extn});			
			assertNotNull("Failed to invoke 'canAccept()'", result);
			return ((Boolean)result).booleanValue();
		}
	}

}
