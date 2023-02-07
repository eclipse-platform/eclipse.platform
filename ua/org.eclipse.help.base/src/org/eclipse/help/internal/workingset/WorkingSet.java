/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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
package org.eclipse.help.internal.workingset;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.help.internal.criteria.CriterionResource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class WorkingSet {
	private String name;
	private List<AdaptableHelpResource> elements;
	private List<CriterionResource> criteria;

	public WorkingSet(String name) {
		this(name, (List<AdaptableHelpResource>) null, (List<CriterionResource>) null);
	}

	public WorkingSet(String name, List<AdaptableHelpResource> elements) {
		this(name, elements, null);
	}

	public WorkingSet(String name, List<AdaptableHelpResource> elements, List<CriterionResource> criteria) {
		this.name = name;

		if (elements == null)
			elements = new ArrayList<>();
		this.elements = elements;

		if (criteria != null) {
			this.criteria = criteria;
		} else {
			this.criteria = new ArrayList<>();
		}
	}

	public WorkingSet(String name, AdaptableHelpResource[] elements) {
		this(name, elements, null);
	}

	public WorkingSet(String name, AdaptableHelpResource[] elements, CriterionResource[] criteria) {
		this.name = name;

		if (elements == null)
			elements = new AdaptableHelpResource[0];

		this.elements = new ArrayList<>(elements.length);
		Collections.addAll(this.elements, elements);

		if (criteria == null)
			criteria = new CriterionResource[0];

		this.criteria = new ArrayList<>(criteria.length);
		Collections.addAll(this.criteria, criteria);
	}

	public void removeElement(AdaptableHelpResource element) {
		// Note: this is based on equality of IHelpResource and
		// AdaptableHelpResource
		elements.remove(element);
	}

	public String getName() {
		return name;
	}

	public void setName(String newName) {
		if (newName == null)
			return;
		name = newName;
	}

	public AdaptableHelpResource[] getElements() {
		AdaptableHelpResource[] array = new AdaptableHelpResource[elements
				.size()];
		elements.toArray(array);
		return array;
	}

	public void setElements(AdaptableHelpResource[] elements) {
		this.elements = new ArrayList<>(elements.length);
		Collections.addAll(this.elements, elements);
	}


	public void setCriteria(CriterionResource[] criteria) {
		this.criteria = new ArrayList<>(criteria.length);
		Collections.addAll(this.criteria, criteria);
	}

	public CriterionResource[] getCriteria(){
		CriterionResource[] array = new CriterionResource[criteria.size()];
		criteria.toArray(array);
		return array;
	}

	public void saveState(Element parent) {
		Document doc = parent.getOwnerDocument();
		Element ws = doc.createElement("workingSet"); //$NON-NLS-1$
		ws.setAttribute("name", name); //$NON-NLS-1$
		parent.appendChild(ws);

		Element contents = doc.createElement("contents"); //$NON-NLS-1$
		ws.appendChild(contents);
		for (AdaptableHelpResource helpResource : elements) {
			Element child = doc.createElement("item"); //$NON-NLS-1$
			helpResource.saveState(child);
			contents.appendChild(child);
		}

		if (!criteria.isEmpty()){
			Element criteriaElement = doc.createElement("criteria"); //$NON-NLS-1$
			ws.appendChild(criteriaElement);

			for (CriterionResource criterion : criteria) {
				Element criterionItem = doc.createElement("criterion"); //$NON-NLS-1$
				criteriaElement.appendChild(criterionItem);
				String criterionName = criterion.getCriterionName();
				criterionItem.setAttribute("name", criterionName);//$NON-NLS-1$
				List<String> criterionValues = criterion.getCriterionValues();
				if(!criterionValues.isEmpty()){
					for (String value : criterionValues) {
						if(value != null){
							Element item = doc.createElement("item"); //$NON-NLS-1$
							criterionItem.appendChild(item);
							item.setAttribute("value", value.trim());//$NON-NLS-1$
						}
					}
				}
			}
		}
	}
}
