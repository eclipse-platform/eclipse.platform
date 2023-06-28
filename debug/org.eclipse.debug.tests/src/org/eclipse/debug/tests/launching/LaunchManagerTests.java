/*******************************************************************************
 * Copyright (c) 2009, 2013 IBM Corporation and others.
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.internal.core.LaunchManager;
import org.eclipse.debug.tests.launching.CancellingLaunchDelegate.CancellingLaunch;
import org.eclipse.debug.tests.launching.ThrowingLaunchDelegate.ThrowingEnum;
import org.eclipse.debug.tests.launching.ThrowingLaunchDelegate.ThrowingLaunch;
import org.junit.Test;

/**
 *
 * Variety of tests for the {@link org.eclipse.debug.internal.core.LaunchManager}
 *
 * @since 3.6
 */
@SuppressWarnings("deprecation")
public class LaunchManagerTests extends AbstractLaunchTest {


	/**
	 * Tests generating a valid launch configuration name
	 */
	@Test
	public void testGenereateConfigName() {
		String configname = "launch_configuration"; //$NON-NLS-1$
		String name = getLaunchManager().generateLaunchConfigurationName(configname);
		assertTrue("the name nust be '" + configname + "'", name.equals(configname)); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Tests generating a launch configuration name with an unsupported char
	 * using the deprecated method
	 */
	@Test
	public void testGenereateConfigNameBadChar() {
		String configname = "config:name"; //$NON-NLS-1$
		String name = getLaunchManager().generateUniqueLaunchConfigurationNameFrom(configname);
		assertEquals("config name should be '" + configname + "'", configname, name); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Tests that a valid name is accepted as is.
	 */
	@Test
	public void testGenerateValidName() {
		String configname = "thisisavalidname"; //$NON-NLS-1$
		String name = getLaunchManager().generateLaunchConfigurationName(configname);
		assertEquals("Should be the same as the seed", configname, name); //$NON-NLS-1$
	}

	/**
	 * Tests generating a launch configuration name using a name that is an OS
	 * reserved name. Win 32 test only.
	 */
	@Test
	public void testGenerateConfigNameReservedName() {
		if(Platform.OS_WIN32.equals(Platform.getOS())) {
			String configname = "aux"; //$NON-NLS-1$
			String name = getLaunchManager().generateUniqueLaunchConfigurationNameFrom(configname);
			assertEquals("config name should be 'aux'", configname, name); //$NON-NLS-1$
		}
	}

	/**
	 * Tests generating a configuration name that contains an invalid character
	 */
	@Test
	public void testGenerateBadConfigName() {
		String configname = "config:name"; //$NON-NLS-1$
		String name = getLaunchManager().generateLaunchConfigurationName(configname);
		assertEquals("config name should be 'config_name'", "config_name", name); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Tests generating a name that conflicts with an OS reserved name. This
	 * test is for windows only as there are no reserved names on other OS's.
	 */
	@Test
	public void testGenerateConflictingName() {
		if(Platform.OS_WIN32.equals(Platform.getOS())) {
			String configname = "aux"; //$NON-NLS-1$
			String name = getLaunchManager().generateLaunchConfigurationName(configname);
			assertEquals("config name should be 'launch_configuration'", "launch_configuration", name); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	/**
	 * Tests generating a configuration name that contains an invalid character
	 * and where there is another config with the replaced name already
	 */
	@Test
	public void testGenerateBadCharConflict() throws Exception {
		String configname = "config:name"; //$NON-NLS-1$
		String name = getLaunchManager().generateLaunchConfigurationName(configname);
		assertEquals("config name should be 'config_name'", "config_name", name); //$NON-NLS-1$ //$NON-NLS-2$
		getLaunchConfiguration(name);
		name = getLaunchManager().generateLaunchConfigurationName(configname);
		assertEquals("config name should be 'config_name (1)'", "config_name (1)", name); //$NON-NLS-1$ //$NON-NLS-2$
		ILaunchConfiguration config = getLaunchConfiguration("config_name"); //$NON-NLS-1$
		config.delete();
	}

	/**
	 * Tests generating a name that conflicts with an OS reserved name and that
	 * there is a config with the replaced name already. This test is for
	 * windows only as there are no reserved names on other OS's.
	 */
	@Test
	public void testGenerateBadNameConflict() throws Exception {
		if(Platform.OS_WIN32.equals(Platform.getOS())) {
			String configname = "com2"; //$NON-NLS-1$
			String name = getLaunchManager().generateLaunchConfigurationName(configname);
			assertEquals("config name should be 'launch_configuration'", "launch_configuration", name); //$NON-NLS-1$ //$NON-NLS-2$
			getLaunchConfiguration(name);
			name = getLaunchManager().generateLaunchConfigurationName(configname);
			assertEquals("config name should be 'launch_configuration (1)'", "launch_configuration (1)", name); //$NON-NLS-1$ //$NON-NLS-2$
			ILaunchConfiguration config = getLaunchConfiguration("launch_configuration"); //$NON-NLS-1$
			config.delete();
		}
	}

	/**
	 * Tests the
	 * {@link org.eclipse.debug.core.ILaunchManager#isValidLaunchConfigurationName(String)}
	 * method for correctness
	 */
	@Test
	public void testValidateConfigGoodName() {
		String configname = "configname"; //$NON-NLS-1$
		try {
			getLaunchManager().isValidLaunchConfigurationName(configname);
		}
		catch(IllegalArgumentException iae) {
			fail("the config name should not have thrown an exception during validation"); //$NON-NLS-1$
		}
	}

	/**
	 * Tests the
	 * {@link org.eclipse.debug.core.ILaunchManager#isValidLaunchConfigurationName(String)}
	 * method for correctness
	 */
	@Test
	public void testValidateConfigBadCharName() {
		String configname = "config:name"; //$NON-NLS-1$
		try {
			getLaunchManager().isValidLaunchConfigurationName(configname);
		}
		catch(IllegalArgumentException iae) {
			return;
		}
		fail("the config name should have thrown an exception during validation"); //$NON-NLS-1$
	}

	/**
	 * Tests the
	 * {@link org.eclipse.debug.core.ILaunchManager#isValidLaunchConfigurationName(String)}
	 * method for correctness
	 */
	@Test
	public void testValidateConfigBadName() {
		if(Platform.OS_WIN32.equals(Platform.getOS())) {
			String configname = "com1"; //$NON-NLS-1$
			try {
				getLaunchManager().isValidLaunchConfigurationName(configname);
			}
			catch(IllegalArgumentException iae) {
				return;
			}
			fail("the config name should have thrown an exception during validation"); //$NON-NLS-1$
		}
	}

	/**
	 * Tests that generating a configuration name when there exists a
	 * configuration with that name already properly updates a '(N)' counter at
	 * the end
	 */
	@Test
	public void testGenerateNameExistingConfig() throws Exception {
		String configname = "x.y.z.configname"; //$NON-NLS-1$
		getLaunchConfiguration(configname);
		String name = getLaunchManager().generateLaunchConfigurationName(configname);
		assertEquals("the configuration name should have been " + configname + " (1)", configname + " (1)", name); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		getLaunchConfiguration(name);
		name = getLaunchManager().generateLaunchConfigurationName(name);
		assertEquals("the configuration name should have been " + configname + " (2)", configname + " (2)", name); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		ILaunchConfiguration config = getLaunchConfiguration(configname);
		if(config != null) {
			config.delete();
		}
		config = getLaunchConfiguration(configname + " (1)"); //$NON-NLS-1$
		if(config != null) {
			config.delete();
		}
	}

	/**
	 * Tests that removing an accelerator properly removes it without affecting the base string (readable) value
	 */
	@Test
	public void testRemoveAcc() {
		String text = "&Remove"; //$NON-NLS-1$
		String label = LaunchManager.removeAccelerators(text);
		assertEquals("the label should be 'Remove'", "Remove", label); //$NON-NLS-1$ //$NON-NLS-2$
		text = "Remo&ve"; //$NON-NLS-1$
		label = LaunchManager.removeAccelerators(text);
		assertEquals("the label should be 'Remove'", "Remove", label); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Create a new configuration that will cancel one of the three checks: 1.
	 * preLaunchCheck 2. finalLaunchCheck 3. buildForLaunch
	 *
	 * @param pre If the prelaunchCheck should cancel
	 * @param fin If the fnalLaunchCheck should cancel
	 * @param build If the buildForLaunch check should cancel
	 * @return the new {@link ILaunchConfiguration}
	 * @since 3.9.100
	 */
	ILaunchConfiguration getCancellingConfiguration(boolean pre, boolean fin, boolean build) throws Exception {
		ILaunchConfigurationType type = getLaunchManager().getLaunchConfigurationType("cancelling.type"); //$NON-NLS-1$
		if (type != null) {
			ILaunchConfigurationWorkingCopy copy = type.newInstance(null, getLaunchManager().generateLaunchConfigurationName("cancelling")); //$NON-NLS-1$
			copy.setAttribute("cancel.preLaunchCheck", !pre); //$NON-NLS-1$
			copy.setAttribute("cancel.finalLaunchCheck", !fin); //$NON-NLS-1$
			copy.setAttribute("cancel.buildForLaunch", !build); //$NON-NLS-1$
			return copy.doSave();
		}
		return null;
	}

	/**
	 * Checks if the expected number of <i>type</i> launches appear in the
	 * manager
	 *
	 * @param count the expected count
	 * @param type the type of launches to count
	 */
	private void hasLaunches(Class<?> type, int count) {
		ILaunch[] launches = getLaunchManager().getLaunches();
		int num = 0;
		for (ILaunch launche : launches) {
			if (type.isInstance(launche)) {
				num++;
			}
		}
		assertEquals("The number of expected launches is wrong", count, num); //$NON-NLS-1$
	}

	/**
	 * Checks if the expected number of cancelled launches appear in the manager
	 *
	 * @param count the expected count
	 * @since 3.9.100
	 */
	void hasCancellingLaunches(int count) {
		hasLaunches(CancellingLaunch.class, count);
	}

	/**
	 * Tests if a launch is properly removed from the launch manager when
	 * #preLaunchCheck is cancelled
	 *
	 * @throws Exception
	 * @see https://bugs.eclipse.org/bugs/show_bug.cgi?id=437122
	 * @since 3.9.100
	 */
	@Test
	public void testCancelledPreLaunchCheck() throws Exception {
		ILaunchConfiguration config = getCancellingConfiguration(true, false, false);
		assertNotNull("The cancelling config should have been created", config); //$NON-NLS-1$
		try {
			hasCancellingLaunches(0);
			config.launch("run", new NullProgressMonitor()); //$NON-NLS-1$
			hasCancellingLaunches(0);
		} finally {
			ILaunch[] launches = getLaunchManager().getLaunches();
			for (ILaunch launche : launches) {
				getLaunchManager().removeLaunch(launche);
			}
			config.delete();
		}
	}

	/**
	 * Tests if a launch is properly removed from the launch manager when
	 * #finalLaunchCheck is cancelled
	 *
	 * @throws Exception
	 * @see https://bugs.eclipse.org/bugs/show_bug.cgi?id=437122
	 * @since 3.9.100
	 */
	@Test
	public void testCancelledFinalLaunchCheck() throws Exception {
		ILaunchConfiguration config = getCancellingConfiguration(false, true, false);
		assertNotNull("The cancelling config should have been created", config); //$NON-NLS-1$
		try {
			hasCancellingLaunches(0);
			config.launch("run", new NullProgressMonitor()); //$NON-NLS-1$
			hasCancellingLaunches(0);
		} finally {
			ILaunch[] launches = getLaunchManager().getLaunches();
			for (ILaunch launche : launches) {
				getLaunchManager().removeLaunch(launche);
			}
			config.delete();
		}
	}

	/**
	 * Tests if a launch is properly removed from the launch manager when
	 * #buildFoLaunch is cancelled
	 *
	 * @throws Exception
	 * @see https://bugs.eclipse.org/bugs/show_bug.cgi?id=437122
	 * @since 3.9.100
	 */
	@Test
	public void testCancelledBuildForLaunch() throws Exception {
		ILaunchConfiguration config = getCancellingConfiguration(false, false, true);
		assertNotNull("The cancelling config should have been created", config); //$NON-NLS-1$
		try {
			hasCancellingLaunches(0);
			config.launch("run", new NullProgressMonitor()); //$NON-NLS-1$
			hasCancellingLaunches(1); // XXX #buildForLaunch does not remove the
										// launch
		} finally {
			ILaunch[] launches = getLaunchManager().getLaunches();
			for (ILaunch launche : launches) {
				getLaunchManager().removeLaunch(launche);
			}
			config.delete();
		}
	}

	/**
	 * There was a race condition in getting a unique name for a launch
	 * configuration.
	 * <p>
	 * Note, due to the nature of the bug, it is possible that running this test
	 * will not trigger the original bug. To increase the probability of hitting
	 * the NPE in the unpatched code, increase the size of config. However,
	 * increasing the number increases the runtime of the test substantially.
	 */
	@Test
	public void testNPE_Bug484882() throws Exception {
		// In this thread continuously creates configs so that
		// org.eclipse.debug.internal.core.LaunchManager.clearConfigNameCache()
		// is called repeatedly. We also want to make lots of configurations so
		// the runtime of getAllSortedConfigNames (called by
		// isExistingLaunchConfigurationName
		// below) is long enough to hit the race condition.
		final boolean stop[] = new boolean[] { false };
		final Throwable exception[] = new Throwable[] { null };
		Thread thread = new Thread() {
			@Override
			public void run() {
				ILaunchConfiguration config[] = new ILaunchConfiguration[10000];
				try {
					for (int i = 0; i < config.length && !stop[0]; i++) {
						config[i] = getLaunchConfiguration("Name" + i); //$NON-NLS-1$
					}
					for (ILaunchConfiguration c : config) {
						if (c != null) {
							c.delete();
						}
					}
				} catch (CoreException e) {
					exception[0] = e;
				}
			}
		};
		thread.start();
		try {
			ILaunchManager launchManager = getLaunchManager();
			while (thread.isAlive()) {
				// This call generated an NPE
				launchManager.isExistingLaunchConfigurationName("Name"); //$NON-NLS-1$
			}
		} finally {
			stop[0] = true;
			thread.join(1000);
			assertFalse(thread.isAlive());
			if (exception[0] != null) {
				throw new Exception("Exception in Thread", exception[0]); //$NON-NLS-1$
			}
		}
	}

	/**
	 * Create a new configuration that will throw exception in one of the four
	 * launch delegate methods
	 *
	 * @param throwingEnum the method that should throw exception
	 * @return the new {@link ILaunchConfiguration}
	 */
	private ILaunchConfiguration getThrowingConfiguration(ThrowingEnum throwingEnum) throws Exception {
		ILaunchConfigurationType type = getLaunchManager().getLaunchConfigurationType("throwing.type"); //$NON-NLS-1$
		if (type != null) {
			ILaunchConfigurationWorkingCopy copy = type.newInstance(null, getLaunchManager().generateLaunchConfigurationName("throwing " + throwingEnum)); //$NON-NLS-1$
			copy.setAttribute("throw.preLaunchCheck", ThrowingEnum.preLaunchCheck.equals(throwingEnum)); //$NON-NLS-1$
			copy.setAttribute("throw.finalLaunchCheck", ThrowingEnum.finalLaunchCheck.equals(throwingEnum)); //$NON-NLS-1$
			copy.setAttribute("throw.buildForLaunch", ThrowingEnum.buildForLaunch.equals(throwingEnum)); //$NON-NLS-1$
			copy.setAttribute("throw.launch", ThrowingEnum.launch.equals(throwingEnum)); //$NON-NLS-1$
			return copy.doSave();
		}
		return null;
	}

	/**
	 * Tests if a launch is properly removed from the launch manager when
	 * <i>throwingEnum</i> method throws exception
	 *
	 * @throws Exception
	 * @see https://bugs.eclipse.org/bugs/show_bug.cgi?id=578302
	 */
	private void testThrowingLaunchDelegateMethod(ThrowingEnum throwingEnum) throws Exception {
		ILaunchConfiguration config = getThrowingConfiguration(throwingEnum);
		assertNotNull("The throwing config should have been created", config); //$NON-NLS-1$
		try {
			hasLaunches(ThrowingLaunch.class, 0);
			CoreException exception = assertThrows(CoreException.class, () -> config.launch("run", new NullProgressMonitor(), true, true)); //$NON-NLS-1$
			assertEquals("Wrong method throwed exception", exception.getMessage(), throwingEnum.toString());
			hasLaunches(ThrowingLaunch.class, 0);
		} finally {
			ILaunch[] launches = getLaunchManager().getLaunches();
			for (ILaunch launche : launches) {
				getLaunchManager().removeLaunch(launche);
			}
			config.delete();
		}
	}

	/**
	 * Tests if a launch is properly removed from the launch manager when
	 * #preLaunchCheck throws exception
	 *
	 * @throws Exception
	 * @see https://bugs.eclipse.org/bugs/show_bug.cgi?id=578302
	 */
	@Test
	public void testThrowingPreLaunchCheck() throws Exception {
		testThrowingLaunchDelegateMethod(ThrowingEnum.preLaunchCheck);
	}

	/**
	 * Tests if a launch is properly removed from the launch manager when
	 * #finalLaunchCheck throws exception
	 *
	 * @throws Exception
	 * @see https://bugs.eclipse.org/bugs/show_bug.cgi?id=578302
	 */
	@Test
	public void testThrowingFinalLaunchCheck() throws Exception {
		testThrowingLaunchDelegateMethod(ThrowingEnum.finalLaunchCheck);
	}

	/**
	 * Tests if a launch is properly removed from the launch manager when
	 * #buildFoLaunch throws exception
	 *
	 * @throws Exception
	 * @see https://bugs.eclipse.org/bugs/show_bug.cgi?id=578302
	 */
	@Test
	public void testThrowingBuildForLaunch() throws Exception {
		testThrowingLaunchDelegateMethod(ThrowingEnum.buildForLaunch);
	}

	/**
	 * Tests if a launch is properly removed from the launch manager when
	 * #buildFoLaunch throws exception
	 *
	 * @throws Exception
	 * @see https://bugs.eclipse.org/bugs/show_bug.cgi?id=578302
	 */
	@Test
	public void testThrowingLaunch() throws Exception {
		testThrowingLaunchDelegateMethod(ThrowingEnum.launch);
	}
}
