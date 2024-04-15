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
 *     Mickael Istria (Red Hat Inc.) - [263316] regexp for file association
 *******************************************************************************/
package org.eclipse.core.internal.content;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.content.*;
import org.eclipse.core.runtime.content.IContentTypeManager.ISelectionPolicy;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.osgi.util.NLS;

public final class ContentTypeCatalog {
	private static final IContentType[] NO_CONTENT_TYPES = new IContentType[0];

	/**
	 * All fields are guarded by lock on "this"
	 */
	private final Map<ContentType, ContentType[]> allChildren = new HashMap<>();
	private final Map<String, IContentType> contentTypes = new HashMap<>();
	private final Map<String, Set<ContentType>> fileExtensions = new HashMap<>();
	private final Map<String, Set<ContentType>> fileNames = new HashMap<>();
	private final Map<String, Pattern> compiledRegexps = new HashMap<>();
	private final Map<Pattern, String> initialPatternForRegexp = new HashMap<>();
	private final Map<Pattern, Set<ContentType>> fileRegexps = new HashMap<>();
	private final int generation;
	private final ContentTypeManager manager;

	/**
	 * Return true if type1 is an ancestor of type2 or if type2 is an ancestor of
	 * type1
	 *
	 * @return true type1 is ancestor or type2, or vice versa. false otherwise
	 */
	private static boolean isAncestor(ContentType type1, ContentType type2) {
		return type1.isKindOf(type2) || type2.isKindOf(type1);
	}

	/**
	 * A sorting policy where the more generic content type wins. Lexicographical
	 * comparison is done as a last resort when all other criteria fail.
	 */
	private final Comparator<IContentType> policyConstantGeneralIsBetter = (IContentType o1, IContentType o2) -> {
		ContentType type1 = (ContentType) o1;
		ContentType type2 = (ContentType) o2;
		if (isAncestor(type1, type2)) {
			// first criteria: depth - the lower, the better
			int depthCriteria = type1.getDepth() - type2.getDepth();
			if (depthCriteria != 0)
				return depthCriteria;
		}
		// second criteria: priority - the higher, the better
		int priorityCriteria = type1.getPriority() - type2.getPriority();
		if (priorityCriteria != 0)
			return -priorityCriteria;
		// they have same depth and priority - choose one arbitrarily (stability is important)
		return type1.getId().compareTo(type2.getId());
	};

	/**
	 * A sorting policy where the more specific content type wins. Lexicographical comparison is done
	 * as a last resort when all other criteria fail.
	 */
	private final Comparator<IContentType> policyConstantSpecificIsBetter = (IContentType o1, IContentType o2) -> {
		ContentType type1 = (ContentType) o1;
		ContentType type2 = (ContentType) o2;
		if (isAncestor(type1, type2)) {
			// first criteria: depth - the higher, the better
			int depthCriteria = type1.getDepth() - type2.getDepth();
			if (depthCriteria != 0)
				return -depthCriteria;
		}
		// second criteria: priority - the higher, the better
		int priorityCriteria = type1.getPriority() - type2.getPriority();
		if (priorityCriteria != 0)
			return -priorityCriteria;
		// they have same depth and priority - choose one arbitrarily (stability is important)
		return type1.getId().compareTo(type2.getId());
	};

	/**
	 * A sorting policy where the more general content type wins.
	 */
	private final Comparator<IContentType> policyGeneralIsBetter = (IContentType o1, IContentType o2) -> {
		ContentType type1 = (ContentType) o1;
		ContentType type2 = (ContentType) o2;
		if (isAncestor(type1, type2)) {
			// first criteria: depth - the lower, the better
			int depthCriteria = type1.getDepth() - type2.getDepth();
			if (depthCriteria != 0)
				return depthCriteria;
		}
		// second criteria: priority - the higher, the better
		int priorityCriteria = type1.getPriority() - type2.getPriority();
		if (priorityCriteria != 0)
			return -priorityCriteria;
		return 0;
	};

	/**
	 * A sorting policy where content types are sorted by id.
	 */
	private final Comparator<IContentType> policyLexicographical = (IContentType o1, IContentType o2) -> {
		ContentType type1 = (ContentType) o1;
		ContentType type2 = (ContentType) o2;
		return type1.getId().compareTo(type2.getId());
	};
	/**
	 * A sorting policy where the more specific content type wins.
	 */
	private final Comparator<IContentType> policySpecificIsBetter = (IContentType o1, IContentType o2) -> {
		ContentType type1 = (ContentType) o1;
		ContentType type2 = (ContentType) o2;
		if (isAncestor(type1, type2)) {
			// first criteria: depth - the higher, the better
			int depthCriteria = type1.getDepth() - type2.getDepth();
			if (depthCriteria != 0)
				return -depthCriteria;
		}
		// second criteria: priority - the higher, the better
		int priorityCriteria = type1.getPriority() - type2.getPriority();
		if (priorityCriteria != 0)
			return -priorityCriteria;
		return 0;
	};

	private static IContentType[] concat(IContentType[][] types) {
		int size = 0;
		IContentType[] nonEmptyOne = NO_CONTENT_TYPES;
		for (IContentType[] array : types) {
			size += array.length;
			if (array.length > 0) {
				nonEmptyOne = array;
			}
		}
		if (nonEmptyOne.length == size) { // no other array has content
			return nonEmptyOne;
		}
		IContentType[] result = new IContentType[size];
		int currentIndex = 0;
		for (IContentType[] array : types) {
			System.arraycopy(array, 0, result, currentIndex, array.length);
			currentIndex += array.length;
		}
		return result;
	}

	public ContentTypeCatalog(ContentTypeManager manager, int generation) {
		this.manager = manager;
		this.generation = generation;
	}

	synchronized void addContentType(IContentType contentType) {
		contentTypes.put(contentType.getId(), contentType);
	}

	/**
	 * Applies a client-provided selection policy.
	 */
	private IContentType[] applyPolicy(final IContentTypeManager.ISelectionPolicy policy, final IContentType[] candidates, final boolean fileName, final boolean contents) {
		final IContentType[][] result = new IContentType[][] {candidates};
		SafeRunner.run(new ISafeRunnable() {
			@Override
			public void handleException(Throwable exception) {
				// already logged in SafeRunner#run()
				// default result is the original array
				// nothing to be done
			}

			@Override
			public void run() throws Exception {
				result[0] = policy.select(candidates, fileName, contents);
			}
		});
		return result[0];
	}

	private void associate(ContentType contentType) {
		String[] builtInFileNames = contentType.getFileSpecs(IContentType.IGNORE_USER_DEFINED | IContentType.FILE_NAME_SPEC);
		for (String builtInFileName : builtInFileNames)
			associate(contentType, builtInFileName, IContentType.FILE_NAME_SPEC);
		String[] builtInFileExtensions = contentType.getFileSpecs(IContentType.IGNORE_USER_DEFINED | IContentType.FILE_EXTENSION_SPEC);
		for (String builtInFileExtension : builtInFileExtensions)
			associate(contentType, builtInFileExtension, IContentType.FILE_EXTENSION_SPEC);
		String[] builtInFilePatterns = contentType
				.getFileSpecs(IContentType.IGNORE_USER_DEFINED | IContentType.FILE_PATTERN_SPEC);
		for (String builtInFilePattern : builtInFilePatterns) {
			associate(contentType, builtInFilePattern, IContentType.FILE_PATTERN_SPEC);
		}
	}

	String toRegexp(String filePattern) {
		return filePattern.replace(".", "\\.").replace('?', '.').replace("*", ".*"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	}

	synchronized void associate(ContentType contentType, String text, int type) {
		Map<String, Set<ContentType>> fileSpecMap = null;
		if ((type & IContentType.FILE_NAME_SPEC) != 0) {
			fileSpecMap = fileNames;
		} else if ((type & IContentType.FILE_EXTENSION_SPEC) != 0) {
			fileSpecMap = fileExtensions;
		}
		if (fileSpecMap != null) {
			String mappingKey = FileSpec.getMappingKeyFor(text);
			Set<ContentType> existing = fileSpecMap.get(mappingKey);
			if (existing == null)
				fileSpecMap.put(mappingKey, existing = new HashSet<>());
			existing.add(contentType);
		} else if ((type & IContentType.FILE_PATTERN_SPEC) != 0) {
			Pattern compiledPattern = compiledRegexps.get(text);
			if (compiledPattern == null) {
				compiledPattern = Pattern.compile(toRegexp(text));
				compiledRegexps.put(text, compiledPattern);
				initialPatternForRegexp.put(compiledPattern, text);
				fileRegexps.put(compiledPattern, new HashSet<>());
			}
			fileRegexps.get(compiledPattern).add(contentType);
		}
	}

	private int collectMatchingByContents(int valid, IContentType[] subset, List<ContentType> destination, ILazySource contents, Map<String, Object> properties) throws IOException {
		for (IContentType element : subset) {
			ContentType current = (ContentType) element;
			IContentDescriber describer = current.getDescriber();
			int status = IContentDescriber.INDETERMINATE;
			if (describer != null) {
				if (contents.isText() && !(describer instanceof ITextContentDescriber))
					// for text streams we skip content types that do not provide text-based content describers
					continue;
				status = describe(current, contents, null, properties);
				if (status == IContentDescriber.INVALID)
					continue;
			}
			if (status == IContentDescriber.VALID)
				destination.add(valid++, current);
			else
				destination.add(current);
		}
		return valid;
	}

	@SuppressWarnings("deprecation")
	int describe(ContentType type, ILazySource contents, ContentDescription description, Map<String, Object> properties) throws IOException {
		IContentDescriber describer = type.getDescriber();
		try {
			if (contents.isText()) {
				if (describer instanceof XMLRootElementContentDescriber2) {
					return ((XMLRootElementContentDescriber2) describer).describe((Reader) contents, description, properties);
				} else if (describer instanceof XMLRootElementContentDescriber) {
					return ((XMLRootElementContentDescriber) describer).describe((Reader) contents, description, properties);
				}
				return ((ITextContentDescriber) describer).describe((Reader) contents, description);
			} else {
				if (describer instanceof XMLRootElementContentDescriber2) {
					return ((XMLRootElementContentDescriber2) describer).describe((InputStream) contents, description, properties);
				} else if (describer instanceof XMLRootElementContentDescriber) {
					return ((XMLRootElementContentDescriber) describer).describe((InputStream) contents, description, properties);
				}
				return (describer).describe((InputStream) contents, description);
			}
		} catch (RuntimeException re) {
			// describer seems to be buggy. just disable it (logging the reason)
			type.invalidateDescriber(re);
		} catch (Error e) {
			// describer got some serious problem. disable it (logging the reason) and throw the error again
			type.invalidateDescriber(e);
			throw e;
		} catch (LowLevelIOException llioe) {
			// throw the actual exception
			throw llioe.getActualException();
		} catch (IOException ioe) {
			// bugs 67841/ 62443  - non-low level IOException should be "ignored"
			String message = NLS.bind(ContentMessages.content_errorReadingContents, type.getId());
			ContentType.log(message, ioe);
			// we don't know what the describer would say if the exception didn't occur
			return IContentDescriber.INDETERMINATE;
		} finally {
			contents.rewind();
		}
		return IContentDescriber.INVALID;
	}

	synchronized void dissociate(ContentType contentType, String text, int type) {
		Map<String, Set<ContentType>> fileSpecMap = null;
		if ((type & IContentType.FILE_NAME_SPEC) != 0) {
			fileSpecMap = fileNames;
		} else if ((type & IContentType.FILE_EXTENSION_SPEC) != 0) {
			fileSpecMap = fileExtensions;
		}
		if (fileSpecMap != null) {
			String mappingKey = FileSpec.getMappingKeyFor(text);
			Set<ContentType> existing = fileSpecMap.get(mappingKey);
			if (existing == null)
				return;
			existing.remove(contentType);
		} else if ((type & IContentType.FILE_PATTERN_SPEC) != 0) {
			Pattern pattern = compiledRegexps.get(text);
			fileRegexps.get(pattern).remove(contentType);
		}
	}

	/**
	 * A content type will be valid if:
	 * <ol>
	 * <li>it does not designate a base type, or</li>
	 * <li>it designates a base type that exists and is valid</li>
	 * </ol>
	 * <p>And</p>:
	 * <ol>
	 * <li>it does not designate an alias type, or</li>
	 * <li>it designates an alias type that does not exist, or</li>
	 * <li>it designates an alias type that exists and is valid</li>
	 * </ol>
	 */
	private boolean ensureValid(ContentType type) {
		if (type.getValidation() != ContentType.STATUS_UNKNOWN)
			// already processed
			return type.isValid();
		// set this type temporarily as invalid to prevent cycles
		// all types in a cycle would remain as invalid
		type.setValidation(ContentType.STATUS_INVALID);
		if (type.isAlias())
			// it is an alias, leave as invalid
			return false;
		// check base type
		ContentType baseType = null;
		if (type.getBaseTypeId() != null) {
			baseType = (ContentType) contentTypes.get(type.getBaseTypeId());
			if (baseType == null)
				// invalid: specified base type is not known
				return false;
			// base type exists, ensure it is valid
			baseType = baseType.getAliasTarget(true);
			ensureValid(baseType);
			if (baseType.getValidation() != ContentType.STATUS_VALID)
				// invalid: base type was invalid
				return false;
		}
		// valid: all conditions satisfied
		type.setValidation(ContentType.STATUS_VALID);
		type.setBaseType(baseType);
		return true;
	}

	IContentType[] findContentTypesFor(ContentTypeMatcher matcher, InputStream contents, String fileName) throws IOException {
		final ILazySource buffer = ContentTypeManager.readBuffer(contents);
		IContentType[] selected = internalFindContentTypesFor(matcher, buffer, fileName, true);
		// give the policy a chance to change the results
		ISelectionPolicy policy = matcher.getPolicy();
		if (policy != null)
			selected = applyPolicy(policy, selected, fileName != null, true);
		return selected;
	}

	IContentType[] findContentTypesFor(ContentTypeMatcher matcher, final String fileName) {
		IContentType[] selected = concat(internalFindContentTypesFor(matcher, fileName, policyConstantGeneralIsBetter));
		// give the policy a chance to change the results
		ISelectionPolicy policy = matcher.getPolicy();
		if (policy != null)
			selected = applyPolicy(policy, selected, true, false);
		return selected;
	}

	synchronized public IContentType[] getAllContentTypes() {
		List<ContentType> result = new ArrayList<>(contentTypes.size());
		for (IContentType iContentType : contentTypes.values()) {
			ContentType type = (ContentType) iContentType;
			if (type.isValid() && !type.isAlias())
				result.add(type);
		}
		return result.toArray(new IContentType[result.size()]);
	}

	private ContentType[] getChildren(ContentType parent) {
		ContentType[] children = allChildren.get(parent);
		if (children != null)
			return children;
		List<ContentType> result = new ArrayList<>(5);
		for (IContentType iContentType : this.contentTypes.values()) {
			ContentType next = (ContentType) iContentType;
			if (next.getBaseType() == parent)
				result.add(next);
		}
		children = result.toArray(new ContentType[result.size()]);
		allChildren.put(parent, children);
		return children;
	}

	public ContentType getContentType(String contentTypeIdentifier) {
		ContentType type = internalGetContentType(contentTypeIdentifier);
		return (type != null && type.isValid() && !type.isAlias()) ? type : null;
	}

	private IContentDescription getDescriptionFor(ContentTypeMatcher matcher, ILazySource contents, String fileName, QualifiedName[] options) throws IOException {
		IContentType[] selected = internalFindContentTypesFor(matcher, contents, fileName, false);
		if (selected.length == 0)
			return null;
		// give the policy a chance to change the results
		ISelectionPolicy policy = matcher.getPolicy();
		if (policy != null) {
			selected = applyPolicy(policy, selected, fileName != null, true);
			if (selected.length == 0)
				return null;
		}
		return matcher.getSpecificDescription(((ContentType) selected[0]).internalGetDescriptionFor(contents, options));
	}

	public IContentDescription getDescriptionFor(ContentTypeMatcher matcher, InputStream contents, String fileName, QualifiedName[] options) throws IOException {
		return getDescriptionFor(matcher, ContentTypeManager.readBuffer(contents), fileName, options);
	}

	public IContentDescription getDescriptionFor(ContentTypeMatcher matcher, Reader contents, String fileName, QualifiedName[] options) throws IOException {
		return getDescriptionFor(matcher, ContentTypeManager.readBuffer(contents), fileName, options);
	}

	public int getGeneration() {
		return generation;
	}

	public ContentTypeManager getManager() {
		return manager;
	}

	private boolean internalAccept(ContentTypeVisitor visitor, ContentType root) {
		if (!root.isValid() || root.isAlias())
			return true;
		int result = visitor.visit(root);
		switch (result) {
				// stop traversing the tree
			case ContentTypeVisitor.STOP :
				return false;
				// stop traversing this subtree
			case ContentTypeVisitor.RETURN :
				return true;
		}
		ContentType[] children = getChildren(root);
		if (children == null)
			// this content type has no sub-types - keep traversing the tree
			return true;
		for (ContentType c : children) {
			if (!internalAccept(visitor, c)) {
				// stop the traversal
				return false;
			}
		}
		return true;
	}

	private IContentType[] internalFindContentTypesFor(ILazySource buffer, IContentType[][] subset, Comparator<IContentType> validPolicy, Comparator<IContentType> indeterminatePolicy) throws IOException {
		Map<String, Object> properties = new HashMap<>();
		final List<ContentType> appropriate = new ArrayList<>(5);
		final int validFullName = collectMatchingByContents(0, subset[0], appropriate, buffer, properties);
		final int appropriateFullName = appropriate.size();
		final int validExtension = collectMatchingByContents(validFullName, subset[1], appropriate, buffer, properties) - validFullName;
		final int appropriateExtension = appropriate.size() - appropriateFullName;
		final int validPattern = collectMatchingByContents(validExtension, subset[2], appropriate, buffer, properties)
				- validExtension;
		final int appropriatePattern = appropriate.size() - appropriateFullName - appropriateExtension;
		IContentType[] result = appropriate.toArray(new IContentType[appropriate.size()]);
		if (validFullName > 1)
			Arrays.sort(result, 0, validFullName, validPolicy);
		if (validExtension > 1)
			Arrays.sort(result, validFullName, validFullName + validExtension, validPolicy);
		if (validPattern > 1) {
			Arrays.sort(result, validFullName + validExtension, validFullName + validExtension + validPattern,
					validPolicy);
		}
		if (appropriateFullName - validFullName > 1)
			Arrays.sort(result, validFullName + validExtension, appropriateFullName + validExtension, indeterminatePolicy);
		if (appropriateExtension - validExtension > 1)
			Arrays.sort(result, appropriateFullName + validExtension, appropriate.size() - validPattern,
					indeterminatePolicy);
		if (appropriatePattern - validPattern > 1) {
			Arrays.sort(result, appropriate.size() - validPattern, appropriate.size(), indeterminatePolicy);
		}
		return result;
	}

	private IContentType[] internalFindContentTypesFor(ContentTypeMatcher matcher, ILazySource buffer, String fileName, boolean forceValidation) throws IOException {
		final IContentType[][] subset;
		final Comparator<IContentType> validPolicy;
		Comparator<IContentType> indeterminatePolicy;
		if (fileName == null) {
			// we only have a single array, by need to provide a two-dimensional, 3-element
			// array
			subset = new IContentType[][] { getAllContentTypes(), NO_CONTENT_TYPES, NO_CONTENT_TYPES };
			indeterminatePolicy = policyConstantGeneralIsBetter;
			validPolicy = policyConstantSpecificIsBetter;
		} else {
			subset = internalFindContentTypesFor(matcher, fileName, policyLexicographical);
			indeterminatePolicy = policyGeneralIsBetter;
			validPolicy = policySpecificIsBetter;
		}
		int total = subset[0].length + subset[1].length + subset[2].length;
		if (total == 0)
			// don't do further work if subset is empty
			return NO_CONTENT_TYPES;
		if (!forceValidation && total == 1) {
			// do not do validation if not forced and only one was found (caller will validate later)
			IContentType[] found = subset[0].length == 1 ? subset[0] : (subset[1].length == 1 ? subset[1] : subset[2]);
			// bug 100032 - ignore binary content type if contents are text
			if (!buffer.isText())
				// binary buffer, caller can call the describer with no risk
				return found;
			// text buffer, need to check describer
			IContentDescriber describer = ((ContentType) found[0]).getDescriber();
			if (describer == null || describer instanceof ITextContentDescriber)
				// no describer or text describer, that is fine
				return found;
			// only eligible content type is binary and contents are text, ignore it
			return NO_CONTENT_TYPES;
		}
		return internalFindContentTypesFor(buffer, subset, validPolicy, indeterminatePolicy);
	}

	/**
	 * This is the implementation for file name based content type matching.
	 *
	 * @return all matching content types in the preferred order
	 * @see IContentTypeManager#findContentTypesFor(String)
	 */
	synchronized private IContentType[][] internalFindContentTypesFor(ContentTypeMatcher matcher, final String fileName, Comparator<IContentType> sortingPolicy) {
		IScopeContext context = matcher.getContext();
		IContentType[][] result = { NO_CONTENT_TYPES, NO_CONTENT_TYPES, NO_CONTENT_TYPES };

		Set<ContentType> existing = new HashSet<>();

		final Set<ContentType> allByFileName;
		if (context.equals(manager.getContext()))
			allByFileName = getDirectlyAssociated(fileName, IContentTypeSettings.FILE_NAME_SPEC);
		else {
			allByFileName = new HashSet<>(getDirectlyAssociated(fileName, IContentTypeSettings.FILE_NAME_SPEC | IContentType.IGNORE_USER_DEFINED));
			allByFileName.addAll(matcher.getDirectlyAssociated(this, fileName, IContentTypeSettings.FILE_NAME_SPEC));
		}
		Set<ContentType> selectedByName = selectMatchingByName(context, allByFileName, Collections.emptySet(), fileName,
				IContentType.FILE_NAME_SPEC);
		existing.addAll(selectedByName);
		result[0] = selectedByName.toArray(new IContentType[selectedByName.size()]);
		if (result[0].length > 1)
			Arrays.sort(result[0], sortingPolicy);

		final String fileExtension = ContentTypeManager.getFileExtension(fileName);
		if (fileExtension != null) {
			final Set<ContentType> allByFileExtension;
			if (context.equals(manager.getContext()))
				allByFileExtension = getDirectlyAssociated(fileExtension, IContentTypeSettings.FILE_EXTENSION_SPEC);
			else {
				allByFileExtension = new HashSet<>(getDirectlyAssociated(fileExtension, IContentTypeSettings.FILE_EXTENSION_SPEC | IContentType.IGNORE_USER_DEFINED));
				allByFileExtension.addAll(matcher.getDirectlyAssociated(this, fileExtension, IContentTypeSettings.FILE_EXTENSION_SPEC));
			}
			Set<ContentType> selectedByExtension = selectMatchingByName(context, allByFileExtension, selectedByName, fileExtension, IContentType.FILE_EXTENSION_SPEC);
			existing.addAll(selectedByExtension);
			if (!selectedByExtension.isEmpty())
				result[1] = selectedByExtension.toArray(new IContentType[selectedByExtension.size()]);
		}
		if (result[1].length > 1)
			Arrays.sort(result[1], sortingPolicy);

		final Set<ContentType> allByFilePattern;
		if (context.equals(manager.getContext()))
			allByFilePattern = getMatchingRegexpAssociated(fileName, IContentTypeSettings.FILE_PATTERN_SPEC);
		else {
			allByFilePattern = new HashSet<>(getMatchingRegexpAssociated(fileName,
					IContentTypeSettings.FILE_PATTERN_SPEC | IContentType.IGNORE_USER_DEFINED));
			allByFilePattern
					.addAll(matcher.getMatchingRegexpAssociated(this, fileName,
							IContentTypeSettings.FILE_PATTERN_SPEC));
		}
		existing.addAll(allByFilePattern);
		if (!allByFilePattern.isEmpty())
			result[2] = allByFilePattern.toArray(new IContentType[allByFilePattern.size()]);

		return result;
	}

	private Set<ContentType> getMatchingRegexpAssociated(String fileName, int typeMask) {
		if ((typeMask & IContentType.FILE_PATTERN_SPEC) == 0) {
			throw new IllegalArgumentException("This method requires FILE_PATTERN_SPEC."); //$NON-NLS-1$
		}
		Set<ContentType> res = new HashSet<>();
		for (Entry<Pattern, Set<ContentType>> spec : this.fileRegexps.entrySet()) {
			if (spec.getKey().matcher(fileName).matches()) {
				res.addAll(filterOnDefinitionSource(initialPatternForRegexp.get(spec.getKey()), typeMask,
						spec.getValue()));
			}
		}
		return res;
	}

	/**
	 * Returns content types directly associated with the given file spec.
	 *
	 * @param text a file name or extension
	 * @param typeMask a bit-wise or of the following flags:
	 * <ul>
	 * 		<li>IContentType.FILE_NAME, </li>
	 * 		<li>IContentType.FILE_EXTENSION, </li>
	 * 		<li>IContentType.IGNORE_PRE_DEFINED, </li>
	 * 		<li>IContentType.IGNORE_USER_DEFINED</li>
	 *	</ul>
	 * @return a set of content types
	 */
	private Set<ContentType> getDirectlyAssociated(String text, int typeMask) {
		if ((typeMask & IContentType.FILE_PATTERN_SPEC) != 0) {
			throw new IllegalArgumentException("This method don't allow FILE_REGEXP_SPEC."); //$NON-NLS-1$
		}
		Map<String, Set<ContentType>> associations = (typeMask & IContentTypeSettings.FILE_NAME_SPEC) != 0 ? fileNames : fileExtensions;
		Set<ContentType> result = associations.get(FileSpec.getMappingKeyFor(text));
		if ((typeMask & (IContentType.IGNORE_PRE_DEFINED | IContentType.IGNORE_USER_DEFINED)) != 0) {
			result = filterOnDefinitionSource(text, typeMask, result);
		}
		return result == null ? Collections.EMPTY_SET : result;
	}

	/**
	 * Filters a set of content-types on whether they have a mapping that matches
	 * provided criteria.
	 *
	 * @param text
	 *            file name, file extension or file regexp (depending on value of
	 *            {@code typeMask}.
	 * @param typeMask
	 *            the type mask. Spec type, and definition source (pre-defined or
	 *            user-defined) will be used
	 * @param contentTypes
	 *            content types to filter from (not modified during method
	 *            execution)
	 * @return set of filtered content-type
	 */
	private Set<ContentType> filterOnDefinitionSource(String text, int typeMask, Set<ContentType> contentTypes) {
		if ((typeMask & (IContentType.IGNORE_PRE_DEFINED | IContentType.IGNORE_USER_DEFINED)) == 0) {
			return contentTypes;
		}
		if (contentTypes != null && !contentTypes.isEmpty()) {
			// copy so we can modify
			contentTypes = new HashSet<>(contentTypes);
			// invert the last two bits so it is easier to compare
			typeMask ^= (IContentType.IGNORE_PRE_DEFINED | IContentType.IGNORE_USER_DEFINED);
			for (Iterator<ContentType> i = contentTypes.iterator(); i.hasNext();) {
				ContentType contentType = i.next();
				if (!contentType.hasFileSpec(text, typeMask, true))
					i.remove();
			}
		}
		return contentTypes;
	}

	synchronized ContentType internalGetContentType(String contentTypeIdentifier) {
		return (ContentType) contentTypes.get(contentTypeIdentifier);
	}

	private void makeAliases() {
		// process all content types marking aliases appropriately
		for (IContentType iContentType : contentTypes.values()) {
			ContentType type = (ContentType) iContentType;
			String targetId = type.getAliasTargetId();
			if (targetId == null)
				continue;
			ContentType target = internalGetContentType(targetId);
			if (target != null)
				type.setAliasTarget(target);
		}
	}

	/**
	 * Resolves inter-content type associations (inheritance and aliasing).
	 */
	synchronized protected void organize() {
		// build the aliasing
		makeAliases();
		// do the validation
		for (IContentType iContentType : contentTypes.values()) {
			ContentType type = (ContentType) iContentType;
			if (ensureValid(type))
				associate(type);
		}
		if (ContentTypeManager.DebuggingHolder.DEBUGGING)
			for (IContentType iContentType : contentTypes.values()) {
				ContentType type = (ContentType) iContentType;
				if (!type.isValid())
					ContentMessages.message("Invalid: " + type); //$NON-NLS-1$
			}
	}

	/**
	 * Processes all content types in source, adding those matching the given file spec to the
	 * destination collection.
	 */
	private Set<ContentType> selectMatchingByName(final IScopeContext context, Collection<ContentType> source, final Collection<ContentType> existing, final String fileSpecText, final int fileSpecType) {
		if (source == null || source.isEmpty())
			return Collections.EMPTY_SET;
		final Set<ContentType> destination = new HashSet<>(5);
		// process all content types in the given collection
		for (ContentType root : source) {
			// From a given content type, check if it matches, and
			// include any children that match as well.
			internalAccept(contentType -> {
				if (contentType != root && contentType.hasBuiltInAssociations())
					// this content type has built-in associations - visit it later as root
					return ContentTypeVisitor.RETURN;
				if (contentType == root && !contentType.hasFileSpec(context, fileSpecText, fileSpecType))
					// it is the root and does not match the file name - do not add it nor look into its children
					return ContentTypeVisitor.RETURN;
				// either the content type is the root and matches the file name or
				// is a sub content type and does not have built-in files specs
				if (!existing.contains(contentType))
					destination.add(contentType);
				return ContentTypeVisitor.CONTINUE;
			}, root);
		}
		return destination;
	}

	void removeContentType(String contentTypeIdentifier) throws CoreException {
		ContentType contentType = getContentType(contentTypeIdentifier);
		if (contentType == null) {
			return;
		}
		if (!contentType.isUserDefined()) {
			throw new IllegalArgumentException("Content type must be user-defined."); //$NON-NLS-1$
		}
		contentTypes.remove(contentType.getId());
	}

}
