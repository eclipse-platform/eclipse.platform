/*******************************************************************************
 * Copyright (c) 2000, 2025 IBM Corporation and others.
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
 *     Tue Ton - support for FreeBSD
 *******************************************************************************/
package org.eclipse.core.internal.runtime;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;
import org.eclipse.core.internal.preferences.exchange.ILegacyPreferences;
import org.eclipse.core.internal.preferences.exchange.IProductPreferencesService;
import org.eclipse.core.internal.preferences.legacy.InitLegacyPreferences;
import org.eclipse.core.internal.preferences.legacy.ProductPreferencesService;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IAdapterManager;
import org.eclipse.core.runtime.IBundleGroupProvider;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.ILogListener;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProduct;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.RegistryFactory;
import org.eclipse.core.runtime.ServiceCaller;
import org.eclipse.core.runtime.content.IContentTypeManager;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.equinox.internal.app.Activator;
import org.eclipse.equinox.internal.app.CommandLineArgs;
import org.eclipse.equinox.internal.app.EclipseAppContainer;
import org.eclipse.equinox.internal.app.IBranding;
import org.eclipse.equinox.log.ExtendedLogReaderService;
import org.eclipse.equinox.log.ExtendedLogService;
import org.eclipse.equinox.log.Logger;
import org.eclipse.osgi.container.Module;
import org.eclipse.osgi.container.ModuleContainer;
import org.eclipse.osgi.framework.log.FrameworkLog;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.service.environment.EnvironmentInfo;
import org.eclipse.osgi.service.resolver.PlatformAdmin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.VersionRange;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleRevisions;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.resource.Namespace;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Bootstrap class for the platform. It is responsible for setting up the
 * platform class loader and passing control to the actual application class
 */
public final class InternalPlatform {

	private static final String[] ARCH_LIST = { Platform.ARCH_AARCH64, Platform.ARCH_PPC64LE, Platform.ARCH_RISCV64,
			Platform.ARCH_X86_64 };

	public static final StackWalker STACK_WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

	// debug support:  set in loadOptions()
	public static boolean DEBUG = false;
	public static boolean DEBUG_PLUGIN_PREFERENCES = false;

	private boolean splashEnded = false;
	private volatile boolean initialized;
	private volatile boolean stopped;
	private static final String KEYRING = "-keyring"; //$NON-NLS-1$
	private String keyringFile;

	private final ConcurrentMap<Bundle, Log> logs = new ConcurrentHashMap<>(5);

	private static final String[] OS_LIST = { Platform.OS_FREEBSD, Platform.OS_LINUX, Platform.OS_MACOSX, Platform.OS_WIN32 };
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

	// OSGI IDE specific property, copied from DataArea
	private static final String PROP_REQUIRES_EXPLICIT_INIT = "osgi.dataAreaRequiresExplicitInit"; //$NON-NLS-1$ ;

	// Eclipse System Properties
	public static final String PROP_PRODUCT = "eclipse.product"; //$NON-NLS-1$
	public static final String PROP_WS = "osgi.ws"; //$NON-NLS-1$
	public static final String PROP_ACTIVATE_PLUGINS = "eclipse.activateRuntimePlugins"; //$NON-NLS-1$

	private static final InternalPlatform singleton = new InternalPlatform();

	private static final String[] WS_LIST = { Platform.WS_COCOA, Platform.WS_GTK, Platform.WS_WIN32 };
	private IPath cachedInstanceLocation; // Cache the path of the instance location
	private ServiceTracker<Location,Location> configurationLocation = null;
	private BundleContext context;
	private FrameworkWiring fwkWiring;

	private final Map<IBundleGroupProvider,ServiceRegistration<IBundleGroupProvider>> groupProviders = new HashMap<>(3);
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
		if (!initialized) {
			if (stopped) {
				Assert.isTrue(false, Messages.meta_appStopped);
			} else {
				Assert.isTrue(false, Messages.meta_appNotInit);
			}
		}
	}

	/**
	 * @see Platform#endSplash()
	 */
	public void endSplash() {
		synchronized (this) {
			if (splashEnded) {
				return; // do not do this more than once
			}
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
		return value == null ? defaultValue : Boolean.parseBoolean(value);
	}

	public BundleContext getBundleContext() {
		return context;
	}

	/**
	 * Returns the bundle id of the bundle that contains the provided object, or
	 * <code>null</code> if the bundle could not be determined.
	 */
	public String getBundleId(Object object) {
		if (object == null) {
			return null;
		}
		Bundle source = FrameworkUtil.getBundle(object.getClass());
		if (source != null && source.getSymbolicName() != null) {
			return source.getSymbolicName();
		}

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
		if (registration == null) {
			return;
		}
		// unregister the provider
		registration.unregister();
	}

	public Bundle getBundle(String symbolicName) {
		Stream<Bundle> bundles = getBundles0(symbolicName, null);
		return bundles.findFirst().orElse(null);
	}

	public Bundle[] getBundles(String symbolicName, String versionRange) {
		Stream<Bundle> result = getBundles0(symbolicName, versionRange);
		Bundle[] results = result.toArray(Bundle[]::new);
		return results.length > 0 ? results : null;
	}

	static final Comparator<Bundle> DESCENDING_BUNDLE_VERION = Comparator.comparing(Bundle::getVersion).reversed();

	private Stream<Bundle> getBundles0(String symbolicName, String versionRange) {
		if (!isRunning()) {
			return Stream.empty();
		}
		if (Constants.SYSTEM_BUNDLE_SYMBOLICNAME.equals(symbolicName)) {
			symbolicName = context.getBundle(Constants.SYSTEM_BUNDLE_LOCATION).getSymbolicName();
		}
		Map<String, String> directives = Map.of(Namespace.REQUIREMENT_FILTER_DIRECTIVE,
				getRequirementFilter(symbolicName, versionRange));
		Collection<BundleCapability> matchingBundleCapabilities = fwkWiring.findProviders(ModuleContainer
				.createRequirement(IdentityNamespace.IDENTITY_NAMESPACE, directives, Collections.emptyMap()));

		return matchingBundleCapabilities.stream().map(c -> c.getRevision().getBundle())
				// Remove all the bundles that are installed or uninstalled
				.filter(bundle -> (bundle.getState() & (Bundle.INSTALLED | Bundle.UNINSTALLED)) == 0)
				.sorted(DESCENDING_BUNDLE_VERION); // highest version first
	}

	private String getRequirementFilter(String symbolicName, String versionRange) {
		String identity = "(" + IdentityNamespace.IDENTITY_NAMESPACE + "=" + symbolicName + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		if (versionRange == null) {
			return identity;
		}
		String version = new VersionRange(versionRange).toFilterString(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE);
		return "(&" + identity + version + ")"; //$NON-NLS-1$ //$NON-NLS-2$
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
		if (location == null) {
			throw new IllegalStateException("The installation location must not be null"); //$NON-NLS-1$
		}
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
			if (location == null) {
				return null;
			}
			if (!location.isSet()) {
				boolean explicitInitRequired = Boolean
						.parseBoolean(getBundleContext().getProperty(PROP_REQUIRES_EXPLICIT_INIT));
				if (explicitInitRequired) {
					// See bug 514333: don't allow clients to initialize instance location if the
					// instance area is not explicitly defined yet
					throw new IllegalStateException(CommonMessages.meta_instanceDataUnspecified);
				}
			}

			// Note: if not explicitly set, this call will resolve to default location
			// therefore we have the check above, but only if PROP_REQUIRES_EXPLICIT_INIT is set.
			URL url = location.getURL();
			if (url == null) {
				throw new IllegalStateException("Instance location is not (yet) set"); //$NON-NLS-1$
			}
			//	This makes the assumption that the instance location is a file: URL
			File file = new File(url.getFile());
			cachedInstanceLocation = IPath.fromOSString(file.toString());
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
		if (isRunning()) {
			return logs.computeIfAbsent(bundle, b -> {
				ExtendedLogService logService = extendedLogTracker.getService();
				Logger logger = logService != null ? logService.getLogger(b, PlatformLogWriter.EQUINOX_LOGGER_NAME)
						: null;
				Log log = new Log(b, logger);
				ExtendedLogReaderService logReader = logReaderTracker.getService();
				if (logReader != null) {
					logReader.addLogListener(log, log);
				}
				return log;
			});
		}
		return new Log(bundle, null);
	}

	/**
	 * Unicode locale extensions are defined using command line parameter -nlExtensions,
	 * or the system property "osgi.nl.extensions".
	 */
	public String getNLExtensions() {
		String nlExtensions = PlatformActivator.getContext().getProperty("osgi.nl.extensions"); //$NON-NLS-1$
		if (nlExtensions == null) {
			return ""; //$NON-NLS-1$
		}
		if (!nlExtensions.startsWith("@")) { //$NON-NLS-1$
			nlExtensions = '@' + nlExtensions;
		}
		return nlExtensions;
	}

	/**
	 * @see Platform
	 */
	public String getOption(String option) {
		DebugOptions options = getDebugOptions();
		if (options != null) {
			return options.getOption(option);
		}
		return null;
	}

	public String getOS() {
		return getContextProperty(PROP_OS);
	}

	public String getWS() {
		return getContextProperty(PROP_WS);
	}

	public String getOSArch() {
		return getContextProperty(PROP_ARCH);
	}

	public String getNL() {
		return getContextProperty(PROP_NL);
	}

	private String getContextProperty(String key) {
		BundleContext ctx = context;
		return ctx != null ? ctx.getProperty(key) : System.getProperty(key);
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
		if (product != null) {
			return product;
		}
		EclipseAppContainer container = Activator.getContainer();
		IBranding branding = container == null ? null : container.getBranding();
		if (branding == null) {
			return null;
		}
		Object brandingProduct = branding.getProduct();
		if (!(brandingProduct instanceof IProduct)) {
			brandingProduct = new Product(branding);
		}
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
		if (create) {
			result.toFile().mkdirs();
		}
		return result;
	}

	public long getStateTimeStamp() {
		return Arrays.stream(getBundleContext().getBundles()).map(bundle -> bundle.adapt(Module.class))
				.filter(Objects::nonNull).mapToLong(Module::getLastModified).sum();
	}

	public Location getUserLocation() {
		assertInitialized();
		return userLocation.getService();
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
		if (args == null || args.length == 0) {
			return;
		}

		for (int i = 0; i < args.length; i++) {
			// check for args with parameters
			if (i == args.length - 1 || args[i + 1].startsWith("-")) { //$NON-NLS-1$
				continue;
			}
			String arg = args[++i];

			// look for the keyring file
			if (args[i - 1].equalsIgnoreCase(KEYRING)) {
				keyringFile = arg;
			}
			// look for the user password.
			if (args[i - 1].equalsIgnoreCase(PASSWORD)) {
				password = arg;
			}
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
		stopped = false;
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
		stopped = true;
		context = null;
	}

	private void openOSGiTrackers() {
		instanceLocation = createOpenTracker(Location.INSTANCE_FILTER);
		userLocation = createOpenTracker(Location.USER_FILTER);
		configurationLocation = createOpenTracker(Location.CONFIGURATION_FILTER);
		installLocation = createOpenTracker(Location.INSTALL_FILTER);
		logTracker = createOpenTracker(FrameworkLog.class);
		platformTracker = createOpenTracker(PlatformAdmin.class);
		contentTracker = createOpenTracker(IContentTypeManager.class);
		preferencesTracker = createOpenTracker(IPreferencesService.class);
		groupProviderTracker = createOpenTracker("(objectClass=" + IBundleGroupProvider.class.getName() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		logReaderTracker = createOpenTracker(ExtendedLogReaderService.class);
		extendedLogTracker = createOpenTracker(ExtendedLogService.class);
		environmentTracker = createOpenTracker(EnvironmentInfo.class);
		debugTracker = createOpenTracker(DebugOptions.class);
	}

	private <T> ServiceTracker<T, T> createOpenTracker(String filterStr) {
		try {
			Filter filter = FrameworkUtil.createFilter(filterStr);
			ServiceTracker<T, T> tracker = new ServiceTracker<>(context, filter, null);
			tracker.open();
			return tracker;
		} catch (InvalidSyntaxException e) {
			// It should never happen as we have tested the above format.
			throw new AssertionError("Invalid service tracker filter"); //$NON-NLS-1$
		}
	}

	private <T> ServiceTracker<T, T> createOpenTracker(Class<T> service) {
		ServiceTracker<T, T> tracker = new ServiceTracker<>(context, service, null);
		tracker.open();
		return tracker;
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
		ExtendedLogReaderService logReader = logReaderTracker.getService();
		if (logReader != null) {
			logs.forEach((b, log) -> logReader.removeLogListener(log));
		}
		logs.clear();
		closeTracker(preferencesTracker);
		closeTracker(contentTracker);
		closeTracker(debugTracker);
		closeTracker(platformTracker);
		closeTracker(logTracker);
		closeTracker(groupProviderTracker);
		closeTracker(environmentTracker);
		closeTracker(logReaderTracker);
		closeTracker(extendedLogTracker);
		closeTracker(installLocation);
		closeTracker(userLocation);
		closeTracker(configurationLocation);
		closeTracker(instanceLocation);
	}

	private static void closeTracker(ServiceTracker<?, ?> tracker) {
		if (tracker != null) {
			tracker.close();
		}
	}

	/**
	 * Print a debug message to the console.
	 * Pre-pend the message with the current date and the name of the current thread.
	 */
	public static void message(String message) {
		System.out.println(String.format("%s - [%s] %s", new Date(), Thread.currentThread().getName(), message)); //$NON-NLS-1$
	}

	public static void start(Bundle bundle) throws BundleException {
		int originalState = bundle.getState();
		if ((originalState & Bundle.ACTIVE) != 0) {
			return; // bundle is already active
		}
		try {
			// attempt to activate the bundle
			bundle.start(Bundle.START_TRANSIENT);
		} catch (BundleException e) {
			if ((originalState & Bundle.STARTING) != 0 && (bundle.getState() & Bundle.STARTING) != 0) {
				// This can happen if the bundle was in the process of being activated on this thread, just return
				return;
			}
			throw e;
		}
	}
}
