package org.eclipse.update.internal.ui.forms;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.net.URL;
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.resource.*;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.update.configuration.*;
import org.eclipse.update.core.*;
import org.eclipse.update.internal.ui.*;
import org.eclipse.update.internal.ui.model.*;
import org.eclipse.update.internal.ui.pages.UpdateFormPage;
import org.eclipse.update.internal.ui.parts.SWTUtil;
import org.eclipse.update.internal.ui.preferences.UpdateColors;
import org.eclipse.update.internal.ui.search.*;
import org.eclipse.update.internal.ui.views.DetailsView;
import org.eclipse.update.internal.ui.wizards.*;
import org.eclipse.update.ui.forms.internal.*;

public class DetailsForm extends PropertyWebForm {
	// NL keys

	private static final String KEY_PROVIDER = "FeaturePage.provider";
	private static final String KEY_VERSION = "FeaturePage.version";
	private static final String KEY_IVERSION = "FeaturePage.installedVersion";
	private static final String KEY_PENDING_VERSION =
		"FeaturePage.pendingVersion";
	private static final String KEY_SIZE = "FeaturePage.size";
	private static final String KEY_OS = "FeaturePage.os";
	private static final String KEY_WS = "FeaturePage.ws";
	private static final String KEY_NL = "FeaturePage.nl";
	private static final String KEY_ARCH = "FeaturePage.arch";
	private static final String KEY_PLATFORMS = "FeaturePage.platforms";
	private static final String KEY_DESC = "FeaturePage.description";
	private static final String KEY_INFO_LINK = "FeaturePage.infoLink";
	private static final String KEY_LICENSE_LINK = "FeaturePage.licenseLink";
	private static final String KEY_COPYRIGHT_LINK =
		"FeaturePage.copyrightLink";
	private static final String KEY_NOT_INSTALLED = "FeaturePage.notInstalled";
	private static final String KEY_SIZE_VALUE = "FeaturePage.sizeValue";
	private static final String KEY_UNKNOWN_SIZE_VALUE =
		"FeaturePage.unknownSizeValue";
	private static final String KEY_DO_UNCONFIGURE =
		"FeaturePage.doButton.unconfigure";
	private static final String KEY_DO_CONFIGURE =
		"FeaturePage.doButton.configure";
	private static final String KEY_DO_REPAIR = "FeaturePage.doButton.repair";
	private static final String KEY_DO_CHANGE = "FeaturePage.doButton.change";
	private static final String KEY_DO_UPDATE = "FeaturePage.doButton.update";
	private static final String KEY_DO_INSTALL = "FeaturePage.doButton.install";
	private static final String KEY_DO_UNINSTALL =
		"FeaturePage.doButton.uninstall";
	private static final String KEY_DIALOG_UTITLE = "FeaturePage.dialog.utitle";
	private static final String KEY_DIALOG_TITLE = "FeaturePage.dialog.title";
	private static final String KEY_DIALOG_CTITLE = "FeaturePage.dialog.ctitle";
	private static final String KEY_DIALOG_UCTITLE =
		"FeaturePage.dialog.uctitle";
	private static final String KEY_DIALOG_UMESSAGE =
		"FeaturePage.dialog.umessage";
	private static final String KEY_DIALOG_MESSAGE =
		"FeaturePage.dialog.message";
	private static final String KEY_DIALOG_CMESSAGE =
		"FeaturePage.dialog.cmessage";
	private static final String KEY_DIALOG_UCMESSAGE =
		"FeaturePage.dialog.ucmessage";
	private static final String KEY_MISSING_TITLE = "FeaturePage.missing.title";
	private static final String KEY_MISSING_MESSAGE =
		"FeaturePage.missing.message";
	private static final String KEY_MISSING_SEARCH =
		"FeaturePage.missing.search";
	private static final String KEY_MISSING_ABORT = "FeaturePage.missing.abort";
	private static final String KEY_SEARCH_OBJECT_NAME =
		"FeaturePage.missing.searchObjectName";
	private static final String KEY_OPTIONAL_INSTALL_MESSAGE = "FeaturePage.optionalInstall.message";
	private static final String KEY_OPTIONAL_INSTALL_TITLE = "FeaturePage.optionalInstall.title";
	//	
	private static final int REPAIR = 1;
	private static final int CHANGE = 2;
	private int reinstallCode = 0;

	private Label imageLabel;
	private Label providerLabel;
	private Label versionLabel;
	private Label installedVersionLabel;
	private Label sizeLabel;
	private Label osLabel;
	private Label wsLabel;
	private Label nlLabel;
	private Label archLabel;
	private Label descriptionText;
	private URL infoLinkURL;
	private SelectableFormLabel infoLinkLabel;
	private InfoLink licenseLink;
	private InfoLink copyrightLink;
	private ReflowGroup supportedPlatformsGroup;
	private Image providerImage;
	private Button uninstallButton;
	private Button doButton;
	private IFeature currentFeature;
	private IFeatureAdapter currentAdapter;
	private ModelListener modelListener;
	private Hashtable imageCache = new Hashtable();
	private HyperlinkHandler sectionHandler;
	private boolean alreadyInstalled;
	private IFeature[] installedFeatures;
	private boolean newerVersion;

	class ModelListener implements IUpdateModelChangedListener {
		/**
		 * @see IUpdateModelChangedListener#objectAdded(Object, Object)
		 */
		public void objectsAdded(Object parent, Object[] children) {
			if (isCurrentFeature(children)) {
				SWTUtil.getStandardDisplay().asyncExec(new Runnable() {
					public void run() {
						refresh();
					}
				});
			}
		}

		boolean isCurrentFeature(Object[] children) {
			for (int i = 0; i < children.length; i++) {
				Object obj = children[i];
				if (obj instanceof PendingChange) {
					PendingChange job = (PendingChange) obj;
					if (job.getFeature().equals(currentFeature)) {
						return true;
					}
				}
			}
			return false;
		}

		/**
		 * @see IUpdateModelChangedListener#objectRemoved(Object, Object)
		 */
		public void objectsRemoved(Object parent, Object[] children) {
			if (isCurrentFeature(children)) {
				SWTUtil.getStandardDisplay().asyncExec(new Runnable() {
					public void run() {
						doButton.setEnabled(true);
					}
				});
			}
		}

		/**
		 * @see IUpdateModelChangedListener#objectChanged(Object, String)
		 */
		public void objectChanged(Object object, String property) {
		}
	}

	abstract class LinkListener implements IHyperlinkListener {
		public abstract URL getURL();
		public void linkActivated(Control linkLabel) {
			URL url = getURL();
			if (url != null)
				openURL(url.toString());
		}
		public void linkEntered(Control linkLabel) {
			URL url = getURL();
			if (url != null)
				showStatus(url.toString());
		}
		public void linkExited(Control linkLabel) {
			showStatus(null);
		}

		private void showStatus(String text) {
			IViewSite site = getPage().getView().getViewSite();
			IStatusLineManager sm = site.getActionBars().getStatusLineManager();
			sm.setMessage(text);
		}
	}

	abstract class ReflowGroup extends ExpandableGroup {
		public void expanded() {
			reflow();
			updateSize();
		}
		public void collapsed() {
			reflow();
			updateSize();
		}
		protected SelectableFormLabel createTextLabel(
			Composite parent,
			FormWidgetFactory factory) {
			SelectableFormLabel label = super.createTextLabel(parent, factory);
			label.setFont(JFaceResources.getBannerFont());
			return label;
		}
		protected HyperlinkHandler getHyperlinkHandler(FormWidgetFactory factory) {
			return sectionHandler;
		}
	}

	public DetailsForm(UpdateFormPage page) {
		super(page);
		providerImage = UpdateUIPluginImages.DESC_PROVIDER.createImage();
		modelListener = new ModelListener();
		UpdateModel model = UpdateUIPlugin.getDefault().getUpdateModel();
		model.addUpdateModelChangedListener(modelListener);
		sectionHandler = new HyperlinkHandler();
	}

	public void dispose() {
		UpdateModel model = UpdateUIPlugin.getDefault().getUpdateModel();
		model.removeUpdateModelChangedListener(modelListener);
		providerImage.dispose();
		for (Enumeration enum = imageCache.elements();
			enum.hasMoreElements();
			) {
			Image image = (Image) enum.nextElement();
			image.dispose();
		}
		imageCache.clear();
		sectionHandler.dispose();
		super.dispose();
	}

	public void initialize(Object modelObject) {
		setHeadingText("");
		super.initialize(modelObject);
	}

	private void configureSectionHandler(
		FormWidgetFactory factory,
		Display display) {
		sectionHandler.setHyperlinkUnderlineMode(
			HyperlinkHandler.UNDERLINE_NEVER);
		sectionHandler.setBackground(factory.getBackgroundColor());
		sectionHandler.setForeground(UpdateColors.getTopicColor(display));
	}

	protected void updateHeadings() {
		sectionHandler.setForeground(
			UpdateColors.getTopicColor(getControl().getDisplay()));
		super.updateHeadings();
	}

	public void createContents(Composite container) {
		HTMLTableLayout layout = new HTMLTableLayout();
		layout.numColumns = 2;
		container.setLayout(layout);
		layout.rightMargin = 0;
		GridData gd;

		configureSectionHandler(factory, container.getDisplay());

		GridLayout glayout = new GridLayout();
		Composite properties = factory.createComposite(container);
		properties.setLayout(glayout);
		glayout.marginWidth = glayout.marginHeight = 0;
		glayout.verticalSpacing = 0;

		providerLabel =
			createProperty(
				properties,
				UpdateUIPlugin.getResourceString(KEY_PROVIDER));
		versionLabel =
			createProperty(
				properties,
				UpdateUIPlugin.getResourceString(KEY_VERSION));
		installedVersionLabel =
			createProperty(
				properties,
				UpdateUIPlugin.getResourceString(KEY_IVERSION));
		sizeLabel =
			createProperty(
				properties,
				UpdateUIPlugin.getResourceString(KEY_SIZE));
		supportedPlatformsGroup = new ReflowGroup() {
			public void fillExpansion(
				Composite expansion,
				FormWidgetFactory factory) {
				GridLayout layout = new GridLayout();
				expansion.setLayout(layout);
				layout.marginWidth = 0;
				osLabel =
					createProperty(
						expansion,
						UpdateUIPlugin.getResourceString(KEY_OS));
				wsLabel =
					createProperty(
						expansion,
						UpdateUIPlugin.getResourceString(KEY_WS));
				nlLabel =
					createProperty(
						expansion,
						UpdateUIPlugin.getResourceString(KEY_NL));
				archLabel =
					createProperty(
						expansion,
						UpdateUIPlugin.getResourceString(KEY_ARCH));
			}
		};
		supportedPlatformsGroup.setText(
			UpdateUIPlugin.getResourceString(KEY_PLATFORMS));
		new Label(properties, SWT.NULL);
		supportedPlatformsGroup.createControl(properties, factory);
		setFocusControl(supportedPlatformsGroup.getControl());

		imageLabel = factory.createLabel(container, null);
		TableData td = new TableData();
		td.align = TableData.CENTER;
		//td.valign = TableData.MIDDLE;
		imageLabel.setLayoutData(td);

		Label label =
			createHeading(
				container,
				UpdateUIPlugin.getResourceString(KEY_DESC));
		td = new TableData();
		td.colspan = 2;
		label.setLayoutData(td);
		descriptionText = factory.createLabel(container, null, SWT.WRAP);
		td = new TableData();
		td.colspan = 2;
		td.grabHorizontal = true;
		descriptionText.setLayoutData(td);

		glayout = new GridLayout();
		glayout.numColumns = 5;
		glayout.horizontalSpacing = 20;
		glayout.marginWidth = 10;

		Composite l = factory.createCompositeSeparator(container);
		l.setBackground(factory.getBorderColor());
		td = new TableData();
		td.colspan = 2;
		td.heightHint = 1;
		td.align = TableData.FILL;
		l.setLayoutData(td);

		Composite footer = factory.createComposite(container);
		td = new TableData();
		td.colspan = 2;
		td.align = TableData.FILL;
		td.valign = TableData.FILL;
		footer.setLayoutData(td);
		footer.setLayout(glayout);

		LinkListener listener = new LinkListener() {
			public URL getURL() {
				return infoLinkURL;
			}
		};
		infoLinkLabel = new SelectableFormLabel(footer, SWT.NULL);
		infoLinkLabel.setText(UpdateUIPlugin.getResourceString(KEY_INFO_LINK));
		factory.turnIntoHyperlink(infoLinkLabel, listener);
		gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
		infoLinkLabel.setLayoutData(gd);
		licenseLink = new InfoLink((DetailsView) getPage().getView());
		licenseLink.setText(UpdateUIPlugin.getResourceString(KEY_LICENSE_LINK));
		licenseLink.createControl(footer, factory);
		gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
		licenseLink.getControl().setLayoutData(gd);
		copyrightLink = new InfoLink((DetailsView) getPage().getView());
		copyrightLink.setText(
			UpdateUIPlugin.getResourceString(KEY_COPYRIGHT_LINK));
		copyrightLink.createControl(footer, factory);
		gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
		copyrightLink.getControl().setLayoutData(gd);

		uninstallButton = factory.createButton(footer, "", SWT.PUSH);
		uninstallButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				doUninstall();
			}
		});
		gd =
			new GridData(
				GridData.HORIZONTAL_ALIGN_END
					| GridData.VERTICAL_ALIGN_BEGINNING);
		gd.grabExcessHorizontalSpace = true;
		uninstallButton.setVisible(false);
		uninstallButton.setText(
			UpdateUIPlugin.getResourceString(KEY_DO_UNINSTALL));
		uninstallButton.setLayoutData(gd);

		doButton = factory.createButton(footer, "", SWT.PUSH);
		doButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				doButtonSelected();
			}
		});
		gd =
			new GridData(
				GridData.HORIZONTAL_ALIGN_END
					| GridData.VERTICAL_ALIGN_BEGINNING);
		//gd.grabExcessHorizontalSpace = true;
		doButton.setLayoutData(gd);
		WorkbenchHelp.setHelp(container, "org.eclipse.update.ui.DetailsForm");

	}

	public void expandTo(final Object obj) {
		BusyIndicator.showWhile(getControl().getDisplay(), new Runnable() {
			public void run() {
				if (obj instanceof IFeature) {
					currentAdapter = null;
					currentFeature = (IFeature) obj;
					refresh();
				} else if (obj instanceof IFeatureAdapter) {
					IFeatureAdapter adapter = (IFeatureAdapter) obj;
					try {
						currentAdapter = adapter;
						currentFeature = adapter.getFeature();
					} catch (CoreException e) {
						//UpdateUIPlugin.logException(e);
						currentFeature =
							new MissingFeature(
								adapter.getSite(),
								adapter.getURL());
					} finally {
						refresh();
					}
				} else {
					currentFeature = null;
					currentAdapter = null;
					refresh();
				}
			}
		});
	}

	private String getInstalledVersion(IFeature feature) {
		alreadyInstalled = false;
		VersionedIdentifier vid = feature.getVersionedIdentifier();
		PluginVersionIdentifier version = vid.getVersion();
		newerVersion = installedFeatures.length > 0;

		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < installedFeatures.length; i++) {
			IFeature installedFeature = installedFeatures[i];
			VersionedIdentifier ivid =
				installedFeature.getVersionedIdentifier();
			if (buf.length() > 0)
				buf.append(", ");
			PluginVersionIdentifier iversion = ivid.getVersion();
			buf.append(iversion.toString());
			if (ivid.equals(vid)) {
				alreadyInstalled = true;
			} else {
				if (iversion.isGreaterOrEqualTo(version))
					newerVersion = false;
			}
		}
		if (buf.length() > 0) {
			String versionText = buf.toString();
			UpdateModel model = UpdateUIPlugin.getDefault().getUpdateModel();
			PendingChange change = model.findRelatedPendingChange(feature);
			if (change != null) {
				return UpdateUIPlugin.getFormattedMessage(
					KEY_PENDING_VERSION,
					versionText);
			} else
				return versionText;
		} else
			return null;
	}

	private void refresh() {
		IFeature feature = currentFeature;

		if (feature == null)
			return;

		installedFeatures = UpdateUIPlugin.getInstalledFeatures(feature);

		setHeadingText(feature.getLabel());
		providerLabel.setText(feature.getProvider());
		versionLabel.setText(
			feature.getVersionedIdentifier().getVersion().toString());
		String installedVersion = getInstalledVersion(feature);
		if (installedVersion == null)
			installedVersion =
				UpdateUIPlugin.getResourceString(KEY_NOT_INSTALLED);
		installedVersionLabel.setText(installedVersion);
		long size = feature.getDownloadSize();
		String format = null;
		if (size != -1) {
			String stext = Long.toString(size);
			String pattern = UpdateUIPlugin.getResourceString(KEY_SIZE_VALUE);
			format = UpdateUIPlugin.getFormattedMessage(pattern, stext);
		} else {
			format = UpdateUIPlugin.getResourceString(KEY_UNKNOWN_SIZE_VALUE);
		}
		sizeLabel.setText(format);
		if (feature.getDescription() != null)
			descriptionText.setText(feature.getDescription().getAnnotation());
		else
			descriptionText.setText("");
		Image logoImage = loadProviderImage(feature);
		if (logoImage == null)
			logoImage = providerImage;
		imageLabel.setImage(logoImage);
		infoLinkURL = null;
		if (feature.getDescription() != null)
			infoLinkURL = feature.getDescription().getURL();
		infoLinkLabel.setVisible(infoLinkURL != null);

		setOS(feature.getOS());
		setWS(feature.getWS());
		setNL(feature.getNL());
		setArch(feature.getArch());

		licenseLink.setInfo(feature.getLicense());
		copyrightLink.setInfo(feature.getCopyright());
		this.reinstallCode = 0;
		doButton.setVisible(getDoButtonVisibility());
		uninstallButton.setVisible(getUninstallButtonVisibility());
		if (doButton.isVisible())
			updateButtonText(newerVersion);
		reflow();
		updateSize();
		((Composite) getControl()).redraw();
	}

	private boolean getDoButtonVisibility() {
		if (currentFeature instanceof MissingFeature) {
			MissingFeature mf = (MissingFeature) currentFeature;
			if (mf.isOptional() && mf.getOriginatingSiteURL() != null)
				return true;
			else
				return false;
		}

		if (currentAdapter == null)
			return false;

		boolean localContext = currentAdapter instanceof IConfiguredSiteContext;

		if (currentAdapter.isIncluded()) {
			if (!localContext)
				return false;
			if (!currentAdapter.isOptional())
				return false;
		}
		
		if (localContext) {
			IConfiguredSiteContext context = (IConfiguredSiteContext)currentAdapter;
			if (!context.getInstallConfiguration().isCurrent())
				return false;
		}

		UpdateModel model = UpdateUIPlugin.getDefault().getUpdateModel();
		if (model.findRelatedPendingChange(currentFeature) != null)
			return false;

		if (localContext) {
			IConfiguredSiteContext context =
				(IConfiguredSiteContext) currentAdapter;
			if (!context.getInstallConfiguration().isCurrent())
				return false;
			else
				return true;
		} else {
			// found on a remote site
			// Cannot install feature without a license
			if (!UpdateModel.hasLicense(currentFeature))
				return false;
		}
		// Random site feature
		if (alreadyInstalled) {
			if (isBrokenFeatureUpdate()) {
				reinstallCode = REPAIR;
				return true;
			}
			if (isOptionalFeatureInstall()) {
				reinstallCode = CHANGE;
				return true;
			}
			return false;
		}
		// Not installed - check if there are other 
		// features with this ID that are installed
		// and that are newer than this one
		if (installedFeatures.length > 0 && !newerVersion)
			return false;
		return true;
	}

	private boolean isBrokenFeatureUpdate() {
		if (installedFeatures.length != 1)
			return false;
		IFeature installedFeature = installedFeatures[0];
		if (installedFeature
			.getVersionedIdentifier()
			.equals(currentFeature.getVersionedIdentifier())) {
			return isBroken(installedFeature);
		}
		return false;
	}

	private boolean isBroken(IFeature feature) {
		try {
			IStatus status =
				SiteManager.getLocalSite().getFeatureStatus(feature);
			if (status != null && status.getSeverity() == IStatus.ERROR)
				return true;
		} catch (CoreException e) {
		}
		return false;
	}

	private boolean isOptionalFeatureInstall() {
		return hasMissingOptionalFeatures(installedFeatures[0]);
	}

	private boolean hasMissingOptionalFeatures(IFeature feature) {
		try {
			IFeatureReference refs[] = feature.getIncludedFeatureReferences();
			for (int i = 0; i < refs.length; i++) {
				IFeatureReference ref = refs[i];

				try {
					IFeature child = ref.getFeature();

					// not missing - try children
					if (hasMissingOptionalFeatures(child))
						return true;
				} catch (CoreException e) {
					// missing - if optional, return true
					if (ref.isOptional())
						return true;
				}
			}
		} catch (CoreException e) {
			// problem with the feature itself
		}
		return false;
	}

	private boolean getUninstallButtonVisibility() {
		/*
		 * We will not allow uninstalls for now.
		if (currentFeature instanceof MissingFeature)
			return false;
		if (currentAdapter == null || currentAdapter.isIncluded())
			return false;
		UpdateModel model = UpdateUIPlugin.getDefault().getUpdateModel();
		if (model.isPending(currentFeature))
			return false;
		if (currentAdapter instanceof IConfiguredSiteContext) {
			boolean configured = isConfigured();
			return !configured;
		}
		*/
		return false;
	}

	private boolean isConfigured() {
		if (currentAdapter instanceof IConfiguredSiteContext) {
			IConfiguredSiteContext context =
				(IConfiguredSiteContext) currentAdapter;
			IConfiguredSite csite = context.getConfigurationSite();
			IFeatureReference fref =
				csite.getSite().getFeatureReference(currentFeature);
			IFeatureReference[] cfeatures = csite.getConfiguredFeatures();
			for (int i = 0; i < cfeatures.length; i++) {
				if (cfeatures[i].equals(fref))
					return true;
			}
		}
		return false;
	}

	private void updateButtonText(boolean update) {
		if (reinstallCode==REPAIR) {
			doButton.setText(
				UpdateUIPlugin.getResourceString(KEY_DO_REPAIR));
			return;
		}
		if (reinstallCode==CHANGE) {
			doButton.setText(
				UpdateUIPlugin.getResourceString(KEY_DO_CHANGE));
			return;
		}
		if (currentFeature instanceof MissingFeature) {
			MissingFeature mf = (MissingFeature) currentFeature;
			if (mf.isOptional() && mf.getOriginatingSiteURL() != null) {
				doButton.setText(
					UpdateUIPlugin.getResourceString(KEY_DO_INSTALL));
				return;
			}
		}
		if (currentAdapter instanceof IConfiguredSiteContext) {
			boolean configured = isConfigured();
			if (configured)
				doButton.setText(
					UpdateUIPlugin.getResourceString(KEY_DO_UNCONFIGURE));
			else
				doButton.setText(
					UpdateUIPlugin.getResourceString(KEY_DO_CONFIGURE));
		} else if (update && !alreadyInstalled) {
			doButton.setText(UpdateUIPlugin.getResourceString(KEY_DO_UPDATE));
		} else
			doButton.setText(UpdateUIPlugin.getResourceString(KEY_DO_INSTALL));
	}

	private Image loadProviderImage(IFeature feature) {
		Image image = null;
		URL imageURL = feature.getImage();
		if (imageURL == null)
			return null;
		// check table
		image = (Image) imageCache.get(imageURL);
		if (image == null) {
			ImageDescriptor id = ImageDescriptor.createFromURL(imageURL);
			try {
				image = id.createImage();
			} catch (SWTException e) {
				image = null;
			}
			if (image != null)
				imageCache.put(imageURL, image);
		}
		return image;
	}

	private void reflow() {
		versionLabel.getParent().layout(true);
		doButton.getParent().layout(true);
		imageLabel.getParent().layout(true);
		((Composite) getControl()).layout(true);
	}

	private void setOS(String os) {
		if (os == null)
			osLabel.setText("");
		else {
			String[] array = getTokens(os);
			StringBuffer buf = new StringBuffer();
			for (int i = 0; i < array.length; i++) {
				if (i > 0)
					buf.append("\n");
				buf.append(mapOS(array[i]));
			}
			osLabel.setText(buf.toString());
		}
	}

	private String mapOS(String key) {
		return key;
	}

	private String mapWS(String key) {
		return key;
	}

	private String mapArch(String key) {
		return key;
	}

	private String mapNL(String nl) {
		String language, country;

		int loc = nl.indexOf('_');
		if (loc != -1) {
			language = nl.substring(0, loc);
			country = nl.substring(loc + 1);
		} else {
			language = nl;
			country = "";
		}
		Locale locale = new Locale(language, country);
		return locale.getDisplayName();
	}

	private void setWS(String ws) {
		if (ws == null)
			wsLabel.setText("");
		else {
			String[] array = getTokens(ws);
			StringBuffer buf = new StringBuffer();
			for (int i = 0; i < array.length; i++) {
				if (i > 0)
					buf.append("\n");
				buf.append(mapWS(array[i]));
			}
			wsLabel.setText(buf.toString());
		}
	}

	private void setArch(String arch) {
		if (arch == null)
			archLabel.setText("");
		else {
			String[] array = getTokens(arch);
			StringBuffer buf = new StringBuffer();
			for (int i = 0; i < array.length; i++) {
				if (i > 0)
					buf.append("\n");
				buf.append(mapArch(array[i]));
			}
			archLabel.setText(buf.toString());
		}
	}

	private void setNL(String nl) {
		if (nl == null)
			nlLabel.setText("");
		else {
			String[] array = getTokens(nl);
			StringBuffer buf = new StringBuffer();
			for (int i = 0; i < array.length; i++) {
				if (i > 0)
					buf.append("\n");
				buf.append(mapNL(array[i]));
			}
			nlLabel.setText(buf.toString());
		}
	}

	private String[] getTokens(String source) {
		Vector result = new Vector();
		StringTokenizer stok = new StringTokenizer(source, ",");
		while (stok.hasMoreTokens()) {
			String tok = stok.nextToken();
			result.add(tok);
		}
		return (String[]) result.toArray(new String[result.size()]);
	}

	private void openURL(final String url) {
		BusyIndicator.showWhile(getControl().getDisplay(), new Runnable() {
			public void run() {
				DetailsView dv = (DetailsView) getPage().getView();
				dv.showURL(url);
			}
		});
	}

	private void doUninstall() {
		executeJob(PendingChange.UNINSTALL);
	}

	private void executeJob(int mode) {
		if (currentFeature != null) {
			if (mode == PendingChange.INSTALL) {
				if (testDependencies(currentFeature) == false)
					return;
			}
			PendingChange job = createPendingChange(mode);
			executeJob(job);
		}
	}

	private void executeOptionalInstall(MissingFeature mf) {
		// Locate remote site, find the optional feature
		// and install it
		final VersionedIdentifier vid = mf.getVersionedIdentifier();
		final URL siteURL = mf.getOriginatingSiteURL();
		final IFeature [] result = new IFeature[1];
		final CoreException [] exception = new CoreException [1];
		BusyIndicator.showWhile(getControl().getDisplay(), new Runnable() {
			public void run() {
				try {
					ISite site = SiteManager.getSite(siteURL);
					IFeatureReference [] refs = site.getFeatureReferences();
					result[0] = findFeature(vid, refs);
				} catch (CoreException e) {
					exception[0] = e;
				}
			}
		});
		IStatus status = null;
		if (exception[0]!=null) {
			// Show error dialog
			status = exception[0].getStatus();
		}
		else if (result[0]!=null) {
			IConfiguredSite targetSite = null;
			if (mf.getParent()!=null) {
				ISite psite = mf.getParent().getSite();
				targetSite = psite.getCurrentConfiguredSite();
			}
			PendingChange job = new PendingChange(result[0], targetSite);
			executeJob(job);
		}
		else {
			String message = UpdateUIPlugin.getFormattedMessage(KEY_OPTIONAL_INSTALL_MESSAGE, siteURL.toString());
			status = new Status(IStatus.ERROR, UpdateUIPlugin.PLUGIN_ID, IStatus.OK, message, null);
		}
		if (status!=null) {
			// Show error dialog
			ErrorDialog.openError(getControl().getShell(), UpdateUIPlugin.getResourceString(KEY_OPTIONAL_INSTALL_TITLE), null, status);
		}
	}
	
	private IFeature findFeature(VersionedIdentifier vid, IFeatureReference [] refs) {
		for (int i=0; i<refs.length; i++) {
			IFeatureReference ref = refs[i];
			try {
				VersionedIdentifier refVid = ref.getVersionedIdentifier();
				if (refVid.equals(vid)) {
					return ref.getFeature();
				}
				// Try children
				IFeature feature = ref.getFeature();
				IFeatureReference [] irefs = feature.getIncludedFeatureReferences();
				IFeature result = findFeature(vid, irefs);
				if (result!=null)
					return result;
			}
			catch (CoreException e) {
			}
		}
		return null;
	}

	private void executeJob(final PendingChange job) {
		IStatus validationStatus =
			ActivityConstraints.validatePendingChange(job);
		if (validationStatus != null) {
			ErrorDialog.openError(
				UpdateUIPlugin.getActiveWorkbenchShell(),
				null,
				null,
				validationStatus);
			return;
		}
		if (job.getJobType()==PendingChange.UNCONFIGURE &&
			UpdateUIPlugin.isPatch(job.getFeature())) {
			unconfigurePatch(job.getFeature());
			return;
		}
		BusyIndicator.showWhile(getControl().getDisplay(), new Runnable() {
			public void run() {
				InstallWizard wizard = new InstallWizard(job);
				WizardDialog dialog =
					new InstallWizardDialog(
						UpdateUIPlugin.getActiveWorkbenchShell(),
						wizard);
				dialog.create();
				dialog.getShell().setSize(600, 500);
				dialog.open();
				if (wizard.isSuccessfulInstall())
					UpdateUIPlugin.informRestartNeeded();
			}
		});
	}
	
	private void unconfigurePatch(IFeature feature) {
		IInstallConfiguration config = UpdateUIPlugin.getBackupConfigurationFor(feature);
		if (config==null) {
			String message = "This feature is a patch and cannot be directly disabled. Locate a configuration before it was installed and revert to it instead.";
			MessageDialog.openError(getControl().getShell(), "Disable Feature", message);
			return;
		}
		try {
			ILocalSite localSite = SiteManager.getLocalSite();
			boolean success = RevertSection.performRevert(config, false, false);
			if (success) {
				localSite.removeFromPreservedConfigurations(config);
				localSite.save();
				UpdateUIPlugin.informRestartNeeded();
			}
		}
		catch (CoreException e) {
			UpdateUIPlugin.logException(e);
		}
	}

	private void doButtonSelected() {
		if (currentFeature != null) {
			if (currentFeature instanceof MissingFeature) {
				MissingFeature mf = (MissingFeature) currentFeature;
				if (mf.isOptional()
					&& mf.getOriginatingSiteURL() != null) {
					executeOptionalInstall(mf);
					return;
				}
			}
			int mode;
			if (currentAdapter instanceof IConfiguredSiteContext) {
				boolean configured = isConfigured();
				if (configured)
					mode = PendingChange.UNCONFIGURE;
				else
					mode = PendingChange.CONFIGURE;
			} else {
				mode = PendingChange.INSTALL;
			}
			executeJob(mode);
		}
	}

	private boolean testDependencies(IFeature feature) {
		// NOTE: testing and searching for dependencies is disabled
		//       at this point. The code needs to correctly handle
		//       matching rules that can be specified on the dependencies.
		if (true)
			return true;

		IImport[] imports = feature.getImports();
		if (imports.length == 0)
			return true;
		ArrayList missing = new ArrayList();
		try {
			ILocalSite localSite = SiteManager.getLocalSite();
			IInstallConfiguration config = localSite.getCurrentConfiguration();
			IConfiguredSite[] configSites = config.getConfiguredSites();

			for (int i = 0; i < imports.length; i++) {
				if (!isOnTheList(imports[i], configSites)) {
					missing.add(imports[i]);
				}
			}
		} catch (CoreException e) {
			UpdateUIPlugin.logException(e);
			return false;
		}
		if (missing.size() > 0) {
			// show missing plug-in dialog and ask to search
			MessageDialog dialog =
				new MessageDialog(
					getControl().getShell(),
					UpdateUIPlugin.getResourceString(KEY_MISSING_TITLE),
					(Image) null,
					UpdateUIPlugin.getResourceString(KEY_MISSING_MESSAGE),
					MessageDialog.WARNING,
					new String[] {
						UpdateUIPlugin.getResourceString(KEY_MISSING_SEARCH),
						UpdateUIPlugin.getResourceString(KEY_MISSING_ABORT)},
					0);
			int result = dialog.open();
			if (result == 0)
				initiatePluginSearch(missing);
			return false;
		} else
			return true;
	}
	private boolean isOnTheList(
		IImport iimport,
		IConfiguredSite[] configSites) {
		for (int i = 0; i < configSites.length; i++) {
			IConfiguredSite csite = configSites[i];
			ISite site = csite.getSite();
			IPluginEntry[] entries = site.getPluginEntries();
			if (isOnTheList(iimport, entries))
				return true;
		}
		return false;
	}
	private boolean isOnTheList(IImport iimport, IPluginEntry[] entries) {
		VersionedIdentifier importId = iimport.getVersionedIdentifier();
		PluginVersionIdentifier version = importId.getVersion();
		boolean noVersion =
			version.getMajorComponent() == 0
				&& version.getMinorComponent() == 0
				&& version.getServiceComponent() == 0;
		for (int i = 0; i < entries.length; i++) {
			IPluginEntry entry = entries[i];
			VersionedIdentifier entryId = entry.getVersionedIdentifier();
			if (noVersion) {
				if (importId.getIdentifier().equals(entryId.getIdentifier()))
					return true;
			} else if (entryId.equals(importId))
				return true;
		}
		return false;
	}

	private void initiatePluginSearch(ArrayList missing) {
		SearchCategoryDescriptor desc =
			SearchCategoryRegistryReader.getDefault().getDescriptor(
				"org.eclipse.update.ui.plugins");
		if (desc == null)
			return;
		String name =
			UpdateUIPlugin.getFormattedMessage(
				KEY_SEARCH_OBJECT_NAME,
				currentFeature.getLabel());
		SearchObject search = new SearchObject(name, desc, true);
		search.setPersistent(false);
		search.setInstantSearch(true);
		search.setSearchBookmarks(true);
		search.setSearchDiscovery(true);
		String value = PluginsSearchCategory.encodeImports(missing);
		search.getSettings().put("imports", value);
		UpdateModel model = UpdateUIPlugin.getDefault().getUpdateModel();
		try {
			UpdateUIPlugin.getActivePage().showView(
				UpdatePerspective.ID_UPDATES);
		} catch (PartInitException e) {
		}
		model.addBookmark(search);
		try {
			UpdateUIPlugin.getActivePage().showView(
				UpdatePerspective.ID_DETAILS);
		} catch (PartInitException e) {
		}
	}

	private PendingChange createPendingChange(int type) {
		if (type == PendingChange.INSTALL && installedFeatures.length > 0) {
			return new PendingChange(
				installedFeatures[0],
				currentFeature,
				alreadyInstalled);
		} else {
			return new PendingChange(currentFeature, type);
		}
	}
}