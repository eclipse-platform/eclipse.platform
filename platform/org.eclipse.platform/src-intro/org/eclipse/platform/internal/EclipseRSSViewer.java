/*******************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.platform.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.intro.config.IIntroContentProvider;
import org.eclipse.ui.intro.config.IIntroContentProviderSite;
import org.eclipse.ui.intro.config.IIntroURL;
import org.eclipse.ui.intro.config.IntroURLFactory;
import org.osgi.framework.Bundle;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class EclipseRSSViewer implements IIntroContentProvider {
	private static final String NEWS_URL = "http://www.eclipse.org/home/eclipsenews.rss"; //$NON-NLS-1$
	private static final String INTRO_SHOW_IN_BROWSER = "http://org.eclipse.ui.intro/openBrowser?url="; //$NON-NLS-1$
	private static final int MAX_NEWS_ITEMS = 5;
	private static final String HREF_BULLET = "bullet"; //$NON-NLS-1$

	private IIntroContentProviderSite site;

	private boolean disposed;

	private String id;

	private FormToolkit toolkit;

	private Composite parent;

	private Image bulletImage;

	private List items;

	private FormText formText;

	static class NewsItem {
		String label;

		String url;

		void setLabel(String label) {
			this.label = label;
		}

		void setUrl(String url) {
			this.url = url;
		}
	}

	class NewsFeed implements Runnable {
		public void run() {
			// important: don't do the work if the
			// part gets disposed in the process
			if (disposed)
				return;
			createNewsItems();
			if (disposed)
				return;
			PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
				public void run() {
					if (parent != null) {
						// we must recreate the content
						// for SWT because we will use
						// a gentle incremental reflow.
						// HTML reflow will simply reload the page.
						createContent(id, parent, toolkit);
						reflow(formText);
					}
					site.reflow(EclipseRSSViewer.this, true);
				}
			});
		}
	}

	/**
	 * Handles RSS XML and populates the items list with at most
	 * MAX_NEWS_ITEMS items.
	 */
	private class RSSHandler extends DefaultHandler {

		private static final String ELEMENT_RSS = "rss"; //$NON-NLS-1$
		private static final String ELEMENT_CHANNEL = "channel"; //$NON-NLS-1$
		private static final String ELEMENT_ITEM = "item"; //$NON-NLS-1$
		private static final String ELEMENT_TITLE = "title"; //$NON-NLS-1$
		private static final String ELEMENT_LINK = "link"; //$NON-NLS-1$

		private Stack stack = new Stack();
		private StringBuffer buf;
		private NewsItem item;
		
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			stack.push(qName);
			// it's a title/link in an item
			if ((ELEMENT_TITLE.equals(qName) || ELEMENT_LINK.equals(qName))
					&& (item != null)) {
				// prepare the buffer; we're expecting chars
				buf = new StringBuffer();
			}
			// it's an item in a channel in rss
			else if (ELEMENT_ITEM.equals(qName)
					&& (ELEMENT_CHANNEL.equals(stack.get(1)))
					&& (ELEMENT_RSS.equals(stack.get(0)))
					&& (stack.size() == 3)
					&& (items.size() < MAX_NEWS_ITEMS)) {
				// prepare the item
				item = new NewsItem();
			}
		}
		
		public void endElement(String uri, String localName, String qName) throws SAXException {
			stack.pop();
			if (item != null) {
				if (buf != null) {
					if (ELEMENT_TITLE.equals(qName)) {
						item.setLabel(buf.toString().trim());
						buf = null;
					}
					else if (ELEMENT_LINK.equals(qName)) {
						item.setUrl(buf.toString().trim());
						buf = null;
					}
				}
				else {
					if (ELEMENT_ITEM.equals(qName)) {
						// ensure we have a valid item
						if (item.label != null && item.label.length() > 0 &&
								item.url != null && item.url.length() > 0) {
							items.add(item);
						}
						item = null;
					}
				}
			}
		}
		
		public void characters(char[] ch, int start, int length) throws SAXException {
			// were we expecting chars?
			if (buf != null) {
				buf.append(new String(ch, start, length));
			}
		}
	}

	
	public void init(IIntroContentProviderSite site) {
		this.site = site;
		Thread newsWorker = new Thread(new NewsFeed());
		newsWorker.start();
	}

	public void createContent(String id, PrintWriter out) {
		if (disposed)
			return;
		this.id = id;
		if (items == null) {
			out.print("<p class=\"status-text\">"); //$NON-NLS-1$
			out.print(Messages.getString("EclipseRSSViewer_loading")); //$NON-NLS-1$
			out.println("</p>"); //$NON-NLS-1$
		} else {
			if (items.size() > 0) {
				out.println("<ul id=\"eclipse-news\">"); //$NON-NLS-1$
				for (int i = 0; i < items.size(); i++) {
					NewsItem item = (NewsItem) items.get(i);
					out.print("<li>"); //$NON-NLS-1$
					out.print("<a class=\"topicList\" href=\""); //$NON-NLS-1$
					out.print(createExternalURL(item.url));
					out.print("\">"); //$NON-NLS-1$
					out.print(item.label);
					out.print("</a>"); //$NON-NLS-1$
					out.println("</li>"); //$NON-NLS-1$
				}
			} else {
				out.print("<p class=\"status-text\">"); //$NON-NLS-1$
				out.print(Messages.getString("EclipseRSSViewer_noNews")); //$NON-NLS-1$
				out.println("</p>"); //$NON-NLS-1$
			}
			out.println("</ul>"); //$NON-NLS-1$
		}
	}

	public void createContent(String id, Composite parent, FormToolkit toolkit) {
		if (disposed)
			return;
		if (formText == null) {
			// a one-time pass
			formText = toolkit.createFormText(parent, true);
			formText.addHyperlinkListener(new HyperlinkAdapter() {
				public void linkActivated(HyperlinkEvent e) {
					doNavigate((String) e.getHref());
				}
			});
			bulletImage = createImage(new Path("images/topiclabel/arrow.gif")); //$NON-NLS-1$
			if (bulletImage!=null)
				formText.setImage(HREF_BULLET, bulletImage);
			this.parent = parent;
			this.toolkit = toolkit;
			this.id = id;
		}
		StringBuffer buffer = new StringBuffer();
		buffer.append("<form>"); //$NON-NLS-1$
		if (items == null) {
			buffer.append("<p>"); //$NON-NLS-1$
			buffer.append(Messages.getString("EclipseRSSViewer_loading")); //$NON-NLS-1$
			buffer.append("</p>"); //$NON-NLS-1$
		} else {
			if (items.size() > 0) {
				for (int i = 0; i < items.size(); i++) {
					NewsItem item = (NewsItem) items.get(i);
					buffer.append("<li style=\"image\" value=\""); //$NON-NLS-1$
					buffer.append(HREF_BULLET);
					buffer.append("\">"); //$NON-NLS-1$
					buffer.append("<a href=\""); //$NON-NLS-1$
					buffer.append(item.url);
					buffer.append("\">"); //$NON-NLS-1$
					buffer.append(item.label);
					buffer.append("</a>"); //$NON-NLS-1$
					buffer.append("</li>"); //$NON-NLS-1$
				}
			} else {
				buffer.append("<p>"); //$NON-NLS-1$
				buffer.append(Messages.getString("EclipseRSSViewer_noNews")); //$NON-NLS-1$
				buffer.append("</p>"); //$NON-NLS-1$
			}
		}
		buffer.append("</form>"); //$NON-NLS-1$
		formText.setText(buffer.toString(), true, false);
	}

	private String createExternalURL(String url) {
		//TODO don't know which encoding to pass here - revisit
		return INTRO_SHOW_IN_BROWSER+
							URLEncoder.encode(url);
	}

	private Image createImage(IPath path) {
		Bundle bundle = Platform.getBundle("org.eclipse.platform"); //$NON-NLS-1$
		URL url = FileLocator.find(bundle, path, null);
		try {
			url = FileLocator.toFileURL(url);
			ImageDescriptor desc = ImageDescriptor.createFromURL(url);
			return desc.createImage();
		} catch (IOException e) {
			return null;
		}
	}

	private void doNavigate(final String url) {
		BusyIndicator.showWhile(PlatformUI.getWorkbench().getDisplay(),
				new Runnable() {
					public void run() {
						IIntroURL introUrl = IntroURLFactory
								.createIntroURL(url);
						if (introUrl != null) {
							// execute the action embedded in the IntroURL
							introUrl.execute();
							return;
						}
						// delegate to the browser support
						openBrowser(url);
					}
				});
	}

	private void openBrowser(String href) {
		try {
			URL url = new URL(href);
			IWorkbenchBrowserSupport support = PlatformUI.getWorkbench()
					.getBrowserSupport();
			support.getExternalBrowser().openURL(url);
		} catch (PartInitException e) {
		} catch (MalformedURLException e) {
		}
	}

	private void createNewsItems() {
		items = Collections.synchronizedList(new ArrayList());
		InputStream in = null;
		try {
			URL url = new URL(NEWS_URL);
			in = url.openStream();
			SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
			parser.parse(in, new RSSHandler());
		}
		catch (Exception e) {
			// if anything goes wrong, fail silently; it will show a
			// "no news available" message.
		}
		finally {
			try {
				if (in != null) {
					in.close();
				}
			}
			catch (IOException e) {
				// nothing we can do here
			}
		}
	}

	/*
	 * This method is copied from Section and seems useful in general. Perhaps
	 * we should move it into content provider site, something like
	 * 'reflow(Control startingControl)'
	 */

	private void reflow(Control initiator) {
		Control c = initiator;
		while (c != null) {
			c.setRedraw(false);
			c = c.getParent();
			if (c instanceof ScrolledForm) {
				break;
			}
		}
		c = initiator;
		while (c != null) {
			if (c instanceof Composite)
				((Composite) c).layout(true);
			c = c.getParent();
			if (c instanceof ScrolledForm) {
				((ScrolledForm) c).reflow(true);
				break;
			}
		}
		c = initiator;
		while (c != null) {
			c.setRedraw(true);
			c = c.getParent();
			if (c instanceof ScrolledForm) {
				break;
			}
		}
	}

	public void dispose() {
		if (bulletImage != null) {
			bulletImage.dispose();
			bulletImage = null;
		}
		disposed = true;
	}
}