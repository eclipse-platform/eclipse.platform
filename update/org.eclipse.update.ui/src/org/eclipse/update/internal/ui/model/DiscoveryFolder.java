package org.eclipse.update.internal.ui.model;

import org.eclipse.update.internal.ui.UpdateUIPlugin;
import org.eclipse.update.core.*;
import org.eclipse.update.configuration.*;
import org.eclipse.core.runtime.CoreException;

public class DiscoveryFolder extends BookmarkFolder {
	public DiscoveryFolder() {
		super(UpdateUIPlugin.getResourceString("DiscoveryFolder"));
		setModel(UpdateUIPlugin.getDefault().getUpdateModel());
	}
	public void initialize() {
		children.clear();
		try {
			ILocalSite site = SiteManager.getLocalSite();
			IInstallConfiguration config = site.getCurrentConfiguration();
			IConfiguredSite[] csites = config.getConfiguredSites();
			for (int i = 0; i < csites.length; i++) {
				IConfiguredSite csite = csites[i];
				IFeatureReference[] refs = csite.getConfiguredFeatures();
				for (int j = 0; j < refs.length; j++) {
					IFeatureReference ref = refs[j];
					IFeature feature = ref.getFeature();
					IURLEntry[] entries = feature.getDiscoverySiteEntries();
					if (entries.length > 0) {
						// Only add discovery sites if
						// of root features
						if (!isIncluded(ref, refs))
							addBookmarks(entries);
					}
				}
			}
		} catch (CoreException e) {
			UpdateUIPlugin.logException(e);
		}
	}
	private boolean isIncluded(
		IFeatureReference ref,
		IFeatureReference[] refs) {
		try {
			VersionedIdentifier vid = ref.getVersionedIdentifier();
			for (int i = 0; i < refs.length; i++) {
				IFeatureReference candidate = refs[i];
				// Ignore self
				if (candidate.equals(ref))
					continue;
				IFeature cfeature = candidate.getFeature();
				IFeatureReference[] irefs =
					cfeature.getIncludedFeatureReferences();
				for (int j = 0; j < irefs.length; j++) {
					IFeatureReference iref = irefs[j];
					VersionedIdentifier ivid = iref.getVersionedIdentifier();
					if (ivid.equals(vid)) {
						// bingo - included in at least one feature
						return true;
					}
				}
			}
		} catch (CoreException e) {
		}
		return false;
	}
	private void addBookmarks(IURLEntry[] entries) {
		for (int i = 0; i < entries.length; i++) {
			IURLEntry entry = entries[i];
			SiteBookmark bookmark =
				new SiteBookmark(entry.getAnnotation(), entry.getURL());
			if (!contains(bookmark))
				internalAdd(bookmark);
		}
	}
	private boolean contains(SiteBookmark bookmark) {
		for (int i = 0; i < children.size(); i++) {
			Object o = children.get(i);
			if (o instanceof SiteBookmark) {
				// note: match on URL, not the label
				if (bookmark.getURL().equals(((SiteBookmark) o).getURL()))
					return true;
			}
		}
		return false;
	}
	public Object[] getChildren(Object parent) {
		if (hasChildren() == false)
			initialize();
		return super.getChildren(parent);
	}
}