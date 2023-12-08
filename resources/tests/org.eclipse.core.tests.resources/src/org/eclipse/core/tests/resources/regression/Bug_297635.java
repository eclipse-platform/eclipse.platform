/*******************************************************************************
 *  Copyright (c) 2010, 2015 IBM Corporation and others.
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
 *     Alexander Kurtakov <akurtako@redhat.com> - Bug 459343
 *******************************************************************************/
package org.eclipse.core.tests.resources.regression;

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestPluginConstants.PI_RESOURCES_TESTS;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Field;
import java.util.function.Consumer;
import org.eclipse.core.internal.resources.SaveManager;
import org.eclipse.core.resources.ISaveContext;
import org.eclipse.core.resources.ISaveParticipant;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.tests.harness.BundleTestingHelper;
import org.eclipse.core.tests.resources.WorkspaceTestRule;
import org.eclipse.core.tests.resources.content.ContentTypeTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

/**
 * Tests regression of bug 297635
 */
public class Bug_297635 {

	@Rule
	public WorkspaceTestRule workspaceRule = new WorkspaceTestRule();

	@Before
	public void setUp() throws Exception {
		BundleWithSaveParticipant.install();
		saveFull();
	}

	@After
	public void tearDown() throws BundleException {
		BundleWithSaveParticipant.uninstall();
	}

	@Test
	public void testCleanSaveStateBySaveParticipantOnSnapshotSave() throws Exception {
		executeWithSaveManagerSpy(saveManagerSpy -> {
			try {
				saveSnapshot(saveManagerSpy);
			} catch (CoreException e) {
			}
			verify(saveManagerSpy).forgetSavedTree(BundleWithSaveParticipant.getBundleName());
		});
	}

	private void saveFull() throws CoreException {
		getWorkspace().save(true, createTestMonitor());
	}

	private void saveSnapshot(SaveManager saveManager) throws CoreException {
		saveManager.save(ISaveContext.SNAPSHOT, true, null, createTestMonitor());
	}

	private void executeWithSaveManagerSpy(Consumer<SaveManager> executeOnSpySaveManager) throws Exception {
		IWorkspace workspace = getWorkspace();
		String saveManagerFieldName = "saveManager";
		SaveManager originalSaveManager = (SaveManager) getField(workspace, saveManagerFieldName);
		SaveManager spySaveManager = spy(originalSaveManager);
		try {
			setField(workspace, saveManagerFieldName, spySaveManager);
			executeOnSpySaveManager.accept(spySaveManager);
		} finally {
			setField(workspace, saveManagerFieldName, originalSaveManager);
		}
	}

	private static Object getField(Object object, String fieldName) throws Exception {
		Field field = object.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);
		return field.get(object);
	}

	private static void setField(Object object, String fieldName, Object value) throws Exception {
		Field field = object.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);
		field.set(object, value);
	}

	private static final class BundleWithSaveParticipant {
		private static String TEST_BUNDLE_LOCATION = "content/bundle01";

		private static Bundle bundle;

		private static ISaveParticipant saveParticipant = new ISaveParticipant() {
			@Override
			public void doneSaving(ISaveContext context) {
				// nothing to do
			}

			@Override
			public void prepareToSave(ISaveContext context) {
				context.needDelta();
				context.needSaveNumber();
			}

			@Override
			public void rollback(ISaveContext context) {
				// nothing to do
			}

			@Override
			public void saving(ISaveContext context) {
				// nothing to do
			}
		};

		public static String getBundleName() {
			if (bundle == null) {
				throw new IllegalStateException("Bundle has not been installed");
			}
			return bundle.getSymbolicName();
		}

		public static void uninstall() throws BundleException {
			if (bundle != null) {
				bundle.uninstall();
			}
		}

		public static void install() throws Exception {
			bundle = BundleTestingHelper.installBundle("", getContext(),
					ContentTypeTest.TEST_FILES_ROOT + TEST_BUNDLE_LOCATION);
			BundleTestingHelper.resolveBundles(getContext(), new Bundle[] { bundle });
			bundle.start(Bundle.START_TRANSIENT);
			registerSaveParticipant(bundle);
		}

		private static BundleContext getContext() {
			return Platform.getBundle(PI_RESOURCES_TESTS).getBundleContext();
		}

		private static void registerSaveParticipant(Bundle saveParticipantsBundle) throws CoreException {
			getWorkspace().addSaveParticipant(saveParticipantsBundle.getSymbolicName(), saveParticipant);
		}

	}
}
