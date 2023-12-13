package org.eclipse.core.pki;

import org.eclipse.osgi.framework.log.FrameworkLog;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

public class MyRunme implements BundleActivator {
	
	public static final String ID = "org.eclipse.core.pki"; //$NON-NLS-1$
	private static MyRunme instance;

	private static ServiceTracker<FrameworkLog, FrameworkLog> logTracker;
	
	public MyRunme() {
		super();
		instance = this;
	}
	
	@Override
	public void start(BundleContext context) throws Exception {
		// TODO Auto-generated method stub
		System.out.println("MYRUNME EARLY Startup");
	}
	@Override
	public void stop(BundleContext context) throws Exception {
		// TODO Auto-generated method stub
		
	}
}
