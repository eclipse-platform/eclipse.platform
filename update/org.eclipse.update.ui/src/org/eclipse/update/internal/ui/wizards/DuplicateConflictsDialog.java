package org.eclipse.update.internal.ui.wizards;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.update.configuration.IConfiguredSite;
import org.eclipse.update.configuration.IInstallConfiguration;
import org.eclipse.update.configuration.ILocalSite;
import org.eclipse.update.core.IFeature;
import org.eclipse.update.core.IFeatureReference;
import org.eclipse.update.core.SiteManager;
import org.eclipse.update.core.VersionedIdentifier;
import org.eclipse.update.internal.ui.UpdateUIPlugin;
import org.eclipse.update.internal.ui.UpdateUIPluginImages;
import org.eclipse.update.internal.ui.model.PendingChange;
import org.eclipse.update.internal.ui.parts.DefaultContentProvider;
import org.eclipse.update.internal.ui.parts.OverlayIcon;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

/**
 * 
 */
public class DuplicateConflictsDialog extends MessageDialog {
	private static final String KEY_TITLE = "DuplicateConflictsDialog.title";
	private static final String KEY_MESSAGE =
		"DuplicateConflictsDialog.message";
	private static final String KEY_TREE_LABEL =
		"DuplicateConflictsDialog.treeLabel";
	private static final String KEY_CONFLICT =
		"DuplicateConflictsDialog.conflict";

	private TreeViewer treeViewer;
	private ArrayList conflicts;
	private Image featureImage;
	private Image warningFeatureImage;

	static class IdEntry {
		IConfiguredSite csite;
		IFeature feature;

		public IdEntry(IFeature feature, IConfiguredSite csite) {
			this.feature = feature;
			this.csite = csite;
		}
		public boolean isInstallCandidate() {
			return csite != null;
		}
		public IFeature getFeature() {
			return feature;
		}

		public String getIdentifier() {
			return feature.getVersionedIdentifier().getIdentifier();
		}
		public IConfiguredSite getConfiguredSite() {
			if (csite != null)
				return csite;
			return feature.getSite().getCurrentConfiguredSite();
		}
		public boolean sameLevel(IdEntry entry) {
			VersionedIdentifier vid = feature.getVersionedIdentifier();
			VersionedIdentifier evid =
				entry.getFeature().getVersionedIdentifier();
			return vid.equals(evid);
		}
		public String toString() {
			IConfiguredSite configSite = getConfiguredSite();
			String version =
				feature.getVersionedIdentifier().getVersion().toString();
			String location = configSite.getSite().getURL().getFile();
			return UpdateUIPlugin.getFormattedMessage(
				KEY_CONFLICT,
				new String[] { version, location });
		}
	}

	class ConflictContentProvider
		extends DefaultContentProvider
		implements ITreeContentProvider, IStructuredContentProvider {
		public Object[] getElements(Object input) {
			return getChildren(input);
		}
		public Object getParent(Object child) {
			return null;
		}
		public boolean hasChildren(Object parent) {
			if (parent instanceof ArrayList)
				return true;
			return false;
		}
		public Object[] getChildren(Object parent) {
			if (parent instanceof ArrayList)
				return ((ArrayList) parent).toArray();
			return new Object[0];
		}
	}

	class ConflictLabelProvider extends LabelProvider {
		public String getText(Object obj) {
			if (obj instanceof ArrayList) {
				ArrayList list = (ArrayList) obj;
				for (int i = 0; i < list.size(); i++) {
					IdEntry entry = (IdEntry) (list).get(i);
					if (entry.isInstallCandidate())
						return entry.getFeature().getLabel();
				}
			}
			return super.getText(obj);
		}
		public Image getImage(Object obj) {
			if (obj instanceof ArrayList)
				return warningFeatureImage;
			if (obj instanceof IdEntry)
				return featureImage;
			return null;
		}
	}

	public DuplicateConflictsDialog(Shell shell, ArrayList conflicts) {
		super(
			shell,
			UpdateUIPlugin.getResourceString(KEY_TITLE),
			null,
			UpdateUIPlugin.getResourceString(KEY_MESSAGE),
			WARNING,
			new String[] {
				IDialogConstants.YES_LABEL,
				IDialogConstants.NO_LABEL },
			0);
		this.conflicts = conflicts;
		featureImage = UpdateUIPluginImages.DESC_FEATURE_OBJ.createImage();
		ImageDescriptor desc =
			new OverlayIcon(
				UpdateUIPluginImages.DESC_FEATURE_OBJ,
				new ImageDescriptor[][] { {
			}, {
			}, {
				UpdateUIPluginImages.DESC_WARNING_CO }
		});
		warningFeatureImage = desc.createImage();
	}

	public boolean close() {
		featureImage.dispose();
		warningFeatureImage.dispose();
		return super.close();
	}

	protected Control createCustomArea(Composite parent) {
		Composite client = new Composite(parent, SWT.NULL);
		client.setLayoutData(new GridData(GridData.FILL_BOTH));
		GridLayout layout = new GridLayout();
		client.setLayout(layout);

		Label label = new Label(client, SWT.NULL);
		label.setText(UpdateUIPlugin.getResourceString(KEY_TREE_LABEL));

		treeViewer = new TreeViewer(client, SWT.SINGLE | SWT.BORDER);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.heightHint = 200;
		gd.widthHint = 300;
		treeViewer.getTree().setLayoutData(gd);
		treeViewer.setContentProvider(new ConflictContentProvider());
		treeViewer.setLabelProvider(new ConflictLabelProvider());
		treeViewer.setAutoExpandLevel(10);
		treeViewer.setSorter(new ViewerSorter() {
		});
		treeViewer.setInput(conflicts);
		return client;
	}

	static ArrayList computeDuplicateConflicts(
		PendingChange job,
		IInstallConfiguration config,
		IConfiguredSite targetSite,
		IFeatureReference[] optionalFeatures) {
		Hashtable featureTable = new Hashtable();
		try {
			computePresentState(featureTable, config);
			computeNewFeature(
				job.getFeature(),
				targetSite,
				featureTable,
				optionalFeatures);
			return computeConflicts(featureTable);
		} catch (CoreException e) {
			return null;
		}
	}

	static ArrayList computeDuplicateConflicts(
		PendingChange[] jobs,
		IInstallConfiguration config) {
		Hashtable featureTable = new Hashtable();
		computePresentState(featureTable, config);
		computeNewFeatures(jobs, config, featureTable);
		return computeConflicts(featureTable);
	}

	private static ArrayList computeConflicts(Hashtable featureTable) {
		ArrayList result = null;
		for (Enumeration enum = featureTable.elements();
			enum.hasMoreElements();
			) {
			ArrayList candidate = (ArrayList) enum.nextElement();
			if (candidate.size() == 1)
				continue;
			ArrayList conflict = checkForConflict(candidate);
			if (conflict != null) {
				if (result == null)
					result = new ArrayList();
				result.add(conflict);
			}
		}
		return result;
	}

	private static ArrayList checkForConflict(ArrayList candidate) {
		IdEntry firstEntry = null;
		for (int i = 0; i < candidate.size(); i++) {
			IdEntry entry = (IdEntry) candidate.get(i);
			if (firstEntry == null)
				firstEntry = entry;
			else if (!entry.sameLevel(firstEntry))
				return candidate;
		}
		return null;
	}

	private static void computePresentState(
		Hashtable table,
		IInstallConfiguration config) {
		IConfiguredSite[] csites = config.getConfiguredSites();
		for (int i = 0; i < csites.length; i++) {
			IConfiguredSite csite = csites[i];
			IFeatureReference[] refs = csite.getConfiguredFeatures();
			for (int j = 0; j < refs.length; j++) {
				try {
					addEntry(refs[j].getFeature(), null, table);
				} catch (CoreException e) {
					// don't let one bad feature stop the loop
				}
			}
		}
	}

	private static void computeNewFeatures(
		PendingChange[] jobs,
		IInstallConfiguration config,
		Hashtable featureTable) {
		for (int i = 0; i < jobs.length; i++) {
			PendingChange job = jobs[i];
			IConfiguredSite targetSite =
				TargetPage.getDefaultTargetSite(config, job);
			IFeature newFeature = job.getFeature();
			try {
				computeNewFeature(newFeature, targetSite, featureTable, null);
			} catch (CoreException e) {
			}
		}
	}

	private static void computeNewFeature(
		IFeature feature,
		IConfiguredSite csite,
		Hashtable table,
		IFeatureReference[] optionalFeatures)
		throws CoreException {
		addEntry(feature, csite, table);
		IFeatureReference[] irefs = feature.getIncludedFeatureReferences();
		for (int i = 0; i < irefs.length; i++) {
			IFeatureReference iref = irefs[i];
			boolean add = true;

			if (iref.isOptional() && optionalFeatures != null) {
				boolean found = false;
				for (int j = 0; j < optionalFeatures.length; j++) {
					IFeatureReference checked = optionalFeatures[j];
					if (checked.equals(iref)) {
						found = true;
						break;
					}
				}
				add = found;
			}
			if (add)
				computeNewFeature(
					iref.getFeature(),
					csite,
					table,
					optionalFeatures);
		}
	}

	private static void addEntry(
		IFeature feature,
		IConfiguredSite csite,
		Hashtable featureTable) {
		String id = feature.getVersionedIdentifier().getIdentifier();
		ArrayList entries = (ArrayList) featureTable.get(id);
		if (entries == null) {
			entries = new ArrayList();
			featureTable.put(id, entries);
		}
		IdEntry entry = new IdEntry(feature, csite);
		boolean replaced=false;
		for (int i=0; i<entries.size(); i++) {
			IdEntry existingEntry = (IdEntry)entries.get(i);
			IConfiguredSite existingSite = existingEntry.getConfiguredSite();
			if (existingSite.equals(entry.getConfiguredSite())) {
				// same site - replace it if not new
				if (entry.isInstallCandidate()) {
					entries.set(i, entry);
					entries.remove(existingEntry);
				}
				replaced = true;
				break;
			}
		}
		if (!replaced)
			entries.add(entry);
	}
}