package org.eclipse.update.internal.ui.views;

import java.util.ArrayList;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.update.core.*;
import org.eclipse.update.internal.ui.*;
import org.eclipse.update.internal.ui.model.*;
import org.eclipse.update.internal.ui.search.*;

/**
 * 
 */

public class SearchResultView extends ViewPart implements ISelectionListener {
	private TableViewer viewer;
	private Action showSearchAction;
	private ModelListener modelListener;
	private Image featureImage;
	private SearchObject currentSearch;
	private static final String KEY_C_FEATURE =
		"SearchResultView.column.feature";
	private static final String KEY_C_PROVIDER =
		"SearchResultView.column.provider";
	private static final String KEY_C_SITE = "SearchResultView.column.site";
	private static final String KEY_C_VERSION =
		"SearchResultView.column.version";
	private static final String KEY_C_SIZE = "SearchResultView.column.size";
	private static final String KEY_UNKNOWN_SIZE =
		"SearchResultView.column.sizeUnknown";
	private static final String KEY_TITLE = 
		"SearchResultView.title";
	private static final String KEY_SHOW_SEARCH_LABEL = 
		"SearchResultView.showSearch.label";
	private static final String KEY_SHOW_SEARCH_TOOLTIP = 
		"SearchResultView.showSearch.tooltip";

	/*
	 * The content provider class is responsible for
	 * providing objects to the view. It can wrap
	 * existing objects in adapters or simply return
	 * objects as-is. These objects may be sensitive
	 * to the current input of the view, or ignore
	 * it and always show the same content 
	 * (like Task List, for example).
	 */

	class ViewContentProvider implements IStructuredContentProvider {
		public void inputChanged(Viewer v, Object oldInput, Object newInput) {
		}
		public void dispose() {
		}
		public Object[] getElements(Object parent) {
			if (currentSearch == null)
				return new Object[0];
			ArrayList result = new ArrayList();
			Object[] sites = currentSearch.getChildren(currentSearch);
			for (int i = 0; i < sites.length; i++) {
				SearchResultSite site = (SearchResultSite) sites[i];
				Object[] results = site.getChildren(site);
				for (int j = 0; j < results.length; j++) {
					result.add(results[j]);
				}
			}
			return result.toArray();
		}
	}
	class ViewLabelProvider
		extends LabelProvider
		implements ITableLabelProvider {
		public String getColumnText(Object obj, int col) {
			if (obj instanceof IFeatureAdapter) {
				IFeature feature;
				try {
					feature = ((IFeatureAdapter) obj).getFeature();
				} catch (CoreException e) {
					if (col == 0)
						return getText(obj);
					return "";
				}
				ISite site = ((IFeatureAdapter) obj).getSite();
				switch (col) {
					case 0 :
						return feature.getLabel();
					case 1 :
						return feature
							.getVersionedIdentifier()
							.getVersion()
							.toString();
					case 2 :
						return feature.getProvider();
					case 3 :
						return site.getURL().toString();
					case 4 :
						long size = feature.getDownloadSize();
						if (size == -1)
							return UpdateUIPlugin.getResourceString(
								KEY_UNKNOWN_SIZE);
						else
							return feature.getDownloadSize() + "KB";
				}
			}
			return getText(obj);
		}
		public Image getColumnImage(Object obj, int index) {
			if (index == 0)
				return getImage(obj);
			return null;
		}
		public Image getImage(Object obj) {
			return featureImage;
		}
	}
	class NameSorter extends ViewerSorter {
	}

	class ModelListener implements IUpdateModelChangedListener {
		public void objectsAdded(Object parent, Object[] children) {
			if (parent instanceof SearchResultSite) {
				viewer.add(children);
				updateTitle();
			}
		}
		public void objectsRemoved(Object parent, Object[] children) {
		}
		public void objectChanged(Object object, String property) {
			if (object instanceof SearchObject) {
				if (SearchObject.P_REFRESH.equals(property)) {
					viewer.refresh();
					updateTitle();
				}
			}
		}
	}

	/**
	 * The constructor.
	 */
	public SearchResultView() {
		modelListener = new ModelListener();
		featureImage = UpdateUIPluginImages.DESC_FEATURE_OBJ.createImage();
	}

	/**
	 * This is a callback that will allow us
	 * to create the viewer and initialize it.
	 */
	public void createPartControl(Composite parent) {
		viewer =
			new TableViewer(
				parent,
				SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);
		createColumns();
		viewer.setContentProvider(new ViewContentProvider());
		viewer.setLabelProvider(new ViewLabelProvider());
		viewer.setSorter(new NameSorter());
		viewer.setInput(ResourcesPlugin.getWorkspace());
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent e) {
			}
		});
		makeActions();
		hookContextMenu();
		hookDoubleClickAction();
		contributeToActionBars();
		UpdateModel model = UpdateUIPlugin.getDefault().getUpdateModel();
		model.addUpdateModelChangedListener(modelListener);
		hookSelectionListener(true);
	}

	private void hookSelectionListener(boolean add) {
		IWorkbenchPage page = UpdateUIPlugin.getActivePage();
		if (page != null) {
			if (add)
				page.addSelectionListener(this);
			else
				page.removeSelectionListener(this);
		}
	}

	private void createColumns() {
		Table table = viewer.getTable();
		table.setHeaderVisible(true);

		TableColumn column = new TableColumn(table, SWT.NULL);
		column.setText(UpdateUIPlugin.getResourceString(KEY_C_FEATURE));

		column = new TableColumn(table, SWT.NULL);
		column.setText(UpdateUIPlugin.getResourceString(KEY_C_VERSION));

		column = new TableColumn(table, SWT.NULL);
		column.setText(UpdateUIPlugin.getResourceString(KEY_C_PROVIDER));

		column = new TableColumn(table, SWT.NULL);
		column.setText(UpdateUIPlugin.getResourceString(KEY_C_SITE));

		column = new TableColumn(table, SWT.NULL);
		column.setText(UpdateUIPlugin.getResourceString(KEY_C_SIZE));

		TableLayout layout = new TableLayout();
		layout.addColumnData(new ColumnWeightData(100, 200, true));
		layout.addColumnData(new ColumnWeightData(100, 50, true));
		layout.addColumnData(new ColumnWeightData(100, 100, true));
		layout.addColumnData(new ColumnWeightData(100, 100, true));
		layout.addColumnData(new ColumnWeightData(50, true));
		table.setLayout(layout);
	}

	public void dispose() {
		UpdateModel model = UpdateUIPlugin.getDefault().getUpdateModel();
		model.removeUpdateModelChangedListener(modelListener);
		hookSelectionListener(false);
		featureImage.dispose();
		super.dispose();
	}

	private void hookContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				SearchResultView.this.fillContextMenu(manager);
			}
		});
		Menu menu = menuMgr.createContextMenu(viewer.getControl());
		viewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, viewer);
	}

	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		fillLocalPullDown(bars.getMenuManager());
		fillLocalToolBar(bars.getToolBarManager());
	}

	private void fillLocalPullDown(IMenuManager manager) {
	}

	private void fillContextMenu(IMenuManager manager) {
		manager.add(showSearchAction);
		manager.add(new Separator("additions"));
	}

	private void fillLocalToolBar(IToolBarManager manager) {
		manager.add(showSearchAction);
	}

	private void makeActions() {
		showSearchAction = new Action() {
			public void run() {
				try {
					IWorkbenchPage page = UpdateUIPlugin.getActivePage();
					DetailsView dview =
						(DetailsView) page.showView(
							UpdatePerspective.ID_DETAILS);
					dview.showPage(DetailsView.SEARCH_PAGE, currentSearch);
				} catch (PartInitException e) {
					UpdateUIPlugin.logException(e);
				}
			}
		};
		showSearchAction.setText(UpdateUIPlugin.getResourceString(KEY_SHOW_SEARCH_LABEL));
		showSearchAction.setToolTipText(UpdateUIPlugin.getResourceString(KEY_SHOW_SEARCH_TOOLTIP));
		showSearchAction.setImageDescriptor(
			UpdateUIPluginImages.DESC_SHOW_SEARCH);
	}

	private void hookDoubleClickAction() {
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
			}
		});
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
		viewer.getControl().setFocus();
	}

	public void setCurrentSearch(SearchObject currentSearch) {
		this.currentSearch = currentSearch;
		viewer.setInput(currentSearch);
		updateTitle();
	}

	private void updateTitle() {
		if (currentSearch == null)
			setTitle(getSite().getRegisteredName());
		else {
			String searchLabel = getSearchLabel(currentSearch);
			int count = viewer.getTable().getItemCount();
			String title = UpdateUIPlugin.getFormattedMessage(KEY_TITLE, 
				new String [] {
					getSite().getRegisteredName(),
					getSearchLabel(currentSearch),
					""+count });
			setTitle(title);
		}
	}

	private String getSearchLabel(SearchObject searchObject) {
		SearchCategoryDescriptor sdesc =
			SearchCategoryRegistryReader.getDefault().getDescriptor(
				searchObject.getCategoryId());
		ISearchCategory category = sdesc.createCategory();
		if (category != null)
			return category.getCurrentSearch();
		else
			return searchObject.getName();
	}

	public void setSelectionActive(boolean active) {
		if (active)
			getSite().setSelectionProvider(viewer);
		else
			getSite().setSelectionProvider(null);
		updateTitle();
	}

	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection ssel = (IStructuredSelection) selection;
			if (ssel.size() == 1) {
				Object obj = ssel.getFirstElement();
				if (obj instanceof SearchObject)
					setCurrentSearch((SearchObject) obj);
			}
		}
	}
}