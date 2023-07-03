/*******************************************************************************
 * Copyright (c) 2004, 2017 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.ui.internal.intro.impl.parts;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.eclipse.ui.internal.intro.impl.IIntroConstants;
import org.eclipse.ui.internal.intro.impl.IntroPlugin;
import org.eclipse.ui.internal.intro.impl.Messages;
import org.eclipse.ui.internal.intro.impl.model.AbstractIntroPage;
import org.eclipse.ui.internal.intro.impl.model.IntroModelRoot;
import org.eclipse.ui.internal.intro.impl.model.IntroStandbyContentPart;
import org.eclipse.ui.internal.intro.impl.model.loader.ExtensionPointManager;
import org.eclipse.ui.internal.intro.impl.model.loader.ModelLoaderUtil;
import org.eclipse.ui.internal.intro.impl.util.ImageUtil;
import org.eclipse.ui.internal.intro.impl.util.Log;
import org.eclipse.ui.internal.intro.impl.util.StringUtil;
import org.eclipse.ui.intro.IIntroPart;
import org.eclipse.ui.intro.config.CustomizableIntroPart;
import org.eclipse.ui.intro.config.IStandbyContentPart;

/**
 * Standby part is responsible for managing and creating IStandbycontent parts.
 * It knows how to create and cache content parts. It also handles saving and
 * restoring its own state. It does that by caching the id of the last content
 * part viewed and recreating that part on startup. It also manages the life
 * cycle of content parts by creating and initializing them at the right
 * moments. It also passes the momento at appropriate times to these content
 * parts to enable storing and retrieving of state by content parts. Content
 * parts are responsible for recreating there own state, including input, from
 * the passed momemnto. When the Return to Introduction link is clicked, the
 * Intro goes out of standby content mode, and the standby content parts are not
 * shown anymore until the user explicitly asks for a part again. This is
 * accomplished through a data flag on the CustomizableIntroPart control.
 *
 */
public class StandbyPart implements IIntroConstants {

	private FormToolkit toolkit;
	private IntroModelRoot model;
	protected ImageHyperlink returnLink;
	protected Control separator;
	private Composite container;
	protected Composite content;
	private IIntroPart introPart;
	private EmptyStandbyContentPart emptyPart;
	private IMemento memento;

	private Map<String, ControlKey> cachedContentParts = new HashMap<>();

	private ControlKey cachedControlKey;

	class StandbyLayout extends Layout {

		private int VGAP = 9;
		private int VMARGIN = 5;
		private int HMARGIN = 5;
		private int SEPARATOR_HEIGHT = 1;

		@Override
		protected Point computeSize(Composite composite, int wHint, int hHint,
				boolean flushCache) {
			Point lsize = returnLink.computeSize(SWT.DEFAULT, SWT.DEFAULT,
				flushCache);
			Point csize = content.computeSize(SWT.DEFAULT, SWT.DEFAULT,
				flushCache);
			int width = Math.max(lsize.x + 2 * HMARGIN, csize.x);
			int height = VMARGIN + lsize.y + VGAP + SEPARATOR_HEIGHT + csize.y;
			return new Point(width, height);
		}

		@Override
		protected void layout(Composite composite, boolean flushCache) {
			Rectangle carea = composite.getClientArea();
			int lwidth = carea.width - HMARGIN * 2;
			Point lsize = returnLink.computeSize(lwidth, SWT.DEFAULT,
				flushCache);
			int x = HMARGIN;
			int y = VMARGIN;
			returnLink.setBounds(x, y, lsize.x, lsize.y);
			x = 0;
			y += lsize.y + VGAP;
			separator.setBounds(x, y, carea.width, SEPARATOR_HEIGHT);
			y += SEPARATOR_HEIGHT;
			content.setBounds(x, y, carea.width, carea.height - VMARGIN
					- lsize.y - VGAP - SEPARATOR_HEIGHT);
		}
	}

	/**
	 * @param parent
	 */
	public StandbyPart(IntroModelRoot model) {
		this.model = model;
	}


	public void init(IIntroPart introPart, IMemento memento) {
		this.introPart = introPart;
		this.memento = memento;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.ui.intro.IIntroPart#saveState(org.eclipse.ui.IMemento)
	 */
	private IMemento getMemento(IMemento memento, String key) {
		if (memento == null)
			return null;
		return memento.getChild(key);
	}

	public void createPartControl(Composite parent) {
		toolkit = new FormToolkit(parent.getDisplay());
		// parent container. Has custom layout. Has return link and content
		// stack composite.
		container = toolkit.createComposite(parent);
		container.setLayout(new StandbyLayout());

		// return hyper link.
		ImageUtil.registerImage(ImageUtil.BACK, "full/elcl16/home_nav.png"); //$NON-NLS-1$
		returnLink = toolkit.createImageHyperlink(container, SWT.WRAP
				| SWT.CENTER);
		returnLink.setImage(ImageUtil.getImage(ImageUtil.BACK));
		returnLink.addHyperlinkListener(new HyperlinkAdapter() {

			@Override
			public void linkActivated(HyperlinkEvent e) {
				doReturn();
			}
		});

		// create horizontal separator
		separator = toolkit.createCompositeSeparator(container);
		// content stack container
		content = toolkit.createComposite(container);
		StackLayout slayout = new StackLayout();
		slayout.marginWidth = slayout.marginHeight = 0;
		content.setLayout(slayout);

		boolean success = false;
		if (memento != null) {
			success = restoreState(memento);
			if (!success)
				// add empty standby content.
				addAndShowEmptyPart(Messages.StandbyPart_canNotRestore);
		}

		updateReturnLinkLabel();
	}

	/**
	 * Empty content part used as backup for failures.
	 *
	 */
	private void addAndShowEmptyPart(String message) {
		if (emptyPart == null)
			emptyPart = new EmptyStandbyContentPart();
		addStandbyContentPart(EMPTY_STANDBY_CONTENT_PART, emptyPart);
		emptyPart.setMessage(message);
		setTopControl(EMPTY_STANDBY_CONTENT_PART);
	}

	/**
	 * Tries to create the last content part viewed, based on content part id..
	 *
	 * @param memento
	 * @return
	 */
	private boolean restoreState(IMemento memento) {
		String contentPartId = memento
			.getString(MEMENTO_STANDBY_CONTENT_PART_ID_ATT);
		if (contentPartId == null)
			return false;
		// create the cached content part. Content parts are responsible for
		// storing and reading their input state.
		return showContentPart(contentPartId, null);
	}


	/**
	 * Sets the into part to standby, and shows the passed standby part, with
	 * the given input.
	 *
	 * @param partId
	 * @param input
	 */
	public boolean showContentPart(String partId, String input) {
		// Get the IntroStandbyContentPart that maps to the given partId.
		IntroStandbyContentPart standbyPartContent = ExtensionPointManager
			.getInst().getSharedConfigExtensionsManager()
			.getStandbyPart(partId);

		if (standbyPartContent != null) {
			String standbyContentClassName = standbyPartContent.getClassName();
			String pluginId = standbyPartContent.getPluginId();

			Object standbyContentObject = ModelLoaderUtil.createClassInstance(
				pluginId, standbyContentClassName);
			if (standbyContentObject instanceof IStandbyContentPart) {
				IStandbyContentPart contentPart = (IStandbyContentPart) standbyContentObject;
				Control c = addStandbyContentPart(partId, contentPart);
				if (c != null) {
					try {
						setTopControl(partId);
						setInput(input);
						return true;
					} catch (Exception e) {
						Log.error("Failed to set the input: " + input //$NON-NLS-1$
								+ " on standby part: " + partId, e); //$NON-NLS-1$
					}
				}

				// failed to create the standby part, show empty part and signal
				// failure.
				String message = NLS.bind(Messages.StandbyPart_failedToCreate,
					partId);
				addAndShowEmptyPart(message);
				return false;

			}
		}

		// no content part defined with the passed partId, show empty part and
		// signal failure.
		String message = NLS.bind(Messages.StandbyPart_nonDefined, partId);
		addAndShowEmptyPart(message);
		return false;
	}

	/**
	 * Creates a standbyContent part in the stack only if one is not already
	 * created. The partId is used as tke key in the cache. The value is an
	 * instance of ControlKey that wraps a control/StandbyPart pair along with
	 * the corresponding part id. This is needed to retrive the control of a
	 * given standby part. The IMemento should be passed to the StandbyPart when
	 * it is initialized.
	 *
	 * @param standbyContent
	 */
	public Control addStandbyContentPart(String partId,
			IStandbyContentPart standbyContent) {

		ControlKey controlKey = getCachedContent(partId);
		if (controlKey == null) {

			try {
				standbyContent.init(introPart, getMemento(memento,
					MEMENTO_STANDBY_CONTENT_PART_TAG));
				standbyContent.createPartControl(content, toolkit);
			} catch (Exception e) {
				// a standby content part throws a PartInitException, log fact.
				Log.error(
					"Failed to create part for standby part: " + partId, e); //$NON-NLS-1$
				return null;
			}

			Control control = standbyContent.getControl();
			controlKey = new ControlKey(control, standbyContent, partId);
			cachedContentParts.put(partId, controlKey);
			if (partId.equals(EMPTY_STANDBY_CONTENT_PART))
				// just in case it was created explicity, reuse it.
				emptyPart = (EmptyStandbyContentPart) standbyContent;

			if (controlKey.getControl() == null) {
				// control is null. This means that interface was not
				// implemented properly. log fact.
				String message = StringUtil.concat("Standby Content part: ", partId, //$NON-NLS-1$
						" has a null Control defined. This prevents the part from being displayed."); //$NON-NLS-1$
				Log.error(message, null);
				return null;
			}
		}

		return controlKey.getControl();
	}



	public void setInput(Object input) {
		IStandbyContentPart standbyContent = cachedControlKey.getContentPart();
		standbyContent.setInput(input);
		updateReturnLinkLabel();
		container.layout();
	}


	public void setTopControl(String key) {
		cachedControlKey = getCachedContent(key);
		if (cachedControlKey != null) {
			setTopControl(cachedControlKey.getControl());
		}
	}

	private void setTopControl(Control c) {
		StackLayout layout = (StackLayout) content.getLayout();
		layout.topControl = c;
		if (c instanceof Composite)
			((Composite) c).layout();
		content.layout();
		container.layout();
	}

	private void updateReturnLinkLabel() {
		String linkText = Messages.StandbyPart_returnToIntro;
		returnLink.setText(linkText);
		AbstractIntroPage page = model.getCurrentPage();
		if (page == null)
			// page will be null in static intro.
			return;

		String toolTip = Messages.StandbyPart_returnTo;
		if (page.getTitle() != null)
			toolTip += " " + page.getTitle(); //$NON-NLS-1$

		returnLink.setToolTipText(toolTip);
	}

	protected void doReturn() {
		// remove the flag to indicate that standbypart is no longer needed.
		((CustomizableIntroPart) introPart).getControl().setData(
			IIntroConstants.SHOW_STANDBY_PART, null);
		IntroPlugin.setIntroStandby(false);
	}

	/**
	 * Calls dispose on all cached IStandbyContentParts.
	 *
	 */
	public void dispose() {
		for (ControlKey controlKey : cachedContentParts.values()) {
			controlKey.getContentPart().dispose();
		}
		toolkit.dispose();
	}

	/**
	 * Save the current state of the standby part. It stores the cached content
	 * part id for later creating it on restart. It also creates another
	 * subclass momento to also give the standby content part its own name
	 * space. This was momentos saved by different content parts will not
	 * conflict.
	 *
	 * @param memento
	 *            the memento in which to store state information
	 */
	public void saveState(IMemento memento) {
		// save cached content part id.
		if (cachedControlKey != null) {
			String contentPartId = cachedControlKey.getContentPartId();
			if (contentPartId == EMPTY_STANDBY_CONTENT_PART)
				// do not create memento for empty standby.
				return;
			memento.putString(MEMENTO_STANDBY_CONTENT_PART_ID_ATT,
				contentPartId);
			// give standby part its own child to create a name space for
			// IStandbyPartContent contribution momentos.
			IMemento standbyContentPartMemento = memento
				.createChild(MEMENTO_STANDBY_CONTENT_PART_TAG);
			// pass new memento to correct standby part.
			IStandbyContentPart standbyContentpart = cachedControlKey
				.getContentPart();
			if (standbyContentpart != null)
				standbyContentpart.saveState(standbyContentPartMemento);
		}
	}


	/*
	 * Set focus on the IStandbyContentPart that corresponds to the top control
	 * in the stack.
	 *
	 * @see org.eclipse.ui.internal.intro.impl.parts.IStandbyContentPart#setFocus()
	 */
	public void setFocus() {
		// grab foxus first, then delegate. This way if content part does
		// nothing on focus, part still works.
		returnLink.setFocus();
		if (cachedControlKey != null)
			cachedControlKey.getContentPart().setFocus();
	}



	/**
	 * Checks the standby cache stack if we have already created a similar
	 * IStandbyContentPart. If not, returns null.
	 *
	 * @param standbyContent
	 * @return
	 */
	private ControlKey getCachedContent(String key) {
		if (cachedContentParts.containsKey(key))
			return cachedContentParts.get(key);
		return null;
	}

	/*
	 * Model class to wrap Control and IStandbyContentPart pairs, along with the
	 * representing ID..
	 */
	static class ControlKey {

		Control c;
		IStandbyContentPart part;
		String contentPartId;

		ControlKey(Control c, IStandbyContentPart part, String contentPartId) {
			this.c = c;
			this.part = part;
			this.contentPartId = contentPartId;
		}

		/**
		 * @return Returns the c.
		 */
		public Control getControl() {
			return c;
		}

		/**
		 * @return Returns the content part.
		 */
		public IStandbyContentPart getContentPart() {
			return part;
		}

		/**
		 * @return Returns the part id.
		 */
		public String getContentPartId() {
			return contentPartId;
		}
	}


}
