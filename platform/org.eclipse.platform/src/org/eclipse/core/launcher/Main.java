/*******************************************************************************
 * Copyright (c) 2000,2002 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Common Public License v0.5
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 * IBM - Initial API and implementation
 ******************************************************************************/
package org.eclipse.core.launcher;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Startup class for Eclipse. Creates a class loader using
 * supplied URL of platform installation, loads and calls
 * the Eclipse Boot Loader.  The startup arguments are as follows:
 * <dl>
 * <dd>
 *    -application &lt;id&gt;: the identifier of the application to run
 * </dd>
 * <dd>
 *    -boot &lt;location&gt;: the location, expressed as a URL, of the platform's boot.jar
 * </dd>
 * <dd>
 *    -consolelog : enables log to the console. Handy when combined with -debug
 * </dd>
 * <dd>
 *    -data &lt;location&gt;: sets the workspace location and the default location for projects
 * </dd>
 * <dd>
 *    -debug [options file]: turns on debug mode for the platform and optionally specifies a location
 * for the .options file. This file indicates what debug points are available for a
 * plug-in and whether or not they are enabled. If a location is not specified, the platform searches
 * for the .options file under the install directory
 * </dd>
 * <dd>
 *    -dev [entries]: turns on dev mode and optionally specifies comma-separated class path entries
 * which are added to the class path of each plug-in
 * </dd>
 * <dd>
 *    -keyring &lt;location&gt;: the location of the authorization database on disk. This argument
 * has to be used together with the -password argument
 * </dd>
 * <dd>
 *    -password &lt;passwd&gt;: the password for the authorization database
 * </dd>
 * <dd>
 *    -plugins &lt;location&gt;: The arg is a URL pointing to a file which specs the plugin 
 * path for the platform.  The file is in property file format where the keys are user-defined 
 * names and the values are comma separated lists of either explicit paths to plugin.xml 
 * files or directories containing plugins. (e.g., .../eclipse/plugins).
 * </dd>
 * <dd>
 *    -ws &lt;window system&gt;: sets the window system value
 * </dd>
 * </dl>
 */
public class Main {
	/**
	 * Indicates whether this instance is running in debug mode.
	 */
	protected boolean debug = false;
	
	/**
	 * The location of the launcher to run.
	 */
	protected String bootLocation = null;
	
	/**
	 * The location of the install root
	 */
	protected String rootLocation = null;
	
	/**
	 * The identifier of the application to run.
	 */
	protected String application;
	
	/**
	 * The identifier of the feature to run.
	 */
	protected String feature;
	
	/**
	 * The identifier of the configuration to use.
	 */
	protected String configuration;
	
	/**
	 * The path for finding find plugins.
	 */
	protected URL pluginPathLocation;
	
	/**
	 * The boot path location.
	 */
	protected String location;		
		
	/**
	 * The class path entries.
	 */
	protected String devClassPath = null;
	
	/**
	 * Indicates whether this instance is running in development mode.
	 */
	protected boolean inDevelopmentMode = false;

	// splash handling
	private String showSplash = null; 
	private String endSplash = null; 
	private boolean cmdInitialize = false;
	private boolean cmdFirstUse = false;
	private Process showProcess = null;
	private boolean splashDown = false; 
	private final Runnable endSplashHandler = new Runnable() {
		public void run() {
			takeDownSplash();
		}
	}; 
	
	// configuration properties
	private Properties props = new Properties();
	private HashMap featureIndex = new HashMap();
	private String baseLocation = null;
	
	// constants
	private static final String APPLICATION = "-application"; //$NON-NLS-1$
	private static final String BOOT = "-boot"; //$NON-NLS-1$
	private static final String INSTALL = "-install"; //$NON-NLS-1$
	private static final String INITIALIZE = "-initialize"; //$NON-NLS-1$
	private static final String DEBUG = "-debug"; //$NON-NLS-1$
	private static final String DEV = "-dev"; //$NON-NLS-1$
	private static final String DATA = "-data"; //$NON-NLS-1$
	private static final String CONFIGURATION = "-configuration"; //$NON-NLS-1$
	private static final String FEATURE = "-feature"; //$NON-NLS-1$
	private static final String SHOWSPLASH = "-showsplash"; //$NON-NLS-1$
	private static final String ENDSPLASH = "-endsplash"; //$NON-NLS-1$
	private static final String FIRST_USE = "-firstuse"; //$NON-NLS-1$
	private static final String SPLASH_IMAGE = "splash.bmp"; //$NON-NLS-1$
	private static final String PI_BOOT = "org.eclipse.core.boot"; //$NON-NLS-1$
	private static final String BOOTLOADER = "org.eclipse.core.boot.BootLoader"; //$NON-NLS-1$
	private static final String BOOTJAR = "boot.jar"; //$NON-NLS-1$
	private static final String PLATFORM_URL = "platform:/base/"; //$NON-NLS-1$
	
	// constants: configuration file location
	private static final String CONFIG_FILE = "platform.cfg"; //$NON-NLS-1$
	private static final String CONFIG_FILE_TEMP_SUFFIX = ".tmp"; //$NON-NLS-1$
	private static final String CONFIG_FILE_BAK_SUFFIX = ".bak"; //$NON-NLS-1$
	private static final String ARG_USER_DIR = "user.dir"; //$NON-NLS-1$
	
	// constants: configuration file elements
	private static final String CFG_CORE_BOOT = "bootstrap." + PI_BOOT;  //$NON-NLS-1$
	private static final String CFG_FEATURE_ENTRY = "feature"; //$NON-NLS-1$
	private static final String CFG_FEATURE_ENTRY_DEFAULT = "feature.default.id"; //$NON-NLS-1$
	private static final String CFG_FEATURE_ENTRY_ID = "id"; //$NON-NLS-1$
	private static final String CFG_FEATURE_ENTRY_ROOT = "root"; //$NON-NLS-1$
	private static final String CFG_EOF = "eof"; //$NON-NLS-1$

	// log file handling
	protected static final String SESSION = "!SESSION"; //$NON-NLS-1$
	protected static final String ENTRY = "!ENTRY"; //$NON-NLS-1$
	protected static final String MESSAGE = "!MESSAGE"; //$NON-NLS-1$
	protected static final String STACK = "!STACK"; //$NON-NLS-1$
	protected static final int ERROR = 4;
	protected static final String PLUGIN_ID = "org.eclipse.core.launcher"; //$NON-NLS-1$
	protected static final String logFileName = ".metadata" + File.separator + ".log";//$NON-NLS-1$ //$NON-NLS-2$
	protected static File logFile = null;
	protected static BufferedWriter log = null;
	protected static String[] arguments;
	protected static boolean newSession = true;

/**
 * Executes the launch.
 * 
 * @return the result of performing the launch
 * @param args command-line arguments
 * @exception Exception thrown if a problem occurs during the launch
 */
protected Object basicRun(String[] args) throws Exception {
	// locate boot plugin (may return -dev mode variations)
	URL[] bootPath = getBootPath(bootLocation);
	
	// splash handling is done here, because the default case needs to know
	// the location of the boot plugin we are going to use
	handleSplash(bootPath);
	
	// load the BootLoader and startup the platform
	Class clazz = getBootLoader(bootPath);
	Method method = clazz.getDeclaredMethod("run", new Class[] { String.class, URL.class, String.class, String[].class, Runnable.class }); //$NON-NLS-1$
	try {
		return method.invoke(clazz, new Object[] { application, pluginPathLocation, location, args, endSplashHandler });
	} catch (InvocationTargetException e) {
		if (e.getTargetException() instanceof Error)
			throw (Error) e.getTargetException();
		else
			throw e;
	}
}
/**
 * Returns a string representation of the given URL String.  This converts
 * escaped sequences (%..) in the URL into the appropriate characters.
 * NOTE: due to class visibility there is a copy of this method
 *       in InternalBootLoader
 */
protected String decode(String urlString) {
	//try to use Java 1.4 method if available
	try {
		Class clazz = URLDecoder.class;
		Method method = clazz.getDeclaredMethod("decode", new Class[] {String.class, String.class});//$NON-NLS-1$
		//first encode '+' characters, because URLDecoder incorrectly converts 
		//them to spaces on certain class library implementations.
		if (urlString.indexOf('+') >= 0) {
			int len = urlString.length();
			StringBuffer buf = new StringBuffer(len);
			for (int i = 0; i < len; i++) {
				char c = urlString.charAt(i);
				if (c == '+')
					buf.append("%2B");//$NON-NLS-1$
				else
					buf.append(c);
			}
			urlString = buf.toString();
		}
		Object result = method.invoke(null, new Object[] {urlString, "UTF-8"});//$NON-NLS-1$
		if (result != null)
			return (String)result;
	} catch (Exception e) {
		//JDK 1.4 method not found -- fall through and decode by hand
	}
	//decode URL by hand
	int len = urlString.length();
	ByteArrayOutputStream os = new ByteArrayOutputStream(len);
	for (int i = 0; i < len;) {
		char c = urlString.charAt(i++);
		if (c == '%') {
			if (len >= i + 2) {
				os.write(Integer.parseInt(urlString.substring(i, i + 2), 16));
			}
			i += 2;
		} else {
			os.write(c);
		}
	}
	try {
		return new String(os.toByteArray(), "UTF-8");//$NON-NLS-1$
	} catch (UnsupportedEncodingException e) {
		//use default encoding
		return new String(os.toByteArray());
	}
}
/**
 * Returns the result of converting a list of comma-separated tokens into an array
 * 
 * @return the array of string tokens
 * @param prop the initial comma-separated string
 */
private String[] getArrayFromList(String prop) {
	if (prop == null || prop.trim().equals("")) //$NON-NLS-1$
		return new String[0];
	Vector list = new Vector();
	StringTokenizer tokens = new StringTokenizer(prop, ","); //$NON-NLS-1$
	while (tokens.hasMoreTokens()) {
		String token = tokens.nextToken().trim();
		if (!token.equals("")) //$NON-NLS-1$
			list.addElement(token);
	}
	return list.isEmpty() ? new String[0] : (String[]) list.toArray(new String[0]);
}
/**
 * Creates and returns a platform <code>BootLoader</code> which can be used to start
 * up and run the platform.  
 * 
 * @return the new boot loader
 * @param path search path for the BootLoader
 */
public Class getBootLoader(URL[] path) throws Exception {
	URLClassLoader loader = new URLClassLoader(path, null);
	return loader.loadClass(BOOTLOADER);
}
/**
 * Returns the <code>URL</code>-based class path describing where the boot classes
 * are located when running in development mode.
 * 
 * @return the url-based class path
 * @param base the base location
 * @exception MalformedURLException if a problem occurs computing the class path
 */
protected URL[] getDevPath(URL base) throws MalformedURLException {
	URL url;
	String devBase = base.toExternalForm();
	if (!inDevelopmentMode) {
		url = new URL(devBase + BOOTJAR);
		return new URL[] {url};
	}
	String[] locations = getArrayFromList(devClassPath);
	ArrayList result = new ArrayList(locations.length);
	for (int i = 0; i < locations.length; i++) {
		String spec = devBase + locations[i];
		char lastChar = spec.charAt(spec.length() - 1);
		if ((spec.endsWith(".jar") || (lastChar == '/' || lastChar == '\\'))) //$NON-NLS-1$
			url = new URL (spec);
		else
			url = new URL(spec + "/"); //$NON-NLS-1$
		//make sure URL exists before adding to path
		if (new java.io.File(url.getFile()).exists())
			result.add(url);
	}
	url = new URL(devBase + BOOTJAR);
	if (new java.io.File(url.getFile()).exists())
		result.add(url);
	return (URL[])result.toArray(new URL[result.size()]);
}

/**
 * Returns the <code>URL</code>-based class path describing where the boot classes are located.
 * 
 * @return the url-based class path
 * @param base the base location
 * @exception MalformedURLException if a problem occurs computing the class path
 */
protected URL[] getBootPath(String base) throws MalformedURLException {
	URL url = null;
	// if the given location is not null, assume it is correct and use it. 
	if (base != null) {
		url = new URL(base);
		if (debug)
			System.out.println("Boot URL: " + url.toExternalForm()); //$NON-NLS-1$
		return new URL[] {url};	
	}
	// search for boot in root location
	URL[] result = null;
	url = new URL(rootLocation);
	String path = url.getFile();
	path = searchForBoot(path);
	// add on any dev path elements
	url = new URL(url.getProtocol(), url.getHost(), url.getPort(), path);
	result = getDevPath(url);
	if (debug) {
		System.out.println("Boot URL:"); //$NON-NLS-1$
		for (int i = 0; i < result.length; i++)
			System.out.println("    " + result[i].toExternalForm()); //$NON-NLS-1$
	}
	return result;
}

/**
 * Searches for a boot directory starting in the "plugins" subdirectory
 * of the given location.  If one is found then this location is returned; 
 * otherwise an exception is thrown.
 * 
 * @return the location where boot directory was found
 * @param start the location to begin searching at
 */
protected String searchForBoot(String start) {
	FileFilter filter = new FileFilter() {
		public boolean accept(File candidate) {
			return candidate.isDirectory() &&
				(candidate.getName().equals(PI_BOOT)
				|| candidate.getName().startsWith(PI_BOOT + "_")); //$NON-NLS-1$
		}
	};
	File[] boots = new File(start, "plugins").listFiles(filter); //$NON-NLS-1$
	if (boots == null)
		throw new RuntimeException("Could not find bootstrap code. Check location of boot plug-in or specify -boot."); //$NON-NLS-1$
	String result = null;
	Object maxVersion = null;
	for (int i = 0; i < boots.length; i++) {
		String name = boots[i].getName();
		int index = name.indexOf('_');
		String version;
		Object currentVersion;
		if (index == -1)
			version = "";  //$NON-NLS-1$ // Note: directory with version suffix is always > than directory without version suffix
		else
			version = name.substring(index + 1);
		currentVersion = getVersionElements(version);			
		if (maxVersion == null) {
			result = boots[i].getAbsolutePath();
			maxVersion = currentVersion;
		} else {
			if (compareVersion((Object[])maxVersion, (Object[])currentVersion) < 0) {
				result = boots[i].getAbsolutePath();
				maxVersion = currentVersion;
			}
		}
	}
	if (result == null)
		throw new RuntimeException("Could not find bootstrap code. Check location of boot plug-in or specify -boot."); //$NON-NLS-1$
	return result.replace(File.separatorChar, '/') + "/"; //$NON-NLS-1$
}

/**
 * Compares version strings. 
 * @return result of comparison, as integer;
 * <code><0</code> if left < right;
 * <code>0</code> if left == right;
 * <code>>0</code> if left > right;
 */
private int compareVersion(Object[] left, Object[] right) {
	
	int result = ((Integer)left[0]).compareTo((Integer)right[0]); // compare major
	if (result != 0)
		return result;
		
	result = ((Integer)left[1]).compareTo((Integer)right[1]); // compare minor
	if (result != 0)
		return result;
		
	result = ((Integer)left[2]).compareTo((Integer)right[2]); // compare service
	if (result != 0)
		return result;
		
	return ((String)left[3]).compareTo((String)right[3]); // compare qualifier
}

/**
 * Do a quick parse of version identifier so its elements can be correctly compared.
 * If we are unable to parse the full version, remaining elements are initialized
 * with suitable defaults.
 * @return an array of size 4; first three elements are of type Integer (representing
 * major, minor and service) and the fourth element is of type String (representing
 * qualifier). Note, that returning anything else will cause exceptions in the caller.
 */
private Object[] getVersionElements(String version) {
	Object[] result = {new Integer(0), new Integer(0), new Integer(0), ""}; //$NON-NLS-1$
	StringTokenizer t = new StringTokenizer(version, "."); //$NON-NLS-1$
	String token;
	int i = 0;
	while(t.hasMoreTokens() && i<4) {
		token = t.nextToken();
		if (i<3) {
			// major, minor or service ... numeric values
			try {
				result[i++] = new Integer(token);
			} catch(Exception e) {
				// invalid number format - use default numbers (0) for the rest
				break; 
			}
		} else {
			// qualifier ... string value
			result[i++] = token;
		}
	}
	return result;
}

/**
 * Runs the platform with the given arguments.  The arguments must identify
 * an application to run (e.g., <code>-application com.example.application</code>).
 * After running the application <code>System.exit(N)</code> is executed.
 * The value of N is derived from the value returned from running the application.
 * If the application's return value is an <code>Integer</code>, N is this value.
 * In all other cases, N = 0.
 * <p>
 * Clients wishing to run the platform without a following <code>System.exit</code>
 * call should use <code>run()</code>.
 *
 * @see #run
 * 
 * @param args the command line arguments
 */
public static void main(String[] args) {
	Object result = null;
	Main launcher = new Main();
	arguments = args;
	try {
		result = launcher.run(args);
	} catch (Throwable e) {
		// try and take down the splash screen.
		launcher.takeDownSplash();
		log("Exception launching the Eclipse Platform:"); //$NON-NLS-1$
		log(e);
		// FIXME: this is where we would return a special exit code
		// if we wanted the executable to display a message to the user
		// saying that they should check the log and the location of
		// the log file.
	}
	int exitCode = result instanceof Integer ? ((Integer) result).intValue() : 0;
	System.exit(exitCode);
}

/**
 * Runs this launcher with the arguments specified in the given string.
 * 
 * @param argString the arguments string
 * @exception Exception thrown if a problem occurs during launching
 */
public static void main(String argString) throws Exception {
	Vector list = new Vector(5);
	for (StringTokenizer tokens = new StringTokenizer(argString, " "); tokens.hasMoreElements();) //$NON-NLS-1$
		list.addElement((String) tokens.nextElement());
	main((String[]) list.toArray(new String[list.size()]));
}

/**
 * Processes the command line arguments
 * 
 * @return the arguments to pass through to the launched application
 * @param args the command line arguments
 */
protected String[] processCommandLine(String[] args) throws Exception {
	int[] configArgs = new int[100];
	configArgs[0] = -1; // need to initialize the first element to something that could not be an index.
	int configArgIndex = 0;
	for (int i = 0; i < args.length; i++) {
		boolean found = false;
		// check for args without parameters (i.e., a flag arg)
		// check if debug should be enabled for the entire platform
		if (args[i].equalsIgnoreCase(DEBUG)) {
			debug = true;
			// passed thru this arg (i.e., do not set found = true
			continue;
		}
		
		// check if this is initialization pass
		if (args[i].equalsIgnoreCase(INITIALIZE)) {
			cmdInitialize = true;
			// passed thru this arg (i.e., do not set found = true
			continue;
		}
		
		// check if development mode should be enabled for the entire platform
		// If this is the last arg or there is a following arg (i.e., arg+1 has a leading -), 
		// simply enable development mode.  Otherwise, assume that that the following arg is
		// actually some additional development time class path entries.  This will be processed below.
		if (args[i].equalsIgnoreCase(DEV) && ((i + 1 == args.length) || ((i + 1 < args.length) && (args[i + 1].startsWith("-"))))) { //$NON-NLS-1$
			inDevelopmentMode = true;
			// do not mark the arg as found so it will be passed through
			continue;
		}

		// done checking for args.  Remember where an arg was found 
		if (found) {
			configArgs[configArgIndex++] = i;
			continue;
		}
		// check for args with parameters. If we are at the last argument or if the next one
		// has a '-' as the first character, then we can't have an arg with a parm so continue.
		if (i == args.length - 1 || args[i + 1].startsWith("-"))  //$NON-NLS-1$
			continue;
		String arg = args[++i];

		// look for the laucher to run
		if (args[i - 1].equalsIgnoreCase(BOOT)) {
			bootLocation = arg;
			found = true;
		}

		// look explicitly set install root
		if (args[i - 1].equalsIgnoreCase(INSTALL)) {
			rootLocation = arg;
			found = true;
		}

		// look for the development mode and class path entries.  
		if (args[i - 1].equalsIgnoreCase(DEV)) {
			inDevelopmentMode = true;
			devClassPath = arg;
			continue;
		}

		// look for the location of workspace
		if (args[i - 1].equalsIgnoreCase(DATA)) {
			baseLocation = arg;
			continue; // pass the arg on
		}

		// look for the application to run
		if (args[i - 1].equalsIgnoreCase(APPLICATION)) {
			application = arg;
			found = true;
		}

		// look for the feature to run
		if (args[i - 1].equalsIgnoreCase(FEATURE)) {
			feature = arg;
			// we  mark -feature for removal. It will be
			// reinserted after we determine the actual 
			// feature we'll use
			found = true;
		}

		// look for the configuration to use
		if (args[i - 1].equalsIgnoreCase(CONFIGURATION)) {
			configuration = arg;
			// we  mark -configuration for removal. It will be
			// reinserted after we determine the actual URL of
			// the configuration file to use.
			found = true;
		}

		// look for token to use to show the splash screen
		if (args[i - 1].equalsIgnoreCase(SHOWSPLASH)) {
			showSplash = arg;
			found = true;
		}

		// look for token to use to end the splash screen
		if (args[i - 1].equalsIgnoreCase(ENDSPLASH)) {
			endSplash = arg;
			found = true;
		}

		// done checking for args.  Remember where an arg was found 
		if (found) {
			configArgs[configArgIndex++] = i - 1;
			configArgs[configArgIndex++] = i;
		}
	}
	// remove all the arguments consumed by this argument parsing
	if (configArgIndex == 0)
		return args;
	String[] passThruArgs = new String[args.length - configArgIndex];
	configArgIndex = 0;
	int j = 0;
	for (int i = 0; i < args.length; i++) {
		if (i == configArgs[configArgIndex])
			configArgIndex++;
		else
			passThruArgs[j++] = args[i];
	}
	return passThruArgs;
}
/**
 * Runs the application to be launched.
 * 
 * @return the return value from the launched application
 * @param args the arguments to pass to the application
 * @exception thrown if a problem occurs during launching
 */
public Object run(String[] args) throws Exception {
	String[] passThruArgs = processCommandLine(args);
	passThruArgs = processConfiguration(passThruArgs);
	return basicRun(passThruArgs);
}

/*
 * After the command line arguments have been processed, we try
 * to locate and load the platform configuration file. It contains
 * information maintained by the install/ update support. In 
 * particular, the following are needed at this point
 * in the startup sequence:
 * -> if -boot was not specified, which boot.jar to load 
 *    BootLoader from (original core.boot plugin may have been updated)
 * -> if -feature was not specified, what is the default feature
 *    (product packagers can set the default)
 * -> if we were requested to put up the splash (-showsplash
 *    was specified), which one (based on defaulted or
 *    specified feature information)
 * Note, that if we can't find the platform configuration file,
 * or it does not contain the information we are looking for,
 * the startup support ends up computing "reasonable" defaults
 * as before (based on relative locations within the file system)
 */
private String[] processConfiguration(String[] passThruArgs) throws MalformedURLException {
	// get install root location, if not specified
	if (rootLocation == null)
		rootLocation = getRootURL().toExternalForm();
		
	// attempt to locate configuration file
	URL configURL = null;
	if (configuration != null && !configuration.trim().equals("")) { //$NON-NLS-1$
		configuration = configuration.replace(File.separatorChar, '/');
		if (configuration.equalsIgnoreCase(ARG_USER_DIR)) {
			// configuration is in current working directory
			String tmp = System.getProperty("user.dir"); //$NON-NLS-1$
			if (!tmp.endsWith(File.separator))
				tmp += File.separator;
			configURL = new URL("file:" + tmp.replace(File.separatorChar,'/') + CONFIG_FILE); //$NON-NLS-1$
		} else if(configuration.endsWith("/")) { //$NON-NLS-1$
			// configuration specified as directory URL		
			configURL = new URL(configuration + CONFIG_FILE);
		} else {
			// configuration specified down to a file			
			configURL = new URL(configuration);
		}
	} else {
		// configuration not specified - defer to BootLoader
	}
		
	// load configuration
	loadConfiguration(configURL);
	
	// get boot url, if none was specified
	if (bootLocation == null) {
		String urlString = loadAttribute(props, CFG_CORE_BOOT, null);
		if (urlString != null) {
			try {
				urlString = resolve(urlString);
				URL bootDir = new URL(urlString);
				URL bootURL = new URL(bootDir, BOOTJAR);
				if (bootDir.getProtocol().equals("file")) { //$NON-NLS-1$
					File jar = new File(bootURL.getFile());
					if (jar.exists())
						// verify boot dir exists
						bootLocation = bootURL.toExternalForm();
					else
						// Somebody turned the tables on us. The info in
						// platform.cfg is stale, or the workspace is being used
						// with a new install. Do default search for boot and force
						// "first use" processing (optimistic reconciliation)
						cmdFirstUse = true;
				} else
					bootLocation = bootURL.toExternalForm();
			} catch(MalformedURLException e) {
				// continue ... will do default search for boot
			}
		}
	}
		
	// reconstruct command line arguments for configuration elements
	// (-boot and -application are not passed to BootLoader)
	if (configURL == null && feature == null && rootLocation == null)
		return passThruArgs;
			
	ArrayList args = new ArrayList(Arrays.asList(passThruArgs));
	
	if (configURL != null) {
		args.add(CONFIGURATION);
		args.add(decode(configURL.toExternalForm()));
	}
	
	if (cmdFirstUse) {
		args.add(FIRST_USE);
	}
	
	// pass root location downstream
	args.add(INSTALL);
	args.add(rootLocation);
	
	return (String[])args.toArray(new String[0]);
}

/**
 * Returns url of the location this class was loaded from
 */
private URL getRootURL() throws MalformedURLException {
	if (rootLocation != null)
		return new URL(rootLocation);
		
	URL	url = getClass().getProtectionDomain().getCodeSource().getLocation();
	String path = decode(url.getFile());
	path = new File(path).getAbsolutePath().replace(File.separatorChar,'/');
	if (path.endsWith(".jar")) //$NON-NLS-1$
		path = path.substring(0, path.lastIndexOf("/")+1); //$NON-NLS-1$
	url = new URL(url.getProtocol(), url.getHost(), url.getPort(), path);
	rootLocation = url.toExternalForm();
	return url;
}

/*
 * Load the configuration file. If not specified, default to the workspace
 */
private void loadConfiguration(URL url) {
	if (url == null) {
		// configuration URL was not specified ..... search
		String base = baseLocation;
		if (base == null) {
			// determine default workspace
			base = System.getProperty("user.dir"); //$NON-NLS-1$
			if (!base.endsWith(File.separator))
				base += File.separator;
			base += "workspace" + File.separator; //$NON-NLS-1$
		} else {
			base = base.replace('/',File.separatorChar);
		}
		if (!base.endsWith(File.separator))
			base += File.separator;
		
		File cfg = null;	
		try {
			// first look for configuration in base location (workspace or as specified)
			cfg = new File(base + ".metadata" + File.separator + ".config" + File.separator + CONFIG_FILE); //$NON-NLS-1$ //$NON-NLS-2$
			url = new URL("file", null, 0, cfg.getAbsolutePath()); //$NON-NLS-1$
			props = loadProperties(url);
			if (debug)
				System.out.println("Startup: using configuration " + url.toString()); //$NON-NLS-1$
			return; // we're done looking
		} catch(IOException e) {
			// continue ...
		}
		
		try {
			// look for configuration in install root (set up by -initialize)
			String install = getRootURL().getFile().replace('/',File.separatorChar);
			if (!install.endsWith(File.separator))
				install += File.separator;
			cfg = new File(install + ".config" + File.separator + CONFIG_FILE); //$NON-NLS-1$
			url = new URL("file", null, 0, cfg.getAbsolutePath()); //$NON-NLS-1$
			props = loadProperties(url);
			if (debug)
				System.out.println("Startup: using configuration " + url.toString()); //$NON-NLS-1$
		} catch(IOException e) {
			// continue ...
			if (debug)
				System.out.println("Startup: unable to load configuration\n" + e); //$NON-NLS-1$
		}
	} else {
		try {
			// configuration url was specified ... use it
			props = loadProperties(url);
			if (debug)
				System.out.println("Startup: using configuration " + url.toString()); //$NON-NLS-1$
		} catch(IOException e) {
			// continue ...
			if (debug)
				System.out.println("Startup: unable to load configuration\n" + e); //$NON-NLS-1$
		}
	}
}

private Properties loadProperties(URL url) throws IOException {
		
	// try to load saved configuration file (watch for failed prior save())
	Properties props = null;
	IOException originalException = null;
	try {
		props = load(url, null); // try to load config file
	} catch(IOException e1) {
		originalException = e1;
		try {
			props = load(url, CONFIG_FILE_TEMP_SUFFIX); // check for failures on save
		} catch(IOException e2) {
			try {
				props = load(url, CONFIG_FILE_BAK_SUFFIX); // check for failures on save
			} catch(IOException e3) {
				throw originalException; // we tried, but no config here ...
			}
		}
	}
	
	return props;
}
 
/*
 * Load the configuration  
 */ 
private Properties load(URL url, String suffix) throws IOException {
	// figure out what we will be loading
	if (suffix != null && !suffix.equals("")) //$NON-NLS-1$
		url = new URL(url.getProtocol(),url.getHost(),url.getPort(),url.getFile()+suffix);
			
	// try to load saved configuration file
	Properties props = new Properties();
	InputStream is = null;
	try {
		is = url.openStream();
		props.load(is);
		// check to see if we have complete config file
		if (!CFG_EOF.equals(props.getProperty(CFG_EOF))) {
			throw new IOException();
		}
	} finally {
		if (is!=null) {
			try {
				is.close();
			} catch(IOException e) {
				//ignore failure to close
			}
		}
	}
	
	// load feature index
	if (props != null) {
		String id = props.getProperty(CFG_FEATURE_ENTRY+".0."+CFG_FEATURE_ENTRY_ID); //$NON-NLS-1$
		for (int i=1; id != null; i++) {
			featureIndex.put(id, Integer.toString(i-1));
			id = props.getProperty(CFG_FEATURE_ENTRY+"."+i+"."+CFG_FEATURE_ENTRY_ID); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}
	
	return props;
}	
	
/*
 * Load a configuration attribute
 */	
private String loadAttribute(Properties props, String name, String dflt) {
	if (props == null)
		return dflt;
		
	String prop = props.getProperty(name);
	if (prop == null)
		return dflt;
	else
		return prop.trim();
}

/*
 * Handle splash screen.
 * We support 2 startup scenarios:
 * 
 * (1) the executable launcher put up the splash screen. In that
 *     scenario we are invoked with -endsplash command which is
 *     fully formed to take down the splash screen
 * 
 * (2) the executable launcher did not put up the splash screen,
 *     but invokes Eclipse with partially formed -showsplash command.
 *     In this scenario we determine which splash to display (based on 
 *     feature information) and then call -showsplash command. 
 * 
 * In both scenarios we pass a handler (Runnable) to the platform.
 * The handler is called as a result of the launched application calling
 * Platform.endSplash(). In the first scenario this results in the
 * -endsplash command being executed. In the second scenarios this
 * results in the process created as a result of the -showsplash command
 * being destroyed.
 * 
 * @param bootPath search path for the boot plugin
 */
private void handleSplash(URL[] bootPath) {
	
	// run without splash if we are initializing
	if (cmdInitialize) {
		showSplash = null;
		endSplash = null;
		return;
	}
	
	// if -endsplash is specified, use it and ignore any -showsplash command
	if (endSplash != null) {
		showSplash = null;
		return;
	}
	
	// check if we are running without a splash screen
	if (showSplash == null)
		return;

	// determine the splash path
	String path  = getSplashPath(bootPath);
	if (debug && path != null) {
		System.out.println("Startup: splash path = "+path); //$NON-NLS-1$
	}
		
	// Parse the showsplash command into its separate arguments.
	// The command format is: 
	//     <executable> -show <magicArg> [<splashPath>]
	// If either the <executable> or the <splashPath> arguments contain a
	// space, Runtime.getRuntime().exec( String ) will not work, even
	// if both arguments are enclosed in double-quotes. The solution is to
	// use the Runtime.getRuntime().exec( String[] ) method.
	String[] cmd = new String[ (path != null ? 4 : 3) ];
	int sIndex = 0;
	int eIndex = showSplash.indexOf( " -show" ); //$NON-NLS-1$
	if (eIndex == -1)
		return; // invalid -showsplash command
	cmd[0] = showSplash.substring( sIndex, eIndex );
	sIndex = eIndex + 1;
	eIndex = showSplash.indexOf( " ", sIndex ); //$NON-NLS-1$
	if (eIndex == -1)
		return; // invalid -showsplash command
	cmd[1] = showSplash.substring( sIndex, eIndex );
	cmd[2] = showSplash.substring( eIndex+1 );
	if (path != null)
		cmd[3] = path;
	try {
		showProcess = Runtime.getRuntime().exec(cmd);
	} catch (Exception e) {
		// continue without splash ...
		log("Exception showing splash screen."); //$NON-NLS-1$
		log(e);
	}
	return;
}

/*
 * take down the splash screen. Try both take-down methods just in case
 * (only one should ever be set)
 */
private void takeDownSplash() {
	if (splashDown) // splash is already down
		return;
		
	// check if -endsplash was specified
	if (endSplash != null) {
		try {
			Runtime.getRuntime().exec(endSplash);
		} catch (Exception e) {
			//ignore failure to end splash
		}
	}
	
	// check if -showsplash was specified and executed
	if (showProcess != null) {
		showProcess.destroy();
		showProcess = null;
	}
	
	splashDown = true;
}

/*
 * Return path of the splash image to use
 */
private String getSplashPath(URL[] bootPath) {
	
	// see if we can get a splash for the launched feature
	String[] featurePath = getFeatureRoot();	
	String path = lookupSplash(featurePath);
	if (path != null)
		return path;
	
	// no feature path, default to current boot plugin
	String temp = bootPath[0].getFile(); // take the first path element
	temp = temp.replace('/', File.separatorChar);
	int ix = temp.lastIndexOf("plugins"+File.separator); //$NON-NLS-1$
	if (ix != -1) {
		int pix = temp.indexOf(File.separator, ix+8);
		if (pix != -1) {
			temp = temp.substring(0,pix);
			path = lookupSplash(new String[] { temp });
			return path;
		}
	}
		
	// sorry, no splash found
	return null;
}

/*
 * Do a locale-sensitive lookup of splash image
 */
 private String lookupSplash(String[] searchPath) {
 	if (searchPath == null)
 		return null;
 	
 	// get current locale information
 	String localePath = Locale.getDefault().toString().replace('_', File.separatorChar);
 	
 	// search the specified path
 	while (localePath != null) {
 		String suffix;
 		if (localePath.equals("")) { //$NON-NLS-1$
 			// look for nl'ed splash image
 			suffix = SPLASH_IMAGE;
 		} else {
 			// look for default splash image
 			suffix = "nl" + File.separator + localePath + File.separator + SPLASH_IMAGE; //$NON-NLS-1$
 		}
 			
 		// check for file in searchPath
 		for (int i=0; i<searchPath.length; i++) {
 			String path = searchPath[i];
 			if (!path.endsWith(File.separator))
 				path += File.separator;
 			path += suffix;
 			File splash = new File(path);
 			if (splash.exists())
 				return path; // return the first match found
 		}
 		
 		// try the next variant
 		if (localePath.equals("")) //$NON-NLS-1$
 			localePath = null;
 		else {
 			int ix = localePath.lastIndexOf(File.separator);
 			if (ix == -1)
 				localePath = ""; //$NON-NLS-1$
 			else
 				localePath = localePath.substring(0,ix);
 		}
 	} 
 	
 	// sorry, could not find splash image
 	return null;
 }

/*
 * Return the feature identifier to use (specified or defaulted)
 */
private String getFeatureIdentifier() {
		
	if (feature != null) // -feature was specified on command line
		return feature; 
		
	// feature was not specified on command line
	String dflt = props.getProperty(CFG_FEATURE_ENTRY_DEFAULT);
	if (dflt != null)
		return dflt; // return customized default if set.
	else
		return null; // otherwise let BootLoader pick default
}

/*
 * Return the feature root if known
 */
private String[] getFeatureRoot() {
	String ix = featureIndex(getFeatureIdentifier());
	String urlString = props.getProperty(CFG_FEATURE_ENTRY + "." + ix + "." + CFG_FEATURE_ENTRY_ROOT + ".0"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	if (urlString == null)
		return null;
	
	ArrayList result = new ArrayList();
	URL url = null;
	for (int i=1; urlString != null; i++) {
		try {	
			urlString = resolve(urlString); // resolve platform relative URLs
			url = new URL(urlString);
			if (url.getProtocol().equals("file")) { //$NON-NLS-1$
				result.add(url.getFile().replace('/', File.separatorChar));
			} else
				continue; // in the future may cache
		} catch(MalformedURLException e) {
			// skip bad entries
		}
		urlString = props.getProperty(CFG_FEATURE_ENTRY + "." + ix + "." + CFG_FEATURE_ENTRY_ROOT + "." + i); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
	
	if (result.size()>0)
		return (String[])result.toArray(new String[0]);
	else 
		return null;
}

/*
 * resolve platform:/base/ URLs
 */
 private String resolve(String urlString) throws MalformedURLException { 	
 	if (urlString.startsWith(PLATFORM_URL)) {
 		String root = getRootURL().toExternalForm();
 		if (!root.endsWith("/")) //$NON-NLS-1$
 			root += "/"; //$NON-NLS-1$
 		String path = urlString.substring(PLATFORM_URL.length());
 		return root + path;
 	} else
 		return urlString;
 }

/*
 * 
 */
private String featureIndex(String id) {
	if (id==null)
		return null;
	return (String)featureIndex.get(id);
}

/*
 * Entry point for logging.
 */
private static synchronized void log(Object obj) {
	if (obj == null)
		return;
	try {
		openLogFile();
		try {
			if (newSession) {
				log.write(SESSION);
				log.write(' ');
				for (int i=SESSION.length(); i<78; i++)
					log.write('-');
				log.newLine();
				newSession = false;
			}
			write(obj);
		} finally {
			if (logFile == null) {
				if (log != null)
					log.flush();
			} else
				closeLogFile();
		}
	} catch (Exception e) {
		System.err.println("An exception occurred while writing to the platform log:");//$NON-NLS-1$
		e.printStackTrace(System.err);
		System.err.println("Logging to the console instead.");//$NON-NLS-1$
		//we failed to write, so dump log entry to console instead
		try {
			log = logForStream(System.err);
			write(obj);
			log.flush();
		} catch (Exception e2) {
			System.err.println("An exception occurred while logging to the console:");//$NON-NLS-1$
			e2.printStackTrace(System.err);
		}
	} finally {
			log = null;
	}
}
/*
 * This should only be called from #log()
 */
private static void write(Object obj) throws IOException {
	if (obj == null)
		return;
	if (obj instanceof Throwable) {
		log.write(STACK);
		log.newLine();
		((Throwable) obj).printStackTrace(new PrintWriter(log));
	} else {
		log.write(ENTRY);
		log.write(' ');
		log.write(PLUGIN_ID);
		log.write(' ');
		log.write(String.valueOf(ERROR));
		log.write(' ');
		log.write(String.valueOf(0));
		log.write(' ');
		try {
			DateFormat formatter = new SimpleDateFormat("MMM dd, yyyy kk:mm:ss.SS"); //$NON-NLS-1$
			log.write(formatter.format(new Date()));
		} catch (Exception e) {
			// continue if we can't write out the date
			log.write(Long.toString(System.currentTimeMillis()));
		}
		log.newLine();
		log.write(MESSAGE);
		log.write(' ');
		log.write(String.valueOf(obj));
	}
	log.newLine();
}
private static void computeLogFileLocation() {
	if (logFile != null)
		return;
	// check to see if the user specified a workspace location in the command-line args
	for (int i=0; logFile == null && i<arguments.length; i++) {
		if (arguments[i].equalsIgnoreCase(DATA)) {
			// found the -data command line argument so the next argument should be the
			// workspace location. Ensure that we have another arg to check
			if (i+1<arguments.length)
				logFile = new File(arguments[i+1]);
		}
	}
	// otherwise use the default location
	if (logFile == null)
		logFile = new File(System.getProperty("user.dir"), "workspace");//$NON-NLS-1$ //$NON-NLS-2$
	
	// append the .metadata directory and .log file name to the path
	logFile = new File(logFile, logFileName);
	logFile.getParentFile().mkdirs();
}
private static void openLogFile() throws IOException {
	computeLogFileLocation();
	try {
		log = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(logFile.getAbsolutePath(), true), "UTF-8"));//$NON-NLS-1$
	} catch (IOException e) {
		logFile = null;
		throw e;
	}
}
private static BufferedWriter logForStream(OutputStream output) {
	try {
		return new BufferedWriter(new OutputStreamWriter(output, "UTF-8"));//$NON-NLS-1$
	} catch (UnsupportedEncodingException e) {
		return new BufferedWriter(new OutputStreamWriter(output));
	}
}
private static void closeLogFile() throws IOException {
	try {
		if (log != null) {
			log.flush();
			log.close();
		}
	} finally {
		log = null;
	}
}

}
