/*******************************************************************************
 * Copyright (c) 2006, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Cornel Izbasa <cizbasa@info.uvt.ro> - Removed unwanted items in the Customize functionality on the Intro - https://bugs.eclipse.org/420843
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 515362
 *******************************************************************************/
package org.eclipse.ui.internal.intro.universal;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IProduct;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.intro.impl.FontSelection;
import org.eclipse.ui.internal.intro.impl.IntroPlugin;
import org.eclipse.ui.internal.intro.impl.model.IntroModelRoot;
import org.eclipse.ui.internal.intro.universal.util.BundleUtil;
import org.eclipse.ui.internal.intro.universal.util.ImageUtil;
import org.eclipse.ui.internal.intro.universal.util.Log;
import org.eclipse.ui.intro.IIntroManager;
import org.eclipse.ui.intro.IIntroPart;
import org.eclipse.ui.intro.config.CustomizableIntroPart;
import org.eclipse.ui.intro.config.IIntroURL;
import org.eclipse.ui.intro.config.IntroURLFactory;
import org.osgi.framework.Bundle;
import org.osgi.service.prefs.BackingStoreException;


public class CustomizationContentsArea {

	private static final String INTRO_ROOT_PAGES = "INTRO_ROOT_PAGES"; //$NON-NLS-1$
	private static final String INTRO_DATA = "INTRO_DATA"; //$NON-NLS-1$
	private static final String INTRO_THEME = "INTRO_THEME"; //$NON-NLS-1$
	private static final String NO_ROOT_PAGES = "no_root_pages"; //$NON-NLS-1$
	private CTabFolder tabFolder;
	private String firstPageId;
	private Composite pageContainer;
	private TableViewer themes;
	private TableViewer available;
	private TableViewer topLeft;
	private TableViewer topRight;
	private TableViewer bottomLeft;
	private TableViewer bottomRight;
	private CheckboxTableViewer rootPages;
	private ArrayList<String> introRootPages = new ArrayList<>();
	private ArrayList<IntroTheme> themeList = new ArrayList<>();
	private IntroTheme introTheme;
	private String introThemeId;
	private IntroData introData;
	private Canvas themePreview;
	private TableContentProvider contentProvider;
	private TableLabelProvider labelProvider;
	private Button applyToAll;
	private Button useRelativeFonts;
	private Image extensionImage;
	private Image themeImage;
	private Image ihighImage;
	private Image ilowImage;
	private Image inewImage;
	private Image icalloutImage;
	private Shell shell;
	private static final Transfer[] TRANSFER_TYPES = new Transfer[] { ExtensionDataTransfer.getInstance() };


	private static final RootPage ROOT_PAGE_TABLE[] = new RootPage[] {
			new RootPage(IUniversalIntroConstants.ID_OVERVIEW,
					Messages.WelcomeCustomizationPreferencePage_overview,
					Messages.WelcomeCustomizationPreferencePage_NoMnemonic_overview),
			new RootPage(IUniversalIntroConstants.ID_FIRSTSTEPS,
					Messages.WelcomeCustomizationPreferencePage_firststeps,
					Messages.WelcomeCustomizationPreferencePage_NoMnemonic_firststeps),
			new RootPage(IUniversalIntroConstants.ID_TUTORIALS,
					Messages.WelcomeCustomizationPreferencePage_tutorials,
					Messages.WelcomeCustomizationPreferencePage_NoMnemonic_tutorials),
			new RootPage(IUniversalIntroConstants.ID_SAMPLES,
					Messages.WelcomeCustomizationPreferencePage_samples,
					Messages.WelcomeCustomizationPreferencePage_NoMnemonic_samples),
			new RootPage(IUniversalIntroConstants.ID_WHATSNEW,
					Messages.WelcomeCustomizationPreferencePage_whatsnew,
					Messages.WelcomeCustomizationPreferencePage_NoMnemonic_whatsnew),
			new RootPage(IUniversalIntroConstants.ID_WEBRESOURCES,
					Messages.WelcomeCustomizationPreferencePage_webresources,
					Messages.WelcomeCustomizationPreferencePage_NoMnemonic_webresources),
			new RootPage(IUniversalIntroConstants.ID_MIGRATE,
					Messages.WelcomeCustomizationPreferencePage_migrate,
					Messages.WelcomeCustomizationPreferencePage_NoMnemonic_migrate) };

	static class RootPage {

		public String id;
		public String name;
		public String nameNoMnemonic;

		public RootPage(String id, String name, String nameNoMnemonic) {
			this.id = id;
			this.name = name;
			this.nameNoMnemonic = nameNoMnemonic;
		}

		public String getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public String getNameNoMnemonic() {
			return nameNoMnemonic;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == this)
				return true;
			if (obj == null)
				return false;
			if (obj instanceof RootPage) {
				RootPage page = (RootPage) obj;
				return page.id.equals(id) && page.name.equals(name);
			}
			return false;
		}

		@Override
		public String toString() {
			return name;
		}
	}

	class TableContentProvider implements IStructuredContentProvider {

		@Override
		public Object[] getElements(Object inputElement) {
			if (inputElement == ROOT_PAGE_TABLE)
				return ROOT_PAGE_TABLE;
			if (inputElement instanceof GroupData) {
				return ((GroupData) inputElement).getChildren();
			}
			if (inputElement == themes) {
				return themeList.toArray();
			}
			return new Object[0];
		}

		@Override
		public void dispose() {
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}
	}

	class TableDragSourceListener implements DragSourceListener {

		TableViewer viewer;
		BaseData[] sel;

		public TableDragSourceListener(TableViewer viewer) {
			this.viewer = viewer;
		}

		@Override
		public void dragStart(DragSourceEvent event) {
			IStructuredSelection ssel = viewer.getStructuredSelection();
			if (ssel.size() > 0) {
				event.doit = true;
			} else {
				event.doit = false;
			}
		}

		@Override
		public void dragSetData(DragSourceEvent event) {
			IStructuredSelection ssel = viewer.getStructuredSelection();
			BaseData[] array = new BaseData[ssel.size()];
			int i = 0;
			for (Iterator<BaseData> iter = ssel.iterator(); iter.hasNext();) {
				array[i++] = iter.next();
			}
			event.data = array;
			sel = array;
		}

		@Override
		public void dragFinished(DragSourceEvent event) {
			if (event.detail == DND.DROP_MOVE) {
				GroupData gd = (GroupData) viewer.getInput();
				for (int i = 0; i < sel.length; i++) {
					BaseData ed = sel[i];
					gd.remove(ed);
				}
				viewer.refresh();
				updateColumnSizes(viewer);
			}
			sel = null;
		}
	}

	CustomizationContentsArea() {
	}

	class TableDropTargetListener extends ViewerDropAdapter {

		public TableDropTargetListener(TableViewer viewer) {
			super(viewer);
		}

		@Override
		public boolean performDrop(Object data) {
			BaseData target = (BaseData) getCurrentTarget();
			int loc = getCurrentLocation();
			GroupData gd = (GroupData) getViewer().getInput();
			if (gd == null)
				gd = createTargetGd(getViewer());
			BaseData[] sel = (BaseData[]) data;

			int index = target != null ? gd.getIndexOf(target) : -1;
			int startingIndex = getStartIndex(gd, sel);
			if (target != null) {
				if (loc == LOCATION_AFTER
						|| (loc == LOCATION_ON && startingIndex != -1 && startingIndex < index))
					index++;
				else if (index > 0 && loc == LOCATION_BEFORE)
					index--;
			}

			for (int i = 0; i < sel.length; i++) {
				BaseData ed = sel[i];
				if (index == -1)
					gd.add(ed);
				else
					gd.add(index++, ed);
			}
			if (getViewer().getInput() != null)
				getViewer().refresh();
			else
				getViewer().setInput(gd);
			updateColumnSizes((TableViewer) getViewer());
			return true;
		}

		private int getStartIndex(GroupData gd, BaseData[] sel) {
			for (int i = 0; i < sel.length; i++) {
				BaseData ed = sel[i];
				int index = gd.getIndexOf(ed.getId());
				if (index != -1)
					return index;
			}
			return -1;
		}

		@Override
		public boolean validateDrop(Object target, int operation, TransferData transferType) {
			return ExtensionDataTransfer.getInstance().isSupportedType(transferType);
		}
	}

	class TableLabelProvider extends LabelProvider implements ITableLabelProvider {

		@Override
		public String getText(Object obj) {
			if (obj instanceof RootPage) {
				return ((RootPage) obj).getNameNoMnemonic();
			}
			if (obj instanceof ExtensionData) {
				ExtensionData ed = (ExtensionData) obj;
				String name = ed.getName();
				if (name != null && name.length() > 0)
					return name;
				return ed.getId();
			}
			if (obj instanceof SeparatorData) {
				return Messages.WelcomeCustomizationPreferencePage_horizontalSeparator;
			}
			if (obj instanceof IntroTheme) {
				IntroTheme bg = (IntroTheme) obj;
				return bg.getName();
			}
			return super.getText(obj);
		}

		@Override
		public Image getImage(Object obj) {
			if (obj instanceof ExtensionData) {
				ExtensionData ed = (ExtensionData) obj;
				switch (ed.getImportance()) {
				case ExtensionData.HIGH:
					return ihighImage;
				case ExtensionData.LOW:
					return ilowImage;
				case ExtensionData.NEW:
					return inewImage;
				case ExtensionData.CALLOUT:
					return icalloutImage;
				}
				return extensionImage;
			}
			if (obj instanceof IntroTheme)
				return themeImage;
			return null;
		}

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			if (columnIndex == 0)
				return getImage(element);
			return null;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			if (columnIndex == 1 || element instanceof IntroTheme || element instanceof RootPage)
				return getText(element);
			return null;
		}
	}

	static class IntroTheme {
		IConfigurationElement element;
		Image previewImage;

		public String getName() {
			return element.getAttribute("name"); //$NON-NLS-1$
		}

		public String getId() {
			return element.getAttribute("id"); //$NON-NLS-1$
		}

		public boolean isScalable() {
			return "true".equals(element.getAttribute(FontSelection.ATT_SCALABLE)); //$NON-NLS-1$
		}

		public IntroTheme(IConfigurationElement element) {
			this.element = element;
		}

		public Image getPreviewImage() {
			if (previewImage==null) {
				String path = element.getAttribute("previewImage"); //$NON-NLS-1$
				if (path!=null) {
					String bid = element.getDeclaringExtension().getContributor().getName();
					Bundle bundle = Platform.getBundle(bid);
					if (bundle!=null) {
						ImageDescriptor desc = ImageUtil.createImageDescriptor(bundle, path);
						previewImage = desc.createImage();
					}
				}
			}
			return previewImage;
		}

		public void dispose() {
			if (previewImage!=null) {
				previewImage.dispose();
				previewImage=null;
			}
		}
	}

	protected Control createContents(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridData data = new GridData();
		data.verticalAlignment = GridData.FILL;
		data.horizontalAlignment = GridData.FILL;
		container.setLayoutData(data);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		container.setLayout(layout);
		tabFolder = new CTabFolder(container, SWT.BORDER);
		tabFolder.setLayoutData(new GridData(GridData.FILL_BOTH));
		tabFolder.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				CTabItem selection = tabFolder.getSelection();
				onTabChange(selection);
			}
		});
		useRelativeFonts = new Button(container, SWT.CHECK);
		useRelativeFonts.setText(Messages.WelcomeCustomizationPreferencePage_useRelative);
		applyToAll = new Button(container, SWT.CHECK);
		applyToAll.setText(Messages.WelcomeCustomizationPreferencePage_applyToAll);
		contentProvider = new TableContentProvider();
		labelProvider = new TableLabelProvider();
		if (isCustomizationMode()) {
			Button serialize = new Button(container, SWT.PUSH);
			serialize.setText(Messages.WelcomeCustomizationPreferencePage_serialize);
			serialize.addSelectionListener(new SelectionAdapter() {

				@Override
				public void widgetSelected(SelectionEvent e) {
					doSerializeState();
				}
			});
		}
		extensionImage = ImageUtil.createImage("full/obj16/extension_obj.png"); //$NON-NLS-1$
		ihighImage = ImageUtil.createImage("full/obj16/ihigh_obj.png"); //$NON-NLS-1$
		ilowImage = ImageUtil.createImage("full/obj16/ilow_obj.png"); //$NON-NLS-1$
		inewImage = ImageUtil.createImage("full/obj16/inew_obj.png"); //$NON-NLS-1$
		icalloutImage = ImageUtil.createImage("full/obj16/icallout_obj.png"); //$NON-NLS-1$
		themeImage = ImageUtil.createImage("welcome16.png"); //$NON-NLS-1$
		addPages();
		org.eclipse.jface.dialogs.Dialog.applyDialogFont(container);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, "org.eclipse.ui.intro.universal.universalWelcomePreference"); //$NON-NLS-1$
		return container;
	}

	private void doSerializeState() {
		FileDialog fd = new FileDialog(shell, SWT.SAVE);
		fd.setText(Messages.WelcomeCustomizationPreferencePage_serializeTitle);
		String fileName = fd.open();
		if (fileName != null) {
			try {
				FileWriter writer = new FileWriter(fileName);
				PrintWriter pwriter = new PrintWriter(writer);
				introData.write(pwriter);
				pwriter.close();
			} catch (IOException e) {
				// show an error dialog in addition
				Log.error("Error while saving the intro data file", e); //$NON-NLS-1$
			}
		}
	}

	private boolean isCustomizationMode() {
		/*
		 * String[] args = Platform.getApplicationArgs(); for (int i = 0; i < args.length; i++) { if
		 * (args[i].equalsIgnoreCase("-welcomeCustomization")) //$NON-NLS-1$ return true; } return
		 * false;
		 */
		return true;
	}

	public void init(IWorkbench workbench) {
	}

	private void addPages() {
		loadData(false);
		addHomePage();
		createPageContainer();
		addRootPages();
		updateWidgetsFromData();
		selectFirstPage();
	}

	private void addRootPages() {
		addPage(IUniversalIntroConstants.ID_OVERVIEW);
		addPage(IUniversalIntroConstants.ID_FIRSTSTEPS);
		addPage(IUniversalIntroConstants.ID_TUTORIALS);
		addPage(IUniversalIntroConstants.ID_SAMPLES);
		addPage(IUniversalIntroConstants.ID_WHATSNEW);
		addPage(IUniversalIntroConstants.ID_WEBRESOURCES);
		addPage(IUniversalIntroConstants.ID_MIGRATE);
	}

	private void createPageContainer() {
		pageContainer = new Composite(tabFolder, SWT.NULL);
		GridLayout layout = new GridLayout();
		//layout.horizontalSpacing = 10;
		pageContainer.setLayout(layout);
		layout.numColumns = 4;
		//layout.makeColumnsEqualWidth = true;
		Label label;
		label = new Label(pageContainer, SWT.WRAP);
		label.setText(Messages.WelcomeCustomizationPreferencePage_pageDesc);
		GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan = 4;
		label.setLayoutData(gd);
		label = new Label(pageContainer, SWT.SEPARATOR | SWT.HORIZONTAL);
		gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan = 4;
		label.setLayoutData(gd);
		label = new Label(pageContainer, SWT.NULL);
		label.setText(Messages.WelcomeCustomizationPreferencePage_available);
		gd = new GridData();
		label.setLayoutData(gd);
		label = new Label(pageContainer, SWT.SEPARATOR | SWT.VERTICAL);
		gd = new GridData(GridData.VERTICAL_ALIGN_FILL);
		gd.verticalSpan = 3;
		gd.widthHint = 10;
		label.setLayoutData(gd);

		label = new Label(pageContainer, SWT.NULL);
		label.setText(Messages.WelcomeCustomizationPreferencePage_left);
		label = new Label(pageContainer, SWT.NULL);
		label.setText(Messages.WelcomeCustomizationPreferencePage_right);
		available = createTableViewer(pageContainer, "hidden"); //$NON-NLS-1$
		available.setComparator(new ViewerComparator());
		gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL);
		gd.verticalSpan = 2;
		gd.widthHint = 100;
		//gd.horizontalSpan = 2;
		available.getControl().setLayoutData(gd);

		topLeft = createTableViewer(pageContainer, "top-left"); //$NON-NLS-1$
		gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = 100;
		topLeft.getControl().setLayoutData(gd);

		topRight = createTableViewer(pageContainer, "top-right"); //$NON-NLS-1$
		gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = 100;
		topRight.getControl().setLayoutData(gd);

		//label = new Label(pageContainer, SWT.NULL);
		//label.setText(Messages.WelcomeCustomizationPreferencePage_bottom);
		bottomLeft = createTableViewer(pageContainer, "bottom-left"); //$NON-NLS-1$
		gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = 100;
		bottomLeft.getControl().setLayoutData(gd);

		bottomRight = createTableViewer(pageContainer, "bottom-right"); //$NON-NLS-1$
		gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = 100;
		bottomRight.getControl().setLayoutData(gd);
	}

	private void updatePageContainer(String pageId, PageData pd) {
		if (pageId == null)
			return;
		refreshQuadrant(topLeft, pd, IUniversalIntroConstants.DIV_LAYOUT_TOP_LEFT);
		refreshQuadrant(topRight, pd, IUniversalIntroConstants.DIV_LAYOUT_TOP_RIGHT);
		refreshQuadrant(bottomLeft, pd, IUniversalIntroConstants.DIV_LAYOUT_BOTTOM_LEFT);
		refreshQuadrant(bottomRight, pd, IUniversalIntroConstants.DIV_LAYOUT_BOTTOM_RIGHT);
		refreshQuadrant(available, pd, IUniversalIntroConstants.HIDDEN);
	}

	private void refreshQuadrant(TableViewer viewer, PageData pd, String quadrant) {
		GroupData gd = pd!=null?pd.findGroup(quadrant):null;
		viewer.setInput(gd);
		if (gd!=null)
			updateColumnSizes(viewer);
	}

	private void onTabChange(CTabItem item) {
		String id = (String) item.getData();
		if (item.getControl() == pageContainer)
			updatePageContainer(id, (PageData) item.getData("pageData")); //$NON-NLS-1$
	}

	private void loadData(boolean fromDefault) {
		IProduct product = Platform.getProduct();
		if (product == null)
			return;
		String pid = product.getId();
		introRootPages.clear();
		// 1. Root pages
		// try product-qualified value first
		String rootPages = getIntroPreference(INTRO_ROOT_PAGES, fromDefault, pid, UniversalIntroPlugin.PLUGIN_ID);
		if (rootPages.length() > 0) {
			StringTokenizer stok = new StringTokenizer(rootPages, ","); //$NON-NLS-1$
			while (stok.hasMoreTokens()) {
				String tok = stok.nextToken().trim();
				if (!NO_ROOT_PAGES.equals(tok)) {
					introRootPages.add(tok);
				}
			}
		}
		// 2. Font Style
		String fontStyle = FontSelection.getFontStyle();
		useRelativeFonts.setSelection(FontSelection.FONT_RELATIVE.equals(fontStyle));
		// 3. Active theme
		String value = getIntroPreference(INTRO_THEME, fromDefault, pid, IntroPlugin.PLUGIN_ID);
		if (value.length() > 0)
			introThemeId = value;
		// 4. Intro data
		value = getIntroPreference(INTRO_DATA, fromDefault, pid, UniversalIntroPlugin.PLUGIN_ID);
		if (value.length() == 0)
			value = null;
		if (value != null && value.startsWith("product:")) //$NON-NLS-1$
			value = value.substring(8);
		value = BundleUtil.getResolvedResourceLocation(value, product.getDefiningBundle());
		introData = new IntroData(pid, value, true);
		introData.addImplicitContent();
	}

	private String getIntroPreference(String key, boolean fromDefault,
			String pid, String pluginId) {
		IEclipsePreferences prefs;
		String pidKey = pid + "_" + key; //$NON-NLS-1$
		String value;
		if (!fromDefault) {
			prefs = InstanceScope.INSTANCE.getNode(pluginId);
			value = getPreference(key, prefs, pidKey, null);
			if (value != null) {
				return value;
			}
		}
		prefs = DefaultScope.INSTANCE.getNode(pluginId);
		return getPreference(key, prefs, pidKey, ""); //$NON-NLS-1$
	}

	private String getPreference(String key, IEclipsePreferences prefs,
			String pidKey, String defaultValue) {
		String value = prefs.get(pidKey, null);
		if (value == null) {
			value = prefs.get(key, defaultValue);
		}
		return value;
	}

	public void dispose() {
		Iterator<IntroTheme> iter = themeList.iterator();
		while (iter.hasNext()) {
			iter.next().dispose();
		}
		extensionImage.dispose();
		ihighImage.dispose();
		ilowImage.dispose();
		inewImage.dispose();
		icalloutImage.dispose();
		themeImage.dispose();
	}

	private void updateIntroThemeFromData() {
		if (introThemeId != null) {
			for (int i = 0; i < themeList.size(); i++) {
				IntroTheme theme = themeList.get(i);
				if (theme.getId().equals(introThemeId)) {
					introTheme = theme;
					break;
				}
			}
		}
		updateThemePreview();
	}

	private void updateWidgetsFromData() {
		// sync up intro background part
		updateIntroThemeFromData();
		enableFontsButton();
		// sync up the root page checklist
		rootPages.setInput(ROOT_PAGE_TABLE);
		ArrayList<RootPage> selected = new ArrayList<>();
		for (int i = 0; i < ROOT_PAGE_TABLE.length; i++) {
			String id = ROOT_PAGE_TABLE[i].id;
			if (introRootPages.contains(id))
				selected.add(ROOT_PAGE_TABLE[i]);
		}
		rootPages.setCheckedElements(selected.toArray());
	}

	private void enableFontsButton() {
		if (introTheme != null) {
			useRelativeFonts.setEnabled(introTheme.isScalable());
		}
	}

	private void updateThemePreview() {
		themes.setInput(themes);
		if (introTheme != null)
			themes.setSelection(new StructuredSelection(introTheme), true);
		themePreview.redraw();
	}

	public boolean performOk() {
		saveData();
		BusyIndicator.showWhile(shell.getDisplay(), this::restartIntro);
		return true;
	}

	/**
	 * Remember the current page, close intro, reopen it and show the saved page.
	 *
	 */
	private void restartIntro() {
		IIntroManager manager = PlatformUI.getWorkbench().getIntroManager();
		IIntroPart part = manager.getIntro();
		if (part != null && part instanceof CustomizableIntroPart) {
			IntroModelRoot modelRoot = IntroPlugin.getDefault().getIntroModelRoot();
			String currentPageId = modelRoot.getCurrentPageId();
			IWorkbenchWindow window = part.getIntroSite().getWorkbenchWindow();
			boolean standby = manager.isIntroStandby(part);
			PlatformUI.getWorkbench().getIntroManager().closeIntro(part);
			IntroPlugin.getDefault().resetVolatileImageRegistry();
			UniversalIntroPlugin.getDefault().resetVolatileImageRegistry();
			part = PlatformUI.getWorkbench().getIntroManager().showIntro(window, standby);
			if (part != null  && !standby) {
				StringBuilder url = new StringBuilder();
				url.append("http://org.eclipse.ui.intro/showPage?id="); //$NON-NLS-1$
				url.append(currentPageId);
				IIntroURL introURL = IntroURLFactory.createIntroURL(url.toString());
				if (introURL != null)
					introURL.execute();
			}
		}
	}

	protected void performDefaults() {
		loadData(true);
		// Dispose all the root page tabs
		CTabItem[] items = tabFolder.getItems();
		for (int i = 0; i < items.length; i++) {
			if (items[i].getData("pageData") != null) //$NON-NLS-1$
				items[i].dispose();
		}
		// Add them back in based on the checked state
		addRootPages();
		updateWidgetsFromData();
		// Get the items again
		items = tabFolder.getItems();
		// Select root
		onTabChange(items[0]);
	}

	private void saveData() {
		IEclipsePreferences iprefs = InstanceScope.INSTANCE.getNode(IntroPlugin.PLUGIN_ID);
		IEclipsePreferences uprefs = InstanceScope.INSTANCE.getNode(UniversalIntroPlugin.PLUGIN_ID);
		boolean toAll = applyToAll.getSelection();
		IProduct product = Platform.getProduct();
		if (product == null)
			return;
		String pid = product.getId();
		StringBuilder sbuf = new StringBuilder();
		if (introRootPages.isEmpty()) {
			// An empty string means no preference so special value needed
			// to indicate no root pages.
			sbuf.append(NO_ROOT_PAGES);
		}
		for (int i = 0; i < introRootPages.size(); i++) {
			if (i > 0)
				sbuf.append(","); //$NON-NLS-1$
			sbuf.append(introRootPages.get(i));
		}
		String key = pid + "_" + INTRO_ROOT_PAGES; //$NON-NLS-1$
		uprefs.put(key, sbuf.toString());
		if (toAll) {
			key = INTRO_ROOT_PAGES;
			uprefs.put(key, sbuf.toString());
		}
		// Store font style
		key = pid + "_" + FontSelection.VAR_FONT_STYLE; //$NON-NLS-1$
		String fontStyle = useRelativeFonts.getSelection() ? FontSelection.FONT_RELATIVE :
			FontSelection.FONT_ABSOLUTE;
		if (fontStyle.equals(FontSelection.FONT_ABSOLUTE)) {
			// reset font scaling for next time relative is selected
			FontSelection.resetScalePercentage();
		}
		iprefs.put(key, fontStyle);
		if (toAll) {
			key = FontSelection.VAR_FONT_STYLE;
			iprefs.put(key, fontStyle);
		}
		// store page layouts
		StringWriter writer = new StringWriter();
		PrintWriter pwriter = new PrintWriter(writer);
		introData.write(pwriter);
		pwriter.close();
		String value = writer.toString();
		key = pid + "_" + INTRO_DATA; //$NON-NLS-1$
		uprefs.put(key, value);
		if (toAll) {
			key = INTRO_DATA;
			uprefs.put(key, value);
		}
		if (introTheme != null) {
			key = pid + "_" + INTRO_THEME; //$NON-NLS-1$
			value = introTheme.getId();
			iprefs.put(key, value);
		}
		if (toAll) {
			key = INTRO_THEME;
			iprefs.put(key, value);
		}
		try {
			uprefs.flush();
			iprefs.flush();
		} catch (BackingStoreException e) {
			e.printStackTrace();
		}
	}

	private void addHomePage() {
		CTabItem item = new CTabItem(tabFolder, SWT.NULL);
		item.setText(Messages.WelcomeCustomizationPreferencePage_home);
		Composite container = new Composite(tabFolder, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		container.setLayout(layout);
		Composite leftColumn = new Composite(container, SWT.NULL);
		layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 0;
		leftColumn.setLayout(layout);
		leftColumn.setLayoutData(new GridData(GridData.FILL_BOTH));
		Composite rightColumn = new Composite(container, SWT.NULL);
		layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 0;
		rightColumn.setLayout(layout);
		rightColumn.setLayoutData(new GridData(GridData.FILL_BOTH));

		Label themeLabel = new Label(leftColumn, SWT.NULL);
		themeLabel.setText(Messages.WelcomeCustomizationPreferencePage_background);
		GridData gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
		themeLabel.setLayoutData(gd);
		themes = new TableViewer(leftColumn, SWT.BORDER);
		themes.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
		themes.setContentProvider(contentProvider);
		themes.setLabelProvider(labelProvider);
		themes.addSelectionChangedListener(e -> {
			Object sel = ((StructuredSelection) e.getSelection()).getFirstElement();
			introTheme = (IntroTheme) sel;
			themePreview.redraw();
			enableFontsButton();
		});
		loadThemes();
		Label previewLabel = new Label(rightColumn, SWT.NULL);
		previewLabel.setText(Messages.WelcomeCustomizationPreferencePage_preview);
		themePreview = new Canvas(rightColumn, SWT.BORDER);
		gd = new GridData();
		gd.widthHint = 160+20;
		gd.heightHint = 120+20;
		themePreview.setLayoutData(gd);
		themePreview.addPaintListener(e -> {
			if (introTheme == null)
				return;
			Image bgImage = introTheme.getPreviewImage();
			if (bgImage == null)
				return;
			//Rectangle carea = themePreview.getClientArea();
			Rectangle ibounds = bgImage.getBounds();
			e.gc.drawImage(bgImage, 0, 0, ibounds.width, ibounds.height, 10, 10, 160, 120);
		});
		Label label = new Label(container, SWT.NULL);
		label.setText(Messages.WelcomeCustomizationPreferencePage_rootpages);
		gd = new GridData();
		gd.horizontalSpan = 2;
		label.setLayoutData(gd);
		rootPages = CheckboxTableViewer.newCheckList(container, SWT.BORDER);
		rootPages.setContentProvider(contentProvider);
		rootPages.setLabelProvider(labelProvider);
		// Filter empty pages
		ViewerFilter[] rootPageFilters = new ViewerFilter[] {
			new ViewerFilter() {
				@Override
				public boolean select(Viewer viewer, Object parentElement,
						Object element) {
					if (element instanceof RootPage) {
						RootPage rootPageElement = (RootPage) element;
						String rootPageId = rootPageElement.getId();
						return (introData.getPage(rootPageId) != null);
					}
					return false;
				}
			}
		};
		rootPages.setFilters(rootPageFilters);
		gd = new GridData(GridData.FILL_HORIZONTAL | GridData.FILL_VERTICAL);
		gd.horizontalSpan = 2;
		rootPages.getControl().setLayoutData(gd);
		rootPages.addCheckStateListener(event -> {
			RootPage page = (RootPage) event.getElement();
			boolean checked = event.getChecked();
			onPageChecked(page.id, checked);
		});
		item.setControl(container);
	}

	private void loadThemes() {
		IConfigurationElement [] elements = Platform.getExtensionRegistry().getConfigurationElementsFor("org.eclipse.ui.intro.configExtension"); //$NON-NLS-1$
		for (int i=0; i<elements.length; i++) {
			if (elements[i].getName().equals("theme")) { //$NON-NLS-1$
				themeList.add(new IntroTheme(elements[i]));
			}
		}
	}

	private void addPage(String id) {
		if (!getRootPageSelected(id))
			return;
		CTabItem item = new CTabItem(tabFolder, SWT.NULL);
		item.setText(getRootPageName(id));
		item.setControl(pageContainer);
		item.setData(id);
		PageData pd = introData.getPage(id);
		item.setData("pageData", pd); //$NON-NLS-1$
	}

	private void onPageChecked(String id, boolean checked) {
		CTabItem[] items = tabFolder.getItems();
		if (checked) {
			for (int i = 0; i < items.length; i++) {
				CTabItem item = items[i];
				if (item.getData() != null)
					item.dispose();
			}
			introRootPages.add(id);
			addRootPages();
		} else {
			for (int i = 0; i < items.length; i++) {
				CTabItem item = items[i];
				String itemId = (String) item.getData();
				if (itemId != null && itemId.equals(id)) {
					item.dispose();
					introRootPages.remove(id);
					return;
				}
			}
		}
	}

	private String getRootPageName(String id) {
		for (int i = 0; i < ROOT_PAGE_TABLE.length; i++) {
			if (ROOT_PAGE_TABLE[i].id.equals(id))
				return ROOT_PAGE_TABLE[i].getName();
		}
		return "?"; //$NON-NLS-1$
	}

	private boolean getRootPageSelected(String id) {
		for (int i = 0; i < introRootPages.size(); i++) {
			String cid = introRootPages.get(i);
			if (cid.equals(id))
				return true;
		}
		return false;
	}

	public void setInitializationData(IConfigurationElement config, String propertyName, Object data)
			throws CoreException {
	}

	private void createPopupMenu(final TableViewer viewer) {
		MenuManager manager = new MenuManager();
		manager.createContextMenu(viewer.getControl());
		viewer.getControl().setMenu(manager.getMenu());
		manager.setRemoveAllWhenShown(true);
		manager.addMenuListener(manager1 -> fillPopupMenu(manager1, viewer));
	}

	private void addDNDSupport(TableViewer viewer) {
		viewer.addDragSupport(DND.DROP_MOVE, TRANSFER_TYPES, new TableDragSourceListener(viewer));
		viewer.addDropSupport(DND.DROP_MOVE, TRANSFER_TYPES, new TableDropTargetListener(viewer));
	}

	private TableViewer createTableViewer(Composite parent, String id) {
		final Table table = new Table(parent, SWT.BORDER | SWT.FULL_SELECTION);
		final TableColumn column1 = new TableColumn(table, SWT.NULL);
		column1.setMoveable(false);
		column1.setWidth(20);
		column1.setResizable(false);
		final TableColumn column2 = new TableColumn(table, SWT.NULL);
		column2.setResizable(true);
		final TableViewer viewer = new TableViewer(table);
		CellEditor[] editors = new CellEditor[2];
		editors[0] = new ComboBoxCellEditor(table, ExtensionData.IMPORTANCE_NAME_TABLE, SWT.READ_ONLY);
		viewer.setCellEditors(editors);
		viewer.setColumnProperties(new String[] { IUniversalIntroConstants.P_IMPORTANCE,
				IUniversalIntroConstants.P_NAME });
		viewer.setCellModifier(new ICellModifier() {

			@Override
			public boolean canModify(Object element, String property) {
				return property.equals(IUniversalIntroConstants.P_IMPORTANCE);
			}

			@Override
			public Object getValue(Object element, String property) {
				if (element instanceof ExtensionData) {
					ExtensionData ed = (ExtensionData) element;
					if (property.equals(IUniversalIntroConstants.P_IMPORTANCE))
						return Integer.valueOf(ed.getImportance());
				}
				return null;
			}

			@Override
			public void modify(Object element, String property, Object value) {
				Integer ivalue = (Integer) value;
				TableItem item = (TableItem) element;
				ExtensionData ed = (ExtensionData) item.getData();
				ed.setImportance(ivalue.intValue());
				viewer.update(ed, new String [] {IUniversalIntroConstants.P_IMPORTANCE});
			}
		});
		viewer.setContentProvider(contentProvider);
		viewer.setLabelProvider(labelProvider);
		viewer.setData("id", id); //$NON-NLS-1$
		createPopupMenu(viewer);
		addDNDSupport(viewer);
		return viewer;
	}

	private void fillPopupMenu(IMenuManager manager, final TableViewer viewer) {
		StructuredSelection ssel = (StructuredSelection) viewer.getSelection();

		manager.add(new Separator());
		Action addSeparator = new Action(Messages.WelcomeCustomizationPreferencePage_addSeparator) {
			@Override
			public void run() {
				doAddSeparator(viewer);
			}
		};

		manager.add(addSeparator);
		manager.add(new Separator());

		if (ssel.size() == 1 && viewer != available) {
			Action upAction = new Action(Messages.WelcomeCustomizationPreferencePage_up) {

				@Override
				public void run() {
					doMove(viewer, true);
				}
			};
			Action downAction = new Action(Messages.WelcomeCustomizationPreferencePage_down) {

				@Override
				public void run() {
					doMove(viewer, false);
				}
			};
			BaseData ed = (BaseData) ssel.getFirstElement();
			GroupData gd = (GroupData) viewer.getInput();
			upAction.setEnabled(gd.canMoveUp(ed));
			downAction.setEnabled(gd.canMoveDown(ed));
			manager.add(upAction);
			manager.add(downAction);
		}
		if (ssel.size() > 0) {
			manager.add(new Separator());
			MenuManager menu = new MenuManager(Messages.WelcomeCustomizationPreferencePage_moveTo);
			addMoveToAction(menu, available, viewer,
					Messages.WelcomeCustomizationPreferencePage_menu_available);
			addMoveToAction(menu, topLeft, viewer, Messages.WelcomeCustomizationPreferencePage_menu_top_left);
			addMoveToAction(menu, topRight, viewer, Messages.WelcomeCustomizationPreferencePage_menu_top_right);
			addMoveToAction(menu, bottomLeft, viewer, Messages.WelcomeCustomizationPreferencePage_menu_bottom_left);
			addMoveToAction(menu, bottomRight, viewer, Messages.WelcomeCustomizationPreferencePage_menu_bottom_right);
			manager.add(menu);

			boolean addDeleteSeparator=false;

			for (Iterator<?> iter=ssel.iterator(); iter.hasNext();) {
				Object obj = iter.next();
				if (obj instanceof SeparatorData)
					addDeleteSeparator=true;
				else {
					addDeleteSeparator=false;
					break;
				}
			}
			if (addDeleteSeparator) {
				Action deleteSeparator = new Action(Messages.WelcomeCustomizationPreferencePage_removeSeparator) {
					@Override
					public void run() {
						doRemoveSeparators(viewer);
					}
				};
				manager.add(deleteSeparator);
			}
		}
	}

	private void addMoveToAction(MenuManager menu, final TableViewer target, final TableViewer source,
			String name) {
		if (source == target)
			return;
		Action action = new Action(name) {

			@Override
			public void run() {
				doMoveTo(source, target);
			}
		};
		menu.add(action);
	}

	private void doMove(Viewer viewer, boolean up) {
		Object obj = ((StructuredSelection) viewer.getSelection()).getFirstElement();
		GroupData gd = (GroupData) viewer.getInput();
		if (up)
			gd.moveUp((BaseData) obj);
		else
			gd.moveDown((BaseData) obj);
		viewer.refresh();
	}

	private void doAddSeparator(Viewer viewer) {
		Object obj = ((StructuredSelection) viewer.getSelection()).getFirstElement();
		GroupData gd = (GroupData) viewer.getInput();
		if (gd == null) {
			gd = createTargetGd(viewer);
			viewer.setInput(gd);
		}
		gd.addSeparator((BaseData)obj);
		viewer.refresh();
		updateColumnSizes((TableViewer)viewer);
	}

	private void doRemoveSeparators(Viewer viewer) {
		StructuredSelection ssel = ((StructuredSelection) viewer.getSelection());
		GroupData gd = (GroupData) viewer.getInput();
		for (Iterator<SeparatorData> iter=ssel.iterator(); iter.hasNext();) {
			SeparatorData sdata = iter.next();
			gd.remove(sdata);
		}
		viewer.refresh();
	}

	private void doMoveTo(TableViewer source, TableViewer target) {
		Object[] selObjs = ((StructuredSelection) source.getSelection()).toArray();
		GroupData sourceGd = (GroupData) source.getInput();
		GroupData targetGd = (GroupData) target.getInput();
		if (targetGd == null) {
			targetGd = createTargetGd(target);
		}
		for (int i = 0; i < selObjs.length; i++) {
			BaseData ed = (BaseData) selObjs[i];
			sourceGd.remove(ed);
			targetGd.add(ed);
		}
		source.refresh();
		updateColumnSizes(source);
		if (target.getInput() != null)
			target.refresh();
		else
			target.setInput(targetGd);
		updateColumnSizes(target);
	}

	private void updateColumnSizes(TableViewer viewer) {
		TableColumn sc = viewer.getTable().getColumn(1);
		sc.pack();
	}

	private GroupData createTargetGd(Viewer target) {
		GroupData targetGd = null;
		if (target == topLeft)
			targetGd = new GroupData(PageData.P_TOP_LEFT, false);
		else if (target == topRight)
			targetGd = new GroupData(PageData.P_TOP_RIGHT, false);
		else if (target == bottomLeft)
			targetGd = new GroupData(PageData.P_BOTTOM_LEFT, false);
		else if (target == bottomRight)
			targetGd = new GroupData(PageData.P_BOTTOM_RIGHT, false);
		else if (target == available)
			targetGd = new GroupData(IUniversalIntroConstants.HIDDEN, false);
		else
			return null;
		CTabItem item = tabFolder.getSelection();
		PageData pd = (PageData) item.getData("pageData"); //$NON-NLS-1$
		if (pd == null) {
			String pageId = (String)item.getData();
			pd = new PageData(pageId);
			item.setData("pageData", pd); //$NON-NLS-1$
			introRootPages.add(pageId);
		}
		pd.add(targetGd);
		return targetGd;
	}

	private void selectFirstPage() {
		if (firstPageId == null)
			return;
		CTabItem[] items = tabFolder.getItems();
		for (int i = 0; i < items.length; i++) {
			CTabItem item = items[i];
			PageData pd = (PageData) item.getData("pageData"); //$NON-NLS-1$
			if (pd != null && pd.getId().equals(firstPageId)) {
				tabFolder.setSelection(i);
				onTabChange(item);
				return;
			}
		}
	}

	public void setCurrentPage(String pageId) {
		firstPageId = pageId;
	}

	public void setShell(Shell shell) {
		this.shell = shell;
	}
}
