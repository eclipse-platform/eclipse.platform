/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
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
package org.eclipse.debug.tests.launching;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchHistory;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.junit.Test;

/**
 * Test the utilization of launch histories: sizing, ordering, completeness and correctness
 *
 * @see org.eclipse.debug.internal.ui.launchConfigurations.LaunchHistory
 * @see org.eclipse.debug.internal.ui.launchConfigurations.LaunchConfigurationManager
 * @see org.eclipse.debug.internal.ui.ILaunchHistoryChangedListener
 * @see org.eclipse.debug.ui.actions.AbstractLaunchHistoryAction
 *
 * @since 3.3
 */
public class LaunchHistoryTests extends AbstractLaunchTest {

	/**
	 * Returns the run launch history
	 * @return
	 */
	private LaunchHistory getRunLaunchHistory() {
		return getLaunchConfigurationManager().getLaunchHistory(IDebugUIConstants.ID_RUN_LAUNCH_GROUP);
	}

	/**
	 * Returns the maximum allowed size of the launch histories
	 * @return the maximum size of the launch histories
	 */
	private int getMaxHistorySize() {
		return DebugUIPlugin.getDefault().getPreferenceStore().getInt(IDebugUIConstants.PREF_MAX_HISTORY_SIZE);
	}

	/**
	 * Sets the maximum size of the launch history to the specified value
	 * @param value the new maximum size for launch histories
	 */
	private void setMaxHistorySize(int value) {
		setPreference(DebugUIPlugin.getDefault().getPreferenceStore(), IDebugUIConstants.PREF_MAX_HISTORY_SIZE, value);
	}

	/**
	 * Returns the debug launch history
	 * @return
	 */
	private LaunchHistory getDebugLaunchHistory() {
		return getLaunchConfigurationManager().getLaunchHistory(IDebugUIConstants.ID_DEBUG_LAUNCH_GROUP);
	}

	/**
	 * This method tests that an item added to the history is added to the head
	 * of history.
	 */
	@Test
	public void testHistoryAddition() throws CoreException {
		LaunchHistory runhistory = getRunLaunchHistory();
		assertNotNull("The run launch history should not be null", runhistory); //$NON-NLS-1$
		ILaunchConfiguration config = getLaunchConfiguration("LaunchHistoryTest"); //$NON-NLS-1$
		assertNotNull("LaunchHistoryTest launch config should not be null", config); //$NON-NLS-1$
		config.launch(ILaunchManager.RUN_MODE, new NullProgressMonitor());
		assertTrue("The run history should contain the LaunchHistoryTest config", runhistory.contains(config)); //$NON-NLS-1$
		assertEquals("The most recent launch should be LaunchHistoryTest", runhistory.getRecentLaunch(), config); //$NON-NLS-1$
	}

	/**
	 * As both the run and the debug launch histories will accept a java
	 * application launch config, both launch histories should contain the test
	 * launch configuration and it should be the recent launch for both of them
	 */
	@Test
	public void testHistoriesInSync() throws CoreException {
		LaunchHistory runhistory = getRunLaunchHistory();
		assertNotNull("The run launch history should not be null", runhistory); //$NON-NLS-1$
		LaunchHistory debughistory = getDebugLaunchHistory();
		assertNotNull("the debug launch history should not be null", debughistory); //$NON-NLS-1$
		ILaunchConfiguration config = getLaunchConfiguration("LaunchHistoryTest"); //$NON-NLS-1$
		assertNotNull("LaunchHistoryTest launch config should not be null", config); //$NON-NLS-1$
		config.launch(ILaunchManager.RUN_MODE, new NullProgressMonitor());
		assertTrue("the run history should contain LaunchHistoryTest", runhistory.contains(config)); //$NON-NLS-1$
		assertEquals("the run recent launch should be LaunchHistoryTest", runhistory.getRecentLaunch(), config); //$NON-NLS-1$
		assertTrue("the debug history should contain LaunchHistoryTest", debughistory.contains(config)); //$NON-NLS-1$
		assertEquals("the debug recent launch should be LaunchHistoryTest", debughistory.getRecentLaunch(), config); //$NON-NLS-1$
	}

	/**
	 * If we launch config A, then config B, and then config A again, A should
	 * be the most recent launch
	 */
	@Test
	public void testHistoryReodering() throws CoreException {
		LaunchHistory runhistory = getRunLaunchHistory();
		assertNotNull("The run launch history should not be null", runhistory); //$NON-NLS-1$
		ILaunchConfiguration config = getLaunchConfiguration("LaunchHistoryTest"); //$NON-NLS-1$
		assertNotNull("LaunchHistoryTest launch config should not be null", config); //$NON-NLS-1$
		config.launch(ILaunchManager.RUN_MODE, new NullProgressMonitor());
		assertTrue("The run history should contain the LaunchHistoryTest config", runhistory.contains(config)); //$NON-NLS-1$
		assertEquals("The most recent launch should be LaunchHistoryTest", runhistory.getRecentLaunch(), config); //$NON-NLS-1$
		config = getLaunchConfiguration("LaunchHistoryTest2"); //$NON-NLS-1$
		assertNotNull("LaunchHistoryTest2 launch config should not be null", config); //$NON-NLS-1$
		config.launch(ILaunchManager.RUN_MODE, new NullProgressMonitor());
		assertTrue("The run history should contain the LaunchHistoryTest2 config", runhistory.contains(config)); //$NON-NLS-1$
		assertEquals("The most recent launch should be LaunchHistoryTest2", runhistory.getRecentLaunch(), config); //$NON-NLS-1$
		config = getLaunchConfiguration("LaunchHistoryTest"); //$NON-NLS-1$
		assertNotNull("LaunchHistoryTest launch config should not be null", config); //$NON-NLS-1$
		config.launch(ILaunchManager.RUN_MODE, new NullProgressMonitor());
		assertTrue("The run history should contain the LaunchHistoryTest config", runhistory.contains(config)); //$NON-NLS-1$
		assertEquals("The most recent launch should be LaunchHistoryTest", runhistory.getRecentLaunch(), config); //$NON-NLS-1$
	}

	/**
	 * If we rename a launch configuration it should not effect the launch
	 * history if the renamed configuration is present in the history.
	 */
	@Test
	public void testRenameConfigHistoryUpdate() throws CoreException {
		LaunchHistory runhistory = getRunLaunchHistory();
		assertNotNull("The run launch history should not be null", runhistory); //$NON-NLS-1$
		ILaunchConfiguration config = getLaunchConfiguration("LaunchHistoryTest"); //$NON-NLS-1$
		assertNotNull("LaunchHistoryTest launch config should not be null", config); //$NON-NLS-1$
		config.launch(ILaunchManager.RUN_MODE, new NullProgressMonitor());
		assertTrue("The run history should contain the LaunchHistoryTest config", runhistory.contains(config)); //$NON-NLS-1$
		assertEquals("The most recent launch should be LaunchHistoryTest", runhistory.getRecentLaunch(), config); //$NON-NLS-1$
		ILaunchConfigurationWorkingCopy copy = config.getWorkingCopy();
		copy.rename("RenamedLaunchHistoryItem"); //$NON-NLS-1$
		config = copy.doSave();
		assertEquals("the renamed config should still be the first on in the history", runhistory.getRecentLaunch(), config); //$NON-NLS-1$

		//rename the configuration back to what it was
		copy = config.getWorkingCopy();
		copy.rename("LaunchHistoryTest"); //$NON-NLS-1$
		config = copy.doSave();
	}

	/**
	 * If we delete a launch configuration and the configuration is present in
	 * the launch history, it should be removed from the history and the history
	 * should be shifted up one place.
	 */
	@Test
	public void testDeleteLaunchConfigurationHistoryUpdate() throws CoreException {
		LaunchHistory runhistory = getRunLaunchHistory();
		assertNotNull("The run launch history should not be null", runhistory); //$NON-NLS-1$
		ILaunchConfiguration config = getLaunchConfiguration("LaunchHistoryTest"); //$NON-NLS-1$
		assertNotNull("LaunchHistoryTest launch config should not be null", config); //$NON-NLS-1$
		config.launch(ILaunchManager.RUN_MODE, new NullProgressMonitor());
		assertTrue("The run history should contain the LaunchHistoryTest config", runhistory.contains(config)); //$NON-NLS-1$
		assertEquals("The most recent launch should be LaunchHistoryTest", runhistory.getRecentLaunch(), config); //$NON-NLS-1$
		config = getLaunchConfiguration("LaunchHistoryTest2"); //$NON-NLS-1$
		assertNotNull("LaunchHistoryTest2 launch config should not be null", config); //$NON-NLS-1$
		config.launch(ILaunchManager.RUN_MODE, new NullProgressMonitor());
		assertTrue("The run history should contain the LaunchHistoryTest2 config", runhistory.contains(config)); //$NON-NLS-1$
		assertEquals("The most recent launch should be LaunchHistoryTest2", runhistory.getRecentLaunch(), config); //$NON-NLS-1$
		config.delete();
		config = getLaunchConfiguration("LaunchHistoryTest"); //$NON-NLS-1$
		assertEquals("the run history should have LaunchHistoryTest as the recent launch after delete", runhistory.getRecentLaunch(), config); //$NON-NLS-1$
	}

	/**
	 * Tests that setting the size of the launch history appropriately changes
	 * what will be returned when the history is queried for it contents
	 */
	@Test
	public void testLaunchHistorySize() throws CoreException {
		LaunchHistory runhistory = getRunLaunchHistory();
		assertNotNull("The run launch history should not be null", runhistory); //$NON-NLS-1$
		setMaxHistorySize(2);
		assertTrue("the maximum history size should be 2", getMaxHistorySize() == 2); //$NON-NLS-1$
		ILaunchConfiguration config = getLaunchConfiguration("LaunchHistoryTest"); //$NON-NLS-1$
		assertNotNull("LaunchHistoryTest launch config should not be null", config); //$NON-NLS-1$
		config.launch(ILaunchManager.RUN_MODE, new NullProgressMonitor());
		config = getLaunchConfiguration("LaunchHistoryTest2"); //$NON-NLS-1$
		assertNotNull("LaunchHistoryTest2 launch config should not be null", config); //$NON-NLS-1$
		config.launch(ILaunchManager.RUN_MODE, new NullProgressMonitor());
		assertTrue("there should only be two items in the history", runhistory.getHistory().length == getMaxHistorySize()); //$NON-NLS-1$
		assertTrue("the complete launch history should be greater than or equal to the history size", runhistory.getCompleteLaunchHistory().length >= runhistory.getHistory().length); //$NON-NLS-1$
	}
}
