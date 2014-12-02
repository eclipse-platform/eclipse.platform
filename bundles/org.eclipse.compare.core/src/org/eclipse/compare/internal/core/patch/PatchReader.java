/*******************************************************************************
 * Copyright (c) 2006, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.internal.core.patch;

import java.io.BufferedReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.SimpleDateFormat;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;

import org.eclipse.compare.patch.IFilePatch2;

public class PatchReader {

	private static final boolean DEBUG= false;
	
	private static final String DEV_NULL= "/dev/null"; //$NON-NLS-1$

	protected static final String MARKER_TYPE= "org.eclipse.compare.rejectedPatchMarker"; //$NON-NLS-1$

	// diff formats
	//	private static final int CONTEXT= 0;
	//	private static final int ED= 1;
	//	private static final int NORMAL= 2;
	//	private static final int UNIFIED= 3;

	// we recognize the following date/time formats
	private DateFormat[] fDateFormats= new DateFormat[] {
		new SimpleDateFormat("EEE MMM dd kk:mm:ss yyyy"), //$NON-NLS-1$
		new SimpleDateFormat("yyyy/MM/dd kk:mm:ss"), //$NON-NLS-1$
		new SimpleDateFormat("EEE MMM dd kk:mm:ss yyyy", Locale.US) //$NON-NLS-1$
	};

	private boolean fIsWorkspacePatch;
	private boolean fIsGitPatch;
	private DiffProject[] fDiffProjects;
	private FilePatch2[] fDiffs;

	// API for writing new multi-project patch format
	public static final String MULTIPROJECTPATCH_HEADER= "### Eclipse Workspace Patch"; //$NON-NLS-1$

	public static final String MULTIPROJECTPATCH_VERSION= "1.0"; //$NON-NLS-1$

	public static final String MULTIPROJECTPATCH_PROJECT= "#P"; //$NON-NLS-1$

	private static final Pattern GIT_PATCH_PATTERN= Pattern.compile("^diff --git a/.+ b/.+[\r\n]+$"); //$NON-NLS-1$

	/**
	 * Create a patch reader for the default date formats.
	 */
	public PatchReader() {
		// nothing here
	}

	/**
	 * Create a patch reader for the given date formats.
	 * 
	 * @param dateFormats
	 *            Array of <code>DateFormat</code>s to be used when
	 *            extracting dates from the patch.
	 */
	public PatchReader(DateFormat[] dateFormats) {
		this();
		this.fDateFormats = dateFormats;
	}
	
	public void parse(BufferedReader reader) throws IOException {
		List diffs= new ArrayList();
		HashMap diffProjects= new HashMap(4);
		String line= null;
		boolean reread= false;
		String diffArgs= null;
		String fileName= null;
		// no project means this is a single patch,create a placeholder project for now
		// which will be replaced by the target selected by the user in the preview pane
		String projectName= ""; //$NON-NLS-1$
		this.fIsWorkspacePatch= false;
		this.fIsGitPatch = false;

		LineReader lr= new LineReader(reader);
		if (!Platform.WS_CARBON.equals(Platform.getWS()))
			lr.ignoreSingleCR(); // Don't treat single CRs as line feeds to be consistent with command line patch

		// Test for our format
		line= lr.readLine();
		if (line != null && line.startsWith(PatchReader.MULTIPROJECTPATCH_HEADER)) {
			this.fIsWorkspacePatch= true;
		} else {
			parse(lr, line);
			return;
		}

		// read leading garbage
		while (true) {
			if (!reread)
				line= lr.readLine();
			reread= false;
			if (line == null)
				break;
			if (line.length() < 4)
				continue; // too short

			if (line.startsWith(PatchReader.MULTIPROJECTPATCH_PROJECT)) {
				projectName= line.substring(2).trim();
				continue;
			}

			if (line.startsWith("Index: ")) { //$NON-NLS-1$
				fileName= line.substring(7).trim();
				continue;
			}
			if (line.startsWith("diff")) { //$NON-NLS-1$
				diffArgs= line.substring(4).trim();
				continue;
			}

			if (line.startsWith("--- ")) { //$NON-NLS-1$
				// if there is no current project or
				// the current project doesn't equal the newly parsed project
				// reset the current project to the newly parsed one, create a new DiffProject
				// and add it to the array
				DiffProject diffProject;
				if (!diffProjects.containsKey(projectName)) {
					diffProject= new DiffProject(projectName);
					diffProjects.put(projectName, diffProject);
				} else {
					diffProject= (DiffProject) diffProjects.get(projectName);
				}

				line= readUnifiedDiff(diffs, lr, line, diffArgs, fileName, diffProject);
				diffArgs= fileName= null;
				reread= true;
			}
		}

		lr.close();

		this.fDiffProjects= (DiffProject[]) diffProjects.values().toArray(new DiffProject[diffProjects.size()]);
		this.fDiffs = (FilePatch2[]) diffs.toArray(new FilePatch2[diffs.size()]);
	}

	protected FilePatch2 createFileDiff(IPath oldPath, long oldDate,
			IPath newPath, long newDate) {
		return new FilePatch2(oldPath, oldDate, newPath, newDate);
	}

	private String readUnifiedDiff(List diffs, LineReader lr, String line, String diffArgs, String fileName, DiffProject diffProject) throws IOException {
		List newDiffs= new ArrayList();
		String nextLine= readUnifiedDiff(newDiffs, lr, line, diffArgs, fileName);
		for (Iterator iter= newDiffs.iterator(); iter.hasNext();) {
			FilePatch2 diff= (FilePatch2) iter.next();
			diffProject.add(diff);
			diffs.add(diff);
		}
		return nextLine;
	}
	
	public void parse(LineReader lr, String line) throws IOException {
		List diffs= new ArrayList();
		boolean reread= false;
		String diffArgs= null;
		String fileName= null;
		List headerLines = new ArrayList();
		boolean foundDiff= false;

		// read leading garbage
		reread= line!=null;
		while (true) {
			if (!reread)
				line= lr.readLine();
			reread= false;
			if (line == null)
				break;
								
			// remember some infos
			if (line.startsWith("Index: ")) { //$NON-NLS-1$
				fileName= line.substring(7).trim();
			} else if (line.startsWith("diff")) { //$NON-NLS-1$
				if (!foundDiff && GIT_PATCH_PATTERN.matcher(line).matches())
					this.fIsGitPatch= true;
				foundDiff= true;
				diffArgs= line.substring(4).trim();
			} else if (line.startsWith("--- ")) { //$NON-NLS-1$
				line= readUnifiedDiff(diffs, lr, line, diffArgs, fileName);
				if (!headerLines.isEmpty())
					setHeader((FilePatch2)diffs.get(diffs.size() - 1), headerLines);
				diffArgs= fileName= null;
				reread= true;
			} else if (line.startsWith("*** ")) { //$NON-NLS-1$
				line= readContextDiff(diffs, lr, line, diffArgs, fileName);
				if (!headerLines.isEmpty())
					setHeader((FilePatch2)diffs.get(diffs.size() - 1), headerLines);
				diffArgs= fileName= null;
				reread= true;
			}
			
			// Any lines we read here are header lines.
			// However, if reread is set, we will add them to the header on the next pass through
			if (!reread) {
				headerLines.add(line);
			}
		}
		
		lr.close();
		
		this.fDiffs = (FilePatch2[]) diffs.toArray(new FilePatch2[diffs.size()]);
	}
	
	private void setHeader(FilePatch2 diff, List headerLines) {
		String header = LineReader.createString(false, headerLines);
		diff.setHeader(header);
		headerLines.clear();
	}

	/*
	 * Returns the next line that does not belong to this diff
	 */
	protected String readUnifiedDiff(List diffs, LineReader reader, String line, String args, String fileName) throws IOException {

		String[] oldArgs= split(line.substring(4));

		// read info about new file
		line= reader.readLine();
		if (line == null || !line.startsWith("+++ ")) //$NON-NLS-1$
			return line;
			
		String[] newArgs= split(line.substring(4));
	
		FilePatch2 diff = createFileDiff(extractPath(oldArgs, 0, fileName),
				extractDate(oldArgs, 1), extractPath(newArgs, 0, fileName),
				extractDate(newArgs, 1));
		diffs.add(diff);
				   
		int[] oldRange= new int[2];
		int[] newRange= new int[2];
		int remainingOld= -1; // remaining old lines for current hunk
		int remainingNew= -1; // remaining new lines for current hunk
		List lines= new ArrayList();

		boolean encounteredPlus = false;
		boolean encounteredMinus = false;
		boolean encounteredSpace = false;
		
		try {
			// read lines of hunk
			while (true) {

				line= reader.readLine();
				if (line == null)
					return null;

				if (reader.lineContentLength(line) == 0) {
					//System.out.println("Warning: found empty line in hunk; ignored");
					//lines.add(' ' + line);
					continue;
				}

				char c= line.charAt(0);
				if (remainingOld == 0 && remainingNew == 0 && c != '@' && c != '\\') {
					return line;
				}

				switch (c) {
					case '@':
						if (line.startsWith("@@ ")) { //$NON-NLS-1$
							// flush old hunk
							if (lines.size() > 0) {
								Hunk.createHunk(diff, oldRange, newRange, lines, encounteredPlus, encounteredMinus, encounteredSpace);
								lines.clear();
							}

							// format: @@ -oldStart,oldLength +newStart,newLength @@
							extractPair(line, '-', oldRange);
							extractPair(line, '+', newRange);
							remainingOld= oldRange[1];
							remainingNew= newRange[1];
							continue;
						}
						break;
					case ' ':
						encounteredSpace= true;
						remainingOld--;
						remainingNew--;
						lines.add(line);
						continue;
					case '+':
						encounteredPlus= true;
						remainingNew--;
						lines.add(line);
						continue;
					case '-':
						encounteredMinus= true;
						remainingOld--;
						lines.add(line);
						continue;
					case '\\':
						if (line.indexOf("newline at end") > 0) { //$NON-NLS-1$
							int lastIndex= lines.size();
							if (lastIndex > 0) {
								line= (String)lines.get(lastIndex - 1);
								int end= line.length() - 1;
								char lc= line.charAt(end);
								if (lc == '\n') {
									end--;
									if (end > 0 && line.charAt(end) == '\r')
										end--;
								} else if (lc == '\r') {
									end--;
								}
								line= line.substring(0, end + 1);
								lines.set(lastIndex - 1, line);
							}
							continue;
						}
						break;
					case '#':
						break;
					case 'I':
						if (line.indexOf("Index:") == 0) //$NON-NLS-1$
							break;
						//$FALL-THROUGH$
					case 'd':
						if (line.indexOf("diff ") == 0) //$NON-NLS-1$
							break;
						//$FALL-THROUGH$
					case 'B':
						if (line.indexOf("Binary files differ") == 0) //$NON-NLS-1$
							break;
						//$FALL-THROUGH$
					default:
						break;
				}
				return line;
			}
		} finally {
			if (lines.size() > 0)
				Hunk.createHunk(diff, oldRange, newRange, lines, encounteredPlus, encounteredMinus, encounteredSpace);
		}
	}
	
	/*
	 * Returns the next line that does not belong to this diff
	 */
	private String readContextDiff(List diffs, LineReader reader, String line, String args, String fileName) throws IOException {
		
		String[] oldArgs= split(line.substring(4));
		
		// read info about new file
		line= reader.readLine();
		if (line == null || !line.startsWith("--- ")) //$NON-NLS-1$
			return line;
		
		String[] newArgs= split(line.substring(4));
						
		FilePatch2 diff = createFileDiff(extractPath(oldArgs, 0, fileName),
				extractDate(oldArgs, 1), extractPath(newArgs, 0, fileName),
				extractDate(newArgs, 1));
		diffs.add(diff);
				   
		int[] oldRange= new int[2];
		int[] newRange= new int[2];
		List oldLines= new ArrayList();
		List newLines= new ArrayList();
		List lines= oldLines;
		

		boolean encounteredPlus = false;
		boolean encounteredMinus = false;
		boolean encounteredSpace = false;
		
		try {
			// read lines of hunk
			while (true) {
				
				line= reader.readLine();
				if (line == null)
					return line;
				
				int l= line.length();
				if (l == 0)
					continue;
				if (l > 1) {
					switch (line.charAt(0)) {
					case '*':	
						if (line.startsWith("***************")) {	// new hunk //$NON-NLS-1$
							// flush old hunk
							if (oldLines.size() > 0 || newLines.size() > 0) {
								Hunk.createHunk(diff, oldRange, newRange, unifyLines(oldLines, newLines), encounteredPlus, encounteredMinus, encounteredSpace);
								oldLines.clear();
								newLines.clear();
							}
							continue;
						}
						if (line.startsWith("*** ")) {	// old range //$NON-NLS-1$
							// format: *** oldStart,oldEnd ***
							extractPair(line, ' ', oldRange);
							if (oldRange[0] == 0) {
								oldRange[1] = 0; // In case of the file addition
							} else {
								oldRange[1] = oldRange[1] - oldRange[0] + 1;
							}
							lines= oldLines;
							continue;
						}
						break;
					case ' ':	// context line
						if (line.charAt(1) == ' ') {
							lines.add(line);
							continue;
						}
						break;
					case '+':	// addition
						if (line.charAt(1) == ' ') {
							encounteredPlus = true;
							lines.add(line);
							continue;
						}
						break;
					case '!':	// change
						if (line.charAt(1) == ' ') {
							encounteredSpace = true;
							lines.add(line);
							continue;
						}
						break;
					case '-':
						if (line.charAt(1) == ' ') {	// deletion
							encounteredMinus = true;
							lines.add(line);
							continue;
						}
						if (line.startsWith("--- ")) {	// new range //$NON-NLS-1$
							// format: *** newStart,newEnd ***
							extractPair(line, ' ', newRange);
							if (newRange[0] == 0) {
								newRange[1] = 0; // In case of the file removal
							} else {
								newRange[1] = newRange[1] - newRange[0] + 1;
							}
							lines= newLines;
							continue;
						}
						break;
					default:
						break;
					}
				}
				return line;
			}
		} finally {
			// flush last hunk
			if (oldLines.size() > 0 || newLines.size() > 0)
				Hunk.createHunk(diff, oldRange, newRange, unifyLines(oldLines, newLines), encounteredPlus, encounteredMinus, encounteredSpace);
		}
	}
	
	/*
	 * Creates a List of lines in the unified format from
	 * two Lists of lines in the 'classic' format.
	 */
	private List unifyLines(List oldLines, List newLines) {
		List result= new ArrayList();

		String[] ol= (String[]) oldLines.toArray(new String[oldLines.size()]);
		String[] nl= (String[]) newLines.toArray(new String[newLines.size()]);
		
		int oi= 0, ni= 0;
		
		while (true) {
			
			char oc= 0;
			String o= null;
			if (oi < ol.length) {
				o= ol[oi];
				oc= o.charAt(0);
			}
			
			char nc= 0;
			String n= null;
			if (ni < nl.length) {
				n= nl[ni];
				nc= n.charAt(0);
			}
			
			// EOF
			if (oc == 0 && nc == 0)
				break;
				
			// deletion in old
			if (oc == '-') {
				do {
					result.add('-' + o.substring(2));
					oi++;
					if (oi >= ol.length)
						break;
					o= ol[oi];
				} while (o.charAt(0) == '-');
				continue;
			}
			
			// addition in new
			if (nc == '+') {
				do {
					result.add('+' + n.substring(2));
					ni++;
					if (ni >= nl.length)
						break;
					n= nl[ni];
				} while (n.charAt(0) == '+');
				continue;
			}
			
			// differing lines on both sides
			if (oc == '!' && nc == '!') {
				// remove old
				do {
					result.add('-' + o.substring(2));
					oi++;
					if (oi >= ol.length)
						break;
					o= ol[oi];
				} while (o.charAt(0) == '!');
				
				// add new
				do {
					result.add('+' + n.substring(2));
					ni++;
					if (ni >= nl.length)
						break;
					n= nl[ni];
				} while (n.charAt(0) == '!');
				
				continue;
			}
			
			// context lines
			if (oc == ' ' && nc == ' ') {
				do {
					Assert.isTrue(o.equals(n), "non matching context lines"); //$NON-NLS-1$
					result.add(' ' + o.substring(2));
					oi++;
					ni++;
					if (oi >= ol.length || ni >= nl.length)
						break;
					o= ol[oi];
					n= nl[ni];
				} while (o.charAt(0) == ' ' && n.charAt(0) == ' ');
				continue;
			}
			
			if (oc == ' ') {
				do {
					result.add(' ' + o.substring(2));
					oi++;
					if (oi >= ol.length)
						break;
					o= ol[oi];
				} while (o.charAt(0) == ' ');
				continue;
			}

			if (nc == ' ') {
				do {
					result.add(' ' + n.substring(2));
					ni++;
					if (ni >= nl.length)
						break;
					n= nl[ni];
				} while (n.charAt(0) == ' ');
				continue;
			}
			
			Assert.isTrue(false, "unexpected char <" + oc + "> <" + nc + ">"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		
		return result;
	}
	
	/*
	 * @return the parsed time/date in milliseconds or IFilePatch.DATE_UNKNOWN
	 * (0) on error
	 */
	private long extractDate(String[] args, int n) {
		if (n < args.length) {
			String line= args[n];
			for (int i= 0; i < this.fDateFormats.length; i++) {
				this.fDateFormats[i].setLenient(true);
				try {
					Date date= this.fDateFormats[i].parse(line);
					return date.getTime();		
				} catch (ParseException ex) {
					// silently ignored
				}
			}
			// System.err.println("can't parse date: <" + line + ">");
		}
		return IFilePatch2.DATE_UNKNOWN;
	}
	
	/*
	 * Returns null if file name is "/dev/null".
	 */
	private IPath extractPath(String[] args, int n, String path2) {
		if (n < args.length) {
			String path= args[n];
			if (DEV_NULL.equals(path))
				return null;
			int pos= path.lastIndexOf(':');
			if (pos >= 0)
				path= path.substring(0, pos);
			if (path2 != null && !path2.equals(path)) {
				if (DEBUG) System.out.println("path mismatch: " + path2); //$NON-NLS-1$
				path= path2;
			}
			return new Path(path);
		}
		return null;
	}
	
	/*
	 * Tries to extract two integers separated by a comma.
	 * The parsing of the line starts at the position after
	 * the first occurrence of the given character start an ends
	 * at the first blank (or the end of the line).
	 * If only a single number is found this is assumed to be the start of a one line range.
	 * If an error occurs the range -1,-1 is returned.
	 */
	private void extractPair(String line, char start, int[] pair) {
		pair[0]= pair[1]= -1;
		int startPos= line.indexOf(start);
		if (startPos < 0) {
			if (DEBUG) System.out.println("parsing error in extractPair: couldn't find \'" + start + "\'"); //$NON-NLS-1$ //$NON-NLS-2$
			return;
		}
		line= line.substring(startPos+1);
		int endPos= line.indexOf(' ');
		if (endPos < 0) {
			if (DEBUG) System.out.println("parsing error in extractPair: couldn't find end blank"); //$NON-NLS-1$
			return;
		}
		line= line.substring(0, endPos);
		int comma= line.indexOf(',');
		if (comma >= 0) {
			pair[0]= Integer.parseInt(line.substring(0, comma));
			pair[1]= Integer.parseInt(line.substring(comma+1));
		} else {	// abbreviated form for one line patch
			pair[0]= Integer.parseInt(line);
			pair[1]= 1;
		}
	}
	
	
	/*
	 * Breaks the given string into tab separated substrings.
	 * Leading and trailing whitespace is removed from each token.
	 */ 
	private String[] split(String line) {
		List l= new ArrayList();
		StringTokenizer st= new StringTokenizer(line, "\t"); //$NON-NLS-1$
		while (st.hasMoreElements()) {
			String token= st.nextToken().trim();
			if (token.length() > 0)
 				l.add(token);
		}
		return (String[]) l.toArray(new String[l.size()]);
	}

	public boolean isWorkspacePatch() {
		return this.fIsWorkspacePatch;
	}

	public boolean isGitPatch() {
		return this.fIsGitPatch;
	}

	public DiffProject[] getDiffProjects() {
		return this.fDiffProjects;
	}

	public FilePatch2[] getDiffs() {
		return this.fDiffs;
	}
	
	public FilePatch2[] getAdjustedDiffs() {
		if (!isWorkspacePatch() || this.fDiffs.length == 0)
			return this.fDiffs;
		List result = new ArrayList();
		for (int i = 0; i < this.fDiffs.length; i++) {
			FilePatch2 diff = this.fDiffs[i];
			result.add(diff.asRelativeDiff());
		}
		return (FilePatch2[]) result.toArray(new FilePatch2[result.size()]);
	}

}
