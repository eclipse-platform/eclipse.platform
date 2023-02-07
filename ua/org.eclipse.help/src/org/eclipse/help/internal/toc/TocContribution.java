/*******************************************************************************
 * Copyright (c) 2007, 2015 IBM Corporation and others.
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
package org.eclipse.help.internal.toc;

import org.eclipse.help.IToc;
import org.eclipse.help.ITocContribution;
import org.eclipse.help.internal.util.ProductPreferences;

public class TocContribution implements ITocContribution {

	private String categoryId;
	private String contributorId;
	private String[] extraDocuments;
	private String id;
	private String locale;
	private Toc toc;
	private boolean isPrimary;
	private boolean isSubToc;

	public TocContribution() {
		isSubToc = false;
	}

	@Override
	public String getCategoryId() {
		return categoryId;
	}

	public void setCategoryId(String categoryId) {
		this.categoryId = categoryId;
	}

	@Override
	public String getContributorId() {
		return contributorId;
	}

	public void setContributorId(String contributorId) {
		this.contributorId = contributorId;
	}

	@Override
	public String[] getExtraDocuments() {
		return extraDocuments;
	}

	public void setExtraDocuments(String[] extraDocuments) {
		this.extraDocuments = extraDocuments;
	}

	@Override
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@Override
	public String getLocale() {
		return locale;
	}

	public void setLocale(String locale) {
		this.locale = locale;
	}

	@Override
	public String getLinkTo() {
		String link = toc.getLinkTo();
		return ProductPreferences.resolveSpecialIdentifiers(link);
	}

	public void setLinkTo(String linkTo) {
		toc.setLinkTo(linkTo);
	}

	@Override
	public IToc getToc() {
		return toc;
	}

	public void setToc(Toc toc) {
		this.toc = toc;
		toc.setTocContribution(this);
	}

	@Override
	public boolean isPrimary() {
		return isPrimary;
	}

	public void setPrimary(boolean isPrimary) {
		this.isPrimary = isPrimary;
	}

	public boolean isSubToc() {
		return isSubToc;
	}

	public void setSubToc(boolean isSubToc) {
		this.isSubToc = isSubToc;
	}
}
