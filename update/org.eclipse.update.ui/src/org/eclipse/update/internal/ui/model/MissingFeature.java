package org.eclipse.update.internal.ui.model;

import java.io.InputStream;
import java.net.URL;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.update.configuration.ILocalSite;
import org.eclipse.update.core.IFeature;
import org.eclipse.update.core.IFeatureContentConsumer;
import org.eclipse.update.core.IFeatureContentProvider;
import org.eclipse.update.core.IFeatureReference;
import org.eclipse.update.core.IImport;
import org.eclipse.update.core.IInstallHandlerEntry;
import org.eclipse.update.core.INonPluginEntry;
import org.eclipse.update.core.IPluginEntry;
import org.eclipse.update.core.ISite;
import org.eclipse.update.core.IURLEntry;
import org.eclipse.update.core.IVerificationListener;
import org.eclipse.update.core.SiteManager;
import org.eclipse.update.core.VersionedIdentifier;
import org.eclipse.update.core.model.InstallAbortedException;
import org.eclipse.update.internal.ui.UpdateUIPlugin;

public class MissingFeature implements IFeature {

	private URL url;
	private ISite site;
	private IFeatureReference reference;
	private IFeature parent;
	private IURLEntry desc;
	private VersionedIdentifier id = new VersionedIdentifier(UpdateUIPlugin.getResourceString("MissingFeature.id"), "0.0.0"); //$NON-NLS-1$ //$NON-NLS-2$
	public MissingFeature(ISite site, URL url) {
		this.site = site;
		this.url = url;
		desc = new IURLEntry() {
			public URL getURL() {
				return null;
			}
			public String getAnnotation() {
				return UpdateUIPlugin.getResourceString("MissingFeature.desc.unknown"); //$NON-NLS-1$
			}
			public Object getAdapter(Class key) {
				return null;
			}
		};
	}
	public MissingFeature(IFeatureReference ref) {
		this(null, ref);
	}
	
	public MissingFeature(IFeature parent, IFeatureReference ref) {
		this(ref.getSite(), ref.getURL());
		this.reference = ref;
		this.parent = parent;
		
		if (ref.isOptional()) {
			desc = new IURLEntry() {
				public URL getURL() {
					return null;
				}
				public String getAnnotation() {
					return UpdateUIPlugin.getResourceString("MissingFeature.desc.optional");  //$NON-NLS-1$
				}
				public Object getAdapter(Class key) {
					return null;
				}
			};
		}
	}
	
	public boolean isOptional() {
		return reference!=null && reference.isOptional();
	}
	
	public IFeature getParent() {
		return parent;
	}
	
	public URL getOriginatingSiteURL() {
		VersionedIdentifier vid = getVersionedIdentifier();
		if (vid==null) return null;
		String key = vid.getIdentifier();
		return UpdateUIPlugin.getOriginatingURL(key);
	}

	/*
	 * @see IFeature#getIdentifier()
	 */
	public VersionedIdentifier getVersionedIdentifier() {
		if (reference!=null) {
			try {
				return reference.getVersionedIdentifier();
			}
			catch (CoreException e) {
			}
		}
		return id;
	}

	/*
	 * @see IFeature#getSite()
	 */
	public ISite getSite() {
		return site;
	}

	/*
	 * @see IFeature#getLabel()
	 */
	public String getLabel() {
		if (reference!=null) {
			String name = reference.getName();
			if (name!=null) return name;
		}
		return url.toString();
	}

	/*
	 * @see IFeature#getURL()
	 */
	public URL getURL() {
		return url;
	}

	/*
	 * @see IFeature#getUpdateInfo()
	 */
	public IURLEntry getUpdateSiteEntry() {
		return null;
	}

	/*
	 * @see IFeature#getDiscoveryInfos()
	 */
	public IURLEntry[] getDiscoverySiteEntries() {
		return null;
	}

	/*
	 * @see IFeature#getProvider()
	 */
	public String getProvider() {
		return UpdateUIPlugin.getResourceString("MissingFeature.provider"); //$NON-NLS-1$
	}

	/*
	 * @see IFeature#getDescription()
	 */
	public IURLEntry getDescription() {
		return desc;
	}

	/*
	 * @see IFeature#getCopyright()
	 */
	public IURLEntry getCopyright() {
		return null;
	}

	/*
	 * @see IFeature#getLicense()
	 */
	public IURLEntry getLicense() {
		return null;
	}

	/*
	 * @see IFeature#getOS()
	 */
	public String getOS() {
		return null;
	}

	/*
	 * @see IFeature#getWS()
	 */
	public String getWS() {
		return null;
	}

	/*
	 * @see IFeature#getNL()
	 */
	public String getNL() {
		return null;
	}

	/*
	 * @see IFeature#getArch()
	 */
	public String getArch() {
		return null;
	}

	/*
	 * @see IFeature#getImage()
	 */
	public URL getImage() {
		return null;
	}

	/*
	 * @see IFeature#getImports()
	 */
	public IImport[] getImports() {
		return null;
	}


	/*
	 * @see IFeature#getArchives()
	 */
	public String[] getArchives() {
		return null;
	}

	/*
	 * @see IFeature#getDataEntries()
	 */
	public INonPluginEntry[] getNonPluginEntries() {
		return null;
	}

	/*
	 * @see IFeature#addDataEntry(IDataEntry)
	 */
	public void addNonPluginEntry(INonPluginEntry dataEntry) {
	}

	/*
	 * @see IFeature#getDownloadSize()
	 */
	public long getDownloadSize() {
		return 0;
	}

	/*
	 * @see IFeature#getInstallSize(ISite)
	 */
	public long getInstallSize() {
		return 0;
	}

	/*
	 * @see IFeature#isPrimary()
	 */
	public boolean isPrimary() {
		return false;
	}

	/*
	 * @see IFeature#getApplication()
	 */
	public String getApplication() {
		return null;
	}

	/*
	 * @see IPluginContainer#getPluginEntries()
	 */
	public IPluginEntry[] getPluginEntries() {
		return new IPluginEntry[0];
	}

	/*
	 * @see IPluginContainer#getPluginEntryCount()
	 */
	public int getPluginEntryCount() {
		return 0;
	}

	/*
	 * @see IPluginContainer#getDownloadSize(IPluginEntry)
	 */
	public long getDownloadSize(IPluginEntry entry) {
		return 0;
	}

	/*
	 * @see IPluginContainer#getInstallSize(IPluginEntry)
	 */
	public long getInstallSize(IPluginEntry entry) {
		return 0;
	}

	/*
	 * @see IPluginContainer#addPluginEntry(IPluginEntry)
	 */
	public void addPluginEntry(IPluginEntry pluginEntry) {
	}

	/*
	 * @see IPluginContainer#store(IPluginEntry, String, InputStream)
	 */
	public void store(IPluginEntry entry, String name, InputStream inStream)
		throws CoreException {
	}

	/*
	 * @see IAdaptable#getAdapter(Class)
	 */
	public Object getAdapter(Class adapter) {
		return null;
	}

	/*
	 * @see IPluginContainer#remove(IPluginEntry)
	 */
	public void remove(IPluginEntry entry) throws CoreException {
	}

	/*
	 * @see IFeature#setFeatureContentProvider(IFeatureContentProvider)
	 */
	public void setFeatureContentProvider(IFeatureContentProvider featureContentProvider) {
	}

	/*
	 * @see IFeature#getFeatureContentConsumer()
	 */
	public IFeatureContentConsumer getFeatureContentConsumer() throws CoreException {
		return null;
	}
	
	/*
	 * @see IFeature#setSite(ISite)
	 */
	public void setSite(ISite site) throws CoreException {
		this.site = site;
	}

	/*
	 * @see IFeature#getFeatureContentProvider()
	 */
	public IFeatureContentProvider getFeatureContentProvider() throws CoreException {
		return null;
	}

	/*
	 * @see IFeature#install(IFeature,IVerifier, IProgressMonitor)
	 */
	public IFeatureReference install(IFeature targetFeature, IVerificationListener verificationListener, IProgressMonitor monitor) throws CoreException {
		return null;
	}
	
	/*
	 * @see org.eclipse.update.core.IFeature#install(IFeature, IFeatureReference[], IVerificationListener, IProgressMonitor)
	 */
	public IFeatureReference install(IFeature targetFeature, IFeatureReference[] optionalFeatures, IVerificationListener verificationListener, IProgressMonitor monitor) throws InstallAbortedException, CoreException {
		return null;
	}
	/*
	 * @see IFeature#remove(IProgressMonitor)
	 */
	public void remove(IProgressMonitor monitor) throws CoreException {
	}

	/*
	 * @see IPluginContainer#remove(IPluginEntry, IProgressMonitor)
	 */
	public void remove(IPluginEntry entry, IProgressMonitor monitor) throws CoreException {
	}

	/*
	 * @see IFeature#getNonPluginEntryCount()
	 */
	public int getNonPluginEntryCount() {
		return 0;
	}

	/*
	 * @see IFeature#getInstallHandlerEntry()
	 */
	public IInstallHandlerEntry getInstallHandlerEntry() {
		return null;
	}
	/*
	 * @see IFeature#getIncludedFeatureReferences()
	 */
	public IFeatureReference[] getIncludedFeatureReferences()
		throws CoreException {
		return new IFeatureReference[0];
	}

	/**
	 * @see IFeature#getAffinityFeature()
	 */
	public String getAffinityFeature() {
		return null;
	}
}

