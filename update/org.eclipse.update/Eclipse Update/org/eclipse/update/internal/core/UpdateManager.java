package org.eclipse.update.internal.core;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.TreeSet;

import org.eclipse.core.internal.boot.LaunchInfo;
import org.eclipse.core.internal.boot.LaunchInfo.VersionedIdentifier;
import org.eclipse.core.internal.boot.update.IComponentDescriptor;
import org.eclipse.core.internal.boot.update.IComponentEntryDescriptor;
import org.eclipse.core.internal.boot.update.IFragmentEntryDescriptor;
import org.eclipse.core.internal.boot.update.IInstallable;
import org.eclipse.core.internal.boot.update.IPluginEntryDescriptor;
import org.eclipse.core.internal.boot.update.IProductDescriptor;
import org.eclipse.core.internal.boot.update.IUMRegistry;
import org.eclipse.core.internal.boot.update.IURLNamePair;
import org.eclipse.core.internal.boot.update.LogStoreException;
import org.eclipse.core.internal.boot.update.UMEclipseTree;
import org.eclipse.core.internal.boot.update.UMRegistryManager;
import org.eclipse.core.internal.boot.update.UpdateManagerConstants;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.update.internal.ui.JarVerificationService;

/**
 * Update Manager updates a workstation in one of three modes:
 * <ol>
 * <li>local</li>
 * <li>remote</li>
 * <li>remote cached</li>
 * </ol>
 * Usage:
 * <ol>
 * <li>Create an instance of this class</li>
 * <li>Set persistent properties as required</li>
 * <ul>
 * <li>mode</li>
 * <li>update log URL</li>
 * <li>history log URL</li>
 * </ul>
 * <li>Call the initialize function</li>
 * <li>Call the update function</li>
 * </ol>
 * Persistent properties are loaded before any are changed, and saved after any
 * are changed.
 */

public class UpdateManager {

	// Package name for this class
	// Used by UpdateManagerStrings to obtain its resource bundle properties file
	//---------------------------------------------------------------------------
	public static final String _strPackageName = "org.eclipse.update.internal.core";
	public static UpdateManager _updateManagerInstance = null;
	
	// Persistent properties
	//----------------------
	protected int    _iMode         = UpdateManagerConstants.MODE_LOCAL;
	protected String _strUpdateLogURL  = null;
	protected String _strHistoryLogURL = null;

	// Instance state data
	//--------------------
	protected UMRegistryManager _registryManager = null;
	protected UMSessionManager  _sessionManager  = null;
	protected Shell             _shell           = null;
	
	protected JarVerificationService _jarVerifier = null;
/**
 * Constructs an instance of this class, then initializes by loading its persistent properties
 */
public UpdateManager() {
}
/**
 * Constructs an instance of this class, then initializes by loading its persistent properties
 */
public UpdateManager( Shell shell ) {
	_updateManagerInstance = this;
	_shell = shell;
}
/**
 * Deletes all files in the staging area.
 */
public void cleanup() {
	String strStagingDirectory = UMEclipseTree.getFileInPlatformString(UMEclipseTree.getStagingArea());

	File fileStagingDirectory = new File(strStagingDirectory);

	if (fileStagingDirectory.exists() == true) {
		File[] files = fileStagingDirectory.listFiles();
		for (int i = 0; i < files.length; ++i) {
			if (files[i].isDirectory() == true) {
				cleanupDirectory(files[i]);
			}
			boolean bSuccessful = files[i].delete();
			
		}
	}
}
/**
 */
public static void cleanupDirectory(File fileDirectory) {

	File[] files = fileDirectory.listFiles();
	
	for (int i = 0; i < files.length; ++i) {
		if (files[i].isDirectory() == true) {
			cleanupDirectory(files[i]);
		}

		boolean bSuccessful = files[i].delete();
//		if (!bSuccessful)
//			System.out.println("ooops!" + files[i]);
	}
}
/**
 */
public UMSessionManagerSession createSession(IInstallable[] descriptors, boolean bVerifyJars) throws UpdateManagerException {

	// WARNING! TEMPORARY OVERRIDE - DO NOT DO JAR VERIFICATION
	//---------------------------------------------------------
	bVerifyJars = false;

	// Create a new update session
	//----------------------------
	UMSessionManagerSession session = _sessionManager.createUpdateSession();

	// Create a definer that can define the operations required
	//---------------------------------------------------------
	ISessionDefiner sessionDefiner = new UMSessionDefinerReferenceUpdate();
	
	// Define a set of operations
	//---------------------------
	sessionDefiner.defineOperations(session, descriptors, bVerifyJars);

	// Save the update log
	//--------------------
	try {
		_sessionManager.saveUpdateLog();
	}
	catch (LogStoreException ex) {
		throw new UpdateManagerException(ex.getMessage());
	}

	return session;
}
/**
 * Updates the local machine by:
 * <ol>
 * <li>defining the operations</li>
 * <li>downloading any new objects</li>
 * <li>applying any downloaded updates</li>
 * <li>displaying a progress monitor</li>
 * </ol>
 */
public UMSessionManagerSession executeSession( UMSessionManagerSession session, IProgressMonitor progressMonitor ) throws UpdateManagerException {

	// Throw any exception when complete
	//----------------------------------
	UpdateManagerException umException = null;
	
	// Execute the session
	//--------------------
	_sessionManager.executeSession( session, progressMonitor );

	// Move successful sessions from update log to history log
	//--------------------------------------------------------
	try {
		_sessionManager.updateAndSaveLogs();
	}
	catch (LogStoreException ex) {
		if (umException != null) {
			umException = new UpdateManagerException("S_Unable_to_save_update_logs");
		}
	}

	// Throw any exceptions found
	//---------------------------
	if (umException != null) {
		throw umException;
	}

	return session;
}
/**
 * Updates the local machine by:
 * <ol>
 * <li>defining the operations</li>
 * <li>downloading any new objects</li>
 * <li>applying any downloaded updates</li>
 * <li>displaying a progress monitor</li>
 * </ol>
 */
public UMSessionManagerSession executeSessionUndo( UMSessionManagerSession session, IProgressMonitor progressMonitor ) throws UpdateManagerException {

	// Throw any exception when complete
	//----------------------------------
	UpdateManagerException umException = null;
	
	// Execute the session
	//--------------------
	_sessionManager.executeSessionUndo( session, progressMonitor );

	// Move successful sessions from update log to history log
	//--------------------------------------------------------
	try {
		_sessionManager.updateAndSaveLogs();
	}
	catch (LogStoreException ex) {
		if (umException != null) {
			umException = new UpdateManagerException("S Unable to save update logs");
		}
	}

	// Throw any exceptions found
	//---------------------------
	if (umException != null) {
		throw umException;
	}

	return session;
}
/**
 */
public static UpdateManager getCurrentInstance() {
	return _updateManagerInstance;
}
/**
 * Takes a string with an Id + '_' + Version, and returns the Id.
 */
private String getIdFromInfoString(String strInfo) {

	String strId = null;

	int iIndex = strInfo.lastIndexOf('_');
	
	if (iIndex >= 0) {
		strId = strInfo.substring(0, iIndex);
	}

	else {
		strId = strInfo;
	}

	return strId;
}
// return the jar verifier

public JarVerificationService getJarVerifier() {
	if (_jarVerifier == null)	
		_jarVerifier = new JarVerificationService( _shell );
	return _jarVerifier;
}
/**
 * Returns a list of components that require updating by:
 *<ol>
 *<li>Loading all product/component manifests from .install/tree </li>
 *<li>Determining which components require updating by:</li>
 *<ol>
 *<li>Collecting update URLs from each component</li>
 *<li>Collecting update URLs from products that components are a part of</li>
 *</ol>
 *<li>Determining what updates are available by:</li>
 *<ol>
 *<li>Accessing the update URL</li>
 *<li>Determining if the component is a newer version of the existing one</li>
 *</ol>
 *<li>
 *</ol>
 */
public IURLNamePair[] getLocalDiscoveryURLs() {

	IURLNamePair[] urlNPs = null;
	
	// Obtain the local registry
	//--------------------------
	IUMRegistry registry = _registryManager.getLocalRegistry();

	// Create a list of all URLs
	//--------------------------
	TreeSet setURLs = new TreeSet( new UpdateManagerURLComparator() );

	// Obtain a list of all installed components
	//------------------------------------------
	IComponentDescriptor[] componentDescriptors = registry.getComponentDescriptors();

	// Obtain a list of discovery URLs
	//--------------------------------
	for (int i = 0; i < componentDescriptors.length; ++i) {
		urlNPs = componentDescriptors[i].getDiscoveryURLs();

		for (int j = 0; j < urlNPs.length; ++j) {
			setURLs.add(urlNPs[j]);
		}
	}

	// Obtain a list of all installed products
	//----------------------------------------
	IProductDescriptor[] productDescriptors = registry.getProductDescriptors();

	// Obtain a list of discovery URLs
	//--------------------------------
	for (int i = 0; i < productDescriptors.length; ++i) {
		urlNPs = productDescriptors[i].getDiscoveryURLs();

		for (int j = 0; j < urlNPs.length; ++j) {
			setURLs.add(urlNPs[j]);
		}
	}

	urlNPs = new IURLNamePair[ setURLs.size() ];
	System.arraycopy(setURLs.toArray(), 0, urlNPs, 0, setURLs.size() );
	
	return urlNPs;
}
/**
 * Returns a list of components that require updating by:
 *<ol>
 *<li>Loading all product/component manifests from .install/tree </li>
 *<li>Determining which components require updating by:</li>
 *<ol>
 *<li>Collecting update URLs from each component</li>
 *<li>Collecting update URLs from products that components are a part of</li>
 *</ol>
 *<li>Determining what updates are available by:</li>
 *<ol>
 *<li>Accessing the update URL</li>
 *<li>Determining if the component is a newer version of the existing one</li>
 *</ol>
 *<li>
 *</ol>
 */
public IURLNamePair[] getLocalUpdateURLs() {

	IURLNamePair[] urlNPs = null;
	
	// Obtain the local registry
	//--------------------------
	IUMRegistry registry = _registryManager.getLocalRegistry();

	// Create a list of all URLs
	//--------------------------
	TreeSet setURLs = new TreeSet( new UpdateManagerURLComparator() );

	// Obtain a list of all installed components
	//------------------------------------------
	IComponentDescriptor[] componentDescriptors = registry.getComponentDescriptors();

	// Obtain a list of discovery URLs
	//--------------------------------
	for (int i = 0; i < componentDescriptors.length; ++i) {
		urlNPs = componentDescriptors[i].getUpdateURLs();

		for (int j = 0; j < urlNPs.length; ++j) {
			setURLs.add(urlNPs[j]);
		}
	}

	// Obtain a list of all installed products
	//----------------------------------------
	IProductDescriptor[] productDescriptors = registry.getProductDescriptors();

	// Obtain a list of discovery URLs
	//--------------------------------
	for (int i = 0; i < productDescriptors.length; ++i) {
		urlNPs = productDescriptors[i].getUpdateURLs();

		for (int j = 0; j < urlNPs.length; ++j) {
			setURLs.add(urlNPs[j]);
		}
	}

	urlNPs = new IURLNamePair[ setURLs.size() ];
	System.arraycopy(setURLs.toArray(), 0, urlNPs, 0, setURLs.size() );
	
	return urlNPs;
}
// return the local (current) registry

public IUMRegistry getRegistryAt(URL url) {
	
	return _registryManager.getRegistryAt( url );
}
/**
 */
public UMRegistryManager getRegistryManager() {
	return _registryManager;
}
/**
 * Initializes the update and history log URLs,creates a session manager,
 * and creates a registry manager.
 */
public void initialize() throws UpdateManagerException{

	// Obtain install URL
	//-------------------
	URL urlBase = UMEclipseTree.getBaseInstallURL();
	String strUrlBase = urlBase.toExternalForm();
	String strUrlInstall = strUrlBase + "install/";

	String strUrlUpdateLog = strUrlInstall + "update.log";
	String strUrlHistoryLog = strUrlInstall + "history.log";

	setUpdateLogURL(strUrlUpdateLog);
	setHistoryLogURL(strUrlHistoryLog);

	// Create a session manager
	//-------------------------
	try {
		_sessionManager = new UMSessionManager(new URL(_strUpdateLogURL), new URL(_strHistoryLogURL), true);
	}
	catch (MalformedURLException ex) {
		throw new UpdateManagerException("Invalid log URL specification");
	}
	catch (LogStoreException ex) {
		throw new UpdateManagerException(ex.getMessage());
	}

	// Registry Manager
	//-----------------
	_registryManager = new UMRegistryManager(urlBase);
}
/**
 * 
 */
public void removeComponent(IComponentDescriptor componentDescriptor) {
	removeComponent(componentDescriptor, null);
}
/**
 * 
 */
public void removeComponent(IComponentDescriptor componentDescriptor, IProductDescriptor productDescriptor) {

	if( componentDescriptor == null )
		return;
		
	LaunchInfo launchInfo = LaunchInfo.getCurrent();
	LaunchInfo.VersionedIdentifier vid = null;

	// Plugins
	//--------
	IPluginEntryDescriptor[] pluginEntries = componentDescriptor.getPluginEntries();

	for (int i = 0; i < pluginEntries.length; ++i) {
		vid = new LaunchInfo.VersionedIdentifier(pluginEntries[i].getUniqueIdentifier(), pluginEntries[i].getVersionStr());
		launchInfo.removePlugin(vid);
	}

	// Fragments
	//----------
	IFragmentEntryDescriptor[] fragmentEntries = componentDescriptor.getFragmentEntries();

	for (int i = 0; i < fragmentEntries.length; ++i) {
		vid = new LaunchInfo.VersionedIdentifier(fragmentEntries[i].getUniqueIdentifier(), pluginEntries[i].getVersionStr());
		launchInfo.removeFragment(vid);
	}

	// Component
	//----------
	vid = new LaunchInfo.VersionedIdentifier(componentDescriptor.getUniqueIdentifier(), componentDescriptor.getVersionStr());
	launchInfo.removeComponent(vid);
	_registryManager.removeComponentDescriptorFromLocal(componentDescriptor, productDescriptor);
	
	return;
}
/**
 * 
 */
public void removeProduct(IProductDescriptor productDescriptor) {

	if( productDescriptor == null )
		return;
		
	// Components
	//-----------
	IComponentEntryDescriptor[] componentEntries = productDescriptor.getComponentEntries();

	for (int i = 0; i < componentEntries.length; ++i) {
		IComponentDescriptor componentDescriptor = componentEntries[i].getComponentDescriptor();
		if (componentDescriptor.isRemovable(productDescriptor))
			removeComponent(componentDescriptor, productDescriptor);
	}

	// Product
	//--------
	LaunchInfo.VersionedIdentifier vid = new LaunchInfo.VersionedIdentifier(productDescriptor.getUniqueIdentifier(), productDescriptor.getVersionStr());
	LaunchInfo.getCurrent().removeConfiguration(vid);
	_registryManager.removeProductDescriptorFromLocal(productDescriptor);
}
/**
 * Sets the URL for the history log.  This property is persistent.
 */
public void setHistoryLogURL(String strURL) throws UpdateManagerException {

	// Check for valid URL
	//--------------------
	try {
		new URL(strURL);
	}
	catch (MalformedURLException ex) {
		throw new UpdateManagerException("Invalid log URL specification");
	}

	// Change the property
	//--------------------
	_strHistoryLogURL = strURL;
}
/**
 * Sets the URL for the update log.  This property is persistent.
 */
public void setUpdateLogURL(String strURL) throws UpdateManagerException {
	
	// Check for valid URL
	//--------------------
	try {
		new URL(strURL);
	}
	catch (MalformedURLException ex) {
		throw new UpdateManagerException("Invalid log URL specification");
	}

	// Change the property
	//--------------------
	_strUpdateLogURL = strURL;
}
/**
 * Updates the boot loader's launch information with what is currently installed.
 */
public void updateLaunchInfoAndRegistry(UMSessionManagerSession session) {

	// Update launch info even if this session had a failure
	// One or more parcels could have succeeded
	//------------------------------------------------------
	if (session != null) {
		
		LaunchInfo launchInfo = LaunchInfo.getCurrent();

		// Obtain product/component information
		//-------------------------------------
		UMSessionManagerParcel[] parcels = session.getParcels();

		for (int i = 0; i < parcels.length; ++i) {
			updateLaunchInfoAndRegistryParcel( parcels[i], launchInfo );
		}
	}
}
/**
 * Recursively updates the boot loader's information for this parcel and sub-parcels.
 */
protected void updateLaunchInfoAndRegistryParcel(UMSessionManagerParcel parcel, LaunchInfo launchInfo) {

	// Update the profile only if the install was successful
	//------------------------------------------------------
	if (parcel != null && parcel.getStatus().equals(UpdateManagerConstants.STATUS_SUCCEEDED) == true) {

		Object objData = parcel.getData();

		// Product
		//--------
		if (objData instanceof IProductDescriptor) {
			updateLaunchInfoProduct((IProductDescriptor) objData, launchInfo);
			_registryManager.addProductDescriptorToLocal((IProductDescriptor) objData);
		}

		// Component
		//----------
		else if (objData instanceof IComponentDescriptor) {
			updateLaunchInfoComponent((IComponentDescriptor) objData, launchInfo);
			_registryManager.addComponentDescriptorToLocal((IComponentDescriptor) objData, true);
		}
			
		// Component Entry
		//----------------
		else if (objData instanceof IComponentEntryDescriptor) {
			IComponentDescriptor comp = ((IComponentEntryDescriptor)objData).getComponentDescriptor();
			updateLaunchInfoComponent(comp, launchInfo);
			_registryManager.addComponentDescriptorToLocal(comp, false);
		}

		// Do child parcels
		//-----------------
		UMSessionManagerParcel[] parcelChildren = parcel.getParcels();

		for (int i = 0; i < parcelChildren.length; ++i) {
			updateLaunchInfoAndRegistryParcel(parcelChildren[i], launchInfo);
		}
	}
}
/**
 * Updates the boot loader's launch information with what is currently installed.
 */
public void updateLaunchInfoComponent(IComponentDescriptor descriptor, LaunchInfo launchInfo) {

	// Component
	//----------
	launchInfo.setComponent(new LaunchInfo.VersionedIdentifier(descriptor.getUniqueIdentifier(), descriptor.getVersionStr()));

	// Plugins
	//--------
	IPluginEntryDescriptor[] pluginDescriptors = descriptor.getPluginEntries();

	for (int i = 0; i < pluginDescriptors.length; ++i) {
		updateLaunchInfoPlugin(pluginDescriptors[i], launchInfo);
	}

	// Fragments
	//----------
	IFragmentEntryDescriptor[] fragmentDescriptors = descriptor.getFragmentEntries();

	for (int i = 0; i < fragmentDescriptors.length; ++i) {
		updateLaunchInfoFragment(fragmentDescriptors[i], launchInfo);
	}
}
/**
 * Updates the boot loader's launch information with what is currently installed.
 */
public void updateLaunchInfoFragment(IFragmentEntryDescriptor descriptor, LaunchInfo launchInfo) {

	launchInfo.setFragment(new LaunchInfo.VersionedIdentifier(descriptor.getUniqueIdentifier(),descriptor.getVersionStr()));
}
/**
 * Updates the boot loader's launch information with what is currently installed.
 */
public void updateLaunchInfoPlugin(IPluginEntryDescriptor descriptor, LaunchInfo launchInfo) {

	launchInfo.setPlugin(new LaunchInfo.VersionedIdentifier(descriptor.getUniqueIdentifier(),descriptor.getVersionStr()));
}
/**
 * Updates the boot loader's launch information with what is currently installed.
 */
public void updateLaunchInfoProduct(IProductDescriptor descriptor, LaunchInfo launchInfo) {

	launchInfo.setConfiguration(new LaunchInfo.VersionedIdentifier(descriptor.getUniqueIdentifier(),descriptor.getVersionStr()), descriptor.getApplication());
}
}
