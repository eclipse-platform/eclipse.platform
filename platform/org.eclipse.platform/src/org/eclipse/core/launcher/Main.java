/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.launcher;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.*;
import java.security.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * The launcher for Eclipse.
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
    protected URL installLocation = null;

    /**
     * The location of the configuration information for this instance
     */
    protected URL configurationLocation = null;

    /**
     * The location of the configuration information in the install root
     */
    protected String parentConfigurationLocation = null;

    /**
     * The id of the bundle that will contain the framework to run.  Defaults to org.eclipse.osgi.
     */
    protected String framework = OSGI;

    /**
     * The extra development time class path entries.
     */
    protected String devClassPath = null;

    /**
     * Indicates whether this instance is running in development mode.
     */
    protected boolean inDevelopmentMode = false;

    private String exitData = null;
    private String name = null; // The name to brand the launcher
    private String launcher = null; // The full path to the launcher

    private String vm = null;
    private String[] vmargs = null;
    private String[] commands = null;
    private String[] extensionPaths = null;

    // splash handling
    private String showSplash = null;
    private String endSplash = null;
    private boolean initialize = false;
    private Process showProcess = null;
    private boolean splashDown = false;
    private final Runnable endSplashHandler = new Runnable() {
        public void run() {
            takeDownSplash();
        }
    };

    // command line args
    private static final String FRAMEWORK = "-framework"; //$NON-NLS-1$
    private static final String INSTALL = "-install"; //$NON-NLS-1$
    private static final String INITIALIZE = "-initialize"; //$NON-NLS-1$
    private static final String VM = "-vm"; //$NON-NLS-1$
    private static final String VMARGS = "-vmargs"; //$NON-NLS-1$
    private static final String DEBUG = "-debug"; //$NON-NLS-1$
    private static final String DEV = "-dev"; //$NON-NLS-1$
    private static final String CONFIGURATION = "-configuration"; //$NON-NLS-1$
    private static final String EXITDATA = "-exitdata"; //$NON-NLS-1$
    private static final String NOSPLASH = "-nosplash"; //$NON-NLS-1$
    private static final String SHOWSPLASH = "-showsplash"; //$NON-NLS-1$
    private static final String NAME = "-name"; //$NON-NLS-1$
    private static final String LAUNCHER = "-launcher"; //$NON-NLS-1$
	private static final String NL = "-nl";  //$NON-NLS-1$
    private static final String ENDSPLASH = "-endsplash"; //$NON-NLS-1$
    private static final String SPLASH_IMAGE = "splash.bmp"; //$NON-NLS-1$
	private static final String CLEAN = "-clean"; //$NON-NLS-1$
	private static final String NOEXIT = "-noExit";  //$NON-NLS-1$
	
    private static final String OSGI = "org.eclipse.osgi"; //$NON-NLS-1$
    private static final String STARTER = "org.eclipse.core.runtime.adaptor.EclipseStarter"; //$NON-NLS-1$
    private static final String PLATFORM_URL = "platform:/base/"; //$NON-NLS-1$
    private static final String ECLIPSE_PROPERTIES = "eclipse.properties"; //$NON-NLS-1$
    private static final String FILE_SCHEME = "file:"; //$NON-NLS-1$    
    protected static final String REFERENCE_SCHEME = "reference:"; //$NON-NLS-1$
    protected static final String JAR_SCHEME = "jar:"; //$NON-NLS-1$
    
    private static final String DEFAULT_JRE_REQUIRED = "1.4.1"; //$NON-NLS-1$
    
    // constants: configuration file location
    private static final String CONFIG_DIR = "configuration/"; //$NON-NLS-1$
    private static final String CONFIG_FILE = "config.ini"; //$NON-NLS-1$
    private static final String CONFIG_FILE_TEMP_SUFFIX = ".tmp"; //$NON-NLS-1$
    private static final String CONFIG_FILE_BAK_SUFFIX = ".bak"; //$NON-NLS-1$
    private static final String ECLIPSE = "eclipse"; //$NON-NLS-1$
    private static final String PRODUCT_SITE_MARKER = ".eclipseproduct"; //$NON-NLS-1$
    private static final String PRODUCT_SITE_ID = "id"; //$NON-NLS-1$
    private static final String PRODUCT_SITE_VERSION = "version"; //$NON-NLS-1$

    // constants: System property keys and/or configuration file elements
    private static final String PROP_USER_HOME = "user.home"; //$NON-NLS-1$
    private static final String PROP_USER_DIR = "user.dir"; //$NON-NLS-1$
    private static final String PROP_INSTALL_AREA = "osgi.install.area"; //$NON-NLS-1$
    private static final String PROP_CONFIG_AREA = "osgi.configuration.area"; //$NON-NLS-1$
    private static final String PROP_CONFIG_AREA_DEFAULT = "osgi.configuration.area.default"; //$NON-NLS-1$
    private static final String PROP_BASE_CONFIG_AREA = "osgi.baseConfiguration.area"; //$NON-NLS-1$
    private static final String PROP_SHARED_CONFIG_AREA = "osgi.sharedConfiguration.area"; //$NON-NLS-1$
    private static final String PROP_CONFIG_CASCADED = "osgi.configuration.cascaded"; //$NON-NLS-1$
    protected static final String PROP_FRAMEWORK = "osgi.framework"; //$NON-NLS-1$
    private static final String PROP_SPLASHPATH = "osgi.splashPath"; //$NON-NLS-1$
    private static final String PROP_SPLASHLOCATION = "osgi.splashLocation"; //$NON-NLS-1$
    private static final String PROP_CLASSPATH = "osgi.frameworkClassPath"; //$NON-NLS-1$
    private static final String PROP_EXTENSIONS = "osgi.framework.extensions"; //$NON-NLS-1$
    private static final String PROP_FRAMEWORK_SYSPATH = "osgi.syspath"; //$NON-NLS-1$
    private static final String PROP_FRAMEWORK_SHAPE = "osgi.framework.shape"; //$NON-NLS-1$
    private static final String PROP_LOGFILE = "osgi.logfile"; //$NON-NLS-1$
    private static final String PROP_REQUIRED_JAVA_VERSION = "osgi.requiredJavaVersion"; //$NON-NLS-1$
    private static final String PROP_PARENT_CLASSLOADER = "osgi.parentClassloader"; //$NON-NLS-1$
    private static final String PROP_EOF = "eof"; //$NON-NLS-1$
	private static final String PROP_NL = "osgi.nl";  //$NON-NLS-1$
    private static final String PROP_NOSHUTDOWN = "osgi.noShutdown"; //$NON-NLS-1$
    private static final String PROP_DEBUG = "osgi.debug"; //$NON-NLS-1$	
	
    private static final String PROP_EXITCODE = "eclipse.exitcode"; //$NON-NLS-1$
    private static final String PROP_EXITDATA = "eclipse.exitdata"; //$NON-NLS-1$

    private static final String PROP_VM = "eclipse.vm"; //$NON-NLS-1$
    private static final String PROP_VMARGS = "eclipse.vmargs"; //$NON-NLS-1$
    private static final String PROP_COMMANDS = "eclipse.commands"; //$NON-NLS-1$
    private static final String PROP_ECLIPSESECURITY = "eclipse.security"; //$NON-NLS-1$

    // Data mode constants for user, configuration and data locations.
    private static final String NONE = "@none"; //$NON-NLS-1$
    private static final String NO_DEFAULT = "@noDefault"; //$NON-NLS-1$
    private static final String USER_HOME = "@user.home"; //$NON-NLS-1$
    private static final String USER_DIR = "@user.dir"; //$NON-NLS-1$

    // types of parent classloaders the framework can have
    private static final String PARENT_CLASSLOADER_APP = "app"; //$NON-NLS-1$
    private static final String PARENT_CLASSLOADER_EXT = "ext"; //$NON-NLS-1$
    private static final String PARENT_CLASSLOADER_BOOT = "boot"; //$NON-NLS-1$

    // log file handling
    protected static final String SESSION = "!SESSION"; //$NON-NLS-1$
    protected static final String ENTRY = "!ENTRY"; //$NON-NLS-1$
    protected static final String MESSAGE = "!MESSAGE"; //$NON-NLS-1$
    protected static final String STACK = "!STACK"; //$NON-NLS-1$
    protected static final int ERROR = 4;
    protected static final String PLUGIN_ID = "org.eclipse.core.launcher"; //$NON-NLS-1$
    protected File logFile = null;
    protected BufferedWriter log = null;
    protected boolean newSession = true;

    /**
     * A structured form for a version identifier.
     * 
     * @see http://java.sun.com/j2se/versioning_naming.html for information on valid version strings
     */
    static class Identifier {
        private static final String DELIM = ". _-"; //$NON-NLS-1$
        private int major, minor, service;
        Identifier(int major, int minor, int service) {
            super();
            this.major = major;
            this.minor = minor;
            this.service = service;
        }
        /**
         * @throws NumberFormatException if cannot parse the major and minor version components
         */
        Identifier(String versionString) {
            super();
            StringTokenizer tokenizer = new StringTokenizer(versionString, DELIM);

            // major
            if (tokenizer.hasMoreTokens())
                major = Integer.parseInt(tokenizer.nextToken());

            // minor
            if (tokenizer.hasMoreTokens())
                minor = Integer.parseInt(tokenizer.nextToken());

            try {
                // service
                if (tokenizer.hasMoreTokens())
                    service = Integer.parseInt(tokenizer.nextToken());
            } catch (NumberFormatException nfe) {
                // ignore the service qualifier in that case and default to 0
                // this will allow us to tolerate other non-conventional version numbers 
            }
        }
        /**
         * Returns true if this id is considered to be greater than or equal to the given baseline.
         * e.g. 
         * 1.2.9 >= 1.3.1 -> false
         * 1.3.0 >= 1.3.1 -> false
         * 1.3.1 >= 1.3.1 -> true
         * 1.3.2 >= 1.3.1 -> true
         * 2.0.0 >= 1.3.1 -> true
         */
        boolean isGreaterEqualTo(Identifier minimum) {
            if (major < minimum.major)
                return false;
            if (major > minimum.major)
                return true;
            // major numbers are equivalent so check minor
            if (minor < minimum.minor)
                return false;
            if (minor > minimum.minor)
                return true;
            // minor numbers are equivalent so check service
            return service >= minimum.service;
        }
    }   

    /**
     * Executes the launch.
     * 
     * @return the result of performing the launch
     * @param args command-line arguments
     * @exception Exception thrown if a problem occurs during the launch
     */
    protected void basicRun(String[] args) throws Exception {
        System.getProperties().setProperty("eclipse.startTime", Long.toString(System.currentTimeMillis())); //$NON-NLS-1$
        commands = args;
        String[] passThruArgs = processCommandLine(args);
        if (!debug)
        	// debug can be specified as system property as well
        	debug = System.getProperty(PROP_DEBUG) != null;
        setupVMProperties();
        processConfiguration();
        
        //ensure minimum Java version
        if (!checkVersion(System.getProperty("java.version"), System.getProperty(PROP_REQUIRED_JAVA_VERSION, DEFAULT_JRE_REQUIRED))) //$NON-NLS-1$
            return;
        
        // need to ensure that getInstallLocation is called at least once to initialize the value.
        // Do this AFTER processing the configuration to allow the configuration to set
        // the install location.  
        getInstallLocation();

        // locate boot plugin (may return -dev mode variations)
        URL[] bootPath = getBootPath(bootLocation);
        setSecurityPolicy(bootPath);
        // splash handling is done here, because the default case needs to know
        // the location of the boot plugin we are going to use
        handleSplash(bootPath);

        invokeFramework(passThruArgs, bootPath);
    }

    protected void setSecurityPolicy(URL[] bootPath) {
        String eclipseSecurity = System.getProperty(PROP_ECLIPSESECURITY);
        if (eclipseSecurity != null) {
            SecurityManager sm = System.getSecurityManager();
            boolean setSM = false;
            if (sm == null) {
                if (eclipseSecurity.length() < 1) {
                    eclipseSecurity = "java.lang.SecurityManager"; //$NON-NLS-1$
                }
                try {
                    Class clazz = Class.forName(eclipseSecurity);
                    sm = (SecurityManager) clazz.newInstance();
                    setSM = true;
                }
                catch (Throwable t) {
                    System.setProperty("java.security.manager", eclipseSecurity); // let the framework try to load it later. //$NON-NLS-1$
                }
            }
            
            ProtectionDomain domain = Main.class.getProtectionDomain();
            CodeSource source = null; 
            if (domain != null)
            	source = Main.class.getProtectionDomain().getCodeSource();
			if (domain == null || source == null) {
				log("Can not automatically set the security manager. Please use a policy file.");
				return;
			}
            // get the list of codesource URLs to grant AllPermission to
            URL[] rootURLs = new URL[bootPath.length + 1];
			rootURLs[0] = source.getLocation();
            System.arraycopy(bootPath, 0, rootURLs, 1, bootPath.length);
            // replace the security policy
            Policy eclispePolicy = new EclipsePolicy(Policy.getPolicy(), rootURLs);
            Policy.setPolicy(eclispePolicy);
            if (setSM)
                System.setSecurityManager(sm);
        }
    }

    private void invokeFramework(String[] passThruArgs, URL[] bootPath) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, Error, Exception, InvocationTargetException {
        String type = System.getProperty(PROP_PARENT_CLASSLOADER, PARENT_CLASSLOADER_BOOT);
        ClassLoader parent = null;
        if (PARENT_CLASSLOADER_APP.equalsIgnoreCase(type))
            parent = ClassLoader.getSystemClassLoader();
        else if (PARENT_CLASSLOADER_EXT.equalsIgnoreCase(type)) {
            ClassLoader appCL = ClassLoader.getSystemClassLoader();
            if (appCL != null)
                parent = appCL.getParent();
        }
        URLClassLoader loader = new StartupClassLoader(bootPath, parent);
        Class clazz = loader.loadClass(STARTER);
        Method method = clazz.getDeclaredMethod("run", new Class[] {String[].class, Runnable.class}); //$NON-NLS-1$
        try {
            method.invoke(clazz, new Object[] {passThruArgs, endSplashHandler});
        } catch (InvocationTargetException e) {
            if (e.getTargetException() instanceof Error)
                throw (Error) e.getTargetException();
            else if (e.getTargetException() instanceof Exception)
                throw (Exception) e.getTargetException();
            else
                //could be a subclass of Throwable!
                throw e;
        }
    }

    /**
     * Checks whether the given available version is greater or equal to the 
     * given required version.
     * <p>Will set PROP_EXITCODE/PROP_EXITDATA accordingly if check fails.</p>
     *   
     * @return a boolean indicating whether the checking passed 
     */
    private boolean checkVersion(String availableVersion, String requiredVersion) {
        if (requiredVersion == null || availableVersion == null)
            return true;
        try {
            Identifier required = new Identifier(requiredVersion);
            Identifier available = new Identifier(availableVersion);
            boolean compatible = available.isGreaterEqualTo(required);
            if (!compatible) {
                // any non-zero value should do it - 14 used to be used for version incompatibility in Eclipse 2.1 
                System.getProperties().put(PROP_EXITCODE, "14"); //$NON-NLS-1$
                System.getProperties().put(PROP_EXITDATA, "Required Java version: "+ requiredVersion + ". Available: " + availableVersion + '.'); //$NON-NLS-1$ //$NON-NLS-2$
            }
            return compatible;
        } catch (SecurityException e) {
            // If the security manager won't allow us to get the system property, continue for
            // now and let things fail later on their own if necessary.
            return true;
        } catch (NumberFormatException e) {
            // If the version string was in a format that we don't understand, continue and
            // let things fail later on their own if necessary.
            return true;
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
            Method method = clazz.getDeclaredMethod("decode", new Class[] {String.class, String.class}); //$NON-NLS-1$
            //first encode '+' characters, because URLDecoder incorrectly converts 
            //them to spaces on certain class library implementations.
            if (urlString.indexOf('+') >= 0) {
                int len = urlString.length();
                StringBuffer buf = new StringBuffer(len);
                for (int i = 0; i < len; i++) {
                    char c = urlString.charAt(i);
                    if (c == '+')
                        buf.append("%2B"); //$NON-NLS-1$
                    else
                        buf.append(c);
                }
                urlString = buf.toString();
            }
            Object result = method.invoke(null, new Object[] {urlString, "UTF-8"}); //$NON-NLS-1$
            if (result != null)
                return (String) result;
        } catch (Exception e) {
            //JDK 1.4 method not found -- fall through and decode by hand
        }
        //decode URL by hand
        boolean replaced = false;
        byte[] encodedBytes = urlString.getBytes();
        int encodedLength = encodedBytes.length;
        byte[] decodedBytes = new byte[encodedLength];
        int decodedLength = 0;
        for (int i = 0; i < encodedLength; i++) {
            byte b = encodedBytes[i];
            if (b == '%') {
                byte enc1 = encodedBytes[++i];
                byte enc2 = encodedBytes[++i];
                b = (byte) ((hexToByte(enc1) << 4) + hexToByte(enc2));
                replaced = true;
            }
            decodedBytes[decodedLength++] = b;
        }
        if (!replaced)
            return urlString;
        try {
            return new String(decodedBytes, 0, decodedLength, "UTF-8"); //$NON-NLS-1$
        } catch (UnsupportedEncodingException e) {
            //use default encoding
            return new String(decodedBytes, 0, decodedLength);
        }
    }

    /**
     * Returns the result of converting a list of comma-separated tokens into an array
     * 
     * @return the array of string tokens
     * @param prop the initial comma-separated string
     */
    protected String[] getArrayFromList(String prop) {
        if (prop == null || prop.trim().equals("")) //$NON-NLS-1$
            return new String[0];
        Vector list = new Vector();
        StringTokenizer tokens = new StringTokenizer(prop, ","); //$NON-NLS-1$
        while (tokens.hasMoreTokens()) {
            String token = tokens.nextToken().trim();
            if (!token.equals("")) //$NON-NLS-1$
                list.addElement(token);
        }
        return list.isEmpty() ? new String[0] : (String[]) list.toArray(new String[list.size()]);
    }

    /**
     * Returns the <code>URL</code>-based class path describing where the boot classes
     * are located when running in development mode.
     * 
     * @return the url-based class path
     * @param base the base location
     * @exception MalformedURLException if a problem occurs computing the class path
     */
    private URL[] getDevPath(URL base) throws IOException {
        ArrayList result = new ArrayList(5);
        if (inDevelopmentMode)
            addDevEntries(base, result); //$NON-NLS-1$
        //The jars from the base always need to be added, even when running in dev mode (bug 46772)
        addBaseJars(base, result);
        return (URL[]) result.toArray(new URL[result.size()]);
    }

    URL constructURL(URL url, String name) {
        //Recognize the following URLs
        //url: file:foo/dir/
        //url: file:foo/file.jar
        
        String externalForm = url.toExternalForm();
        if (externalForm.endsWith(".jar")) { //$NON-NLS-1$
            try {
                return new URL(JAR_SCHEME + url + "!/" + name); //$NON-NLS-1$
            } catch (MalformedURLException e) {
                //Ignore
            }
        }
        
        try {
            return new URL(url, name);
        } catch (MalformedURLException e) {
            //Ignore
            return null;
        }
    }
    private void readFrameworkExtensions(URL base, ArrayList result) throws IOException {
        String[] extensions = getArrayFromList(System.getProperties().getProperty(PROP_EXTENSIONS));
        String parent = new File(base.getFile()).getParent().toString();
        ArrayList extensionResults = new ArrayList(extensions.length);
        for (int i = 0; i < extensions.length; i++) {
            //Search the extension relatively to the osgi plugin 
            String path = searchFor(extensions[i], parent);
            if (path == null) {
                log("Could not find extension: " + extensions[i]); //$NON-NLS-1$
                continue;
            }
            if (debug)
                System.out.println("Loading extension: " + extensions[i]); //$NON-NLS-1$

            URL extensionURL = null;
            if (installLocation.getProtocol().equals("file")) { //$NON-NLS-1$
                extensionResults.add(path);
                extensionURL = new File(path).toURL();
            } else
                extensionURL = new URL(installLocation.getProtocol(), installLocation.getHost(), installLocation.getPort(), path);

            //Load a property file of the extension, merge its content, and in case of dev mode add the bin entries
            Properties extensionProperties = null;
            try {
                extensionProperties = loadProperties(constructURL(extensionURL, ECLIPSE_PROPERTIES));
            } catch (IOException e) {
                if (debug)
                    System.out.println("\t" + ECLIPSE_PROPERTIES + " not found"); //$NON-NLS-1$ //$NON-NLS-2$
            }
            String extensionClassPath = null;
            if (extensionProperties != null)
                extensionClassPath = extensionProperties.getProperty(PROP_CLASSPATH);
            else // this is a "normal" RFC 101 framework extension bundle just put the base path on the classpath
                extensionProperties = new Properties();
            String[] entries = extensionClassPath == null || extensionClassPath.length() == 0 ? new String[] {""} : getArrayFromList(extensionClassPath); //$NON-NLS-1$
            String qualifiedPath;
			if (System.getProperty(PROP_CLASSPATH)==null)
				qualifiedPath = "."; //$NON-NLS-1$
			else
				qualifiedPath = ""; //$NON-NLS-1$
            for (int j = 0; j < entries.length; j++)
                qualifiedPath += ", " + FILE_SCHEME + path + entries[j]; //$NON-NLS-1$
            extensionProperties.put(PROP_CLASSPATH, qualifiedPath);
            mergeProperties(System.getProperties(), extensionProperties);
            if (inDevelopmentMode) 
                addDevEntries(extensionURL, result);
        }
        extensionPaths = (String[]) extensionResults.toArray(new String[extensionResults.size()]);
    }

    private void addBaseJars(URL base, ArrayList result) throws IOException {
        String baseJarList = System.getProperty(PROP_CLASSPATH);
        if (baseJarList == null) {
            readFrameworkExtensions(base, result);
            baseJarList = System.getProperties().getProperty(PROP_CLASSPATH);
        }
        
        File fwkFile = new File(base.getFile());
        boolean fwkIsDirectory = fwkFile.isDirectory();
        //We found where the fwk is, remember it and its shape
        if (fwkIsDirectory) {
            System.getProperties().setProperty(PROP_FRAMEWORK_SHAPE, "folder");//$NON-NLS-1$
        } else {
            System.getProperties().setProperty(PROP_FRAMEWORK_SHAPE, "jar");//$NON-NLS-1$
        }
	    String fwkPath = new File(base.getFile()).getParentFile().getAbsolutePath();
		if (Character.isUpperCase(fwkPath.charAt(0))) {
			char[] chars = fwkPath.toCharArray();
			chars[0] = Character.toLowerCase(chars[0]);
			fwkPath = new String(chars);
		}
        System.getProperties().setProperty(PROP_FRAMEWORK_SYSPATH, fwkPath);
        
        String[] baseJars = getArrayFromList(baseJarList);
        if (baseJars.length == 0) {
            if (!inDevelopmentMode && new File(base.getFile()).isDirectory())
                throw new IOException("Unable to initialize " + PROP_CLASSPATH); //$NON-NLS-1$
            addEntry(base, result);
            return;
        }
        for (int i = 0; i < baseJars.length; i++) {
            String string = baseJars[i];
            try {
                // if the string is a file: URL then *carefully* construct the
                // URL. Otherwisejust try to build a URL. In either case, if we fail, use
                // string as something to tack on the end of the base.
                
                if (string.equals(".")) { //$NON-NLS-1$
                    addEntry(base, result);
                }
                URL url = null;
                if (string.startsWith(FILE_SCHEME)) //$NON-NLS-1$
                    url = new File(string.substring(5)).toURL();
                else
                    url = new URL(string);
                addEntry(url, result);
            } catch (MalformedURLException e) {
                addEntry(new URL(base, string), result);
            }
        }
    }

	protected void addEntry(URL url, List result) {
        if (new File(url.getFile()).exists())
            result.add(url);
    }

    private void addDevEntries(URL base, List result) throws MalformedURLException {
        String[] locations = getArrayFromList(devClassPath);
        for (int i = 0; i < locations.length; i++) {
            String location = locations[i];
            File path = new File(location);
            URL url;
            if (path.isAbsolute())
                url = path.toURL();
            else {
                // dev path is relative, combine with base location
                char lastChar = location.charAt(location.length() - 1);
                if ((location.endsWith(".jar") || (lastChar == '/' || lastChar == '\\'))) //$NON-NLS-1$
                    url = new URL(base, location);
                else
                    url = new URL(base, location + "/"); //$NON-NLS-1$
            }
            addEntry(url, result);
        }
    }

    /**
     * Returns the <code>URL</code>-based class path describing where the boot classes are located.
     * 
     * @return the url-based class path
     * @param base the base location
     * @exception MalformedURLException if a problem occurs computing the class path
     */
    protected URL[] getBootPath(String base) throws IOException {
        URL url = null;
        if (base != null) {
            url = buildURL(base, true);
        } else {
            // search in the root location
            url = getInstallLocation();
            String path = new File(url.getFile(), "plugins").toString(); //$NON-NLS-1$
            path = searchFor(framework, path);
            if (path == null)
                throw new RuntimeException("Could not find framework"); //$NON-NLS-1$
            if (url.getProtocol().equals("file")) //$NON-NLS-1$
                url = new File(path).toURL();
            else
                url = new URL(url.getProtocol(), url.getHost(), url.getPort(), path);
        }
        if (System.getProperty(PROP_FRAMEWORK) == null)
            System.getProperties().put(PROP_FRAMEWORK, url.toExternalForm());
        if (debug)
            System.out.println("Framework located:\n    " + url.toExternalForm()); //$NON-NLS-1$
        // add on any dev path elements
        URL[] result = getDevPath(url);
        if (debug) {
            System.out.println("Framework classpath:"); //$NON-NLS-1$
            for (int i = 0; i < result.length; i++)
                System.out.println("    " + result[i].toExternalForm()); //$NON-NLS-1$
        }
        return result;
    }

    /**
     * Searches for the given target directory starting in the "plugins" subdirectory
     * of the given location.  If one is found then this location is returned; 
     * otherwise an exception is thrown.
     * 
     * @return the location where target directory was found
     * @param start the location to begin searching
     */
    protected String searchFor(final String target, String start) {
        FileFilter filter = new FileFilter() {
            public boolean accept(File candidate) {
                return candidate.getName().equals(target) || candidate.getName().startsWith(target + "_"); //$NON-NLS-1$
            }
        };
        File[] candidates = new File(start).listFiles(filter); //$NON-NLS-1$
        if (candidates == null)
            return null;
        String[] arrays = new String[candidates.length];
        for (int i = 0; i < arrays.length; i++) {
            arrays[i] = candidates[i].getName();
        }
       int result = findMax(arrays);
        if (result == -1)
            return null;
        return candidates[result].getAbsolutePath().replace(File.separatorChar, '/') + (candidates[result].isDirectory() ? "/" : "");  //$NON-NLS-1$//$NON-NLS-2$
    }

    protected int findMax(String[] candidates) {
        int result = -1;
        Object maxVersion = null;
        for (int i = 0; i < candidates.length; i++) {
            String name = candidates[i];
            String version = ""; //$NON-NLS-1$ // Note: directory with version suffix is always > than directory without version suffix
            int index = name.indexOf('_');
            if (index != -1)
                version = name.substring(index + 1);
            Object currentVersion = getVersionElements(version);
            if (maxVersion == null) {
                result = i;
                maxVersion = currentVersion;
            } else {
                if (compareVersion((Object[]) maxVersion, (Object[]) currentVersion) < 0) {
                    result = i;
                    maxVersion = currentVersion;
                }
            }
        }
        return result;
    }
    /**
     * Compares version strings. 
     * @return result of comparison, as integer;
     * <code><0</code> if left < right;
     * <code>0</code> if left == right;
     * <code>>0</code> if left > right;
     */
    private int compareVersion(Object[] left, Object[] right) {

        int result = ((Integer) left[0]).compareTo((Integer) right[0]); // compare major
        if (result != 0)
            return result;

        result = ((Integer) left[1]).compareTo((Integer) right[1]); // compare minor
        if (result != 0)
            return result;

        result = ((Integer) left[2]).compareTo((Integer) right[2]); // compare service
        if (result != 0)
            return result;

        return ((String) left[3]).compareTo((String) right[3]); // compare qualifier
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
        if (version.endsWith(".jar")) //$NON-NLS-1$
            version = version.substring(0, version.length() - 4);
        Object[] result = {new Integer(0), new Integer(0), new Integer(0), ""}; //$NON-NLS-1$
        StringTokenizer t = new StringTokenizer(version, "."); //$NON-NLS-1$
        String token;
        int i = 0;
        while (t.hasMoreTokens() && i < 4) {
            token = t.nextToken();
            if (i < 3) {
                // major, minor or service ... numeric values
                try {
                    result[i++] = new Integer(token);
                } catch (Exception e) {
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

    private static URL buildURL(String spec, boolean trailingSlash) {
        if (spec == null)
            return null;
        boolean isFile = spec.startsWith(FILE_SCHEME); //$NON-NLS-1$
        try {
            if (isFile) {
                File toAdjust = new File(spec.substring(5));
                if (toAdjust.isDirectory())
                    return adjustTrailingSlash(toAdjust.toURL(), trailingSlash);
                else
                    return toAdjust.toURL();
            } else
                return new URL(spec);
        } catch (MalformedURLException e) {
            // if we failed and it is a file spec, there is nothing more we can do
            // otherwise, try to make the spec into a file URL.
            if (isFile)
                return null;
            try {
                File toAdjust = new File(spec);
                if (toAdjust.isDirectory())
                    return adjustTrailingSlash(toAdjust.toURL(), trailingSlash);
                else
                    return toAdjust.toURL();
            } catch (MalformedURLException e1) {
                return null;
            }
        }
    }

    private static URL adjustTrailingSlash(URL url, boolean trailingSlash) throws MalformedURLException {
        String file = url.getFile();
        if (trailingSlash == (file.endsWith("/"))) //$NON-NLS-1$
            return url;
        file = trailingSlash ? file + "/" : file.substring(0, file.length() - 1); //$NON-NLS-1$
        return new URL(url.getProtocol(), url.getHost(), file);
    }

    private URL buildLocation(String property, URL defaultLocation, String userDefaultAppendage) {
        URL result = null;
        String location = System.getProperty(property);
        System.getProperties().remove(property);
        // if the instance location is not set, predict where the workspace will be and 
        // put the instance area inside the workspace meta area.
        try {
            if (location == null)
                result = defaultLocation;
            else if (location.equalsIgnoreCase(NONE))
                return null;
            else if (location.equalsIgnoreCase(NO_DEFAULT))
                result = buildURL(location, true);
            else {
                if (location.startsWith(USER_HOME)) {
                    String base = substituteVar(location, USER_HOME, PROP_USER_HOME);
                    location = new File(base, userDefaultAppendage).getAbsolutePath();
                } else if (location.startsWith(USER_DIR)) {
                    String base = substituteVar(location, USER_DIR, PROP_USER_DIR);
                    location = new File(base, userDefaultAppendage).getAbsolutePath();
                }
                result = buildURL(location, true);
            }
        } finally {
            if (result != null)
                System.getProperties().put(property, result.toExternalForm());
        }
        return result;
    }

    private String substituteVar(String source, String var, String prop) {
        String value = System.getProperty(prop, ""); //$NON-NLS-1$
        return value + source.substring(var.length());
    }

    /** 
     * Retuns the default file system path for the configuration location.
     * By default the configuration information is in the installation directory
     * if this is writeable.  Otherwise it is located somewhere in the user.home
     * area relative to the current product. 
     * @return the default file system path for the configuration information
     */
    private String computeDefaultConfigurationLocation() {
        // 1) We store the config state relative to the 'eclipse' directory if possible
        // 2) If this directory is read-only 
        //    we store the state in <user.home>/.eclipse/<application-id>_<version> where <user.home> 
        //    is unique for each local user, and <application-id> is the one 
        //    defined in .eclipseproduct marker file. If .eclipseproduct does not
        //    exist, use "eclipse" as the application-id.

        URL install = getInstallLocation();
        // TODO a little dangerous here.  Basically we have to assume that it is a file URL.
        if (install.getProtocol().equals("file")) { //$NON-NLS-1$
            File installDir = new File(install.getFile());
            if (canWrite(installDir))
                return installDir.getAbsolutePath() + File.separator + CONFIG_DIR;
        }
        // We can't write in the eclipse install dir so try for some place in the user's home dir
        return computeDefaultUserAreaLocation(CONFIG_DIR);
    }

    private static boolean canWrite(File installDir) {
        if (installDir.canWrite() == false)
            return false;

        if (!installDir.isDirectory())
            return false;

        File fileTest = null;
        try {
            fileTest = File.createTempFile("writtableArea", null, installDir); //$NON-NLS-1$
        } catch (IOException e) {
            //If an exception occured while trying to create the file, it means that it is not writtable
            return false;
        } finally {
            if (fileTest != null)
                fileTest.delete();
        }
        return true;
    }
    
    /**
     * Returns a files system path for an area in the user.home region related to the
     * current product.  The given appendage is added to this base location
     * @param pathAppendage the path segments to add to computed base
     * @return a file system location in the user.home area related the the current
     *   product and the given appendage
     */
    private String computeDefaultUserAreaLocation(String pathAppendage) {
        //    we store the state in <user.home>/.eclipse/<application-id>_<version> where <user.home> 
        //    is unique for each local user, and <application-id> is the one 
        //    defined in .eclipseproduct marker file. If .eclipseproduct does not
        //    exist, use "eclipse" as the application-id.
        URL installURL = getInstallLocation();
        if (installURL == null)
            return null;
        File installDir = new File(installURL.getFile());
        String appName = "." + ECLIPSE; //$NON-NLS-1$
        File eclipseProduct = new File(installDir, PRODUCT_SITE_MARKER);
        if (eclipseProduct.exists()) {
            Properties props = new Properties();
            try {
                props.load(new FileInputStream(eclipseProduct));
                String appId = props.getProperty(PRODUCT_SITE_ID);
                if (appId == null || appId.trim().length() == 0)
                    appId = ECLIPSE;
                String appVersion = props.getProperty(PRODUCT_SITE_VERSION);
                if (appVersion == null || appVersion.trim().length() == 0)
                    appVersion = ""; //$NON-NLS-1$
                appName += File.separator + appId + "_" + appVersion; //$NON-NLS-1$
            } catch (IOException e) {
                // Do nothing if we get an exception.  We will default to a standard location 
                // in the user's home dir.
            }
        }
        String userHome = System.getProperty(PROP_USER_HOME);
        return new File(userHome, appName + "/" + pathAppendage).getAbsolutePath(); //$NON-NLS-1$
    }

    /**
     * Runs this launcher with the arguments specified in the given string.
     * 
     * @param argString the arguments string
     */
    public static void main(String argString) {
        Vector list = new Vector(5);
        for (StringTokenizer tokens = new StringTokenizer(argString, " "); tokens.hasMoreElements();) //$NON-NLS-1$
            list.addElement(tokens.nextElement());
        main((String[]) list.toArray(new String[list.size()]));
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
     * </p>
     * 
     * @param args the command line arguments
     * @see #run(String[])
     */
	public static void main(String[] args) {
		int result = 0;
		try {
			result = new Main().run(args);
		} catch (Throwable t) {
			// This is *really* unlikely to happen - run() takes care of exceptional situations.
			// In case something weird happens, just dump stack - logging is not available at this point
			t.printStackTrace();
		} finally {
			if (!Boolean.getBoolean(PROP_NOSHUTDOWN))
			// make sure we always terminate the VM
				System.exit(result);
		}
	}


    /**
     * Runs the platform with the given arguments.  The arguments must identify
     * an application to run (e.g., <code>-application com.example.application</code>).
     * Returns the value returned from running the application.
     * If the application's return value is an <code>Integer</code>, N is this value.
     * In all other cases, N = 0.
     *
     * @param args the command line arguments
     */
    public int run(String[] args) {
        int result = 0;
        try {
            basicRun(args);
            String exitCode = System.getProperty(PROP_EXITCODE);
            try {
                result = exitCode == null ? 0 : Integer.parseInt(exitCode);
            } catch (NumberFormatException e) {
                result = 17;
            }
        } catch (Throwable e) {
            // only log the exceptions if they have not been caught by the 
            // EclipseStarter (i.e., if the exitCode is not 13) 
            if (!"13".equals(System.getProperty(PROP_EXITCODE))) { //$NON-NLS-1$
                log("Exception launching the Eclipse Platform:"); //$NON-NLS-1$
                log(e);
                String message = "An error has occurred"; //$NON-NLS-1$
                if (logFile == null)
                    message += " and could not be logged: \n" + e.getMessage(); //$NON-NLS-1$
                else
                    message += ".  See the log file\n" + logFile.getAbsolutePath(); //$NON-NLS-1$
                System.getProperties().put(PROP_EXITDATA, message);
            }
            // Return "unlucky" 13 as the exit code. The executable will recognize
            // this constant and display a message to the user telling them that
            // there is information in their log file.
            result = 13;
        } finally {
            // always try putting down the splash screen just in case the application failed to do so
            takeDownSplash();           
        }
        // Return an int exit code and ensure the system property is set.
        System.getProperties().put(PROP_EXITCODE, Integer.toString(result));
        setExitData();
        return result;
    }

    private void setExitData() {
        String data = System.getProperty(PROP_EXITDATA);
        if (exitData == null || data == null)
            return;
        // sync call to the launcher
        runCommand(true, new String[] {EXITDATA, exitData, data}); //$NON-NLS-1$
    }

    /**
     * Processes the command line arguments.  The general principle is to NOT
     * consume the arguments and leave them to be processed by Eclipse proper.
     * There are a few args which are directed towards main() and a few others
     * which we need to know about. Very few should actually be consumed here.
     * 
     * @return the arguments to pass through to the launched application
     * @param args the command line arguments
     */
    protected String[] processCommandLine(String[] args) {
        if (args.length == 0)
            return args;
        int[] configArgs = new int[args.length];
        configArgs[0] = -1; // need to initialize the first element to something that could not be an index.
        int configArgIndex = 0;
        for (int i = 0; i < args.length; i++) {
            boolean found = false;
            // check for args without parameters (i.e., a flag arg)
            // check if debug should be enabled for the entire platform
            if (args[i].equalsIgnoreCase(DEBUG)) {
                debug = true;
                // passed thru this arg (i.e., do not set found = true)
                continue;
            }

            // look for and consume the nosplash directive.  This supercedes any
            // -showsplash command that might be present.
            if (args[i].equalsIgnoreCase(NOSPLASH)) {
                splashDown = true;
                found = true;
            }

            if (args[i].equalsIgnoreCase(NOEXIT)) {
            	System.getProperties().put(PROP_NOSHUTDOWN, "true"); //$NON-NLS-1$
            	found = true;
            }
            
            // check if this is initialization pass
            if (args[i].equalsIgnoreCase(INITIALIZE)) {
                initialize = true;
                // passed thru this arg (i.e., do not set found = true)
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

            // look for the VM args arg.  We have to do that before looking to see
            // if the next element is a -arg as the thing following -vmargs may in
            // fact be another -arg.
            if (args[i].equalsIgnoreCase(VMARGS)) {
                // consume the -vmargs arg itself
                args[i] = null;
                i++;
                vmargs = new String[args.length - i];
                for (int j = 0; i < args.length; i++) {
                    vmargs[j++] = args[i];
                    args[i] = null;
                }
                continue;
            }

            // check for args with parameters. If we are at the last argument or if the next one
            // has a '-' as the first character, then we can't have an arg with a parm so continue.
            if (i == args.length - 1 || args[i + 1].startsWith("-")) //$NON-NLS-1$
                continue;
            String arg = args[++i];

            // look for the development mode and class path entries.  
            if (args[i - 1].equalsIgnoreCase(DEV)) {
                inDevelopmentMode = true;
                devClassPath = processDevArg(arg);
                continue;
            }

            // look for the framework to run
            if (args[i - 1].equalsIgnoreCase(FRAMEWORK)) {
                framework = arg;
                found = true;
            }

            // look for explicitly set install root
            // Consume the arg here to ensure that the launcher and Eclipse get the 
            // same value as each other.  
            if (args[i - 1].equalsIgnoreCase(INSTALL)) {
                System.getProperties().put(PROP_INSTALL_AREA, arg);
                found = true;
            }

            // look for the configuration to use.  
            // Consume the arg here to ensure that the launcher and Eclipse get the 
            // same value as each other.  
            if (args[i - 1].equalsIgnoreCase(CONFIGURATION)) {
                System.getProperties().put(PROP_CONFIG_AREA, arg);
                found = true;
            }

            // look for the command to use to set exit data in the launcher
            if (args[i - 1].equalsIgnoreCase(EXITDATA)) {
                exitData = arg;
                found = true;
            }

            // look for the command to use to show the splash screen
            if (args[i - 1].equalsIgnoreCase(SHOWSPLASH)) {
                showSplash = arg;
                found = true;
            }

            // look for the name to use by the launcher
            if (args[i - 1].equalsIgnoreCase(NAME)) {
                name = arg;
                found = true;
            }

            // look for the launcher location
            if (args[i - 1].equalsIgnoreCase(LAUNCHER)) {
                launcher = arg;
                found = true;
            }
            
            // look for the command to use to end the splash screen
            if (args[i - 1].equalsIgnoreCase(ENDSPLASH)) {
                endSplash = arg;
                found = true;
            }

            // look for the VM location arg
            if (args[i - 1].equalsIgnoreCase(VM)) {
                vm = arg;
                found = true;
            }

			//look for the nl setting
			if (args[i - 1].equalsIgnoreCase(NL)) {
				System.getProperties().put(PROP_NL, arg);
				found = true;
			}
			
            // done checking for args.  Remember where an arg was found 
            if (found) {
                configArgs[configArgIndex++] = i - 1;
                configArgs[configArgIndex++] = i;
            }
        }
        // remove all the arguments consumed by this argument parsing
        String[] passThruArgs = new String[args.length - configArgIndex - (vmargs == null ? 0 : vmargs.length + 1)];
        configArgIndex = 0;
        int j = 0;
        for (int i = 0; i < args.length; i++) {
            if (i == configArgs[configArgIndex])
                configArgIndex++;
            else if (args[i] != null)
                passThruArgs[j++] = args[i];
        }
        return passThruArgs;
    }

    private String processDevArg(String arg) {
        if (arg == null)
            return null;
        try {
            URL location = new URL(arg);
            Properties props = load(location, null);
            String result = props.getProperty(OSGI);
            return result == null ? props.getProperty("*") : result; //$NON-NLS-1$
        } catch (MalformedURLException e) {
            // the arg was not a URL so use it as is.
            return arg;
        } catch (IOException e) {
            // TODO consider logging here
            return null;
        }
    }

    private URL getConfigurationLocation() {
        if (configurationLocation != null)
            return configurationLocation;
        configurationLocation = buildLocation(PROP_CONFIG_AREA, null, ""); //$NON-NLS-1$
        if (configurationLocation == null) {
            configurationLocation = buildLocation(PROP_CONFIG_AREA_DEFAULT, null, ""); //$NON-NLS-1$
            if (configurationLocation == null)
                configurationLocation = buildURL(computeDefaultConfigurationLocation(), true);
        }
        if (configurationLocation != null)
            System.getProperties().put(PROP_CONFIG_AREA, configurationLocation.toExternalForm());
        if (debug)
            System.out.println("Configuration location:\n    " + configurationLocation); //$NON-NLS-1$
        return configurationLocation;
    }

    private void processConfiguration() {
        // if the configuration area is not already defined, discover the config area by
        // trying to find a base config area.  This is either defined in a system property or
        // is computed relative to the install location.
        // Note that the config info read here is only used to determine a value 
        // for the user configuration area
        URL baseConfigurationLocation = null;
        Properties baseConfiguration = null;
        if (System.getProperty(PROP_CONFIG_AREA) == null) {
            String baseLocation = System.getProperty(PROP_BASE_CONFIG_AREA);
            if (baseLocation != null)
                // here the base config cannot have any symbolic (e..g, @xxx) entries.  It must just
                // point to the config file.
                baseConfigurationLocation = buildURL(baseLocation, true);
            if (baseConfigurationLocation == null)
                try {
                    // here we access the install location but this is very early.  This case will only happen if
                    // the config area is not set and the base config area is not set (or is bogus).
                    // In this case we compute based on the install location.
                    baseConfigurationLocation = new URL(getInstallLocation(), CONFIG_DIR);
                } catch (MalformedURLException e) {
                    // leave baseConfigurationLocation null
                }
            baseConfiguration = loadConfiguration(baseConfigurationLocation);
            if (baseConfiguration != null) {
                // if the base sets the install area then use that value if the property.  We know the 
                // property is not already set.
                String location = baseConfiguration.getProperty(PROP_CONFIG_AREA);
                if (location != null)
                    System.getProperties().put(PROP_CONFIG_AREA, location);
                // if the base sets the install area then use that value if the property is not already set.
                // This helps in selfhosting cases where you cannot easily compute the install location
                // from the code base.
                location = baseConfiguration.getProperty(PROP_INSTALL_AREA);
                if (location != null && System.getProperty(PROP_INSTALL_AREA) == null)
                    System.getProperties().put(PROP_INSTALL_AREA, location);
            }
        }

        // Now we know where the base configuration is supposed to be.  Go ahead and load
        // it and merge into the System properties.  Then, if cascaded, read the parent configuration
        // Note that the parent may or may not be the same parent as we read above since the 
        // base can define its parent.  The first parent we read was either defined by the user
        // on the command line or was the one in the install dir.  
        // if the config or parent we are about to read is the same as the base config we read above,
        // just reuse the base
        Properties configuration = baseConfiguration;
        if (configuration == null || !getConfigurationLocation().equals(baseConfigurationLocation))
            configuration = loadConfiguration(getConfigurationLocation());
        mergeProperties(System.getProperties(), configuration);
        if ("false".equalsIgnoreCase(System.getProperty(PROP_CONFIG_CASCADED))) //$NON-NLS-1$
            // if we are not cascaded then remove the parent property even if it was set.
            System.getProperties().remove(PROP_SHARED_CONFIG_AREA);
        else {
        	ensureAbsolute(PROP_SHARED_CONFIG_AREA);
            URL sharedConfigURL = buildLocation(PROP_SHARED_CONFIG_AREA, null, ""); //$NON-NLS-1$
            if (sharedConfigURL == null)
                try {
                    // there is no shared config value so compute one
                    sharedConfigURL = new URL(getInstallLocation(), CONFIG_DIR);
                } catch (MalformedURLException e) {
                    // leave sharedConfigurationLocation null
                }
            // if the parent location is different from the config location, read it too.
            if (sharedConfigURL != null) {
                if (sharedConfigURL.equals(getConfigurationLocation()))
                    // remove the property to show that we do not have a parent.
                    System.getProperties().remove(PROP_SHARED_CONFIG_AREA);
                else {
                    // if the parent we are about to read is the same as the base config we read above,
                    // just reuse the base
                    configuration = baseConfiguration;
                    if (!sharedConfigURL.equals(baseConfigurationLocation))
                        configuration = loadConfiguration(sharedConfigURL);
                    mergeProperties(System.getProperties(), configuration);
                    System.getProperties().put(PROP_SHARED_CONFIG_AREA, sharedConfigURL.toExternalForm());
                    if (debug)
                        System.out.println("Shared configuration location:\n    " + sharedConfigURL.toExternalForm()); //$NON-NLS-1$
                }
            }
        }
        // setup the path to the framework
        String urlString = System.getProperty(PROP_FRAMEWORK, null);
        if (urlString != null) {
            URL url = buildURL(urlString, true);
            System.getProperties().put(PROP_FRAMEWORK, url.toExternalForm());
            bootLocation = resolve(urlString);
        }
    }

    /**
	 * Ensures the value for a system property is an absolute URL. Relative URLs are translated to
	 * absolute URLs by taking the install URL as reference.
	 *   
	 * @param locationProperty the key for a system property containing a URL
	 */
	private void ensureAbsolute(String locationProperty) {
		String propertyValue = System.getProperty(locationProperty);
		if (propertyValue == null)
			// property not defined
			return;
		URL locationURL = null;
		try {
			locationURL = new URL(propertyValue);
		} catch (MalformedURLException e) {
			// property is not a valid URL
			return;
		}
		String locationPath = locationURL.getPath();
		if (locationPath.startsWith("/")) //$NON-NLS-1$
			// property value is absolute
			return;
		URL installURL = getInstallLocation();
		if (!locationURL.getProtocol().equals(installURL.getProtocol()))
			// not same protocol
			return;
		try {
			URL absoluteURL = new URL(installURL, locationPath);
			System.getProperties().put(locationProperty, absoluteURL.toExternalForm());
		} catch (MalformedURLException e) {
			// should not happen - the relative URL is known to be valid
		}
	}

    /**
     * Returns url of the location this class was loaded from
     */
    private URL getInstallLocation() {
        if (installLocation != null)
            return installLocation;

        // value is not set so compute the default and set the value
        String installArea = System.getProperty(PROP_INSTALL_AREA);
        if (installArea != null) {
            installLocation = buildURL(installArea, true);
            if (installLocation == null)
                throw new IllegalStateException("Install location is invalid: " + installArea); //$NON-NLS-1$
            System.getProperties().put(PROP_INSTALL_AREA, installLocation.toExternalForm());
            if (debug)
                System.out.println("Install location:\n    " + installLocation); //$NON-NLS-1$
            return installLocation;
        }

        ProtectionDomain domain = Main.class.getProtectionDomain();
        CodeSource source = null;
		URL result = null;
        if (domain != null)
        	source = domain.getCodeSource();
        if (source == null || domain == null) {
			if (debug)
				System.out.println("CodeSource location is null. Defaulting the install location to file:startup.jar"); //$NON-NLS-1$
			try {
				result = new URL("file:startup.jar"); //$NON-NLS-1$
			} catch (MalformedURLException e2) {
				//Ignore
			}
        }
		if (source != null)
			result = source.getLocation();
		
        String path = decode(result.getFile());
        // normalize to not have leading / so we can check the form
        File file = new File(path);
        path = file.toString().replace('\\', '/');
        // TODO need a better test for windows
        // If on Windows then canonicalize the drive letter to be lowercase.
        // remember that there may be UNC paths 
        if (File.separatorChar == '\\')
            if (Character.isUpperCase(path.charAt(0))) {
                char[] chars = path.toCharArray();
                chars[0] = Character.toLowerCase(chars[0]);
                path = new String(chars);
            }
        if (path.toLowerCase().endsWith(".jar")) //$NON-NLS-1$
            path = path.substring(0, path.lastIndexOf("/") + 1); //$NON-NLS-1$
        try {
            try {
                // create a file URL (via File) to normalize the form (e.g., put 
                // the leading / on if necessary)
                path = new File(path).toURL().getFile();
            } catch (MalformedURLException e1) {
                // will never happen.  The path is straight from a URL.  
            }
            installLocation = new URL(result.getProtocol(), result.getHost(), result.getPort(), path);
            System.getProperties().put(PROP_INSTALL_AREA, installLocation.toExternalForm());
        } catch (MalformedURLException e) {
            // TODO Very unlikely case.  log here.  
        }
        if (debug)
            System.out.println("Install location:\n    " + installLocation); //$NON-NLS-1$
        return installLocation;
    }

    /*
     * Load the given configuration file
     */
    private Properties loadConfiguration(URL url) {
        Properties result = null;
        try {
            url = new URL(url, CONFIG_FILE);
        } catch (MalformedURLException e) {
            return null;
        }
        try {
            if (debug)
                System.out.print("Configuration file:\n    " + url.toString()); //$NON-NLS-1$
            result = loadProperties(url);
            if (debug)
                System.out.println(" loaded"); //$NON-NLS-1$
        } catch (IOException e) {
            if (debug)
                System.out.println(" not found or not read"); //$NON-NLS-1$
        }
        return result;
    }

    private Properties loadProperties(URL url) throws IOException {
        // try to load saved configuration file (watch for failed prior save())
        if (url == null)
            return null;
        Properties result = null;
        IOException originalException = null;
        try {
            result = load(url, null); // try to load config file
        } catch (IOException e1) {
            originalException = e1;
            try {
                result = load(url, CONFIG_FILE_TEMP_SUFFIX); // check for failures on save
            } catch (IOException e2) {
                try {
                    result = load(url, CONFIG_FILE_BAK_SUFFIX); // check for failures on save
                } catch (IOException e3) {
                    throw originalException; // we tried, but no config here ...
                }
            }
        }
        return result;
    }

    /*
     * Load the configuration  
     */
    private Properties load(URL url, String suffix) throws IOException {
        // figure out what we will be loading
        if (suffix != null && !suffix.equals("")) //$NON-NLS-1$
            url = new URL(url.getProtocol(), url.getHost(), url.getPort(), url.getFile() + suffix);

        // try to load saved configuration file
        Properties props = new Properties();
        InputStream is = null;
        try {
            is = url.openStream();
            props.load(is);
        } finally {
            if (is != null)
                try {
                    is.close();
                } catch (IOException e) {
                    //ignore failure to close
                }
        }
        return props;
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
    private void handleSplash(URL[] defaultPath) {
        // run without splash if we are initializing or nosplash 
        // was specified (splashdown = true)
        if (initialize || splashDown) {
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

        // determine the splash location
        String location = getSplashLocation(defaultPath);
        if (debug)
            System.out.println("Splash location:\n    " + location); //$NON-NLS-1$
        if (location == null)
            return;
        // async call to the launcher
        showProcess = runCommand(false, new String[] {SHOWSPLASH, showSplash, location}); //$NON-NLS-1$
    }
    
    private Process runCommand(boolean block, String[] extraArgs) {
        final int NBR_DEFAULT_ARGS = 3;
        String[] args = new String[NBR_DEFAULT_ARGS + extraArgs.length];
        args[0] = launcher;
        args[1] = NAME;
        args[2] = name;

        for (int i = 0; i < extraArgs.length; i++) {
            args[i + NBR_DEFAULT_ARGS] = extraArgs[i];
        }

        if (debug) {
            System.out.print("runCommand:\n    ");//$NON-NLS-1$ 
            for (int i = 0; i < args.length; i++) {
                System.out.print('<' + args[i] + '>');//$NON-NLS-1$ //$NON-NLS-2$ 
            }
            System.out.println();
        }

        Process result = null;
        try {
            result = Runtime.getRuntime().exec(args);
            if (block)
                result.waitFor();
        } catch (Exception e) {
            log("Exception running command: " + extraArgs != null && extraArgs.length > 0 ? extraArgs[0] : null); //$NON-NLS-1$ 
            log(e);
        }
        return result;
    }

    /*
     * take down the splash screen. Try both take-down methods just in case
     * (only one should ever be set)
     */
    protected void takeDownSplash() {
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
     * Return path of the splash image to use.  First search the defined splash path.
     * If that does not work, look for a default splash.  Currently the splash must be in the file system
     * so the return value here is the file system path.
     */
    private String getSplashLocation(URL[] bootPath) {
        String result = System.getProperty(PROP_SPLASHLOCATION);
        if (result != null)
            return result;
        String splashPath = System.getProperty(PROP_SPLASHPATH);
        if (splashPath != null) {
            String[] entries = getArrayFromList(splashPath);
            ArrayList path = new ArrayList(entries.length);
            for (int i = 0; i < entries.length; i++) {
                String entry = resolve(entries[i]);
                if (entry == null || entry.startsWith(FILE_SCHEME)) {
                    File entryFile = new File(entry.substring(5).replace('/', File.separatorChar));
                    entry = searchFor(entryFile.getName(), entryFile.getParent());
                    if (entry != null)
                        path.add(entry);
                } else
                    log("Invalid splash path entry: " + entries[i]); //$NON-NLS-1$
            }
            // see if we can get a splash given the splash path
            result = searchForSplash((String[]) path.toArray(new String[path.size()]));
            if (result != null) {
                System.getProperties().put(PROP_SPLASHLOCATION, result);
                return result;
            }
        }

        // can't find it on the splashPath so look for a default splash
        String temp = bootPath[0].getFile(); // take the first path element
        temp = temp.replace('/', File.separatorChar);
        int ix = temp.lastIndexOf("plugins" + File.separator); //$NON-NLS-1$
        if (ix != -1) {
            int pix = temp.indexOf(File.separator, ix + 8);
            if (pix != -1) {
                temp = temp.substring(0, pix);
                result = searchForSplash(new String[] {temp});
                if (result != null)
                    System.getProperties().put(PROP_SPLASHLOCATION, result);
            }
        }
        return result;
    }

    /*
     * Do a locale-sensitive lookup of splash image
     */
    private String searchForSplash(String[] searchPath) {
        if (searchPath == null)
            return null;

        // Get the splash screen for the specified locale
		String locale = (String) System.getProperties().get(PROP_NL);
		if (locale == null)
			locale = Locale.getDefault().toString();
		String[] nlVariants = buildNLVariants(locale);

		for (int i=0; i<nlVariants.length; i++) {
			for (int j=0; j<searchPath.length; j++) {
				// do we have a JAR?
				if (isJAR(searchPath[j])) {
					String result = extractSplashFromJAR(searchPath[j], nlVariants[i]);
					if (result != null)
						return result;
				} else {
					// we have a file or a directory
	                String path = searchPath[j];
	                if (!path.endsWith(File.separator))
	                    path += File.separator;
	                path += nlVariants[i];
	                File result = new File(path);
	                if (result.exists())
	                    return result.getAbsolutePath(); // return the first match found [20063]
				}
			}
		}

        // sorry, could not find splash image
        return null;
    }

	/**
	 * Transfers all available bytes from the given input stream to the given output stream. 
	 * Regardless of failure, this method closes both streams.
	 */
	private static void transferStreams(InputStream source, OutputStream destination) {
		byte[] buffer = new byte[8096];
		try {
			while (true) {
				int bytesRead = -1;
				try {
					bytesRead = source.read(buffer);
				} catch (IOException e) {
					return;
				}
				if (bytesRead == -1)
					break;
				try {
					destination.write(buffer, 0, bytesRead);
				} catch (IOException e) {
					return;
				}
			}
		} finally {
			try {
				source.close();
			} catch (IOException e) {
				// ignore
			} finally {
				//close destination in finally in case source.close fails
				try {
					destination.close();
				} catch (IOException e) {
					// ignore
				}
			}
		}
	}

	/*
	 * Look for the specified spash file in the given JAR and extract it to the config 
	 * area for caching purposes.
	 */
	private String extractSplashFromJAR(String jarPath, String splashPath) {
		String configLocation = System.getProperty(PROP_CONFIG_AREA);
		if (configLocation == null) {
			log("Configuration area not set yet. Unable to extract splash from JAR'd plug-in: " + jarPath); //$NON-NLS-1$
			return null;
		}
		URL configURL = buildURL(configLocation, false);
		if (configURL == null)
			return null;
		// cache the splash in the OSGi sub-dir in the config area
		File splash = new File(configURL.getPath(), OSGI);
		splash = new File(splash, splashPath);
		// if we have already extracted this file before, then return
		if (splash.exists()) {
			// if we are running with -clean then delete the cached splash file
			boolean clean = false;
			for (int i=0; i<commands.length; i++) {
				if (CLEAN.equalsIgnoreCase(commands[i])) {
					clean = true;
					splash.delete();
					break;
				}
			}
			if (!clean)
				return splash.getAbsolutePath();
		}
		ZipFile file;
		try {
			file = new ZipFile(jarPath);
		} catch (IOException e) {
			log("Exception looking for splash  in JAR file: " + jarPath); //$NON-NLS-1$
			log(e);
			return null;
		}
		ZipEntry entry = file.getEntry(splashPath.replace(File.separatorChar, '/'));
		if (entry == null)
			return null;
		InputStream input = null;
		try {
			input = file.getInputStream(entry);
		} catch (IOException e) {
			log("Exception opening splash: " + entry.getName() + " in JAR file: " + jarPath);  //$NON-NLS-1$ //$NON-NLS-2$
			log(e);
			return null;
		}
		splash.getParentFile().mkdirs();
		OutputStream output;
		try {
			output = new BufferedOutputStream(new FileOutputStream(splash));
		} catch (FileNotFoundException e) {
			try {
				input.close();
			} catch (IOException e1) {
				// ignore
			}
			return null;
		}
		transferStreams(input, output);
		return splash.exists() ? splash.getAbsolutePath() : null;
	}

	/*
	 * Return a boolean value indicating whether or not the given
	 * path represents a JAR file.
	 */
	private boolean isJAR(String path) {
		if (path.endsWith(File.separator))
			return false;
		int index = path.lastIndexOf('.');
		if (index == -1)
			return false;
		index++;
		// handle the case where we have a '.' at the end
		if (index >= path.length()) 
			return false;
		return "JAR".equalsIgnoreCase(path.substring(index)); //$NON-NLS-1$
	}

	/*
	 * Build an array of path suffixes based on the given NL which is suitable
	 * for splash path searching.  The returned array contains paths in order 
	 * from most specific to most generic. So, in the FR_fr locale, it will return 
	 * "nl/fr/FR/splash.bmp", then "nl/fr/splash.bmp", and finally "splash.bmp". 
	 * (we always search the root)
	 */
	private static String[] buildNLVariants(String locale) {
		//build list of suffixes for loading resource bundles
		String nl = locale;
		ArrayList result = new ArrayList(4);
		int lastSeparator;
		while (true) {
			result.add("nl" + File.separatorChar + nl.replace('_', File.separatorChar) + File.separatorChar + SPLASH_IMAGE); //$NON-NLS-1$
			lastSeparator = nl.lastIndexOf('_');
			if (lastSeparator == -1)
				break;
			nl = nl.substring(0, lastSeparator);
		}
		//add the empty suffix last (most general)
		result.add(SPLASH_IMAGE);
		return (String[]) result.toArray(new String[result.size()]);
	}

    /*
     * resolve platform:/base/ URLs
     */
    private String resolve(String urlString) {
        // handle the case where people mistakenly spec a refererence: url.
        if (urlString.startsWith(REFERENCE_SCHEME)) { //$NON-NLS-1$
            urlString = urlString.substring(10);
            System.getProperties().put(PROP_FRAMEWORK, urlString);
        }
        if (urlString.startsWith(PLATFORM_URL)) {
            String path = urlString.substring(PLATFORM_URL.length());
            return getInstallLocation() + path;
        } else
            return urlString;
    }

    /*
     * Entry point for logging.
     */
    private synchronized void log(Object obj) {
        if (obj == null)
            return;
        try {
            openLogFile();
            try {
                if (newSession) {
                    log.write(SESSION);
                    log.write(' ');
                    String timestamp = new Date().toString();
                    log.write(timestamp);
                    log.write(' ');
                    for (int i = SESSION.length() + timestamp.length(); i < 78; i++)
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
            System.err.println("An exception occurred while writing to the platform log:"); //$NON-NLS-1$
            e.printStackTrace(System.err);
            System.err.println("Logging to the console instead."); //$NON-NLS-1$
            //we failed to write, so dump log entry to console instead
            try {
                log = logForStream(System.err);
                write(obj);
                log.flush();
            } catch (Exception e2) {
                System.err.println("An exception occurred while logging to the console:"); //$NON-NLS-1$
                e2.printStackTrace(System.err);
            }
        } finally {
            log = null;
        }
    }

    /*
     * This should only be called from #log()
     */
    private void write(Object obj) throws IOException {
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
                DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SS"); //$NON-NLS-1$
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

    private void computeLogFileLocation() {
        String logFileProp = System.getProperty(PROP_LOGFILE);
        if (logFileProp != null) {
            if (logFile == null || !logFileProp.equals(logFile.getAbsolutePath())) {
                logFile = new File(logFileProp);
                logFile.getParentFile().mkdirs();
            }
            return;
        }

        // compute the base location and then append the name of the log file
        URL base = buildURL(System.getProperty(PROP_CONFIG_AREA), false);
        if (base == null)
            return;
        logFile = new File(base.getPath(), Long.toString(System.currentTimeMillis()) + ".log"); //$NON-NLS-1$
        logFile.getParentFile().mkdirs();
        System.setProperty(PROP_LOGFILE, logFile.getAbsolutePath());
    }

    /**
     * Converts an ASCII character representing a hexadecimal
     * value into its integer equivalent.
     */
    private int hexToByte(byte b) {
        switch (b) {
            case '0' :
                return 0;
            case '1' :
                return 1;
            case '2' :
                return 2;
            case '3' :
                return 3;
            case '4' :
                return 4;
            case '5' :
                return 5;
            case '6' :
                return 6;
            case '7' :
                return 7;
            case '8' :
                return 8;
            case '9' :
                return 9;
            case 'A' :
            case 'a' :
                return 10;
            case 'B' :
            case 'b' :
                return 11;
            case 'C' :
            case 'c' :
                return 12;
            case 'D' :
            case 'd' :
                return 13;
            case 'E' :
            case 'e' :
                return 14;
            case 'F' :
            case 'f' :
                return 15;
            default :
                throw new IllegalArgumentException("Switch error decoding URL"); //$NON-NLS-1$
        }
    }

    private void openLogFile() throws IOException {
        computeLogFileLocation();
        try {
            log = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(logFile.getAbsolutePath(), true), "UTF-8")); //$NON-NLS-1$
        } catch (IOException e) {
            logFile = null;
            throw e;
        }
    }

    private BufferedWriter logForStream(OutputStream output) {
        try {
            return new BufferedWriter(new OutputStreamWriter(output, "UTF-8")); //$NON-NLS-1$
        } catch (UnsupportedEncodingException e) {
            return new BufferedWriter(new OutputStreamWriter(output));
        }
    }

    private void closeLogFile() throws IOException {
        try {
            if (log != null) {
                log.flush();
                log.close();
            }
        } finally {
            log = null;
        }
    }

    private void mergeProperties(Properties destination, Properties source) {
        if (destination == null || source == null)
            return;
        for (Enumeration e = source.keys(); e.hasMoreElements();) {
            String key = (String) e.nextElement();
            if (key.equals(PROP_CLASSPATH)) {
                String destinationClasspath = destination.getProperty(PROP_CLASSPATH);
                String sourceClasspath = source.getProperty(PROP_CLASSPATH);
                if (destinationClasspath == null)
                    destinationClasspath = sourceClasspath;
                else
                    destinationClasspath = destinationClasspath + sourceClasspath;
                destination.put(PROP_CLASSPATH, destinationClasspath);
                continue;
            }
            if (!key.equals(PROP_EOF)) {
                String value = source.getProperty(key);
                if (destination.getProperty(key) == null)
                    destination.put(key, value);
            }
        }
    }

    private void setupVMProperties() {
        if (vm != null)
            System.getProperties().put(PROP_VM, vm);
        setMultiValueProperty(PROP_VMARGS, vmargs);
        setMultiValueProperty(PROP_COMMANDS, commands);
    }

    private void setMultiValueProperty(String property, String[] value) {
        if (value != null) {
            StringBuffer result = new StringBuffer(300);
            for (int i = 0; i < value.length; i++) {
                if (value[i] != null) {
                    result.append(value[i]);
                    result.append('\n');
                }
            }
            System.getProperties().put(property, result.toString());
        }
    }

    private class EclipsePolicy extends Policy {
        // The policy that this EclipsePolicy is replacing
        private Policy policy;

        // The set of URLs to give AllPermissions to; this is the set of bootURLs
        private URL[] urls;

        // The AllPermissions collection
        private PermissionCollection allPermissions;

        // The AllPermission permission
        private Permission allPermission = new AllPermission();

        EclipsePolicy(Policy policy, URL[] urls) {
            this.policy = policy;
            this.urls = urls;
            allPermissions = new PermissionCollection() {
                private static final long serialVersionUID = 3258131349494708277L;

                // A simple PermissionCollection that only has AllPermission
                public void add(Permission permission) {
                }

                public boolean implies(Permission permission) {
                    return true;
                }

                public Enumeration elements() {
                    return new Enumeration() {
                        int cur = 0;

                        public boolean hasMoreElements() {
                            return cur < 1;
                        }

                        public Object nextElement() {
                            if (cur == 0) {
                                cur = 1;
                                return allPermission;
                            } else
                                throw new NoSuchElementException();
                        }
                    };
                }
            };
        }

        public PermissionCollection getPermissions(CodeSource codesource) {
            if (contains(codesource.getLocation()))
                return allPermissions;
            return policy == null ? allPermissions : policy.getPermissions(codesource);
        }

        public PermissionCollection getPermissions(ProtectionDomain domain) {
            if (contains(domain.getCodeSource().getLocation()))
                return allPermissions;
            return policy == null ? allPermissions : policy.getPermissions(domain);
        }

        public boolean implies(ProtectionDomain domain, Permission permission) {
            if (contains(domain.getCodeSource().getLocation()))
                return true;
            return policy == null ? true : policy.implies(domain, permission);
        }

        public void refresh() {
            if (policy != null)
                policy.refresh();
        }

        private boolean contains(URL url) {
            // Check to see if this URL is in our set of URLs to give AllPermissions to.
            for (int i = 0; i < urls.length; i++) {
                // We do simple equals test here because we assume the URLs will be the same objects.
                if (urls[i] == url)
                    return true;
            }
            return false;
        }
    }

    private class StartupClassLoader extends URLClassLoader {

        public StartupClassLoader(URL[] urls) {
            super(urls);
        }

        public StartupClassLoader(URL[] urls, ClassLoader parent) {
            super(urls, parent);
        }

        public StartupClassLoader(URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory) {
            super(urls, parent, factory);
        }

        protected String findLibrary(String name) {
            if (extensionPaths ==  null)
                return super.findLibrary(name);
            String libName = System.mapLibraryName(name);
            for (int i = 0; i < extensionPaths.length; i++) {
                File libFile = new File(extensionPaths[i], libName);
                if (libFile.isFile())
                    return libFile.getAbsolutePath();
            }
            return super.findLibrary(name);
        }       
    }
}
