package org.eclipse.update.internal.ui.wizards;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import java.util.*;

import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.update.configuration.IInstallConfiguration;
import org.eclipse.update.core.*;
import org.eclipse.update.internal.ui.*;
import org.eclipse.update.internal.ui.model.PendingChange;
import org.eclipse.update.internal.ui.parts.*;

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
	private boolean patch;

	class TreeContentProvider
		extends DefaultContentProvider
		implements ITreeContentProvider {

		public Object[] getChildren(Object parent) {
			if (parent instanceof FeatureHierarchyElement) {
				FeatureHierarchyElement fe = (FeatureHierarchyElement) parent;
				return fe.getChildren(pendingChange.getOldFeature() != null, patch, config);
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
			if (obj instanceof FeatureHierarchyElement) {
				FeatureHierarchyElement fe = (FeatureHierarchyElement) obj;
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
		patch = UpdateUIPlugin.isPatch(pendingChange.getFeature());
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
		FeatureHierarchyElement.computeElements(
			oldFeature,
			newFeature,
			oldFeature != null,
			patch,
			config,
			list);
		elements = list.toArray();
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
			FeatureHierarchyElement element =
				(FeatureHierarchyElement) elements[i];
			if (element.isChecked())
				checked.add(element);
			if (!element.isEditable())
				grayed.add(element);
			Object[] children =
				element.getChildren(pendingChange.getOldFeature() != null, patch, config);
			initializeStates(children, checked, grayed);
		}
	}

	private void selectAll(boolean value) {
		ArrayList selected = new ArrayList();

		for (int i = 0; i < elements.length; i++) {
			FeatureHierarchyElement element =
				(FeatureHierarchyElement) elements[i];
			selectAll(element, selected, value);
		}
		treeViewer.setCheckedElements(selected.toArray());
	}

	private void selectAll(
		FeatureHierarchyElement ref,
		ArrayList selected,
		boolean value) {

		if (ref.isOptional() == false)
			selected.add(ref);
		else {
			if (ref.isEditable()) {
				ref.setChecked(value);
				if (value)
					selected.add(ref);
			}
			else {
				if (ref.isChecked())
					selected.add(ref);
			}
		}
		Object[] included =
			ref.getChildren(pendingChange.getOldFeature() != null, patch, config);
		for (int i = 0; i < included.length; i++) {
			FeatureHierarchyElement fe = (FeatureHierarchyElement) included[i];
			selectAll(fe, selected, value);
		}
	}

	private void handleChecked(Object element, boolean checked) {
		FeatureHierarchyElement fe = (FeatureHierarchyElement) element;

		if (!fe.isEditable())
			treeViewer.setChecked(element, !checked);
		else {
			// update the result
			fe.setChecked(checked);
		}
	}
	
	public Object [] getOptionalElements() {
		return elements;
	}


	public IFeatureReference[] getCheckedOptionalFeatures() {
		HashSet set = new HashSet();
		for (int i = 0; i < elements.length; i++) {
			FeatureHierarchyElement element =
				(FeatureHierarchyElement) elements[i];
			element.addCheckedOptionalFeatures(
				pendingChange.getOldFeature() != null,
				patch,
				config,
				set);
		}
		return (IFeatureReference[]) set.toArray(
			new IFeatureReference[set.size()]);
	}
}