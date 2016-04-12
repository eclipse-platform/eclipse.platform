/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.history;

import java.util.ArrayList;
import com.ibm.icu.util.Calendar;

import org.eclipse.team.core.history.IFileRevision;

public class DateHistoryCategory extends AbstractHistoryCategory {
	
	private String name;
	private Calendar fromDate;
	private Calendar toDate;
	
	private IFileRevision[] revisions;
	
	/**
	 * Creates a new instance of DateCVSHistoryCategory. 
	 * 
	 * @param name	the name of this category
	 * @param fromDate	the start date for this category or <code>null</code> if you want everything up to the end date
	 * @param toDate	the end point for this category or <code>null</code> if you want just all entries in the
	 * start date
	 */
	public DateHistoryCategory(String name, Calendar fromDate, Calendar toDate){
		this.name = name;
		this.fromDate = fromDate;
		this.toDate = toDate;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.AbstractCVSHistoryCategory#getName()
	 */
	@Override
	public String getName() {
		return name;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.AbstractCVSHistoryCategory#collectFileRevisions(org.eclipse.team.core.history.IFileRevision[], boolean)
	 */
	@Override
	public boolean collectFileRevisions(IFileRevision[] fileRevisions, boolean shouldRemove) {
		
		ArrayList pertinentRevisions = new ArrayList();
		ArrayList nonPertinentRevisions = new ArrayList();
		
		for (int i = 0; i < fileRevisions.length; i++) {
			//get the current file revision's date
			Calendar fileRevDate = Calendar.getInstance();
			fileRevDate.setTimeInMillis(fileRevisions[i].getTimestamp());
			
			int fileRevDay = fileRevDate.get(Calendar.DAY_OF_YEAR);
			int fileRevYear = fileRevDate.get(Calendar.YEAR);
			
			if (fromDate == null){
				//check to see if this revision is within the toDate range
				if (((fileRevDay<toDate.get(Calendar.DAY_OF_YEAR)) && (fileRevYear == toDate.get(Calendar.YEAR))) ||
					(fileRevYear < toDate.get(Calendar.YEAR))){
					pertinentRevisions.add(fileRevisions[i]);
				} else {
					//revision is equal or later then the to date, add to rejects list
					nonPertinentRevisions.add(fileRevisions[i]);
				}
			} else if (toDate == null){
				//check to see if this revision falls on the same day as the fromDate
				if ((fileRevDay == fromDate.get(Calendar.DAY_OF_YEAR)) &&
					(fileRevYear == fromDate.get(Calendar.YEAR))){
					pertinentRevisions.add(fileRevisions[i]);
				} else {
					nonPertinentRevisions.add(fileRevisions[i]);
				}
			} else {
				//check the range
				if ((fileRevYear >= fromDate.get(Calendar.YEAR)) &&
					(fileRevYear <= toDate.get(Calendar.YEAR)) &&
					(fileRevDay >= fromDate.get(Calendar.DAY_OF_YEAR)) &&
					(fileRevDay < toDate.get(Calendar.DAY_OF_YEAR))
				) {
					pertinentRevisions.add(fileRevisions[i]);
				} else {
					nonPertinentRevisions.add(fileRevisions[i]);
				}
			}			
		}
		
		//check mode
		if (shouldRemove){
			//TODO: pass in an object containing the file revision arrays and modify the contents
			/*IFileRevision[] tempRevision = (IFileRevision[]) nonPertinentRevisions.toArray(new IFileRevision[nonPertinentRevisions.size()]);
			System.arraycopy(tempRevision, 0, fileRevisions, 0, tempRevision.length);*/
		}
		
		if (pertinentRevisions.size() > 0){
			IFileRevision[] tempRevision = (IFileRevision[]) pertinentRevisions.toArray(new IFileRevision[pertinentRevisions.size()]);
			revisions = new IFileRevision[tempRevision.length];
			System.arraycopy(tempRevision, 0, revisions, 0, tempRevision.length);
			return true;
		}
		
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.AbstractCVSHistoryCategory#getRevisions()
	 */
	@Override
	public IFileRevision[] getRevisions() {
		return revisions;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.AbstractCVSHistoryCategory#hasRevisions()
	 */
	@Override
	public boolean hasRevisions() {
		if (revisions != null && revisions.length > 0)
			return true;
		
		return false;
	}

}
