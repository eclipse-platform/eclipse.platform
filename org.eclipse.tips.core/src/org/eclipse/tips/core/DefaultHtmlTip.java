/*******************************************************************************
 * Copyright (c) 2018 vogella GmbH
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     simon.scholz@vogella.com - initial API and implementation
 *******************************************************************************/
package org.eclipse.tips.core;

import java.util.Date;

/**
 * Default implementation of a {@link Tip}, which implements {@link IHtmlTip}.
 * <p>
 * All return values of the overridden methods can be passed as constructor
 * arguments.
 * </p>
 *
 * @see Tip
 * @see IHtmlTip
 */
public class DefaultHtmlTip extends Tip implements IHtmlTip {

	private final Date creationDate;
	private final String subject;
	private final String html;
	private final TipImage tipImage;

	/**
	 * Constructor, which leaves out the {@link TipImage}.
	 *
	 * @param providerId   id of the {@link TipProvider}, where this tip is added
	 * @param creationDate creation date of this tip
	 * @param subject      subject of this tip
	 * @param html         HTML content of this tip
	 */
	public DefaultHtmlTip(String providerId, Date creationDate, String subject, String html) {
		this(providerId, creationDate, subject, html, null);
	}

	/**
	 * Constructor, which leaves out the html content and just shows the
	 * {@link TipImage}.
	 *
	 * @param providerId   id of the {@link TipProvider}, where this tip is added
	 * @param creationDate creation date of this tip
	 * @param subject      subject of this tip
	 * @param tipImage     {@link TipImage} of this tip, which will be shown as
	 *                     content
	 */
	public DefaultHtmlTip(String providerId, Date creationDate, String subject, TipImage tipImage) {
		this(providerId, creationDate, subject, null, tipImage);
	}

	/**
	 * Constructor, which includes HTML content and tipImage
	 *
	 * @param providerId   id of the {@link TipProvider}, where this tip is added
	 * @param creationDate creation date of this tip
	 * @param subject      subject of this tip
	 * @param html         HTML content of this tip
	 * @param tipImage     {@link TipImage} of this tip, which will be shown as
	 *                     content
	 */
	public DefaultHtmlTip(String providerId, Date creationDate, String subject, String html, TipImage tipImage) {
		super(providerId);
		this.creationDate = creationDate;
		this.subject = subject;
		this.html = html;
		this.tipImage = tipImage;
	}

	@Override
	public Date getCreationDate() {
		return creationDate;
	}

	@Override
	public String getSubject() {
		return subject;
	}

	@Override
	public String getHTML() {
		return html;
	}

	@Override
	public TipImage getImage() {
		return tipImage;
	}
}
