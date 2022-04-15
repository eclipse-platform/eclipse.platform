/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
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
 *     Julian Chen - fix for bug #92572, jclRM
 *     Benjamin Cabe <benjamin.cabe@anyware-tech.com> - fix for bug 265532
 *     Christoph Läubrich - remove InternalPlatform.getDefault().log (bug 55083)
 *******************************************************************************/
package org.eclipse.core.internal.runtime;

import java.io.File;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.eclipse.core.internal.preferences.exchange.ILegacyPreferences;
import org.eclipse.core.internal.preferences.exchange.IProductPreferencesService;
import org.eclipse.core.internal.preferences.legacy.InitLegacyPreferences;
import org.eclipse.core.internal.preferences.legacy.ProductPreferencesService;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.content.IContentTypeManager;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.equinox.internal.app.*;
import org.eclipse.equinox.internal.app.Activator;
import org.eclipse.equinox.log.*;
import org.eclipse.osgi.container.ModuleContainer;
import org.eclipse.osgi.framework.log.FrameworkLog;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.service.environment.EnvironmentInfo;
import org.eclipse.osgi.service.resolver.PlatformAdmin;
import org.osgi.framework.*;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.wiring.*;
import org.osgi.resource.Namespace;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Bootstrap class for the platform. It is responsible for setting up the
 * platform class loader and passing control to the actual application class
 */
public final class InternalPlatform {

	private static final String[] ARCH_LIST = { Platform.ARCH_AARCH64, Platform.ARCH_X86, Platform.ARCH_X86_64 };

	// debug support:  set in loadOptions()
	public static boolean DEBUG = false;
	public static boolean DEBUG_PLUGIN_PREFERENCES = false;

	private boolean splashEnded = false;
	private boolean initialized;
	private static final String KEYRING = "-keyring"; //$NON-NLS-1$
	private String keyringFile;

	private Map<Bundle, Log> logs = new ConcurrentHashMap<>(5);

	private static final String[] OS_LIST = { Platform.OS_LINUX, Platform.OS_MACOSX, Platform.OS_WIN32 };
	private String password = ""; //$NON-NLS-1$
	private static final String PASSWORD = "-password"; //$NON-NLS-1$

	public static final String PROP_APPLICATION = "eclipse.application"; //$NON-NLS-1$
	public static final String PROP_ARCH = "osgi.arch"; //$NON-NLS-1$
	public static final String PROP_CONFIG_AREA = "osgi.configuration.area"; //$NON-NLS-1$
	public static final String PROP_CONSOLE_LOG = "eclipse.consoleLog"; //$NON-NLS-1$
	public static final String PROP_DEBUG = "osgi.debug"; //$NON-NLS-1$
	public static final String PROP_DEV = "osgi.dev"; //$NON-NLS-1$

	// OSGI system properties.  Copied from EclipseStarter
	public static final String PROP_INSTALL_AREA = "osgi.install.area"; //$NON-NLS-1$
	public static final String PROP_NL = "osgi.nl"; //$NON-NLS-1$
	public static final String PROP_OS = "osgi.os"; //$NON-NLS-1$

	// Eclipse System Properties
	public static final String PROP_PRODUCT = "eclipse.product"; //$NON-NLS-1$
	public static final String PROP_WS = "osgi.ws"; //$NON-NLS-1$
	public static final String PROP_ACTIVATE_PLUGINS = "eclipse.activateRuntimePlugins"; //$NON-NLS-1$

	private static final InternalPlatform singleton = new InternalPlatform();

	private static final String[] WS_LIST = { Platform.WS_COCOA, Platform.WS_GTK, Platform.WS_WIN32, Platform.WS_WPF };
	private Path cachedInstanceLocation; // Cache the path of the instance location
	private ServiceTracker<Location,Location> configurationLocation = null;
	private BundleContext context;
	private FrameworkWiring fwkWiring;

	private Map<IBundleGroupProvider,ServiceRegistration<IBundleGroupProvider>> groupProviders = new HashMap<>(3);
	private ServiceTracker<Location,Location> installLocation = null;
	private ServiceTracker<Location,Location> instanceLocation = null;
	private ServiceTracker<Location,Location> userLocation = null;

	private Plugin runtimeInstance; // Keep track of the plugin object for runtime in case the backward compatibility is run.

	private ServiceRegistration<ILegacyPreferences> legacyPreferencesService = null;
	private ServiceRegistration<IProductPreferencesService> customPreferencesService = null;

	private ServiceTracker<EnvironmentInfo,EnvironmentInfo> environmentTracker = null;
	private ServiceTracker<FrameworkLog,FrameworkLog> logTracker = null;
	private ServiceTracker<PlatformAdmin, PlatformAdmin> platformTracker = null;
	private ServiceTracker<DebugOptions,DebugOptions> debugTracker = null;
	private ServiceTracker<IContentTypeManager,IContentTypeManager> contentTracker = null;
	private ServiceTracker<IPreferencesService,IPreferencesService> preferencesTracker = null;
	private ServiceTracker<IBundleGroupProvider,IBundleGroupProvider> groupProviderTracker = null;
	private ServiceTracker<ExtendedLogReaderService,ExtendedLogReaderService> logReaderTracker = null;
	private ServiceTracker<ExtendedLogService,ExtendedLogService> extendedLogTracker = null;

	private IProduct product;

	public static InternalPlatform getDefault() {
		return singleton;
	}

	/**
	 * Private constructor to block instance creation.
	 */
	private InternalPlatform() {
		super();
	}

	/**
	 * @see Platform#addLogListener(ILogListener)
	 */
	public void addLogListener(ILogListener listener) {
		assertInitialized();
		RuntimeLog.addLogListener(listener);
	}

	private void assertInitialized() {
		//avoid the Policy.bind if assertion is true
		if (!initialized)
			Assert.isTrue(false, Messages.meta_appNotInit);
	}

	/**
	 * @see Platform#endSplash()
	 */
	public void endSplash() {
		synchronized (this) {
			if (splashEnded)
				return; // do not do this more than once
			splashEnded = true;
		}
		final String filter = "(eclipse.application.type=main.thread)"; //$NON-NLS-1$
		ServiceCaller.callOnce(InternalPlatform.class, IApplicationContext.class,
				filter, IApplicationContext::applicationRunning);
	}

	/**
	 * @see Platform#getAdapterManager()
	 */
	public IAdapterManager getAdapterManager() {
		assertInitialized();
		return AdapterManager.getDefault();
	}

	/**
	 * XXX Use the Environment info service. Need to see how to set the value of the app args.
	 */
	public String[] getApplicationArgs() {
		return CommandLineArgs.getApplicationArgs();
	}

	public boolean getBooleanOption(String option, boolean defaultValue) {
		String value = getOption(option);
		if (value == null)
			return defaultValue;
		return "true".equalsIgnoreCase(value); //$NON-NLS-1$
	}

	public BundleContext getBundleContext() {
		return context;
	}

	/**
	 * Returns the bundle id of the bundle that contains the provided object, or
	 * <code>null</code> if the bundle could not be determined.
	 */
	public String getBundleId(Object object) {
		if (object == null)
			return null;
		Bundle source = FrameworkUtil.getBundle(object.getClass());
		if (source != null && source.getSymbolicName() != null)
			return source.getSymbolicName();

		return null;
	}

	public IBundleGroupProvider[] getBundleGroupProviders() {
		return groupProviderTracker.getServices(new IBundleGroupProvider[0]);
	}

	public void registerBundleGroupProvider(IBundleGroupProvider provider) {
		// get the bundle context and register the provider as a service
		ServiceRegistration<IBundleGroupProvider> registration = getBundleContext().registerService(IBundleGroupProvider.class, provider, null);
		// store the service registration (map provider -> registration)
		synchronized (groupProviders) {
			groupProviders.put(provider, registration);
		}
	}

	public void unregisterBundleGroupProvider(IBundleGroupProvider provider) {
		// get the service reference (map provider -> reference)
		ServiceRegistration<IBundleGroupProvider> registration;
		synchronized (groupProviders) {
			registration = groupProviders.remove(provider);
		}
		if (registration == null)
			return;
		// unregister the provider
		registration.unregister();
	}

	public Bundle getBundle(String symbolicName) {
		Bundle[] bundles = getBundles(symbolicName, null);
		return bundles != null && bundles.length > 0 ? bundles[0] : null;
	}

	public Bundle[] getBundles(String symbolicName, String versionRange) {
		if(!isRunning()) {
			return null;
		}
		if (Constants.SYSTEM_BUNDLE_SYMBOLICNAME.equals(symbolicName)) {
			symbolicName = context.getBundle(Constants.SYSTEM_BUNDLE_LOCATION).getSymbolicName();
		}
		Map<String, String> directives = Collections.singletonMap(Namespace.REQUIREMENT_FILTER_DIRECTIVE,
				getRequirementFilter(symbolicName, versionRange));
		Collection<BundleCapability> matchingBundleCapabilities = fwkWiring.findProviders(ModuleContainer
				.createRequirement(IdentityNamespace.IDENTITY_NAMESPACE, directives, Collections.emptyMap()));

		if (matchingBundleCapabilities.isEmpty()) {
			return null;
		}

		Bundle[] results = matchingBundleCapabilities.stream().map(c -> c.getRevision().getBundle())
				// Remove all the bundles that are installed or uninstalled
				.filter(bundle -> (bundle.getState() & (Bundle.INSTALLED | Bundle.UNINSTALLED)) == 0)
				.sorted(Comparator.comparing(Bundle::getVersion).reversed()) // highest version first
				.toArray(Bundle[]::new);

		return results.length > 0 ? results : null;
	}

	private String getRequirementFilter(String symbolicName, String versionRange) {
		VersionRange range = versionRange == null ? null : new VersionRange(versionRange);
		StringBuilder filter = new StringBuilder();
		if (range != null) {
			filter.append("(&"); //$NON-NLS-1$
		}
		filter.append('(').append(IdentityNamespace.IDENTITY_NAMESPACE).append('=').append(symbolicName).append(')');

		if (range != null) {
			filter.append(range.toFilterString(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE)).append(')');
		}
		return filter.toString();
	}

	public String[] getCommandLineArgs() {
		return CommandLineArgs.getAllArgs();
	}

	public Location getConfigurationLocation() {
		assertInitialized();
		return configurationLocation.getService();
	}

	/**
	 * Lazy initialize ContentTypeManager - it can only be used after the registry is up and running
	 */
	public IContentTypeManager getContentTypeManager() {
		return contentTracker == null ? null : contentTracker.getService();
	}

	public EnvironmentInfo getEnvironmentInfoService() {
		return environmentTracker == null ? null : environmentTracker.getService();
	}

	public FrameworkLog getFrameworkLog() {
		return logTracker == null ? null : logTracker.getService();
	}

	public Bundle[] getFragments(Bundle bundle) {
		BundleWiring wiring = bundle.adapt(BundleWiring.class);
		if (wiring == null) {
			return null;
		}
		List<BundleWire> hostWires = wiring.getProvidedWires(HostNamespace.HOST_NAMESPACE);
		if (hostWires == null) {
			// we don't hold locks while checking the graph, just return if no longer valid
			return null;
		}
		Bundle[] result = hostWires.stream().map(wire -> wire.getRequirer().getBundle()).filter(Objects::nonNull)
				.toArray(Bundle[]::new);
		return result.length > 0 ? result : null;
	}

	public Bundle[] getHosts(Bundle bundle) {
		BundleWiring wiring = bundle.adapt(BundleWiring.class);
		if (wiring == null) {
			return null;
		}
		List<BundleWire> hostWires = wiring.getRequiredWires(HostNamespace.HOST_NAMESPACE);
		if (hostWires == null) {
			// we don't hold locks while checking the graph, just return if no longer valid
			return null;
		}
		Bundle[] result = hostWires.stream().map(wire -> wire.getProvider().getBundle()).filter(Objects::nonNull)
				.toArray(Bundle[]::new);
		return result.length > 0 ? result : null;
	}

	public Location getInstallLocation() {
		assertInitialized();
		return installLocation.getService();
	}

	public URL getInstallURL() {
		Location location = getInstallLocation();
		// it is pretty much impossible for the install location to be null.  If it is, the
		// system is in a bad way so throw and exception and get the heck outta here.
		if (location == null)
			throw new IllegalStateException("The installation location must not be null"); //$NON-NLS-1$
		return location.getURL();
	}

	public Location getInstanceLocation() {
		assertInitialized();
		return instanceLocation.getService();
	}

	/**
	 * @see Platform#getLocation()
	 */
	public IPath getLocation() throws IllegalStateException {
		if (cachedInstanceLocation == null) {
			Location location = getInstanceLocation();
			if (location == null)
				return null;
			URL url = location.getURL();
			if (url == null) {
				throw new IllegalStateException("instance location is not (yet) set"); //$NON-NLS-1$
			}
			//	This makes the assumption that the instance location is a file: URL
			File file = new File(url.getFile());
			cachedInstanceLocation = new Path(file.toString());
		}
		return cachedInstanceLocation;
	}

	/**
	 * Returns a log for the given plugin. Creates a new one if needed.
	 * XXX change this into a LogMgr service that would keep track of the map. See if it can be a service factory.
	 * It would contain all the logging methods that are here.
	 * Relate to RuntimeLog if appropriate.
	 * The system log listener needs to be optional: turned on or off. What about a system property? :-)
	 */
	public ILog getLog(Bundle bundle) {
		return logs.computeIfAbsent(bundle, b -> {
			ExtendedLogService logService = extendedLogTracker.getService();
			Logger logger = logService != null ? logService.getLogger(b, PlatformLogWriter.EQUINOX_LOGGER_NAME) : null;
			Log log = new Log(b, logger);
			ExtendedLogReaderService logReader = logReaderTracker.getService();
			logReader.addLogListener(log, log);
			return log;
		});
	}

	public String getNL() {
		return getBundleContext().getProperty(PROP_NL);
	}

	/**
	 * Unicode locale extensions are defined using command line parameter -nlExtensions,
	 * or the system property "osgi.nl.extensions".
	 */
	public String getNLExtensions() {
		String nlExtensions = PlatformActivator.getContext().getProperty("osgi.nl.extensions"); //$NON-NLS-1$
		if (nlExtensions == null)
			return ""; //$NON-NLS-1$
		if (!nlExtensions.startsWith("@")) //$NON-NLS-1$
			nlExtensions = '@' + nlExtensions;
		return nlExtensions;
	}

	/**
	 * @see Platform
	 */
	public String getOption(String option) {
		DebugOptions options = getDebugOptions();
		if (options != null)
			return options.getOption(option);
		return null;
	}

	public String getOS() {
		return getBundleContext().getProperty(PROP_OS);
	}

	public String getOSArch() {
		return getBundleContext().getProperty(PROP_ARCH);
	}

	public PlatformAdmin getPlatformAdmin() {
		return platformTracker == null ? null : platformTracker.getService();
	}


	public IPreferencesService getPreferencesService() {
		return preferencesTracker == null ? null : preferencesTracker.getService();
	}

	/*
	 * XXX move this into the app model.
	 */
	public IProduct getProduct() {
		if (product != null)
			return product;
		EclipseAppContainer container = Activator.getContainer();
		IBranding branding = container == null ? null : container.getBranding();
		if (branding == null)
			return null;
		Object brandingProduct = branding.getProduct();
		if (!(brandingProduct instanceof IProduct))
			brandingProduct = new Product(branding);
		product = (IProduct) brandingProduct;
		return product;
	}

	public IExtensionRegistry getRegistry() {
		return RegistryFactory.getRegistry();
	}

	/**
	 * XXX deprecate and use NLS or BundleFinder.find()
	 */
	public ResourceBundle getResourceBundle(Bundle bundle) {
		return ResourceTranslator.getResourceBundle(bundle);
	}

	/**
	 * XXX deprecate and use NLS or BundleFinder.find()
	 */
	public String getResourceString(Bundle bundle, String value) {
		return ResourceTranslator.getResourceString(bundle, value);
	}

	/**
	 * XXX deprecate and use NLS or BundleFinder.find()
	 */
	public String getResourceString(Bundle bundle, String value, ResourceBundle resourceBundle) {
		return ResourceTranslator.getResourceString(bundle, value, resourceBundle);
	}

	/**
	 * This method is only used to register runtime once compatibility has been started.
	 */
	public Plugin getRuntimeInstance() {
		return runtimeInstance;
	}

	/**
	 * XXX Investigate the usage of a service factory
	 */
	public IPath getStateLocation(Bundle bundle) {
		return getStateLocation(bundle, true);
	}

	public IPath getStateLocation(Bundle bundle, boolean create) throws IllegalStateException {
		assertInitialized();
		IPath result = MetaDataKeeper.getMetaArea().getStateLocation(bundle);
		if (create)
			result.toFile().mkdirs();
		return result;
	}

	public long getStateTimeStamp() {
		PlatformAdmin admin = getPlatformAdmin();
		return admin == null ? -1 : admin.getState(false).getTimeStamp();
	}

	public Location getUserLocation() {
		assertInitialized();
		return userLocation.getService();
	}

	public String getWS() {
		return getBundleContext().getProperty(PROP_WS);
	}

	private void initializeAuthorizationHandler() {
		try {
			AuthorizationHandler.setKeyringFile(keyringFile);
			AuthorizationHandler.setPassword(password);
		} catch (NoClassDefFoundError e) {
			// The authorization fragment is not available. If someone tries to use that API, an error will be logged
		}
	}

	/*
	 * Finds and loads the options file
	 */
	void initializeDebugFlags() {
		// load runtime options
		DEBUG = getBooleanOption(Platform.PI_RUNTIME + "/debug", false); //$NON-NLS-1$
		if (DEBUG) {
			DEBUG_PLUGIN_PREFERENCES = getBooleanOption(Platform.PI_RUNTIME + "/preferences/plugin", false); //$NON-NLS-1$
		}
	}

	public boolean isFragment(Bundle bundle) {
		BundleRevisions bundleRevisions = bundle.adapt(BundleRevisions.class);
		List<BundleRevision> revisions = bundleRevisions.getRevisions();
		if (revisions.isEmpty()) {
			// bundle is uninstalled and not current users; just return false
			return false;
		}
		return (revisions.get(0).getTypes() & BundleRevision.TYPE_FRAGMENT) != 0;
	}

	/*
	 *XXX do what you want to do. track osgi, track runtime, or whatever.
	 */
	public boolean isRunning() {
		try {
			return initialized && context != null && context.getBundle().getState() == Bundle.ACTIVE;
		} catch (IllegalStateException e) {
			return false;
		}
	}

	/**
	 * Returns a list of known system architectures.
	 *
	 * @return the list of system architectures known to the system
	 * XXX This is useless
	 */
	public String[] knownOSArchValues() {
		return ARCH_LIST;
	}

	/**
	 * Returns a list of known operating system names.
	 *
	 * @return the list of operating systems known to the system
	 * XXX This is useless
	 */
	public String[] knownOSValues() {
		return OS_LIST;
	}

	/**
	 * Returns a list of known windowing system names.
	 *
	 * @return the list of window systems known to the system
	 * XXX This is useless
	 */
	public String[] knownWSValues() {
		return WS_LIST;
	}

	private void processCommandLine(String[] args) {
		if (args == null || args.length == 0)
			return;

		for (int i = 0; i < args.length; i++) {
			// check for args with parameters
			if (i == args.length - 1 || args[i + 1].startsWith("-")) //$NON-NLS-1$
				continue;
			String arg = args[++i];

			// look for the keyring file
			if (args[i - 1].equalsIgnoreCase(KEYRING))
				keyringFile = arg;
			// look for the user password.
			if (args[i - 1].equalsIgnoreCase(PASSWORD))
				password = arg;
		}
	}

	/**
	 * @see Platform#removeLogListener(ILogListener)
	 */
	public void removeLogListener(ILogListener listener) {
		assertInitialized();
		RuntimeLog.removeLogListener(listener);
	}

	/**
	 * This method is only used to register runtime once compatibility has been started.
	 */
	public void setRuntimeInstance(Plugin runtime) {
		runtimeInstance = runtime;
	}

	/**
	 * Internal method for starting up the platform.  The platform is not started with any location
	 * and should not try to access the instance data area.
	 *
	 * Note: the content type manager must be initialized only after the registry has been created
	 */
	public void start(BundleContext runtimeContext) {
		this.context = runtimeContext;
		this.fwkWiring = runtimeContext.getBundle(Constants.SYSTEM_BUNDLE_LOCATION).adapt(FrameworkWiring.class);
		openOSGiTrackers();
		splashEnded = false;
		processCommandLine(getEnvironmentInfoService().getNonFrameworkArgs());
		initializeDebugFlags();
		initialized = true;
		initializeAuthorizationHandler();
		startServices();
	}

	/**
	 * Shutdown runtime pieces in this order:
	 * Content[auto shutdown] -&gt; Preferences[auto shutdown] -&gt; Registry -&gt; Jobs.
	 * The "auto" shutdown takes place before this code is executed
	 */
	public void stop(BundleContext bundleContext) {
		assertInitialized();
		stopServices(); // should be done after preferences shutdown
		initialized = false;
		closeOSGITrackers();
		context = null;
	}

	private void openOSGiTrackers() {
		Filter filter = null;
		try {
			filter = context.createFilter(Location.INSTANCE_FILTER);
		} catch (InvalidSyntaxException e) {
			// ignore this.  It should never happen as we have tested the above format.
		}
		instanceLocation = new ServiceTracker<>(context, filter, null);
		instanceLocation.open();

		try {
			filter = context.createFilter(Location.USER_FILTER);
		} catch (InvalidSyntaxException e) {
			// ignore this.  It should never happen as we have tested the above format.
		}
		userLocation = new ServiceTracker<>(context, filter, null);
		userLocation.open();

		try {
			filter = context.createFilter(Location.CONFIGURATION_FILTER);
		} catch (InvalidSyntaxException e) {
			// ignore this.  It should never happen as we have tested the above format.
		}
		configurationLocation = new ServiceTracker<>(context, filter, null);
		configurationLocation.open();

		try {
			filter = context.createFilter(Location.INSTALL_FILTER);
		} catch (InvalidSyntaxException e) {
			// ignore this.  It should never happen as we have tested the above format.
		}
		installLocation = new ServiceTracker<>(context, filter, null);
		installLocation.open();

		if (context != null) {
			logTracker = new ServiceTracker<>(context, FrameworkLog.class, null);
			logTracker.open();
		}

		if (context != null) {
			platformTracker = new ServiceTracker<>(context, PlatformAdmin.class, null);
			platformTracker.open();
		}

		if (context != null) {
			contentTracker = new ServiceTracker<>(context, IContentTypeManager.class, null);
			contentTracker.open();
		}

		if (context != null) {
			preferencesTracker = new ServiceTracker<>(context, IPreferencesService.class, null);
			preferencesTracker.open();
		}

		try {
			filter = context.createFilter("(objectClass=" + IBundleGroupProvider.class.getName() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (InvalidSyntaxException e) {
			// ignore this, it should never happen
		}
		groupProviderTracker = new ServiceTracker<>(context, filter, null);
		groupProviderTracker.open();

		logReaderTracker = new ServiceTracker<>(context, ExtendedLogReaderService.class, null);
		logReaderTracker.open();

		extendedLogTracker = new ServiceTracker<>(context, ExtendedLogService.class, null);
		extendedLogTracker.open();

		environmentTracker = new ServiceTracker<>(context, EnvironmentInfo.class, null);
		environmentTracker.open();

		debugTracker = new ServiceTracker<>(context, DebugOptions.class, null);
		debugTracker.open();
	}

	private void startServices() {
		// The check for getProduct() is relatively expensive (about 3% of the headless startup),
		// so we don't want to enforce it here.
		customPreferencesService = context.registerService(IProductPreferencesService.class, new ProductPreferencesService(), new Hashtable<>());

		legacyPreferencesService = context.registerService(ILegacyPreferences.class, new InitLegacyPreferences(), new Hashtable<>());
	}

	private void stopServices() {
		if (legacyPreferencesService != null) {
			legacyPreferencesService.unregister();
			legacyPreferencesService = null;
		}
		if (customPreferencesService != null) {
			customPreferencesService.unregister();
			customPreferencesService = null;
		}
	}

	private DebugOptions getDebugOptions() {
		return debugTracker == null ? null : debugTracker.getService();
	}

	private void closeOSGITrackers() {
		if (preferencesTracker != null) {
			preferencesTracker.close();
		}
		if (contentTracker != null) {
			contentTracker.close();
		}
		if (debugTracker != null) {
			debugTracker.close();
		}
		if (platformTracker != null) {
			platformTracker.close();
		}
		if (logTracker != null) {
			logTracker.close();
		}
		if (groupProviderTracker != null) {
			groupProviderTracker.close();
		}
		if (environmentTracker != null) {
			environmentTracker.close();
		}
		if (logReaderTracker != null) {
			logReaderTracker.close();
		}
		if (extendedLogTracker != null) {
			extendedLogTracker.close();
		}
		if (installLocation != null) {
			installLocation.close();
		}
		if (userLocation != null) {
			userLocation.close();
		}
		if (configurationLocation != null) {
			configurationLocation.close();
		}
		if (instanceLocation != null) {
			instanceLocation.close();
		}
	}

	/**
	 * Print a debug message to the console.
	 * Pre-pend the message with the current date and the name of the current thread.
	 */
	public static void message(String message) {
		StringBuilder buffer = new StringBuilder();
		buffer.append(new Date(System.currentTimeMillis()));
		buffer.append(" - ["); //$NON-NLS-1$
		buffer.append(Thread.currentThread().getName());
		buffer.append("] "); //$NON-NLS-1$
		buffer.append(message);
		System.out.println(buffer.toString());
	}

	public static void start(Bundle bundle) throws BundleException {
		int originalState = bundle.getState();
		if ((originalState & Bundle.ACTIVE) != 0)
			return; // bundle is already active
		try {
			// attempt to activate the bundle
			bundle.start(Bundle.START_TRANSIENT);
		} catch (BundleException e) {
			if ((originalState & Bundle.STARTING) != 0 && (bundle.getState() & Bundle.STARTING) != 0)
				// This can happen if the bundle was in the process of being activated on this thread, just return
				return;
			throw e;
		}
	}
}
