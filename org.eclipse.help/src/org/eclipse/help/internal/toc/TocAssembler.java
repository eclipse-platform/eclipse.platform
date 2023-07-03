/*******************************************************************************
 * Copyright (c) 2006, 2020 IBM Corporation and others.
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
 *     George Suaridze <suag@1c.ru> (1C-Soft LLC) - Bug 560168
 *******************************************************************************/
package org.eclipse.help.internal.toc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.ILog;
import org.eclipse.help.ICriteria;
import org.eclipse.help.ITocContribution;
import org.eclipse.help.IUAElement;
import org.eclipse.help.internal.Anchor;
import org.eclipse.help.internal.HelpPlugin;
import org.eclipse.help.internal.Topic;
import org.eclipse.help.internal.UAElement;
import org.eclipse.help.internal.criteria.CriteriaProviderRegistry;
import org.eclipse.help.internal.dynamic.DocumentProcessor;
import org.eclipse.help.internal.dynamic.DocumentReader;
import org.eclipse.help.internal.dynamic.ExtensionHandler;
import org.eclipse.help.internal.dynamic.IncludeHandler;
import org.eclipse.help.internal.dynamic.ProcessorHandler;
import org.eclipse.help.internal.dynamic.ValidationHandler;

/*
 * Assembles toc contributions (toc fragments) into complete, linked, and
 * assembled books.
 */
public class TocAssembler {

	private DocumentProcessor processor;
	private ProcessorHandler[] handlers;

	private Map<String, Set<String>> anchorsByContributionId;
	private List<TocContribution> contributions;
	private Map<String, TocContribution> contributionsById;
	private Map<String, ITocContribution[]> contributionsByLinkTo;
	private Set<ITocContribution> processedContributions;
	private Map<String, String[]> requiredAttributes;
	private Set<String> tocsToFilter;


	public TocAssembler() {
		this.tocsToFilter = new HashSet<>();
	}

	public TocAssembler(Set<String> tocsToFilter) {
		this.tocsToFilter = tocsToFilter;
	}

	/*
	 * Assembles the given toc contributions into complete, linked
	 * books. The originals are not modified.
	 */
	public List<TocContribution> assemble(List<TocContribution> contributions) {
		this.contributions = contributions;
		anchorsByContributionId = null;
		contributionsById = null;
		contributionsByLinkTo = null;
		processedContributions = null;

		List<TocContribution> books = getBooks();
		Iterator<TocContribution> iter = books.iterator();
		while (iter.hasNext()) {
			TocContribution book = iter.next();
			process(book);
		}
		return books;
	}

	/*
	 * Returns the list of contributions that should appear as root TOCs
	 * (books). Contributions are books if the following conditions are
	 * true:
	 *
	 * 1. isPrimary() returns true.
	 * 2. The toc has no "link_to" attribute defined (does not link into
	 *    another toc), or the link_to target anchor doesn't exist.
	 * 3. No other toc has a link to this contribution (via "link" element).
	 */
	private List<TocContribution> getBooks() {
		Map<String, String> linkedContributionIds = getLinkedContributionIds(contributions);
		List<TocContribution> books = new ArrayList<>();
		Iterator<TocContribution> iter = contributions.iterator();
		while (iter.hasNext()) {
			TocContribution contrib = iter.next();
			boolean isValidLinkTo = hasValidLinkTo(contrib);
			boolean isLinkedId = linkedContributionIds.containsKey(contrib.getId());
			if (!isValidLinkTo && !isLinkedId) {
				if (contrib.isPrimary()) {
					books.add(contrib);
					if (HelpPlugin.DEBUG_TOC) {
						String msg = "Primary Toc Found: " + contrib.getId(); //$NON-NLS-1$
						String linkTo = contrib.getLinkTo();
						if (linkTo != null) {
							msg += " - cannot find link to: "; //$NON-NLS-1$
							msg += linkTo;
						}
						System.out.println(msg);
					}
				} else {
					if (HelpPlugin.DEBUG_TOC) {
						String msg = "Table of contents is not primary and not linked to another TOC " + contrib.getId() + " (skipping)"; //$NON-NLS-1$ //$NON-NLS-2$
						System.out.println(msg);
					}
				}
			} else {
				contrib.setSubToc(true);
				if (HelpPlugin.DEBUG_TOC) {
					String msg = "Toc " + contrib.getId();  //$NON-NLS-1$
					if (isValidLinkTo) {
						msg += " has a valid link to " + contrib.getLinkTo(); //$NON-NLS-1$
					}
					if (isLinkedId) {
						msg += " is linked from " + linkedContributionIds.get(contrib.getId()); //$NON-NLS-1$
					}
					System.out.println(msg);
				}
			}
		}
		return books;
	}

	/*
	 * Returns the set of ids of contributions that are linked to by other
	 * contributions, i.e. at least one other contribution has a link element
	 * pointing to it.
	 */
	private Map<String, String> getLinkedContributionIds(List<TocContribution> contributions) {
		if (processor == null) {
			processor = new DocumentProcessor();
		}
		final Map<String, String> linkedContributionIds = new HashMap<>();
		ProcessorHandler[] linkFinder = new ProcessorHandler[] {
			new ValidationHandler(getRequiredAttributes()),
			new ProcessorHandler() {
				@Override
				public short handle(UAElement element, String id) {
					if (element instanceof Link) {
						Link link = (Link)element;
						String toc = link.getToc();
						if (toc != null) {
							TocContribution srcContribution = getContribution(id);
							linkedContributionIds.put(HrefUtil.normalizeHref(srcContribution.getContributorId(), toc), id);
						}
					}
					return UNHANDLED;
				}
			}
		};
		processor.setHandlers(linkFinder);
		ListIterator<TocContribution> iter = contributions.listIterator();
		while (iter.hasNext()) {
			TocContribution contrib = iter.next();
			try {
				String id = contrib.getId();
				if (!tocsToFilter.contains(id)) {
					processor.process((Toc)contrib.getToc(), id);
				}
			}
			catch (Throwable t) {
				iter.remove();
				String msg = "Error processing help table of contents: " + contrib.getId() + " (skipping)"; //$NON-NLS-1$ //$NON-NLS-2$
				ILog.of(getClass()).error(msg, t);
			}
		}
		return linkedContributionIds;
	}

	/*
	 * Checks whether the toc contribution with the given id contains the
	 * given anchor.
	 */
	private boolean hasAnchor(String tocContributionId, String anchorId) {
		TocContribution contrib = getContribution(tocContributionId);
		if (contrib != null) {
			process(contrib);
			if (anchorsByContributionId != null) {
				Set<String> anchors = anchorsByContributionId.get(tocContributionId);
				if (anchors != null) {
					return anchors.contains(anchorId);
				}
			}
		}
		// invalid contribution, or no anchors
		return false;
	}

	/*
	 * Checks whether the given contribution has a link_to defined, and it
	 * is valid (contribution and anchor exist).
	 */
	private boolean hasValidLinkTo(TocContribution contrib) {
		String linkTo = contrib.getLinkTo();
		if (linkTo != null) {
			String normalized = HrefUtil.normalizeHref(contrib.getContributorId(), linkTo);
			int index = normalized.indexOf('#');
			if (index != -1) {
				String id = normalized.substring(0, index);
				String anchorId = normalized.substring(index + 1);
				return hasAnchor(id, anchorId);
			}
		}
		return false;
	}

	/*
	 * Processes the given contribution, if it hasn't been processed yet. This
	 * performs the following operations:
	 *
	 * 1. Topic hrefs are normalized, e.g. "path/doc.html" ->
	 *    "/my.plugin/path/doc.html"
	 * 2. Links are resolved, link is replaced with target content, extra docs
	 *    are merged.
	 * 3. Anchor contributions are resolved, tocs with link_to's are inserted
	 *    at anchors and extra docs merged.
	 */
	private void process(ITocContribution contribution) {
		if (processedContributions == null) {
			processedContributions = new HashSet<>();
		}
		// don't process the same one twice
		if (!processedContributions.contains(contribution)) {
			if (processor == null) {
				processor = new DocumentProcessor();
			}
			if (handlers == null) {
				DocumentReader reader = new DocumentReader();
				handlers = new ProcessorHandler[] {
					new NormalizeHandler(),
					new LinkHandler(),
					new AnchorHandler(),
					new IncludeHandler(reader, contribution.getLocale()),
					new ExtensionHandler(reader, contribution.getLocale()),
				};
			}
			processor.setHandlers(handlers);
			processor.process((Toc)contribution.getToc(), contribution.getId());
			processedContributions.add(contribution);
		}
	}

	/*
	 * Returns the contribution with the given id.
	 */
	private TocContribution getContribution(String id) {
		if (contributionsById == null) {
			contributionsById = new HashMap<>();
			Iterator<TocContribution> iter = contributions.iterator();
			while (iter.hasNext()) {
				TocContribution contribution = iter.next();
				contributionsById.put(contribution.getId(), contribution);
			}
		}
		return contributionsById.get(id);
	}

	/*
	 * Returns all contributions that define a link_to attribute pointing to
	 * the given anchor path. The path has the form "<contributionId>#<anchorId>",
	 * e.g. "/my.plugin/toc.xml#myAnchor".
	 */
	private ITocContribution[] getAnchorContributions(String anchorPath) {
		if (contributionsByLinkTo == null) {
			contributionsByLinkTo = new HashMap<>();
			Iterator<TocContribution> iter = contributions.iterator();
			while (iter.hasNext()) {
				TocContribution srcContribution = iter.next();
				String linkTo = srcContribution.getLinkTo();
				if (linkTo != null) {
					String destAnchorPath = HrefUtil.normalizeHref(srcContribution.getContributorId(), linkTo);
					ITocContribution[] array = contributionsByLinkTo.get(destAnchorPath);
					if (array == null) {
						array = new TocContribution[] { srcContribution };
					}
					else {
						// If a contribution of this id is already included don't include a second time
						boolean isAlreadyIncluded = false;
						for (int i = 0; i < array.length; i++) {
							if (srcContribution.getId().equals(array[i].getId())) {
								isAlreadyIncluded = true;
							}
						}
						if (!isAlreadyIncluded) {
							TocContribution[] temp = new TocContribution[array.length + 1];
							System.arraycopy(array, 0, temp, 0, array.length);
							temp[array.length] = srcContribution;
							array = temp;
						}
					}
					contributionsByLinkTo.put(destAnchorPath, array);
				}
			}
		}
		ITocContribution[] contributions = contributionsByLinkTo.get(anchorPath);
		if (contributions == null) {
			contributions = new TocContribution[0];
		}
		return contributions;
	}

	private Map<String, String[]> getRequiredAttributes() {
		if (requiredAttributes == null) {
			requiredAttributes = new HashMap<>();
			requiredAttributes.put(Toc.NAME, new String[] { Toc.ATTRIBUTE_LABEL });
			requiredAttributes.put(Topic.NAME, new String[] { Topic.ATTRIBUTE_LABEL });
			requiredAttributes.put("anchor", new String[] { "id" }); //$NON-NLS-1$ //$NON-NLS-2$
			requiredAttributes.put("include", new String[] { "path" }); //$NON-NLS-1$ //$NON-NLS-2$
			requiredAttributes.put("link", new String[] { "toc" }); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return requiredAttributes;
	}

	/*
	 * Adds the given extra documents to the contribution.
	 */
	private void addExtraDocuments(TocContribution contribution, String[] extraDocuments) {
		if (extraDocuments.length > 0) {
			String[] destExtraDocuments = contribution.getExtraDocuments();
			String[] combinedExtraDocuments;
			if (destExtraDocuments.length == 0) {
				combinedExtraDocuments = extraDocuments;
			}
			else {
				Set<String> set = new HashSet<>();
				set.addAll(Arrays.asList(destExtraDocuments));
				set.addAll(Arrays.asList(extraDocuments));
				combinedExtraDocuments = set.toArray(new String[set.size()]);
			}
			contribution.setExtraDocuments(combinedExtraDocuments);
		}
	}

	/*
	 * Handler that resolves link elements (replaces the link element with
	 * the linked-to toc's children.
	 */
	private class LinkHandler extends ProcessorHandler {
		@Override
		public short handle(UAElement element, String id) {
			if (element instanceof Link) {
				Link link = (Link)element;
				UAElement parent = link.getParentElement();
				if (parent != null) {
					String toc = link.getToc();
					if (toc != null) {
						TocContribution destContribution = getContribution(id);
						TocContribution srcContribution = getContribution(HrefUtil.normalizeHref(destContribution.getContributorId(), toc));
						if (srcContribution != null) {
							process(srcContribution);
							IUAElement[] children = srcContribution.getToc().getChildren();
							for (int i=0;i<children.length;++i) {
								parent.insertBefore((UAElement)children[i], link);
							}
							addExtraDocuments(destContribution, srcContribution.getExtraDocuments());
						}
						parent.removeChild(link);
					}
				}
				return HANDLED_SKIP;
			}
			return UNHANDLED;
		}
	}

	/*
	 * Handles anchor contributions. If any contribution's toc wants to link
	 * into this one at the current anchor, link it in.
	 */
	private class AnchorHandler extends ProcessorHandler {
		@Override
		public short handle(UAElement element, String id) {
			if (element instanceof Anchor) {
				if (tocsToFilter.contains(id)) {
					return UNHANDLED;
				}
				Anchor anchor = (Anchor)element;
				UAElement parent = anchor.getParentElement();
				if (parent != null) {
					String anchorId = anchor.getId();
					if (anchorId != null) {
						// add to set of known anchors
						if (anchorsByContributionId == null) {
							anchorsByContributionId = new HashMap<>();
						}
						Set<String> set = anchorsByContributionId.get(id);
						if (set == null) {
							set = new HashSet<>();
							anchorsByContributionId.put(id, set);
						}
						set.add(anchorId);

						// process contributions
						TocContribution destContribution = getContribution(id);
						if (destContribution != null) {
							ITocContribution[] srcContributions = getAnchorContributions(
									destContribution.getId() + '#' + anchorId);
							for (int i=0;i<srcContributions.length;++i) {
								process(srcContributions[i]);
								IUAElement[] children = srcContributions[i].getToc().getChildren();
								for (int j=0;j<children.length;++j) {
									parent.insertBefore((UAElement)children[j], anchor);
								}
								addExtraDocuments(destContribution, srcContributions[i].getExtraDocuments());
							}
						}
					}
				}
			}
			// allow the extension handler to act on anchors afterwards
			return UNHANDLED;
		}
	}

	/*
	 * Normalizes topic hrefs, by prepending the plug-in id to form an href.
	 * e.g. "path/myfile.html" -> "/my.plugin/path/myfile.html"
	 */
	private class NormalizeHandler extends ProcessorHandler {
		@Override
		public short handle(UAElement element, String id) {
			if (element instanceof Topic) {
				Topic topic = (Topic)element;
				String href = topic.getHref();
				if (href != null) {
					topic.setHref(normalize(href, id));
				}

				processCriteria(element, id);

				return HANDLED_CONTINUE;
			}
			else if (element instanceof Toc) {
				Toc toc = (Toc)element;
				toc.setHref(id);
				String topic = toc.getTopic();
				if (topic != null) {
					toc.setTopic(normalize(topic, id));
				}

				processCriteria(element, id);

				return HANDLED_CONTINUE;
			}
			return UNHANDLED;
		}

		private String normalize(String href, String id) {
			ITocContribution contribution = getContribution(id);
			if (contribution != null) {
				String pluginId = contribution.getContributorId();
				return HrefUtil.normalizeHref(pluginId, href);
			}
			else {
				int index = id.indexOf('/', 1);
				if (index != -1) {
					String pluginId = id.substring(1, index);
					return HrefUtil.normalizeHref(pluginId, href);
				}
			}
			return href;
		}

		private void processCriteria(UAElement element, String id) {
			if(HelpPlugin.getCriteriaManager().isCriteriaEnabled()){
				ITocContribution contribution = getContribution(id);
				String locale = contribution.getLocale();
				ICriteria[] criteria = new ICriteria[0];
				if (element instanceof Topic) {
					Topic topic = (Topic) element;
					criteria = CriteriaProviderRegistry.getInstance().getAllCriteria(topic);
				}
				else if (element instanceof Toc) {
					Toc toc = (Toc) element;
					criteria = CriteriaProviderRegistry.getInstance().getAllCriteria(toc);
				}

				HelpPlugin.getCriteriaManager().addCriteriaValues(criteria, locale);
			}
		}
	}
}
