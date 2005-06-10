/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.configurator;

import java.io.*;
import java.net.*;
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.osgi.framework.log.*;
import org.eclipse.osgi.service.datalocation.*;
import org.eclipse.osgi.service.debug.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.update.configurator.*;
import org.osgi.framework.*;
import org.osgi.service.packageadmin.*;
import org.osgi.service.startlevel.*;
import org.osgi.util.tracker.*;

public class ConfigurationActivator implements BundleActivator, IBundleGroupProvider, IConfigurationConstants {

	public static String PI_CONFIGURATOR = "org.eclipse.update.configurator"; //$NON-NLS-1$
	public static final String INSTALL_LOCATION = "osgi.installLocation"; //$NON-NLS-1$
	public static final String LAST_CONFIG_STAMP = "last.config.stamp"; //$NON-NLS-1$
	public static final String NAME_SPACE = "org.eclipse.update"; //$NON-NLS-1$
	public static final String UPDATE_PREFIX = "update@"; //$NON-NLS-1$
	private static final String INITIAL_PREFIX = "initial@"; //$NON-NLS-1$
	
	// debug options
	public static String OPTION_DEBUG = PI_CONFIGURATOR + "/debug"; //$NON-NLS-1$
	// debug values
	public static boolean DEBUG = false;
	
	private static BundleContext context;
	private ServiceTracker platformTracker;
	private ServiceRegistration configurationFactorySR;
	private PlatformConfiguration configuration;
	
	// Location of the configuration data
	private Location configLocation;
	
	//Need to store that because it is not provided by the platformConfiguration
	private long lastTimeStamp;

	// The expected states timestamp
	private long lastStateTimeStamp;
	
	// Singleton
	private static ConfigurationActivator configurator;

	public ConfigurationActivator() {
		configurator = this;
	}
	
	public void start(BundleContext ctx) throws Exception {
		context = ctx;
		loadOptions();
		acquireFrameworkLogService();
		initialize();
		
		//Short cut, if the configuration has not changed
		if (canRunWithCachedData()) {		
			Utils.debug("Running with cached data"); //$NON-NLS-1$
			Platform.registerBundleGroupProvider(this);
			return;
		}

		Utils.debug("Starting update configurator..."); //$NON-NLS-1$

		installBundles();
		Platform.registerBundleGroupProvider(this);
	}


	private void initialize() throws Exception {
		// TODO this test is not really needed any more than any plugin has 
		// to test to see if the runtime is running.  It was there from earlier days
		// where startup was much more disjoint.  Some day that level of decoupling
		// will return but for now...
		if (!Platform.isRunning())
			throw new Exception(Messages.ConfigurationActivator_initialize); 
		
		configLocation = Platform.getConfigurationLocation();
		// create the name space directory for update (configuration/org.eclipse.update)
		if (!configLocation.isReadOnly()) {
			try {
				URL privateURL = new URL(configLocation.getURL(), NAME_SPACE);
				File f = new File(privateURL.getFile());
				if(!f.exists())
					f.mkdirs();
			} catch (MalformedURLException e1) {
				// ignore
			}
		}
		configurationFactorySR = context.registerService(IPlatformConfigurationFactory.class.getName(), new PlatformConfigurationFactory(), null);
		configuration = getPlatformConfiguration(Utils.getInstallURL(), configLocation);
		if (configuration == null)
			throw Utils.newCoreException(NLS.bind(Messages.ConfigurationActivator_createConfig, (new String[] { configLocation.getURL().toExternalForm() })), null);

		DataInputStream stream = null;
		try {
			stream = new DataInputStream(new URL(configLocation.getURL(),NAME_SPACE+'/'+LAST_CONFIG_STAMP).openStream());
			lastTimeStamp = stream.readLong();
			lastStateTimeStamp = stream.readLong();
		} catch (Exception e) {
			lastTimeStamp = configuration.getChangeStamp() - 1;
			lastStateTimeStamp = -1;
		} finally {
			if (stream != null)
				try {
					stream.close();
				} catch (IOException e1) {
					Utils.log(e1.getLocalizedMessage());
				}
		}
	}


	public void stop(BundleContext ctx) throws Exception {
		// quick fix (hack) for bug 47861
		try {
			PlatformConfiguration.shutdown();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		configurationFactorySR.unregister();
	}

	public boolean installBundles() {
		Utils.debug("Installing bundles..."); //$NON-NLS-1$
		ServiceReference reference = context.getServiceReference(StartLevel.class.getName());
		int startLevel = 4;
		String defaultStartLevel = System.getProperty("osgi.bundles.defaultStartLevel"); //$NON-NLS-1$
		if (defaultStartLevel != null) {
			try {
				startLevel = Integer.parseInt(defaultStartLevel);
			} catch (NumberFormatException e1) {
				startLevel = 4;
			}
		}
		if (startLevel < 1)
			startLevel = 4;
		
		StartLevel start = null;
		if (reference != null)
			start = (StartLevel) context.getService(reference);
		try {
			// Get the list of cached bundles and compare with the ones to be installed.
			// Uninstall all the cached bundles that do not appear on the new list
			Bundle[] cachedBundles = context.getBundles();		
			URL[] plugins = configuration.getPluginPath();
				
			// starts the list of bundles to refresh with all currently unresolved bundles (see bug 50680)
			List toRefresh = getUnresolvedBundles();

			Bundle[] bundlesToUninstall = getBundlesToUninstall(cachedBundles, plugins);
			for (int i=0; i<bundlesToUninstall.length; i++) {
				try {
					if (DEBUG)
						Utils.debug("Uninstalling " + bundlesToUninstall[i].getLocation()); //$NON-NLS-1$
					// include every bundle being uninstalled in the list of bundles to refresh (see bug 82393)					
					toRefresh.add(bundlesToUninstall[i]);					
					bundlesToUninstall[i].uninstall();
				} catch (Exception e) {
					Utils.log(NLS.bind(Messages.ConfigurationActivator_uninstallBundle, (new String[] { bundlesToUninstall[i].getLocation() })));
				}
			}
			
			// Get the urls to install
			String[] bundlesToInstall = getBundlesToInstall(cachedBundles, plugins);

			for (int i = 0; i < bundlesToInstall.length; i++) {
				try {
					if (DEBUG)
						Utils.debug("Installing " + bundlesToInstall[i]); //$NON-NLS-1$
					URL bundleURL = new URL("reference:file:"+bundlesToInstall[i]); //$NON-NLS-1$
					//Bundle target = context.installBundle(bundlesToInstall[i]);
					Bundle target = context.installBundle(UPDATE_PREFIX+bundlesToInstall[i], bundleURL.openStream());
					// any new bundle should be refreshed as well
					toRefresh.add(target);
					if (start != null)
						start.setBundleStartLevel(target, startLevel);
				
				} catch (Exception e) {
					if (!Utils.isAutomaticallyStartedBundle(bundlesToInstall[i]))
						Utils.log(NLS.bind(Messages.ConfigurationActivator_installBundle, (new String[] { bundlesToInstall[i] })) + "   " + e.getMessage()); //$NON-NLS-1$
				}
			}
			context.ungetService(reference);
			removeInitialBundles(toRefresh, cachedBundles);
			refreshPackages((Bundle[]) toRefresh.toArray(new Bundle[toRefresh.size()]));
			
			// keep track of the last config successfully processed
			writePlatformConfigurationTimeStamp();
			return true;
		} catch (Exception e) {
			return false;
		} 
	}

	private void removeInitialBundles(List bundles, Bundle[] cachedBundles) {
		String[] initialSymbolicNames = getInitialSymbolicNames(cachedBundles);
		Iterator iter = bundles.iterator();
		while(iter.hasNext()) {
			Bundle bundle = (Bundle) iter.next();
			String symbolicName = bundle.getSymbolicName();
			for(int i = 0; i < initialSymbolicNames.length; i++) {
				if (initialSymbolicNames[i].equals(symbolicName)) {
					iter.remove();
					break;
				}
			}
		}
	}

	private String[] getInitialSymbolicNames(Bundle[] cachedBundles) {
		ArrayList initial = new ArrayList();
		for (int i = 0; i < cachedBundles.length; i++) {
			Bundle bundle = cachedBundles[i];
			if (bundle.getLocation().startsWith(INITIAL_PREFIX)) {
				String symbolicName = bundle.getSymbolicName();
				if (symbolicName != null)
					initial.add(symbolicName);
			}
		}
		return (String[]) initial.toArray(new String[initial.size()]);
	}

	private List getUnresolvedBundles() {
		Bundle[] allBundles = context.getBundles();
		List unresolved = new ArrayList(); 
		for (int i = 0; i < allBundles.length; i++)
			if (allBundles[i].getState() == Bundle.INSTALLED)
				unresolved.add(allBundles[i]);
		return unresolved;
	}

	private String[] getBundlesToInstall(Bundle[] cachedBundles, URL[] newPlugins) {
		// First, create a map of the cached bundles, for faster lookup
		HashSet cachedBundlesSet = new HashSet(cachedBundles.length);
		int offset = UPDATE_PREFIX.length();
		for (int i=0; i<cachedBundles.length; i++) {
			if (cachedBundles[i].getBundleId() == 0)
				continue; // skip the system bundle
			String bundleLocation = cachedBundles[i].getLocation();
			// Ignore bundles not installed by us
			if (!bundleLocation.startsWith(UPDATE_PREFIX))
				continue;
			
			bundleLocation = bundleLocation.substring(offset);
			cachedBundlesSet.add(bundleLocation);
			// On windows, we will be doing case insensitive search as well, so lower it now
			if (Utils.isWindows)
				cachedBundlesSet.add(bundleLocation.toLowerCase());
		}
		
		ArrayList bundlesToInstall = new ArrayList(newPlugins.length);
		for (int i = 0; i < newPlugins.length; i++) {
			String location = Utils.makeRelative(Utils.getInstallURL(), newPlugins[i]).getFile();
			// check if already installed
			if (cachedBundlesSet.contains(location))
				continue;
			if (Utils.isWindows && cachedBundlesSet.contains(location.toLowerCase()))
				continue;
			
			bundlesToInstall.add(location);
		}
		return (String[])bundlesToInstall.toArray(new String[bundlesToInstall.size()]);	
	}
	
	
	private Bundle[] getBundlesToUninstall(Bundle[] cachedBundles, URL[] newPlugins) {
		// First, create a map for faster lookups
		HashSet newPluginsSet = new HashSet(newPlugins.length);
		for (int i=0; i<newPlugins.length; i++) {
			
			String pluginLocation = Utils.makeRelative(Utils.getInstallURL(), newPlugins[i]).getFile();
			newPluginsSet.add(pluginLocation);
			// On windows, we will be doing case insensitive search as well, so lower it now
			if (Utils.isWindows)
				newPluginsSet.add(pluginLocation.toLowerCase());
		}
		
		ArrayList bundlesToUninstall = new ArrayList();
		int offset = UPDATE_PREFIX.length();
		for (int i=0; i<cachedBundles.length; i++) {
			if (cachedBundles[i].getBundleId() == 0)
				continue; // skip the system bundle
			String cachedBundleLocation = cachedBundles[i].getLocation();
			// Only worry about bundles we installed
			if (!cachedBundleLocation.startsWith(UPDATE_PREFIX))
				continue;
			cachedBundleLocation = cachedBundleLocation.substring(offset);

			if (newPluginsSet.contains(cachedBundleLocation))
				continue;
			if (Utils.isWindows && newPluginsSet.contains(cachedBundleLocation.toLowerCase()))
				continue;
			
			bundlesToUninstall.add(cachedBundles[i]);
		}
		return (Bundle[])bundlesToUninstall.toArray(new Bundle[bundlesToUninstall.size()]);
	}
	

	/**
	 * Creates and starts the platform configuration.
	 * @return the just started platform configuration
	 */
	private PlatformConfiguration getPlatformConfiguration(URL installURL, Location configLocation) {
		try {
			PlatformConfiguration.startup(installURL, configLocation);
		} catch (Exception e) {
			if (platformTracker != null) {
				String message = e.getMessage();
				if (message == null)
					message = ""; //$NON-NLS-1$
				Utils.log(Utils.newStatus(message, e));
			}
		}
		return PlatformConfiguration.getCurrent();

	}

	/**
	 * Do PackageAdmin.refreshPackages() in a synchronous way.  After installing
	 * all the requested bundles we need to do a refresh and want to ensure that 
	 * everything is done before returning.
	 * @param bundles
	 */
	private void refreshPackages(Bundle[] bundles) {
		if (bundles.length == 0)
			return;
		ServiceReference packageAdminRef = context.getServiceReference(PackageAdmin.class.getName());
		PackageAdmin packageAdmin = null;
		if (packageAdminRef != null) {
			packageAdmin = (PackageAdmin) context.getService(packageAdminRef);
			if (packageAdmin == null)
				return;
		}
		// TODO this is such a hack it is silly.  There are still cases for race conditions etc
		// but this should allow for some progress...
		// (patch from John A.)
		final boolean[] flag = new boolean[] {false};
		FrameworkListener listener = new FrameworkListener() {
			public void frameworkEvent(FrameworkEvent event) {
				if (event.getType() == FrameworkEvent.PACKAGES_REFRESHED)
					synchronized (flag) {
						flag[0] = true;
						flag.notifyAll();
					}
			}
		};
		context.addFrameworkListener(listener);
		packageAdmin.refreshPackages(bundles);
		synchronized (flag) {
			while (!flag[0]) {
				try {
					flag.wait();
				} catch (InterruptedException e) {
				}
			}
		}
		context.removeFrameworkListener(listener);
		context.ungetService(packageAdminRef);
	}
	
	private void writePlatformConfigurationTimeStamp() {
		DataOutputStream stream = null;
		try {
			if (configLocation.isReadOnly())
				return;
			
			String configArea = configLocation.getURL().getFile();
			lastTimeStamp = configuration.getChangeStamp();
			lastStateTimeStamp = Platform.getPlatformAdmin().getState(false).getTimeStamp();
			stream = new DataOutputStream(new FileOutputStream(configArea +File.separator+ NAME_SPACE+ File.separator+ LAST_CONFIG_STAMP));
			stream.writeLong(lastTimeStamp);
			stream.writeLong(lastStateTimeStamp);
		} catch (Exception e) {
			Utils.log(e.getLocalizedMessage());
		} finally {
			if (stream != null)
				try {
					stream.close();
				} catch (IOException e1) {
					Utils.log(e1.getLocalizedMessage());
				}
		}
	}

	private void loadOptions() {
		// all this is only to get the application args		
		DebugOptions service = null;
		ServiceReference reference = context.getServiceReference(DebugOptions.class.getName());
		if (reference != null)
			service = (DebugOptions) context.getService(reference);
		if (service == null)
			return;
		try {
			DEBUG = service.getBooleanOption(OPTION_DEBUG, false);
		} finally {
			// we have what we want - release the service
			context.ungetService(reference);
		}
	}
	
	private boolean canRunWithCachedData() {
		return  !"true".equals(System.getProperty("osgi.checkConfiguration")) && //$NON-NLS-1$ //$NON-NLS-2$
				lastTimeStamp==configuration.getChangeStamp() &&
				lastStateTimeStamp==Platform.getPlatformAdmin().getState(false).getTimeStamp();
	}
				
	public static BundleContext getBundleContext() {
		return context;
	}
		
	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IBundleGroupProvider#getName()
	 */
	public String getName() {
		return Messages.BundleGroupProvider; 
	}
	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IBundleGroupProvider#getBundleGroups()
	 */
	public IBundleGroup[] getBundleGroups() {
		if (configuration == null)
			return new IBundleGroup[0];
		else {
			IPlatformConfiguration.IFeatureEntry[] features = configuration.getConfiguredFeatureEntries();
			ArrayList bundleGroups = new ArrayList(features.length);
			for (int i=0; i<features.length; i++) {
				if (features[i] instanceof FeatureEntry 
					&& ((FeatureEntry)features[i]).hasBranding())
					bundleGroups.add(features[i]);
			}
			return (IBundleGroup[])bundleGroups.toArray(new IBundleGroup[bundleGroups.size()]);
		}
	}

	public static void setConfigurator(ConfigurationActivator configurator) {
		ConfigurationActivator.configurator = configurator;
	}

	public static ConfigurationActivator getConfigurator() {
		return configurator;
	}
	
	private void acquireFrameworkLogService() throws Exception{
		ServiceReference logServiceReference = context.getServiceReference(FrameworkLog.class.getName());
		if (logServiceReference == null)
			return;
		Utils.log  = (FrameworkLog) context.getService(logServiceReference);
	}
}	

