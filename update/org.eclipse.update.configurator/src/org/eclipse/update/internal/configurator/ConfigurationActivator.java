/*******************************************************************************
 * Copyright (c) 2003, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.configurator;

import java.io.*;
import java.net.*;
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.osgi.service.datalocation.*;
import org.eclipse.osgi.service.debug.*;
import org.eclipse.update.configurator.*;
import org.osgi.framework.*;
import org.osgi.service.packageadmin.*;
import org.osgi.service.startlevel.*;
import org.osgi.util.tracker.*;

public class ConfigurationActivator implements BundleActivator, IBundleGroupProvider {

	public static String PI_CONFIGURATOR = "org.eclipse.update.configurator";
	public static final String INSTALL_LOCATION = "osgi.installLocation";
	public static final String LAST_CONFIG_STAMP = "last.config.stamp";
	public static final String NAME_SPACE = "org.eclipse.update";
	
	// debug options
	public static String OPTION_DEBUG = PI_CONFIGURATOR + "/debug";
	// debug values
	public static boolean DEBUG = false;
	// os
	private static boolean isWindows = System.getProperty("os.name").startsWith("Win");
	
	private static BundleContext context;
	private ServiceTracker platformTracker;
	private ServiceRegistration configurationFactorySR;
	private IPlatform platform;
	private PlatformConfiguration configuration;
	
	// Install location
	private static URL installURL;
	
	// Location of the configuration data
	private Location configLocation;
	
	//Need to store that because it is not provided by the platformConfiguration
	private long lastTimeStamp;
	
	// Singleton
	private static ConfigurationActivator configurator;

	public ConfigurationActivator() {
		configurator = this;
	}
	
	public void start(BundleContext ctx) throws Exception {
		context = ctx;
		
		loadOptions();
		initialize();
		
		//Short cut, if the configuration has not changed
		String application = configuration.getApplicationIdentifier();
		String product = configuration.getPrimaryFeatureIdentifier();
		
		if (lastTimeStamp==configuration.getChangeStamp() && System.getProperties().get("osgi.dev") == null) {		
			Utils.debug("Same last time stamp *****");
			if (System.getProperty("eclipse.application") == null && application != null) {
				Utils.debug("no eclipse.application, setting it and returning");
				System.setProperty("eclipse.application", application);
			}
			if (System.getProperty("eclipse.product") == null && product != null) {
				Utils.debug("no eclipse.product, setting it and returning");
				System.setProperty("eclipse.product", product);
			}
			return;
		}

		Utils.debug("Starting update configurator...");

		installBundles();
		platform.registerBundleGroupProvider(this);
	}


	private void initialize() throws Exception {
		platform = acquirePlatform();
		if (platform==null)
			throw new Exception("Can not start");
		
		// we can now log to the log file for this bundle
		Utils.setLog(platform.getLog(context.getBundle()));
		
		installURL = platform.getInstallURL();
		configLocation = platform.getConfigurationLocation();
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
		configuration = getPlatformConfiguration(installURL, configLocation);
		if (configuration == null)
			throw Utils.newCoreException("Cannot create configuration in " + configLocation.getURL(), null);

		try {
			DataInputStream stream = new DataInputStream(new URL(configLocation.getURL(),NAME_SPACE+'/'+LAST_CONFIG_STAMP).openStream());
			lastTimeStamp = stream.readLong();
			stream.close();
		} catch (Exception e) {
			lastTimeStamp = configuration.getChangeStamp() - 1;
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
		
		platform = null;
		releasePlatform();
		configurationFactorySR.unregister();
	}

	public boolean installBundles() {
		Utils.debug("Installing bundles...");
		ServiceReference reference = context.getServiceReference(StartLevel.class.getName());
		StartLevel start = null;
		if (reference != null)
			start = (StartLevel) context.getService(reference);
		try {
			// Get the list of cached bundles and compare with the ones to be installed.
			// Uninstall all the cached bundles that do not appear on the new list
			Bundle[] cachedBundles = context.getBundles();
			URL[] plugins = configuration.getPluginPath();
			Bundle[] bundlesToUninstall = getBundlesToUninstall(cachedBundles, plugins);
			for (int i=0; i<bundlesToUninstall.length; i++) {
				try {
					if (DEBUG)
						Utils.debug("Uninstalling " + bundlesToUninstall[i].getLocation());
					bundlesToUninstall[i].uninstall();
				} catch (Exception e) {
					Utils.log("Could not uninstall unused bundle " + bundlesToUninstall[i].getLocation());
				}
			}
			
			// starts the list of bundles to refresh with all currently unresolved bundles (see bug 50680)
			List toRefresh = getUnresolvedBundles();

			for (int i = 0; i < plugins.length; i++) {
				String location = plugins[i].toExternalForm();
				try {
					// TODO this is only because of PDE writing "plugin.xml" in platform.xml
					if(location.endsWith(".xml"))
						location = location.substring(0, location.lastIndexOf('/')+1);
					//
					location = "reference:" + location;
					if (!isInstalled(location)) {
						if (DEBUG)
							Utils.debug("Installing " + location);
						Bundle target = context.installBundle(location);
						// any new bundle should be refreshed as well
						toRefresh.add(target);
						if (start != null)
							start.setBundleStartLevel(target, 4);
					}
				} catch (Exception e) {
					if ((location.indexOf("org.eclipse.core.boot") == -1) && (location.indexOf("org.eclipse.osgi") == -1)) {
						Utils.log(Utils.newStatus("Ignoring bundle at: " + location, e));
					}
				}
			}
			context.ungetService(reference);
			refreshPackages((Bundle[]) toRefresh.toArray(new Bundle[toRefresh.size()]));
			
			if (System.getProperty("eclipse.application") == null && configuration.getApplicationIdentifier() != null)
				System.setProperty("eclipse.application", configuration.getApplicationIdentifier());
			if (System.getProperty("eclipse.product") == null && configuration.getPrimaryFeatureIdentifier() != null)
				System.setProperty("eclipse.product", configuration.getPrimaryFeatureIdentifier());
			
			// keep track of the last config successfully processed
			writePlatformConfigurationTimeStamp();
			return true;
		} catch (Exception e) {
			return false;
		} finally {
			releasePlatform();
		}
	}

	private List getUnresolvedBundles() {
		Bundle[] allBundles = context.getBundles();
		List unresolved = new ArrayList(); 
		for (int i = 0; i < allBundles.length; i++)
			if (allBundles[i].getState() == Bundle.INSTALLED)
				unresolved.add(allBundles[i]);
		return unresolved;
	}

	private Bundle[] getBundlesToUninstall(Bundle[] cachedBundles, URL[] newPlugins) {
		ArrayList bundlesToUninstall = new ArrayList();
		for (int i=0; i<cachedBundles.length; i++) {
			if (cachedBundles[i].getBundleId() == 0)
				continue; // skip the system bundle
			String cachedBundleLocation = cachedBundles[i].getLocation();
			boolean found = false;
			for (int j=0; !found && j<newPlugins.length; j++) {
				String newPluginLocation = newPlugins[j].toExternalForm();
				// TODO this is only because of PDE writing "plugin.xml" in platform.xml
				if(newPluginLocation.endsWith(".xml"))
					newPluginLocation = newPluginLocation.substring(0, newPluginLocation.lastIndexOf('/')+1);
				//
				newPluginLocation = "reference:" + newPluginLocation;
				if (newPluginLocation.equals(cachedBundleLocation))
					found = true;
				else if (isWindows && newPluginLocation.equalsIgnoreCase(cachedBundleLocation))
					found = true;
			}
			if (!found)
				bundlesToUninstall.add(cachedBundles[i]);
		}
		return (Bundle[])bundlesToUninstall.toArray(new Bundle[bundlesToUninstall.size()]);
	}

	/**
	 * This is a major hack to try to get the reconciler application running. However we should find a way to not run it.
	 * @param args
	 * @param metaPath
	 * @return
	 */
	private PlatformConfiguration getPlatformConfiguration(URL installURL, Location configLocation) {
		try {
			PlatformConfiguration.startup(installURL, configLocation);
		} catch (Exception e) {
			if (platformTracker != null) {
				String message = e.getMessage();
				if (message == null)
					message = "";
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

	/*
	 * location ends in /  (later we will support jar as well)
	 */
	private boolean isInstalled(String location) {
		Bundle[] installed = context.getBundles();
		for (int i = 0; i < installed.length; i++) {
			Bundle bundle = installed[i];
			String bundleLocation = bundle.getLocation();
			if (location.equals(bundleLocation))
				return true;
			else if (isWindows && location.equalsIgnoreCase(bundleLocation))
				return true;
		}
		return false;
	}
	
	private void writePlatformConfigurationTimeStamp() {
		try {
			if (configLocation.isReadOnly())
				return;
			
			String configArea = configLocation.getURL().getFile();
			lastTimeStamp = configuration.getChangeStamp();
			DataOutputStream stream = new DataOutputStream(new FileOutputStream(configArea +File.separator+ NAME_SPACE+ File.separator+ LAST_CONFIG_STAMP));
			stream.writeLong(lastTimeStamp);
		} catch (Exception e) {
			Utils.log(e.getLocalizedMessage());
		}
	}

	
	private IPlatform acquirePlatform() {
		if (platformTracker == null) {
			platformTracker = new ServiceTracker(context, IPlatform.class.getName(), null);
			platformTracker.open();
		}
		IPlatform result = (IPlatform) platformTracker.getService();
		return result;
	}
	
	private void releasePlatform() {
		if (platformTracker == null)
			return;
		platformTracker.close();
		platformTracker = null;
		Utils.setLog(null);
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
	public static BundleContext getBundleContext() {
		return context;
	}
		
	public static URL getInstallURL() {
		if (installURL == null)
			try {
				installURL = new URL((String) System.getProperty(INSTALL_LOCATION)); //$NON-NLS-1$
			} catch (MalformedURLException e) {
				//This can't fail because the location was set coming in
			}
			return installURL;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IBundleGroupProvider#getName()
	 */
	public String getName() {
		return Messages.getString("BundleGroupProvider");
	}
	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IBundleGroupProvider#getBundleGroups()
	 */
	public IBundleGroup[] getBundleGroups() {
		if (configuration == null)
			return new IBundleGroup[0];
		else {
			// TODO handle unmanaged plugins later
			return (IBundleGroup[])configuration.getConfiguredFeatureEntries();
		}
	}

	public static void setConfigurator(ConfigurationActivator configurator) {
		ConfigurationActivator.configurator = configurator;
	}

	public static ConfigurationActivator getConfigurator() {
		return configurator;
	}
}