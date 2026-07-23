package org.eclipse.platform.testing;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.osgi.service.runnable.ApplicationLauncher;
import org.eclipse.osgi.service.runnable.ParameterizedRunnable;
import org.eclipse.pde.api.tools.annotations.NoInstantiate;
import org.eclipse.pde.api.tools.annotations.NoReference;
import org.eclipse.ui.testing.ITestHarness;
import org.eclipse.ui.testing.TestableObject;
import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.LauncherSessionListener;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.application.ApplicationDescriptor;
import org.osgi.service.application.ApplicationException;
import org.osgi.service.application.ApplicationHandle;
import org.osgi.util.tracker.ServiceTracker;

/**
 * 
 */
@NoInstantiate
public class EclipseLauncherSessionListener implements LauncherSessionListener {

	private TestableObject testableObject;
	private ApplicationHandle applicationHandle;
	private ServiceTracker<TestableObject, TestableObject> testableObjectTracker;
	private ServiceTracker<ApplicationDescriptor, ApplicationDescriptor> applicationDescriptorTracker;
	private ServiceRegistration<?> applicationService;
	private Thread applicationThread;


	@Override
	@NoReference
	public void launcherSessionOpened(LauncherSession session) {
		Bundle bundle = FrameworkUtil.getBundle(EclipseLauncherSessionListener.class);
		if (bundle == null) {
			System.err.println("Not running inside OSGi!");
			return;
		}
		BundleContext bundleContext = bundle.getBundleContext();
		if (bundleContext == null) {
			System.err.println("Not started/resolved?");
			return;
		}
		EclipseApplicationLauncher launcher = new EclipseApplicationLauncher();
		applicationService = bundleContext.registerService(ApplicationLauncher.class, launcher, null);
		testableObjectTracker = new ServiceTracker<>(bundleContext, TestableObject.class, null);
		applicationDescriptorTracker = new ServiceTracker<>(bundleContext, ApplicationDescriptor.class, null);
		applicationDescriptorTracker.open();
		testableObjectTracker.open();
		String testApplication = "org.eclipse.ui.ide.workbench"; // TODO how to access the JUnit config of the
																	// session?!?
		Map<String, ApplicationDescriptor> tracked = applicationDescriptorTracker.getTracked().entrySet().stream()
				.collect(Collectors.toMap(e -> (String) e.getKey().getProperty(ApplicationDescriptor.APPLICATION_PID),
						e -> e.getValue()));
		ApplicationDescriptor applicationDescriptor = tracked.get(testApplication);
		if (applicationDescriptor == null) {
			System.err.println("Test application '" + testApplication + "' was not found available applications are:");
			for (String appId : tracked.keySet()) {
				System.err.println("\t- " + appId);
			}
			return;
		}
		testableObject = testableObjectTracker.getService();
		if (testableObject == null) {
			System.err.println("No TestableObject found!");
			return;
		}
		CountDownLatch latch = new CountDownLatch(1);
		testableObject.setTestHarness(new ITestHarness() {

			@Override
			public void runTests() {
				latch.countDown();
			}
		});
		HashMap<String, Object> launchArgs = new HashMap<>(1);
		String[] args = new String[0];
		launchArgs.put(IApplicationContext.APPLICATION_ARGS, args);
		try {
			applicationHandle = applicationDescriptor.launch(launchArgs);
		} catch (ApplicationException e) {
			e.printStackTrace();
			return;
		}
		if (applicationHandle instanceof ParameterizedRunnable runable) {
			applicationThread = new Thread(new Runnable() {

				@Override
				public void run() {
					try {
						runable.run(args);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
			applicationThread.setName("Eclipse-Test-Application [" + testApplication + "]");
			applicationThread.setDaemon(true);
			applicationThread.start();
			try {
				latch.await();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}

	@Override
	@NoReference
	public void launcherSessionClosed(LauncherSession session) {
		if (testableObject != null) {
			testableObject.testingFinished();
		}
		if (testableObjectTracker != null) {
			testableObjectTracker.close();
		}
		if (applicationDescriptorTracker != null) {
			applicationDescriptorTracker.close();
		}
		if (applicationService != null) {
			applicationService.unregister();
		}
		if (applicationThread != null) {
			try {
				applicationThread.join();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}

}
