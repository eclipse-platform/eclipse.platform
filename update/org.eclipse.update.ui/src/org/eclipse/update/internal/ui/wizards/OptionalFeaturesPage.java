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
	private Object[] elements;

	class TreeContentProvider
		extends DefaultContentProvider
		implements ITreeContentProvider {

		public Object[] getChildren(Object parent) {
			if (parent instanceof FeatureElement) {
				FeatureElement fe = (FeatureElement) parent;
				return fe.getChildren();
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
			if (elements == null)
				computeElements();
			return elements;
		}
	}

	class TreeLabelProvider extends LabelProvider {
		public String getText(Object obj) {
			if (obj instanceof FeatureElement) {
				FeatureElement fe = (FeatureElement) obj;
				String name = fe.getLabel();
				if (name != null)
					return name;
			}
			return super.getText(obj);
		}
		public Image getImage(Object obj) {
			return featureImage;
		}
	}

	class FeatureElement {
		private ArrayList children;
		private IFeatureReference oldFeatureRef;
		private IFeatureReference newFeatureRef;
		boolean checked;

		public FeatureElement(
			IFeatureReference oldRef,
			IFeatureReference newRef) {
			oldFeatureRef = oldRef;
			newFeatureRef = newRef;
		}
		public boolean isEditable() {
			// cannot uncheck non-optional features
			if (newFeatureRef.isOptional() == false)
				return false;
			// cannot uncheck optional feature that
			// has already been installed
			if (oldFeatureRef != null)
				return false;
			return true;
		}
		public boolean isOptional() {
			return newFeatureRef.isOptional();
		}
		public boolean isChecked() {
			return checked;
		}
		public void setChecked(boolean checked) {
			this.checked = checked;
		}
		public String getLabel() {
			try {
				IFeature feature = newFeatureRef.getFeature();
				return getFeatureLabel(feature);
			} catch (CoreException e) {
				if (newFeatureRef.getName() != null)
					return newFeatureRef.getName();
				try {
					VersionedIdentifier vid =
						newFeatureRef.getVersionedIdentifier();
					return vid.toString();
				} catch (CoreException e2) {
				}
			}
			return null;
		}
		private String getFeatureLabel(IFeature feature) {
			return feature.getLabel()
				+ " "
				+ feature.getVersionedIdentifier().getVersion().toString();
		}
		Object[] getChildren() {
			computeChildren();
			return children.toArray();
		}
		public void computeChildren() {
			if (children == null) {
				children = new ArrayList();
				try {
					IFeature oldFeature = null;
					IFeature newFeature = null;
					newFeature = newFeatureRef.getFeature();
					if (oldFeatureRef != null)
						oldFeature = oldFeatureRef.getFeature();
					computeElements(oldFeature, newFeature, children);
				} catch (CoreException e) {
				}
			}
		}
		public void addCheckedOptionalFeatures(Set set) {
			if (isOptional() && isChecked())
				set.add(newFeatureRef);
			Object[] list = getChildren();
			for (int i = 0; i < list.length; i++) {
				FeatureElement element = (FeatureElement) list[i];
				element.addCheckedOptionalFeatures(set);
			}
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
		selectAllButton.setText(
			UpdateUIPlugin.getResourceString(KEY_SELECT_ALL));
		GridData gd =
			new GridData(
				GridData.HORIZONTAL_ALIGN_FILL
					| GridData.VERTICAL_ALIGN_BEGINNING);
		selectAllButton.setLayoutData(gd);
		SWTUtil.setButtonDimensionHint(selectAllButton);

		Button deselectAllButton = new Button(client, SWT.PUSH);
		deselectAllButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				selectAll(false);
			}
		});
		deselectAllButton.setText(
			UpdateUIPlugin.getResourceString(KEY_DESELECT_ALL));
		gd =
			new GridData(
				GridData.HORIZONTAL_ALIGN_FILL
					| GridData.VERTICAL_ALIGN_BEGINNING);
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
		treeViewer.setAutoExpandLevel(AbstractTreeViewer.ALL_LEVELS);
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

	private void computeElements() {
		IFeature oldFeature = pendingChange.getOldFeature();
		IFeature newFeature = pendingChange.getFeature();
		ArrayList list = new ArrayList();
		computeElements(oldFeature, newFeature, list);
		elements = list.toArray();
	}

	private void computeElements(
		IFeature oldFeature,
		IFeature newFeature,
		ArrayList list) {
		Object[] oldChildren = null;
		Object[] newChildren = getIncludedFeatures(newFeature);

		try {
			if (oldFeature != null) {
				oldChildren = getIncludedFeatures(oldFeature);
			}
			for (int i = 0; i < newChildren.length; i++) {
				IFeatureReference oldRef = null;
				IFeatureReference newRef = (IFeatureReference) newChildren[i];
				if (oldChildren != null) {
					String newId =
						newRef.getVersionedIdentifier().getIdentifier();

					for (int j = 0; j < oldChildren.length; j++) {
						IFeatureReference cref =
							(IFeatureReference) oldChildren[j];
						try {
							if (cref
								.getVersionedIdentifier()
								.getIdentifier()
								.equals(newId)) {
								oldRef = cref;
								break;
							}
						} catch (CoreException ex) {
						}
					}
				}
				FeatureElement element = new FeatureElement(oldRef, newRef);
				// If this is an update (old feature exists), 
				// only check the new optional feature if the old exists.
				// Otherwise, always check.
				if (newRef.isOptional()
					&& pendingChange.getOldFeature() != null)
					element.setChecked(oldRef != null);
				else
					element.setChecked(true);
				list.add(element);
				element.computeChildren();
			}
		} catch (CoreException e) {
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
		if (elements == null)
			computeElements();
		ArrayList checked = new ArrayList();
		ArrayList grayed = new ArrayList();
		initializeStates(elements, checked, grayed);
		treeViewer.setCheckedElements(checked.toArray());
		treeViewer.setGrayedElements(grayed.toArray());
	}

	private void initializeStates(
		Object[] elements,
		ArrayList checked,
		ArrayList grayed) {
		for (int i = 0; i < elements.length; i++) {
			FeatureElement element = (FeatureElement) elements[i];
			if (element.isChecked())
				checked.add(element);
			if (!element.isEditable())
				grayed.add(element);
			Object[] children = element.getChildren();
			initializeStates(children, checked, grayed);
		}
	}

	private void selectAll(boolean value) {
		ArrayList selected = new ArrayList();

		for (int i = 0; i < elements.length; i++) {
			FeatureElement element = (FeatureElement) elements[i];
			selectAll(element, selected, value);
		}
		treeViewer.setCheckedElements(selected.toArray());
	}

	private void selectAll(
		FeatureElement ref,
		ArrayList selected,
		boolean value) {

		if (ref.isOptional() == false)
			selected.add(ref);
		else {
			ref.setChecked(value);
			if (value)
				selected.add(ref);
		}
		Object[] included = ref.getChildren();
		for (int i = 0; i < included.length; i++) {
			FeatureElement fe = (FeatureElement) included[i];
			selectAll(fe, selected, value);
		}
	}

	private void handleChecked(Object element, boolean checked) {
		FeatureElement fe = (FeatureElement) element;

		if (!fe.isEditable())
			treeViewer.setChecked(element, !checked);
		else {
			// update the result
			fe.setChecked(checked);
		}
	}

	public IFeatureReference[] getCheckedOptionalFeatures() {
		HashSet set = new HashSet();
		for (int i = 0; i < elements.length; i++) {
			FeatureElement element = (FeatureElement) elements[i];
			element.addCheckedOptionalFeatures(set);
		}
		return (IFeatureReference[]) set.toArray(
			new IFeatureReference[set.size()]);
	}
}