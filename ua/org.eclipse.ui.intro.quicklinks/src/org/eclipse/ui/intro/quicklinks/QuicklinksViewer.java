/*******************************************************************************
 * Copyright (c) 2016 Manumitting Technologies Inc and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Manumitting Technologies Inc - initial API and implementation
 *******************************************************************************/
package org.eclipse.ui.intro.quicklinks;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.commands.CommandManager;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.core.commands.SerializationException;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandImageService;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.internal.intro.impl.model.AbstractIntroPartImplementation;
import org.eclipse.ui.internal.intro.impl.model.IntroTheme;
import org.eclipse.ui.internal.intro.impl.util.Log;
import org.eclipse.ui.internal.menus.MenuHelper;
import org.eclipse.ui.intro.config.IIntroContentProvider;
import org.eclipse.ui.intro.config.IIntroContentProviderSite;
import org.eclipse.ui.services.IServiceLocator;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 * An Intro content provider that populates a list of frequently-used commands
 * from an extension point. The appearance of these quicklinks is normally taken
 * from the command metadata, including the image icon, but can be tailored.
 * These tailorings can be made optional depending on the current theme.
 *
 * This implementation is still experimental and subject to change. Feedback
 * welcome as a <a href="http://eclip.se/9f">bug report on the Eclipse Bugzilla
 * against Platform/User Assistance</a>.
 */
@SuppressWarnings("restriction")
public class QuicklinksViewer implements IIntroContentProvider {

	/** Represents the importance of an element */
	enum Importance {
		HIGH("high", 0), MEDIUM("medium", 1), LOW("low", 2); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		String id;
		int level;

		Importance(String text, int importance) {
			this.id = text;
			this.level = importance;
		}

		public static Importance forId(String id) {
			for (Importance i : values()) {
				if (i.id.equals(id)) {
					return i;
				}
			}
			return LOW;
		}
	}

	/** Model holding the relevant attributes of a Quicklink element */
	static class Quicklink implements Comparable<Quicklink> {
		String commandSpec;
		String url;
		String label;
		String description;
		String iconUrl;
		Importance importance = Importance.MEDIUM;
		long rank;
		String resolution;

		public Quicklink() {
		}

		@Override
		public int compareTo(Quicklink b) {
			int impA = this.importance.level;
			int impB = b.importance.level;
			if (impA != impB) {
				return impA - impB;
			}
			long diff = this.rank - b.rank;
			if (diff > 0) {
				return 1;
			}
			if (diff < 0) {
				return -1;
			}
			return 0;
		}
	}

	/**
	 * Responsible for retrieving Quicklinks and applying any icon overrides
	 */
	class ModelReader implements Supplier<List<Quicklink>> {
		private static final String QL_EXT_PT = "org.eclipse.ui.intro.quicklinks"; //$NON-NLS-1$
		private static final String ELMT_COMMAND = "command"; //$NON-NLS-1$
		private static final String ATT_ID = "id"; //$NON-NLS-1$
		private static final String ELMT_URL = "url"; //$NON-NLS-1$
		private static final String ATT_LOCATION = "location"; //$NON-NLS-1$

		private static final String ELMT_OVERRIDE = "override"; //$NON-NLS-1$
		private static final String ATT_COMMANDID = "command"; //$NON-NLS-1$
		private static final String ATT_THEME = "theme"; //$NON-NLS-1$

		private static final String ATT_LABEL = "label"; //$NON-NLS-1$
		private static final String ATT_DESCRIPTION = "description"; //$NON-NLS-1$
		private static final String ATT_ICON = "icon"; //$NON-NLS-1$
		private static final String ATT_IMPORTANCE = "importance"; //$NON-NLS-1$
		private static final String ATT_RESOLUTION = "resolution"; //$NON-NLS-1$

		/** commandSpec/url &rarr; quicklink */
		private final Map<String, Quicklink> quicklinks = new LinkedHashMap<>();
		/** bundle symbolic name &rarr; bundle id */
		private Map<String, Long> bundleIds;
		private Bundle[] bundles;

		/**
		 * Return the list of configured {@link Quicklink} that can be found.
		 */
		@Override
		public List<Quicklink> get() {
			IExtension extensions[] = getExtensions(QL_EXT_PT);

			// Process definitions from the product bundle first
			Bundle productBundle = Platform.getProduct().getDefiningBundle();
			if(productBundle != null) {
				for (IExtension ext : extensions) {
					if (productBundle.getSymbolicName().equals(ext.getContributor().getName())) {
						for (IConfigurationElement ce : ext.getConfigurationElements()) {
							processDefinition(ce);
						}
					}
				}
			}

			for (IExtension ext : extensions) {
				if (productBundle == null || !productBundle.getSymbolicName().equals(ext.getContributor().getName())) {
					for (IConfigurationElement ce : ext.getConfigurationElements()) {
						processDefinition(ce);
					}
				}
			}

			// Now process all command overrides
			for (IExtension ext : extensions) {
				for (IConfigurationElement ce : ext.getConfigurationElements()) {
					if (!ELMT_OVERRIDE.equals(ce.getName())) {
						continue;
					}
					String theme = ce.getAttribute(ATT_THEME);
					String commandSpecPattern = ce.getAttribute(ATT_COMMANDID);
					String icon = ce.getAttribute(ATT_ICON);
					if (theme != null && icon != null && Objects.equals(theme, getCurrentThemeId())
							&& commandSpecPattern != null) {
						findMatchingQuicklinks(commandSpecPattern)
								.forEach(ql -> ql.iconUrl = getImageURL(ce, ATT_ICON));
					}
				}
			}
			return new ArrayList<>(quicklinks.values());
		}

		private void processDefinition(IConfigurationElement ce) {
			if (!ELMT_COMMAND.equals(ce.getName()) && !ELMT_URL.equals(ce.getName())) {
				return;
			}

			String key = null;
			Quicklink ql = new Quicklink();
			if (ELMT_COMMAND.equals(ce.getName())) {
				key = ce.getAttribute(ATT_ID);
				if (key == null) {
					Log.warning(NLS.bind("Skipping '{0}': missing {1}", ce.getName(), ATT_ID)); //$NON-NLS-1$
					return;
				}
				ql.commandSpec = key;
				ql.label = ce.getAttribute(ATT_LABEL);
				ql.description = ce.getAttribute(ATT_DESCRIPTION);
				ql.iconUrl = getImageURL(ce, ATT_ICON);
			} else if (ELMT_URL.equals(ce.getName())) {
				key = ce.getAttribute(ATT_LOCATION);
				if (key == null) {
					Log.warning(NLS.bind("Skipping '{0}': missing {1}", ELMT_URL, ATT_LOCATION)); //$NON-NLS-1$
					return;
				}
				ql.url = key;
				ql.label = ce.getAttribute(ATT_LABEL);
				ql.description = ce.getAttribute(ATT_DESCRIPTION);
				ql.iconUrl = getImageURL(ce, ATT_ICON);
			}
			ql.rank = getRank(ce.getContributor().getName());
			if (ce.getAttribute(ATT_IMPORTANCE) != null) {
				ql.importance = Importance.forId(ce.getAttribute(ATT_IMPORTANCE));
			}
			if (ce.getAttribute(ATT_RESOLUTION) != null) {
				ql.resolution = ce.getAttribute(ATT_RESOLUTION);
			}
			// discard if already seen
			quicklinks.putIfAbsent(key, ql);
		}

		/**
		 * Find {@link Quicklink}s whose {@code commandSpec} matches the simple
		 * wildcard pattern in {@code commandSpecPattern}
		 *
		 * @param commandSpecPattern
		 *            a simple wildcard pattern supporting *, ?
		 * @return the set of matching Quicklinks
		 */
		private Stream<Quicklink> findMatchingQuicklinks(String commandSpecPattern) {
			// transform simple wildcards into regexp
			String regexp = commandSpecPattern.replace(".", "\\.").replace("(", "\\(").replace(")", "\\)") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
					.replace("*", ".*"); //$NON-NLS-1$ //$NON-NLS-2$
			final Pattern pattern = Pattern.compile(regexp);
			return quicklinks.values().stream().filter(
					ql -> ql.commandSpec != null && (commandSpecPattern.equals(ql.commandSpec)
							|| pattern.matcher(ql.commandSpec).matches()));
		}

		private IExtension[] getExtensions(String extPtId) {
			IExtensionRegistry registry = locator.getService(IExtensionRegistry.class);
			IExtensionPoint extPt = registry.getExtensionPoint(extPtId);
			return extPt == null ? new IExtension[0] : extPt.getExtensions();
		}


		private long getRank(String bundleSymbolicName) {
			if (bundleIds == null) {
				Bundle bundle = FrameworkUtil.getBundle(getClass());
				bundleIds = new HashMap<>();
				bundles = bundle.getBundleContext().getBundles();
			}
			return bundleIds.computeIfAbsent(bundleSymbolicName, bsn -> {
				for (Bundle b : bundles) {
					if (bsn.equals(b.getSymbolicName())
							&& (b.getState() & (Bundle.INSTALLED | Bundle.UNINSTALLED)) == 0) {
						return b.getBundleId();
					}
				}
				return Long.MAX_VALUE;
			});
		}

		/**
		 * @return URL to image, suitable for using in an external browser; may
		 *         be a <code>data:</code> URL; may be null
		 */
		private String getImageURL(IConfigurationElement ce, String attr) {
			String iconURL = MenuHelper.getIconURI(ce, attr);
			if (iconURL != null) {
				return asBrowserURL(iconURL);
			}
			return null;
		}
	}

	/** Source: http://stackoverflow.com/a/417184 */
	private static final int MAX_URL_LENGTH = 2083;

	private IIntroContentProviderSite site;
	private IServiceLocator locator;
	private CommandManager manager;
	private ICommandImageService images;
	private Supplier<List<Quicklink>> model;

	@Override
	public void init(IIntroContentProviderSite site) {
		this.site = site;
		// IIntroContentProviderSite should provide services.
		if (site instanceof IServiceLocator) {
			this.locator = (IServiceLocator) site;
		} else if (site instanceof AbstractIntroPartImplementation) {
			this.locator = ((AbstractIntroPartImplementation) site).getIntroPart().getIntroSite();
		} else {
			this.locator = PlatformUI.getWorkbench();
		}
		manager = locator.getService(CommandManager.class);
		images = locator.getService(ICommandImageService.class);

		model = new ModelReader();
	}

	/**
	 * Find the current Welcome/Intro identifier
	 *
	 * @return the current identifier or {@code null} if no theme
	 */
	protected String getCurrentThemeId() {
		if (site instanceof AbstractIntroPartImplementation) {
			IntroTheme theme = ((AbstractIntroPartImplementation) site).getModel().getTheme();
			return theme.getId();
		}
		return null;
	}

	@Override
	public void createContent(String id, PrintWriter out) {
		// Content is already embedded within a <div id="...">
		getQuicklinks().forEach(ql -> {
			// ah how lovely to embed HTML in code
			String urlEncodedCommand = asEmbeddedURL(ql);
			out.append("<a class='content-link'"); //$NON-NLS-1$
			if (ql.commandSpec != null) {
				out.append(" id='").append(asCSSId(ql.commandSpec)).append("' "); //$NON-NLS-1$ //$NON-NLS-2$
			}
			out.append(" href='"); //$NON-NLS-1$
			out.append(urlEncodedCommand);
			out.append("'>"); //$NON-NLS-1$
			if (ql.iconUrl != null) {
				out.append("<img class='background-image' src='").append(ql.iconUrl).append("'>"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			out.append("\n<div class='link-extra-div'></div>\n"); // UNKNOWN //$NON-NLS-1$
			out.append("<span class='link-label'>"); //$NON-NLS-1$
			out.append(ql.label);
			out.append("</span>"); //$NON-NLS-1$
			if (ql.description != null) {
				out.append("\n<p><span class='text'>"); //$NON-NLS-1$
				out.append(ql.description);
				out.append("</span></p>"); //$NON-NLS-1$
			}
			out.append("</a>"); //$NON-NLS-1$
		});
	}

	private String asEmbeddedURL(Quicklink ql) {
		if (ql.url != null) {
			return ql.url;
		}
		String encoded = URLEncoder.encode(ql.commandSpec, StandardCharsets.UTF_8);
		if (ql.resolution != null) {
			encoded += "&standby=" + ql.resolution; //$NON-NLS-1$
		}
		return "http://org.eclipse.ui.intro/execute?command=" + encoded; //$NON-NLS-1$
	}

	/**
	 * Transform the Eclipse Command identifier (with dots) to a CSS-compatible
	 * class
	 */
	private String asCSSId(String commandSpec) {
		int indexOf = commandSpec.indexOf('(');
		if (indexOf > 0) {
			commandSpec = commandSpec.substring(0, indexOf);
		}
		return commandSpec.replace('.', '_');
	}

	/**
	 * Rewrite or possible extract the icon at the given URL to a stable URL
	 * that can be embedded in HTML and rendered in a browser. May create
	 * temporary files that will be cleaned up on exit.
	 *
	 * @return stable URL
	 */
	private String asBrowserURL(String iconURL) {
		if (iconURL.startsWith("file:") || iconURL.startsWith("http:")) { //$NON-NLS-1$ //$NON-NLS-2$
			return iconURL;
		}
		try {
			URL original = new URL(iconURL);
			URL toLocal = FileLocator.toFileURL(original);
			if (!toLocal.sameFile(original)) {
				return toLocal.toString();
			}
		} catch (IOException e1) {
			/* ignore */
		}

		// extract content
		try {
			return asDataURL(ImageDescriptor.createFromURL(new URL(iconURL)));
		} catch (MalformedURLException e) {
			// should probably log this
			return iconURL;
		}
	}

	/**
	 * Write out the image as a data: URL if possible or to the file-system.
	 *
	 * @return URL with the resulting image
	 */
	private String asDataURL(ImageDescriptor descriptor) {
		if (descriptor == null) {
			return null;
		}
		ImageData data = descriptor.getImageData();
		if (data == null) {
			return null;
		}
		ImageLoader loader = new ImageLoader();
		loader.data = new ImageData[] { data };

		ByteArrayOutputStream output = new ByteArrayOutputStream();
		loader.save(output, SWT.IMAGE_PNG);
		if (output.size() * 4 / 3 < MAX_URL_LENGTH) {
			// You'd think there was a more efficient way to do this...
			return "data:image/png;base64," + Base64.getEncoder().encodeToString(output.toByteArray()); //$NON-NLS-1$
		}
		try {
			File tempFile = File.createTempFile("qlink", "png"); //$NON-NLS-1$ //$NON-NLS-2$
			try (FileOutputStream fos = new FileOutputStream(tempFile)){
				fos.write(output.toByteArray());
			}
			tempFile.deleteOnExit();
			return tempFile.toURI().toString();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public void createContent(String id, Composite parent, FormToolkit toolkit) {
		Section section = toolkit.createSection(parent, ExpandableComposite.EXPANDED);
		TableViewer tableViewer = new TableViewer(toolkit.createTable(section, SWT.FULL_SELECTION));
		tableViewer.setLabelProvider(new URLLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof Quicklink) {
					return ((Quicklink) element).label;
				}
				return super.getText(element);
			}

			@Override
			public Image getImage(Object element) {
				if (element instanceof Quicklink) {
					return super.getImage(((Quicklink) element).iconUrl);
				}
				return super.getImage(element);
			}
		});
		tableViewer.setContentProvider(ArrayContentProvider.getInstance());
		tableViewer.setInput(getQuicklinks().toArray());
	}

	private List<Quicklink> getQuicklinks() {
		List<Quicklink> links = model.get();
		if (links.isEmpty()) {
			links = generateDefaultQuicklinks();
		}
		return links.stream().filter(this::populateQuicklink).sorted(Quicklink::compareTo)
				.collect(Collectors.toList());
	}

	/**
	 * Attempt to populate common fields given other information. For commands,
	 * we look up information in the ICommandService and ICommandImageService.
	 * Return false if this quicklink cannot be found and should not be shown.
	 *
	 * @return true if should be included
	 */
	private boolean populateQuicklink(Quicklink ql) {
		if (ql.commandSpec == null) {
			// non-commmands are fine
			return true;
		}
		try {
			ParameterizedCommand pc = manager.deserialize(ql.commandSpec);
			if (!pc.getCommand().isDefined() || !pc.getCommand().isHandled()
					|| !pc.getCommand().isEnabled()) {
				// not an error: just not found or not enabled
				return false;
			}
			if (ql.label == null) {
				ql.label = pc.getCommand().getName();
			}
			if (ql.description == null) {
				ql.description = pc.getCommand().getDescription();
			}
			if (ql.iconUrl == null && images != null) {
				ImageDescriptor descriptor = images.getImageDescriptor(pc.getId());
				if (descriptor != null) {
					String iconUrl = MenuHelper.getImageUrl(descriptor);
					ql.iconUrl = iconUrl != null ? asBrowserURL(iconUrl) : asDataURL(descriptor);
				}
			}
			return true;
		} catch (NotDefinedException | SerializationException e) {
			// exclude commands that don't exist or are mis-fashioned
			return false;
		}
	}

	/** Simplify creating a quicklink for a command */
	private Quicklink forCommand(String commandSpec) {
		Quicklink ql = new Quicklink();
		ql.commandSpec = commandSpec;
		return ql;
	}

	/** Simplify creating a quicklink for a command */
	private Quicklink forCommand(String commandSpec, Importance importance) {
		Quicklink ql = new Quicklink();
		ql.commandSpec = commandSpec;
		ql.importance = importance;
		return ql;
	}

	/**
	 * Return the default commands to be shown if there is no other content
	 * available
	 */
	private List<Quicklink> generateDefaultQuicklinks() {
		return Arrays.asList(forCommand("org.eclipse.oomph.setup.ui.questionnaire", Importance.HIGH), //$NON-NLS-1$
				forCommand("org.eclipse.ui.cheatsheets.openCheatSheet"), //$NON-NLS-1$
				forCommand("org.eclipse.ui.newWizard"), //$NON-NLS-1$
				forCommand("org.eclipse.ui.file.import"), //$NON-NLS-1$
				forCommand("org.eclipse.epp.mpc.ui.command.showMarketplaceWizard"), //$NON-NLS-1$
				forCommand("org.eclipse.ui.edit.text.openLocalFile", Importance.LOW)); //$NON-NLS-1$
	}

	@Override
	public void dispose() {
	}
}
