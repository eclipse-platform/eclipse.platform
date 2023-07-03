/*******************************************************************************
 * Copyright (c) 2011, 2015 IBM Corporation and others.
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

package org.eclipse.help.internal.search;

import java.net.URL;

import org.eclipse.help.IToc;
import org.eclipse.help.search.ISearchResult;

public class SearchResult implements ISearchResult{

	private String href;
	private String id;
	private String participantId;
	private String description;
	private String summary;
	private String label;
	private URL icon;
	private float score;
	private IToc toc;
	private boolean isPotentialHit;

	@Override
	public String getHref() {
		return href;
	}

	public void setHref(String href) {
		this.href = href;
	}

	@Override
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@Override
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public String getSummary() {
		return summary;
	}

	public void setSummary(String summary) {
		this.summary = summary;
	}

	@Override
	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	@Override
	public URL getIcon() {
		return icon;
	}

	public void setIcon(URL icon) {
		this.icon = icon;
	}

	@Override
	public float getScore() {
		return score;
	}

	public void setScore(float score) {
		this.score = score;
	}


	@Override
	public IToc getToc() {
		return toc;
	}


	public void setToc(IToc toc) {
		this.toc = toc;
	}


	@Override
	public String getParticipantId() {
		return participantId;
	}


	public void setParticipantId(String participantId) {
		this.participantId = participantId;
	}

	@Override
	public boolean isPotentialHit() {
		return isPotentialHit;
	}

	public void setPotentialHit(boolean isPotentialHit) {
		this.isPotentialHit = isPotentialHit;
	}

}
