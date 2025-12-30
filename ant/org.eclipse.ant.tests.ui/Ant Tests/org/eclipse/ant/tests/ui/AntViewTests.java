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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;

import org.eclipse.ant.internal.ui.AntUtil;
import org.eclipse.ant.internal.ui.preferences.FileFilter;
import org.eclipse.ant.internal.ui.views.actions.AddBuildFilesAction;
import org.eclipse.ant.tests.ui.testplugin.AbstractAntUITest;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PlatformUI;
import org.junit.Test;

@SuppressWarnings("restriction")
public class AntViewTests extends AbstractAntUITest {

	@Test
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
				if (action instanceof ActionContributionItem actionItem) {
					if (actionItem.getAction() instanceof AddBuildFilesAction) {
						return (AddBuildFilesAction) actionItem.getAction();
					}
				}
			}
		}
		return null;
	}

	@Test
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

	/**
	 * This is to help in increasing the test coverage by enabling access to fields
	 * and execution of methods irrespective of their Java language access
	 * permissions.
	 *
	 * More accessor methods can be added to this on a need basis
	 */
	private static abstract class TypeProxy {

		Object master = null;

		protected TypeProxy(Object obj) {
			master = obj;
		}

		/**
		 * Gets the method with the given method name and argument types.
		 *
		 * @param methodName the method name
		 * @param types      the argument types
		 * @return the method
		 */
		protected Method get(String methodName, Class<?>[] types) {
			Method method = null;
			try {
				method = master.getClass().getDeclaredMethod(methodName, types);
			} catch (SecurityException | NoSuchMethodException e) {
				fail();
			}
			Assert.isNotNull(method);
			method.setAccessible(true);
			return method;
		}

		/**
		 * Invokes the given method with the given arguments.
		 *
		 * @param method    the given method
		 * @param arguments the method arguments
		 * @return the method return value
		 */
		protected Object invoke(Method method, Object[] arguments) {
			try {
				return method.invoke(master, arguments);
			} catch (IllegalArgumentException | InvocationTargetException | IllegalAccessException e) {
				fail();
			}
			return null;
		}
	}
}
