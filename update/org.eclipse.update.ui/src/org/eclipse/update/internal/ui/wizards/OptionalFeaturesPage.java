package org.eclipse.update.internal.ui.wizards;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.layout.*;
import java.util.*;
import org.eclipse.swt.SWT;
import org.eclipse.update.internal.ui.model.*;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.jface.viewers.*;
import org.eclipse.update.internal.ui.parts.*;
import org.eclipse.update.core.*;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.update.configuration.*;
import org.eclipse.update.internal.ui.*;
import java.net.URL;
import java.io.*;
import org.eclipse.core.boot.IPlatformConfiguration;
import org.eclipse.jface.dialogs.MessageDialog;

public class OptionalFeaturesPage extends BannerPage {
	// NL keys
	private static final String KEY_TITLE =
		"InstallWizard.OptionalFeaturesPage.title";
	private static final String KEY_DESC =
		"InstallWizard.OptionalFeaturesPage.desc";
	private static final String KEY_TREE_LABEL =
		"InstallWizard.OptionalFeaturesPage.treeLabel";
	private static final String KEY_SELECT_ALL =
		"InstallWizard.OptionalFeaturesPage.selectAll";
	private static final String KEY_DESELECT_ALL =
		"InstallWizard.OptionalFeaturesPage.deselectAll";
	private CheckboxTreeViewer treeViewer;
	private IInstallConfiguration config;
	private PendingChange pendingChange;
	private Image featureImage;

	class TreeContentProvider
		extends DefaultContentProvider
		implements ITreeContentProvider {
		public Object[] getChildren(Object parent) {
			if (parent instanceof IFeatureReference) {
				IFeatureReference ref = (IFeatureReference) parent;
				return getIncludedFeatures(ref);
			}
			return new Object[0];
		}

		public Object getParent(Object child) {
			return null;
		}

		public boolean hasChildren(Object parent) {
			return getChildren(parent).length > 0;
		}

		public Object[] getElements(Object input) {
			return getIncludedFeatures(pendingChange.getFeature());
		}
	}

	class TreeLabelProvider extends LabelProvider {
		public String getText(Object obj) {
			if (obj instanceof IFeatureReference) {
				IFeatureReference ref = (IFeatureReference) obj;
				try {
					IFeature feature = ref.getFeature();
					return getFeatureLabel(feature);
				} catch (CoreException e) {
					if (ref.getName() != null)
						return ref.getName();
					try {
						VersionedIdentifier vid = ref.getVersionedIdentifier();
						return vid.toString();
					} catch (CoreException e2) {
					}
				}
			}
			return super.getText(obj);
		}
		private String getFeatureLabel(IFeature feature) {
			return feature.getLabel()
					+ " "
					+ feature
						.getVersionedIdentifier()
						.getVersion()
						.toString();
		}

		public Image getImage(Object obj) {
			return featureImage;
		}
	}

	/**
	 * Constructor for ReviewPage
	 */
	public OptionalFeaturesPage(
		PendingChange pendingChange,
		IInstallConfiguration config) {
		super("OptionalFeatures");
		setTitle(UpdateUIPlugin.getResourceString(KEY_TITLE));
		setDescription(UpdateUIPlugin.getResourceString(KEY_DESC));
		this.config = config;
		this.pendingChange = pendingChange;
		featureImage = UpdateUIPluginImages.DESC_FEATURE_OBJ.createImage();
	}

	public void dispose() {
		if (featureImage != null) {
			featureImage.dispose();
			featureImage = null;
		}
		super.dispose();
	}

	/**
	 * @see DialogPage#createControl(Composite)
	 */
	public Control createContents(Composite parent) {
		Composite client = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginWidth = layout.marginHeight = 0;
		client.setLayout(layout);
		createCheckboxTreeViewer(client);
		Button selectAllButton = new Button(client, SWT.PUSH);
		selectAllButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				selectAll(true);
			}
		});
		selectAllButton.setText(UpdateUIPlugin.getResourceString(KEY_SELECT_ALL));
		GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_BEGINNING);
		selectAllButton.setLayoutData(gd);
		SWTUtil.setButtonDimensionHint(selectAllButton);
		
		Button deselectAllButton = new Button(client, SWT.PUSH);
		deselectAllButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				selectAll(false);
			}
		});
		deselectAllButton.setText(UpdateUIPlugin.getResourceString(KEY_DESELECT_ALL));
		gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_BEGINNING);
		deselectAllButton.setLayoutData(gd);
		SWTUtil.setButtonDimensionHint(deselectAllButton);
		return client;
	}

	private void createCheckboxTreeViewer(Composite parent) {
		Label label = new Label(parent, SWT.NULL);
		label.setText(UpdateUIPlugin.getResourceString(KEY_TREE_LABEL));
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		label.setLayoutData(gd);
		treeViewer =
			new CheckboxTreeViewer(
				parent,
				SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		gd = new GridData(GridData.FILL_BOTH);
		gd.verticalSpan = 2;
		Tree tree = treeViewer.getTree();
		tree.setLayoutData(gd);
		treeViewer.setContentProvider(new TreeContentProvider());
		treeViewer.setLabelProvider(new TreeLabelProvider());
		treeViewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent e) {
				handleChecked(e.getElement(), e.getChecked());
			}
		});
		/*
		treeViewer.addFilter(new ViewerFilter() {
			public boolean select(Viewer v, Object parent, Object element) {
				IFeatureReference reference = (IFeatureReference)element;
				if (reference.isOptional()) return true;
				return InstallWizard.hasOptionalFeatures(reference);
			}
		});
		*/
		treeViewer.setInput(pendingChange);
		initializeStates();
	}

	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			treeViewer.getTree().setFocus();
		}
	}

	private Object[] getIncludedFeatures(IFeatureReference ref) {
		try {
			IFeature feature = ref.getFeature();
			return getIncludedFeatures(feature);
		} catch (CoreException e) {
		}
		return new Object[0];
	}

	private Object[] getIncludedFeatures(IFeature feature) {
		try {
			return feature.getIncludedFeatureReferences();
		} catch (CoreException e) {
		}
		return new Object[0];
	}

	private void initializeStates() {
		IFeature oldFeature = pendingChange.getOldFeature();
		IFeature newFeature = pendingChange.getFeature();

		if (oldFeature==null) {
			// preselect all included children
			selectAll(true);
		}
		else {
			// preselect only installed children
		}
	}
	
	private void selectAll(boolean value) {
		ArrayList selected = new ArrayList();
		ArrayList grayed = new ArrayList();
		Object [] included = getIncludedFeatures(pendingChange.getFeature());
		for (int i=0; i<included.length; i++) {
			IFeatureReference ref = (IFeatureReference)included[i];
			if (ref.isOptional()==false) {
				selected.add(ref);
				grayed.add(ref);
			}
			else if (value)
				selected.add(ref);
			selectAll(ref, selected, grayed, value);
		}
		treeViewer.setCheckedElements(selected.toArray());
		treeViewer.setGrayedElements(grayed.toArray());
	}
	
	private void selectAll(IFeatureReference ref, ArrayList selected, ArrayList grayed, boolean value) {
		Object [] included = getIncludedFeatures(ref);
		for (int i=0; i<included.length; i++) {
			IFeatureReference iref = (IFeatureReference)included[i];
			if (iref.isOptional()==false) {
				selected.add(iref);
				grayed.add(iref);
			}
			else if (value)
				selected.add(iref);
			selectAll(iref, selected, grayed, value);
		}
	}
	
	private void handleChecked(Object element, boolean checked) {
		//see if the object is editable
		boolean editable = true;

		if (element instanceof IFeatureReference) {
			IFeatureReference fref = (IFeatureReference)element;
			editable = fref.isOptional();
		}
		if (!editable)
			treeViewer.setChecked(element, !checked);
		else {
			// update the result
		}
	}
}