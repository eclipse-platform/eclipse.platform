package org.eclipse.ant.internal.core.old;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000 The Apache Software Foundation.  All rights 
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer. 
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:  
 *       "This product includes software developed by the 
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Ant", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written 
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */

import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.PatternSet;
import org.apache.tools.ant.types.Reference;

import java.io.*;
import java.util.Enumeration;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * Named collection of include/exclude tags.  This is largely a copy of the original
 * Ant PatternSet data type.  Unfortunately, that datatype used both the comma and
 * the space character as a token to delimit pattern entries.  This makes it virtually
 * impossible to use for files which have spaces in their names.  The <code>CommaPatternSet</code>
 * is not particularly usable but it is easier than trying to deal with the original.
 * <p>
 * A complete copy was done to ensure that the behavior is correct.  Otherwise there is a
 * risk of getting some values from the private slots of the superclass and some from this
 * class.  Unfortunately, there is no way to link this support into <code>FileSet</code>.
 * </p>
 * <p>
 * <b>Note:</b> This class/interface is part of an interim API that is still under 
 * development and expected to change significantly before reaching stability. 
 * It is being made available at this early stage to solicit feedback from pioneering 
 * adopters on the understanding that any code that uses this API will almost 
 * certainly be broken (repeatedly) as the API evolves.
 * </p>
 * @see PatternSet
 */

public class CommaPatternSet extends PatternSet {
    private Vector includeList = new Vector();
    private Vector excludeList = new Vector();
    private Vector includesFileList = new Vector();
    private Vector excludesFileList = new Vector();

    public CommaPatternSet() {
        super();
    }

	/**
	 * Makes the receiver effectively a reference to another <code>PatternSet</code>
	 * instance.
	 *
	 * <p>Once this element becomes a reference its internal attributes and nested elements
	 * must not be modified.</p>
	 * 
	 * @param r the other <code>PatternSet</code>
	 * @exception BuildException
	 */
    public void setRefid(Reference r) throws BuildException {
        if (!includeList.isEmpty() || !excludeList.isEmpty()) {
            throw tooManyAttributes();
        }
        super.setRefid(r);
    }

	/**
	 * Adds a name entry to the receiver's include list.
	 * 
	 * @return the new name entry
	 */
    public NameEntry createInclude() {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        return addPatternToList(includeList);
    }

    /**
     * add a name entry on the include files list
     */
    public NameEntry createIncludesFile() {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        return addPatternToList(includesFileList);
    }
    
	/**
	 * Adds a name entry to the receiver's exclude list.
	 * 
	 * @return the new name entry
	 */
    public NameEntry createExclude() {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        return addPatternToList(excludeList);
    }
    
    /**
     * add a name entry on the exclude files list
     */
    public NameEntry createExcludesFile() {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        return addPatternToList(excludesFileList);
    }

	/**
	 * Sets the receiver's set of include patterns. Patterns can only be separated
	 * by a comma and by not a space (unlike PatternSet).
	 *
	 * @param includes the include patterns
	 */
    public void setIncludes(String includes) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        if (includes != null && includes.length() > 0) {
            StringTokenizer tok = new StringTokenizer(includes, ",", false);
            while (tok.hasMoreTokens()) {
                createInclude().setName(tok.nextToken());
            }
        }
    }

	/**
	 * Sets the receiver's set of exclude patterns. Patterns can only be separated
	 * by a comma and by not a space (unlike PatternSet).
	 *
	 * @param excludes the exclude patterns
	 */
    public void setExcludes(String excludes) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        if (excludes != null && excludes.length() > 0) {
            StringTokenizer tok = new StringTokenizer(excludes, ",", false);
            while (tok.hasMoreTokens()) {
                createExclude().setName(tok.nextToken());
            }
        }
    }

	/**
	 * Adds a name entry to the given list.
	 * 
	 * @return the new name entry
	 * @param list the target list
	 */
    private NameEntry addPatternToList(Vector list) {
        NameEntry result = new NameEntry();
        list.addElement(result);
        return result;
    }

	/**
	 * Sets the name of the file containing the includes patterns.
	 *
	 * @param incl the file to retrieve the include patterns from
	 */
     public void setIncludesfile(File includesFile) throws BuildException {
         if (isReference()) {
             throw tooManyAttributes();
         }
         createIncludesFile().setName(includesFile.getAbsolutePath());
     }

	/**
	 * Sets the name of the file containing the excludes patterns.
	 *
	 * @param excl the file to retrieve the exclude patterns from
	 */
     public void setExcludesfile(File excludesFile) throws BuildException {
         if (isReference()) {
             throw tooManyAttributes();
         }
         createExcludesFile().setName(excludesFile.getAbsolutePath());
     }
    
	/**
	 * Reads path matching patterns from a file and adds them to the
	 * includes or excludes list as appropriate.
	 * 
	 * @param patternfile the source file
	 * @param patternlist the list of patterns
	 * @param p the target project
	 * @exception BuildException thrown if the file cannot be read
	 */
    private void readPatterns(File patternfile, Vector patternlist, Project p)
        throws BuildException {
        
        try {
            // Get a FileReader
            BufferedReader patternReader = 
                new BufferedReader(new FileReader(patternfile)); 
        
            // Create one NameEntry in the appropriate pattern list for each 
            // line in the file.
            String line = patternReader.readLine();
            while (line != null) {
                if (line.length() > 0) {
                    line = ProjectHelper.replaceProperties(p, line,
                                                           p.getProperties());
                    addPatternToList(patternlist).setName(line);
                }
                line = patternReader.readLine();
            }
        } catch(IOException ioe)  {
			throw new BuildException(Policy.bind("exception.patternFile",patternfile.toString()),ioe);
        }
    }

	/**
	 * Adds the patterns of another <code>PatternSet</code> to the receiver.
	 * 
	 * @param other the other <code>PatternSet</code>
	 * @param p the target project
	 */
    public void append(PatternSet other, Project p) {
        if (isReference()) {
            throw new BuildException(Policy.bind("exception.cannotAppendToReference"));
        }

        String[] incl = other.getIncludePatterns(p);
        if (incl != null) {
            for (int i=0; i<incl.length; i++) {
                createInclude().setName(incl[i]);
            }
        }
        
        String[] excl = other.getExcludePatterns(p);
        if (excl != null) {
            for (int i=0; i<excl.length; i++) {
                createExclude().setName(excl[i]);
            }
        }
    }

	/**
	 * Returns the receiver's filtered include patterns.
	 * 
	 * @return the receiver's filtered include patterns.
	 * @param the target project
	 */
    public String[] getIncludePatterns(Project p) {
        if (isReference()) {
            return getRef(p).getIncludePatterns(p);
        } else {
            readFiles(p);
            return makeArray(includeList, p);
        }
    }

	/**
	 * Returns the receiver's filtered exclude patterns.
	 * 
	 * @return the receiver's filtered exclude patterns.
	 * @param the target project
	 */
    public String[] getExcludePatterns(Project p) {
        if (isReference()) {
            return getRef(p).getExcludePatterns(p);
        } else {
            readFiles(p);
            return makeArray(excludeList, p);
        }
    }

	/**
	 * Returns a boolean indicating whether this instance has any patterns.
	 * 
	 * @return a boolean indicating whether this instance has any patterns
	 */
    boolean hasPatterns() {
        return includesFileList.size() > 0 || excludesFileList.size() > 0 
            || includeList.size() > 0 || excludeList.size() > 0;
    }

	/**
	 * Performs a check for circular references and returns the
	 * referenced <code>PatternSet</code>.
	 * 
	 * @return the referenced <code>PatternSet</code>
	 * @param the target project
	 */
    private PatternSet getRef(Project p) {
        if (!checked) {
            Stack stk = new Stack();
            stk.push(this);
            dieOnCircularReference(stk, p);
        }
        
        Object o = ref.getReferencedObject(p);
        if (!(o instanceof PatternSet)) {
			throw new BuildException(Policy.bind("exception.notAPatternSet",ref.getRefId()));
    	} else {
            return (PatternSet) o;
        }
    }

	/**
	 * Returns a given vector of name entries as an array of strings.
	 * 
	 * @return a string array of name entries
	 * @param list the original vector of name entries
	 * @param p the target project
	 */
    private String[] makeArray(Vector list, Project p) {
        if (list.size() == 0) return null;

        Vector tmpNames = new Vector();
        for (Enumeration e = list.elements() ; e.hasMoreElements() ;) {
            NameEntry ne = (NameEntry)e.nextElement();
            String pattern = ne.evalName(p);
            if (pattern != null && pattern.length() > 0) {
                tmpNames.addElement(pattern);
            }
        }

        String result[] = new String[tmpNames.size()];
        tmpNames.copyInto(result);
        return result;
    }
        
	/**
	 * Reads includefile and excludefile if not already done.
	 * 
	 * @param p the target project
	 */
    private void readFiles(Project p) {
        if (includesFileList.size() > 0) {
            Enumeration e = includesFileList.elements();
            while (e.hasMoreElements()) {
                NameEntry ne = (NameEntry)e.nextElement();
                String fileName = ne.evalName(p);
                if (fileName != null) {
                    File inclFile = p.resolveFile(fileName);
                    if (!inclFile.exists())
                        throw new BuildException("Includesfile "
                                                 + inclFile.getAbsolutePath()
                                                 + " not found.");
                    readPatterns(inclFile, includeList, p);
                }
            }
            includesFileList.removeAllElements();
        }

        if (excludesFileList.size() > 0) {
            Enumeration e = excludesFileList.elements();
            while (e.hasMoreElements()) {
                NameEntry ne = (NameEntry)e.nextElement();
                String fileName = ne.evalName(p);
                if (fileName != null) {
                    File exclFile = p.resolveFile(fileName);
                    if (!exclFile.exists())
                        throw new BuildException("Excludesfile "
                                                 + exclFile.getAbsolutePath()
                                                 + " not found.");
                    readPatterns(exclFile, excludeList, p);
                }
            }
            excludesFileList.removeAllElements();
        }
    }

    public String toString()
    {
        return "patternSet{ includes: " + includeList + 
            " excludes: " + excludeList + " }";
    }
}

