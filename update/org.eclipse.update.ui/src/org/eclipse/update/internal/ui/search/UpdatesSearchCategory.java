package org.eclipse.update.internal.ui.search;

import java.net.URL;
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.update.configuration.*;
import org.eclipse.update.core.*;
import org.eclipse.update.internal.ui.UpdateUIPlugin;
import org.eclipse.update.internal.ui.forms.ActivityConstraints;
import org.eclipse.update.internal.ui.model.*;
import org.eclipse.update.internal.ui.parts.*;
import org.eclipse.update.internal.ui.preferences.MainPreferencePage;
import org.eclipse.update.ui.forms.internal.FormWidgetFactory;

public class UpdatesSearchCategory extends SearchCategory {
	private static final String KEY_CURRENT_SEARCH =
		"UpdatesSearchCategory.currentSearch";
	private CheckboxTableViewer tableViewer;

	class FeatureContentProvider
		extends DefaultContentProvider
		implements IStructuredContentProvider {
		public Object[] getElements(Object parent) {
			if (candidates == null)
				return new Object[0];
			return candidates.toArray();
		}
	}

	class FeatureLabelProvider extends LabelProvider {
		public String getText(Object obj) {
			if (obj instanceof IFeature) {
				IFeature feature = (IFeature) obj;
				return feature.getVersionedIdentifier().toString();
			}
			return obj.toString();
		}
	}

	class Hit {
		IFeature candidate;
		IFeatureReference ref;
		public Hit(IFeature candidate, IFeatureReference ref) {
			this.candidate = candidate;
			this.ref = ref;
		}

		public PendingChange getJob() {
			try {
				IFeature feature = ref.getFeature();
				return new PendingChange(candidate, feature);
			} catch (CoreException e) {
				return null;
			}
		}
	}

	class SiteAdapter implements ISiteAdapter {
		IURLEntry entry;
		SiteAdapter(IURLEntry entry) {
			this.entry = entry;
		}
		public URL getURL() {
			return entry.getURL();
		}
		public String getLabel() {
			String label = entry.getAnnotation();
			if (label == null || label.length() == 0)
				label = getURL().toString();
			return label;
		}
		public ISite getSite() {
			try {
				return SiteManager.getSite(getURL());
			} catch (CoreException e) {
				return null;
			}
		}
	}

	class HitSorter extends Sorter {
		public boolean compare(Object left, Object right) {
			Hit hit1 = (Hit) left;
			Hit hit2 = (Hit) right;
			try {
				VersionedIdentifier hv1 = hit1.ref.getVersionedIdentifier();
				VersionedIdentifier hv2 = hit2.ref.getVersionedIdentifier();
				return isNewerVersion(hv2, hv1);
			} catch (CoreException e) {
				return false;
			}
		}
	}

	class UpdateQuery implements ISearchQuery {
		IFeature candidate;
		ISiteAdapter adapter;

		public UpdateQuery(IFeature candidate) {
			this.candidate = candidate;
			IURLEntry entry = candidate.getUpdateSiteEntry();
			if (entry != null && entry.getURL() != null)
				adapter = new SiteAdapter(entry);
		}
		public ISiteAdapter getSearchSite() {
			return adapter;
		}
		private boolean isBroken() {
			try {
				IStatus status =
					SiteManager.getLocalSite().getFeatureStatus(candidate);
				return status.getSeverity() == IStatus.ERROR;
			} catch (CoreException e) {
				return false;
			}
		}
		private boolean isMissingOptionalChildren(IFeature feature) {
			try {
				IFeatureReference [] children=feature.getIncludedFeatureReferences();
				for (int i=0; i<children.length; i++) {
					IFeatureReference ref = children[i];
					try {
						IFeature child = ref.getFeature();
						// If we are here, the child is not missing.
						// Check it's children recursively.
						if (isMissingOptionalChildren(child))
							return true;
					}
					catch (CoreException e) {
						// Missing child. Return true if optional,
						// otherwise it is a broken feature that we 
						// do not care about.
						if (ref.isOptional()) {
							return true;
						}
					}
				}
			}
			catch (CoreException e) {
			}
			return false;
		}
		public IFeature[] getMatchingFeatures(
			ISite site,
			IProgressMonitor monitor) {
			ArrayList hits = new ArrayList();
			boolean broken = isBroken();
			boolean missingOptionalChildren = false;
			
			// Don't bother to compute missing optional children
			// if the feature is broken - all we want is to 
			// see if we should allow same-version re-install.
			if (!broken) 
				missingOptionalChildren = isMissingOptionalChildren(candidate);
			IFeatureReference[] refs = site.getFeatureReferences();
			monitor.beginTask("", refs.length + 1);
			for (int i = 0; i < refs.length; i++) {
				IFeatureReference ref = refs[i];
				try {
					if (isNewerVersion(candidate.getVersionedIdentifier(),
						ref.getVersionedIdentifier())) {
						hits.add(new Hit(candidate, ref));
					} else {
						// accept the same feature if the installed
						// feature is broken
						if ((broken || missingOptionalChildren)
							&& candidate.getVersionedIdentifier().equals(
								ref.getVersionedIdentifier()))
							hits.add(new Hit(candidate, ref));
					}
				} catch (CoreException e) {
				}
				monitor.worked(1);
				if (monitor.isCanceled())
					return new IFeature[0];
			}
			IFeature[] result;
			if (hits.size() == 0)
				result = new IFeature[0];
			else {
				IFeature topHit = getFirstValid(hits);
				if (topHit == null)
					result = new IFeature[0];
				else
					result = new IFeature[] { topHit };
			}
			monitor.worked(1);
			monitor.done();
			return result;
		}
	}

	private ArrayList candidates;

	public UpdatesSearchCategory() {
	}

	private IFeature getFirstValid(ArrayList hits) {
		Object[] array = hits.toArray();
		HitSorter sorter = new HitSorter();
		sorter.sortInPlace(array);
		for (int i = 0; i < array.length; i++) {
			Hit hit = (Hit) array[i];
			PendingChange job = hit.getJob();
			if (job == null)
				continue;
			// do not accept updates without a license
			if (!UpdateModel.hasLicense(job))
				continue;
			IStatus status = ActivityConstraints.validatePendingChange(job);
			if (status == null)
				return job.getFeature();
		}
		// no valid hits
		return null;
	}

	public void initialize() {
		candidates = new ArrayList();
		try {
			ILocalSite localSite = SiteManager.getLocalSite();
			IInstallConfiguration config = localSite.getCurrentConfiguration();
			IConfiguredSite[] isites = config.getConfiguredSites();
			for (int i = 0; i < isites.length; i++) {
				IFeatureReference[] refs = isites[i].getConfiguredFeatures();
				for (int j = 0; j < refs.length; j++) {
					IFeatureReference ref = refs[j];
					candidates.add(ref.getFeature());
				}
			}
			filterIncludedFeatures(candidates);

		} catch (CoreException e) {
			UpdateUIPlugin.logException(e, false);
		}
	}

	private void filterIncludedFeatures(ArrayList candidates)
		throws CoreException {
		IFeature[] array =
			(IFeature[]) candidates.toArray(new IFeature[candidates.size()]);
		// filter out included features so that only top-level features remain on the list
		for (int i = 0; i < array.length; i++) {
			IFeature feature = array[i];
			IFeatureReference[] included =
				feature.getIncludedFeatureReferences();
			for (int j = 0; j < included.length; j++) {
				IFeatureReference fref = included[j];
				IFeature ifeature = fref.getFeature();
				int index = candidates.indexOf(ifeature);
				if (index != -1)
					candidates.remove(index);
			}
		}
	}
	public ISearchQuery[] getQueries() {
		initialize();
		ArrayList selected = getSelectedCandidates();
		ISearchQuery[] queries = new ISearchQuery[selected.size()];
		for (int i = 0; i < selected.size(); i++) {
			queries[i] = new UpdateQuery((IFeature) selected.get(i));
		}
		return queries;
	}

	public String getCurrentSearch() {
		return UpdateUIPlugin.getResourceString(KEY_CURRENT_SEARCH);
	}

	private boolean isNewerVersion(
		VersionedIdentifier fvi,
		VersionedIdentifier cvi) {
		if (!fvi.getIdentifier().equals(cvi.getIdentifier()))
			return false;
		PluginVersionIdentifier fv = fvi.getVersion();
		PluginVersionIdentifier cv = cvi.getVersion();
		String mode = MainPreferencePage.getUpdateVersionsMode();
		boolean greater = cv.isGreaterThan(fv);
		if (!greater)
			return false;
		if (mode.equals(MainPreferencePage.EQUIVALENT_VALUE))
			return cv.isEquivalentTo(fv);
		else if (mode.equals(MainPreferencePage.COMPATIBLE_VALUE))
			return cv.isCompatibleWith(fv);
		else
			return false;
	}

	public void createControl(Composite parent, FormWidgetFactory factory) {
		Composite container = factory.createComposite(parent);
		GridLayout layout = new GridLayout();
		//layout.numColumns = 2;
		layout.marginWidth = 2;
		layout.marginHeight = 2;
		container.setLayout(layout);
		tableViewer =
			CheckboxTableViewer.newCheckList(
				container,
				SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		tableViewer.setContentProvider(new FeatureContentProvider());
		tableViewer.setLabelProvider(new FeatureLabelProvider());
		tableViewer.setInput(this);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.heightHint = 100;
		tableViewer.getControl().setLayoutData(gd);
		factory.paintBordersFor(container);
		setControl(container);
	}
	public void load(Map map, boolean editable) {
		initialize();
		tableViewer.refresh();
		tableViewer.setAllChecked(true);
		String value = (String) map.get("unchecked");
		if (value == null)
			return;
		StringTokenizer stok = new StringTokenizer(value, ":");
		while (stok.hasMoreTokens()) {
			String token = stok.nextToken();
			int loc = token.lastIndexOf('_');
			String id;
			String version = null;
			if (loc != -1) {
				id = token.substring(0, loc);
				version = token.substring(loc + 1);
			} else {
				id = token;
				version = "0.0.0";
			}
			IFeature feature =
				findCandidate(new VersionedIdentifier(id, version));
			if (feature != null)
				tableViewer.setChecked(feature, false);
		}
	}

	private IFeature findCandidate(VersionedIdentifier vid) {
		if (candidates == null)
			initialize();
		for (int i = 0; i < candidates.size(); i++) {
			IFeature candidate = (IFeature) candidates.get(i);
			if (candidate.getVersionedIdentifier().equals(vid))
				return candidate;
		}
		return null;
	}
	public void store(Map map) {
		if (candidates == null)
			return;
		int counter = 0;
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < candidates.size(); i++) {
			IFeature candidate = (IFeature) candidates.get(i);
			if (tableViewer.getChecked(candidate) == false) {
				if (counter > 0)
					buf.append(":");
				buf.append(candidate.getVersionedIdentifier().toString());
			}
		}
		map.put("unchecked", buf.toString());
	}
	private ArrayList getSelectedCandidates() {
		if (tableViewer == null
			|| tableViewer.getControl() == null
			|| tableViewer.getControl().isDisposed()) {
			return candidates;
		}
		ArrayList selected = new ArrayList();
		Object[] sel = tableViewer.getCheckedElements();
		for (int i = 0; i < sel.length; i++)
			selected.add(sel[i]);
		return selected;
	}
}