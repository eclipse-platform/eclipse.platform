/*******************************************************************************
 *  Copyright (c) 2016, 2019 SSI Schaefer IT Solutions GmbH and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *      SSI Schaefer IT Solutions GmbH
 *******************************************************************************/
package org.eclipse.debug.tests.launching;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchDelegate;
import org.eclipse.debug.core.ILaunchListener;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.IStreamListener;
import org.eclipse.debug.core.model.IDisconnect;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate2;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStreamMonitor;
import org.eclipse.debug.core.model.IStreamsProxy;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;
import org.eclipse.debug.internal.core.LaunchManager;
import org.eclipse.debug.internal.core.groups.GroupLaunchConfigurationDelegate;
import org.eclipse.debug.internal.core.groups.GroupLaunchElement;
import org.eclipse.debug.internal.core.groups.GroupLaunchElement.GroupElementPostLaunchAction;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchHistory;
import org.eclipse.debug.tests.TestUtil;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class LaunchGroupTests extends AbstractLaunchTest {

	private static final String GROUP_TYPE = "org.eclipse.debug.core.groups.GroupLaunchConfigurationType"; //$NON-NLS-1$
	private static final String DEF_GRP_NAME = "Test Group"; //$NON-NLS-1$

	private final AtomicInteger launchCount = new AtomicInteger(0);
	private ILaunchConfiguration lcToCount = null;
	private ILaunchListener lcListener = new ILaunchListener() {
		@Override
		public void launchRemoved(ILaunch launch) {
		}

		@Override
		public void launchChanged(ILaunch launch) {
		}

		@Override
		public void launchAdded(ILaunch launch) {
			if (launch.getLaunchConfiguration().contentsEqual(lcToCount)) {
				launchCount.incrementAndGet();
			}
		}
	};

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();

		// reset count
		launchCount.set(0);
	}

	@Override
	@After
	public void tearDown() throws Exception {
		// make sure listener is removed
		getLaunchManager().removeLaunchListener(lcListener);
		ILaunch[] launches = getLaunchManager().getLaunches();
		for (ILaunch launch : launches) {
			try {
				if (!launch.isTerminated()) {
					IProcess[] processes = launch.getProcesses();
					for (IProcess process : processes) {
						process.terminate();
					}
					launch.terminate();
				}
			} catch (Exception e) {
				TestUtil.log(IStatus.ERROR, name.getMethodName(), "Error terminating launch: " + launch, e);
			}
		}
		super.tearDown();
	}

	private ILaunchConfiguration createLaunchGroup(String name, GroupLaunchElement... children) throws CoreException {
		ILaunchConfigurationWorkingCopy grp = getLaunchManager().getLaunchConfigurationType(GROUP_TYPE).newInstance(null, name);
		GroupLaunchConfigurationDelegate.storeLaunchElements(grp, Arrays.asList(children));
		return grp.doSave();
	}

	private GroupLaunchElement createLaunchGroupElement(ILaunchConfiguration source, GroupElementPostLaunchAction action, Object param, boolean adopt) {
		GroupLaunchElement e = new GroupLaunchElement();

		e.name = source.getName();
		e.data = source;
		e.action = action;
		e.actionParam = param;
		e.mode = GroupLaunchElement.MODE_INHERIT;
		e.enabled = true;
		e.adoptIfRunning = adopt;

		return e;
	}

	private LaunchHistory getRunLaunchHistory() {
		LaunchHistory h = getLaunchConfigurationManager().getLaunchHistory(IDebugUIConstants.ID_RUN_LAUNCH_GROUP);

		// clear the history
		for (ILaunchConfiguration c : h.getHistory()) {
			h.removeFromHistory(c);
		}

		return h;
	}

	@Test
	public void testNone() throws Exception {
		ILaunchConfiguration t1 = getLaunchConfiguration("Test1"); //$NON-NLS-1$
		ILaunchConfiguration t2 = getLaunchConfiguration("Test2"); //$NON-NLS-1$
		ILaunchConfiguration grp = createLaunchGroup(DEF_GRP_NAME, createLaunchGroupElement(t1, GroupElementPostLaunchAction.NONE, null, false), createLaunchGroupElement(t2, GroupElementPostLaunchAction.NONE, null, false));

		// attention: need to do this before launching!
		LaunchHistory runHistory = getRunLaunchHistory();
		grp.launch(ILaunchManager.RUN_MODE, new NullProgressMonitor());

		ILaunchConfiguration[] history = runHistory.getHistory();
		assertEquals(3, history.length);
		assertTrue("history[0] should be Test Group", history[0].contentsEqual(grp)); //$NON-NLS-1$
		assertTrue("history[1] should be Test2", history[1].contentsEqual(t2)); //$NON-NLS-1$
		assertTrue("history[2] should be Test1", history[2].contentsEqual(t1)); //$NON-NLS-1$
	}

	@Test
	public void testDelay() throws Exception {
		ILaunchConfiguration t1 = getLaunchConfiguration("Test1"); //$NON-NLS-1$
		ILaunchConfiguration t2 = getLaunchConfiguration("Test2"); //$NON-NLS-1$
		ILaunchConfiguration grp = createLaunchGroup(DEF_GRP_NAME, createLaunchGroupElement(t1, GroupElementPostLaunchAction.DELAY, 2, false), createLaunchGroupElement(t2, GroupElementPostLaunchAction.NONE, null, false));

		long start = System.currentTimeMillis();
		// attention: need to do this before launching!
		LaunchHistory runHistory = getRunLaunchHistory();
		grp.launch(ILaunchManager.RUN_MODE, new NullProgressMonitor());

		assertTrue("delay was not awaited", (System.currentTimeMillis() - start) >= 2000); //$NON-NLS-1$

		ILaunchConfiguration[] history = runHistory.getHistory();
		assertEquals(3, history.length);
		assertTrue("history[0] should be Test Group", history[0].contentsEqual(grp)); //$NON-NLS-1$
		assertTrue("history[1] should be Test2", history[1].contentsEqual(t2)); //$NON-NLS-1$
		assertTrue("history[2] should be Test1", history[2].contentsEqual(t1)); //$NON-NLS-1$
	}

	@Test
	public void testTerminated() throws Exception {
		final ILaunchConfiguration t1 = getLaunchConfiguration("Test1"); //$NON-NLS-1$
		final ILaunchConfiguration t2 = getLaunchConfiguration("Test2"); //$NON-NLS-1$
		ILaunchConfiguration grp = createLaunchGroup(DEF_GRP_NAME, createLaunchGroupElement(t1, GroupElementPostLaunchAction.WAIT_FOR_TERMINATION, null, false), createLaunchGroupElement(t2, GroupElementPostLaunchAction.NONE, null, false));

		long start = System.currentTimeMillis();
		new Thread("Terminate Test1") { //$NON-NLS-1$
			@Override
			public void run() {
				try {
					// wait for some time
					Thread.sleep(2000);

					// now find and nuke Test1
					for (ILaunch l : getLaunchManager().getLaunches()) {
						if (l.getLaunchConfiguration().contentsEqual(t1)) {
							// add a dummy process, otherwise the launch never
							// terminates...
							attachDummyProcess(l);
							l.terminate();
						}
					}
				} catch (Exception e) {
					TestUtil.log(IStatus.ERROR, getName(), e.getMessage(), e);
				}
			}

		}.start();

		// attention: need to do this before launching!
		LaunchHistory runHistory = getRunLaunchHistory();
		grp.launch(ILaunchManager.RUN_MODE, new NullProgressMonitor());

		assertTrue("returned before termination of Test1", (System.currentTimeMillis() - start) >= 2000); //$NON-NLS-1$

		// is there a way to assert that the group waited for test1 to
		// terminate? don't think so - at least run the code path to have it
		// covered.
		ILaunchConfiguration[] history = runHistory.getHistory();
		assertEquals(3, history.length);
		assertTrue("history[0] should be Test Group", history[0].contentsEqual(grp)); //$NON-NLS-1$
		assertTrue("history[1] should be Test2", history[1].contentsEqual(t2)); //$NON-NLS-1$
		assertTrue("history[2] should be Test1", history[2].contentsEqual(t1)); //$NON-NLS-1$
	}

	@Test
	public void testAdopt() throws Exception {
		final ILaunchConfiguration t1 = getLaunchConfiguration("Test1"); //$NON-NLS-1$
		final ILaunchConfiguration grp = createLaunchGroup(DEF_GRP_NAME, createLaunchGroupElement(t1, GroupElementPostLaunchAction.NONE, null, false), createLaunchGroupElement(t1, GroupElementPostLaunchAction.NONE, null, true));

		// attention: need to do this before launching!
		LaunchHistory runHistory = getRunLaunchHistory();

		lcToCount = t1;
		getLaunchManager().addLaunchListener(lcListener);
		grp.launch(ILaunchManager.RUN_MODE, new NullProgressMonitor());

		ILaunchConfiguration[] history = runHistory.getHistory();
		assertEquals(2, history.length);
		assertTrue("history[0] should be Test Group", history[0].contentsEqual(grp)); //$NON-NLS-1$
		assertTrue("history[1] should be Test1", history[1].contentsEqual(t1)); //$NON-NLS-1$
		assertEquals("Test1 should be launched only once", 1, launchCount.get()); //$NON-NLS-1$
	}

	@Test
	public void testAdoptComplex() throws Exception {
		final ILaunchConfiguration t1 = getLaunchConfiguration("Test1"); //$NON-NLS-1$

		// Group 1 has Test1 (adopt = false)
		// Group 2 has Group 1
		// Group 3 has Group 2 and Test1 (adopt = true)

		final ILaunchConfiguration grp = createLaunchGroup(DEF_GRP_NAME, createLaunchGroupElement(t1, GroupElementPostLaunchAction.NONE, null, false));
		final ILaunchConfiguration grp2 = createLaunchGroup("Group 2", createLaunchGroupElement(grp, GroupElementPostLaunchAction.NONE, null, false)); //$NON-NLS-1$
		final ILaunchConfiguration grp3 = createLaunchGroup("Group 3", createLaunchGroupElement(grp2, GroupElementPostLaunchAction.NONE, null, false), createLaunchGroupElement(t1, GroupElementPostLaunchAction.DELAY, 10, true)); //$NON-NLS-1$

		// attention: need to do this before launching!
		LaunchHistory runHistory = getRunLaunchHistory();

		lcToCount = t1;
		getLaunchManager().addLaunchListener(lcListener);

		long startTime = System.currentTimeMillis();
		grp3.launch(ILaunchManager.RUN_MODE, new NullProgressMonitor());

		ILaunchConfiguration[] history = runHistory.getHistory();
		assertTrue("post launch should not be run", (System.currentTimeMillis() - startTime) < 9_000); //$NON-NLS-1$
		assertEquals(4, history.length);
		assertTrue("history[0] should be Group 3", history[0].contentsEqual(grp3)); //$NON-NLS-1$
		assertTrue("history[1] should be Group 2", history[1].contentsEqual(grp2)); //$NON-NLS-1$
		assertTrue("history[2] should be Group 1", history[2].contentsEqual(grp)); //$NON-NLS-1$
		assertTrue("history[3] should be Test1", history[3].contentsEqual(t1)); //$NON-NLS-1$
		assertEquals("Test1 should be launched only once", 1, launchCount.get()); //$NON-NLS-1$
	}

	@Test
	public void testWaitForOutput() throws Exception {
		String testOutput = "TestOutput"; //$NON-NLS-1$

		final ILaunchConfiguration t1 = getLaunchConfiguration("Test1"); //$NON-NLS-1$
		ILaunchConfiguration t2 = getLaunchConfiguration("Test2"); //$NON-NLS-1$
		ILaunchConfiguration grp = createLaunchGroup(DEF_GRP_NAME, createLaunchGroupElement(t1, GroupElementPostLaunchAction.OUTPUT_REGEXP, testOutput, false), createLaunchGroupElement(t2, GroupElementPostLaunchAction.NONE, null, false));

		// attach a dummy process to the launch once it is launched.
		final DummyAttachListener attachListener = new DummyAttachListener(t1);
		getLaunchManager().addLaunchListener(attachListener);

		final AtomicBoolean finished = new AtomicBoolean();
		long start = System.currentTimeMillis();
		// start a thread that will produce output on the dummy process after
		// some time
		new Thread("Output Producer") { //$NON-NLS-1$
			@Override
			public void run() {
				try {
					// wait some time before causing the group to continue
					Thread.sleep(2000);
					synchronized (finished) {
						attachListener.getStream().write("TestOutput"); //$NON-NLS-1$
						finished.set(true);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}.start();

		// attention: need to do this before launching!
		LaunchHistory runHistory = getRunLaunchHistory();

		// launching the group must block until the output is produced
		grp.launch(ILaunchManager.RUN_MODE, new NullProgressMonitor());

		synchronized (finished) {
			// if the output appeared we have to wait for the thread to finish
			// setting state.
			getLaunchManager().removeLaunchListener(attachListener);
		}

		assertTrue("thread did not finish", finished.get()); //$NON-NLS-1$
		assertTrue("output was not awaited", (System.currentTimeMillis() - start) >= 2000); //$NON-NLS-1$

		ILaunchConfiguration[] history = runHistory.getHistory();
		assertEquals(3, history.length);
		assertTrue("history[0] should be Test Group", history[0].contentsEqual(grp)); //$NON-NLS-1$
		assertTrue("history[1] should be Test2", history[1].contentsEqual(t2)); //$NON-NLS-1$
		assertTrue("history[2] should be Test1", history[2].contentsEqual(t1)); //$NON-NLS-1$
	}

	@Test
	public void testRename() throws Exception {
		ILaunchConfiguration t1 = getLaunchConfiguration("Test1"); //$NON-NLS-1$
		ILaunchConfiguration t2 = getLaunchConfiguration("Test2"); //$NON-NLS-1$
		ILaunchConfiguration grp = createLaunchGroup(DEF_GRP_NAME, createLaunchGroupElement(t1, GroupElementPostLaunchAction.NONE, null, false), createLaunchGroupElement(t2, GroupElementPostLaunchAction.NONE, null, false));

		ILaunchConfigurationWorkingCopy workingCopy = t1.getWorkingCopy();
		workingCopy.rename("AnotherTest"); //$NON-NLS-1$
		workingCopy.doSave();

		assertTrue("name should not be transiently updated", grp.getName().equals(DEF_GRP_NAME)); //$NON-NLS-1$

		// need to re-fetch configuration
		grp = getLaunchConfiguration(DEF_GRP_NAME);
		List<GroupLaunchElement> elements = GroupLaunchConfigurationDelegate.createLaunchElements(grp);

		assertTrue("group element should be updated", elements.get(0).name.equals("AnotherTest")); //$NON-NLS-1$//$NON-NLS-2$
	}

	/**
	 * Test for Bug 529651. Build before launch was not invoked for launches
	 * started as part of group launch.
	 */
	@Test
	public void testBuildBeforeLaunch() throws CoreException {
		final AtomicInteger launched = new AtomicInteger(0);
		final AtomicInteger buildRequested = new AtomicInteger(0);
		final ILaunchConfigurationDelegate2 customLaunchDelegate = new LaunchConfigurationDelegate() {
			@Override
			public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException {
				launched.incrementAndGet();
			}

			@Override
			public boolean buildForLaunch(ILaunchConfiguration configuration, String mode, IProgressMonitor monitor) throws CoreException {
				buildRequested.incrementAndGet();
				return false;
			}
		};
		final ILaunchDelegate launchDelegate = ((LaunchManager) DebugPlugin.getDefault().getLaunchManager()).getLaunchDelegate(LaunchConfigurationTests.ID_TEST_LAUNCH_TYPE);
		final TestLaunchDelegate testLaunchDelegate = (TestLaunchDelegate) launchDelegate.getDelegate();
		testLaunchDelegate.setDelegate(customLaunchDelegate);
		final boolean oldBuildBeforePref = DebugUIPlugin.getDefault().getPreferenceStore().getBoolean(IDebugUIConstants.PREF_BUILD_BEFORE_LAUNCH);
		try {
			ILaunchConfigurationWorkingCopy lc = getLaunchConfiguration("Test1").getWorkingCopy(); //$NON-NLS-1$
			GroupLaunchElement ge = createLaunchGroupElement(lc, GroupElementPostLaunchAction.NONE, null, false);
			ILaunchConfiguration group = createLaunchGroup(DEF_GRP_NAME, ge);

			DebugUIPlugin.getDefault().getPreferenceStore().setValue(IDebugUIConstants.PREF_BUILD_BEFORE_LAUNCH, false);
			group.launch(ILaunchManager.RUN_MODE, new NullProgressMonitor(), false);
			assertEquals("Element not launched.", 1, launched.get()); //$NON-NLS-1$
			assertEquals("Build even though it was disabled.", 0, buildRequested.get()); //$NON-NLS-1$

			DebugUIPlugin.getDefault().getPreferenceStore().setValue(IDebugUIConstants.PREF_BUILD_BEFORE_LAUNCH, true);
			group.launch(ILaunchManager.RUN_MODE, new NullProgressMonitor(), true);
			assertEquals("Element not launched.", 2, launched.get()); //$NON-NLS-1$
			assertEquals("Requested build was ignored.", 1, buildRequested.get()); //$NON-NLS-1$
		} finally {
			testLaunchDelegate.setDelegate(null);
			DebugUIPlugin.getDefault().getPreferenceStore().setValue(IDebugUIConstants.PREF_BUILD_BEFORE_LAUNCH, oldBuildBeforePref);
		}
	}

	private static DummyStream attachDummyProcess(final ILaunch l) {
		final DummyStream dummy = new DummyStream();
		final InvocationHandler streamProxyHandler = new InvocationHandler() {
			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				String name = method.getName();
				if (name.equals("getOutputStreamMonitor")) { //$NON-NLS-1$
					return dummy;
				}
				return null;
			}
		};

		final InvocationHandler handler = new InvocationHandler() {
			boolean terminated = false;
			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				String name = method.getName();
				if (name.equals("equals")) { //$NON-NLS-1$
					return args.length == 1 && proxy == args[0];
				}
				if (name.equals("getStreamsProxy")) { //$NON-NLS-1$
					return Proxy.newProxyInstance(LaunchGroupTests.class.getClassLoader(), new Class[] {
							IStreamsProxy.class }, streamProxyHandler);
				}
				if (name.equals("getLaunch")) { //$NON-NLS-1$
					return l;
				}
				if (name.equals("getLabel")) { //$NON-NLS-1$
					return l.getLaunchConfiguration().getName();
				}
				if (name.equals("getAttribute")) { //$NON-NLS-1$
					return null;
				}
				if (name.equals("isTerminated")) { //$NON-NLS-1$
					return terminated;
				}
				if (name.equals("terminate")) { //$NON-NLS-1$
					terminated = true;
				}
				if (name.equals("getAdapter")) { //$NON-NLS-1$
					return null;
				}
				if (name.equals("hashCode")) { //$NON-NLS-1$
					return Integer.valueOf(0);
				}
				return Boolean.TRUE;
			}
		};
		IProcess process = (IProcess) Proxy.newProxyInstance(LaunchGroupTests.class.getClassLoader(), new Class[] {
				IProcess.class, IDisconnect.class }, handler);
		l.addProcess(process);
		return dummy;
	}

	private static final class DummyStream implements IStreamMonitor {

		private final List<IStreamListener> listeners = new ArrayList<>();

		@Override
		public void addListener(IStreamListener listener) {
			listeners.add(listener);
		}

		@Override
		public String getContents() {
			return null;
		}

		@Override
		public void removeListener(IStreamListener listener) {
			listeners.remove(listener);
		}

		public void write(String s) {
			for (IStreamListener l : listeners) {
				l.streamAppended(s, null);
			}
		}

	}

	private static final class DummyAttachListener implements ILaunchListener {

		private ILaunchConfiguration cfg;
		private DummyStream stream;

		public DummyAttachListener(ILaunchConfiguration cfg) {
			this.cfg = cfg;
		}

		public DummyStream getStream() {
			return stream;
		}

		@Override
		public void launchRemoved(ILaunch launch) {
		}

		@Override
		public void launchAdded(ILaunch launch) {
			if (launch.getLaunchConfiguration().equals(cfg)) {
				stream = attachDummyProcess(launch);
			}
		}

		@Override
		public void launchChanged(ILaunch launch) {
		}

	}

}
