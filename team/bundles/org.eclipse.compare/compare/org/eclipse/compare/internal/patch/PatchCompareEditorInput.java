/*******************************************************************************
 * Copyright (c) 2005, 2017 IBM Corporation and others.
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
 *     Alex Blewitt <alex.blewitt@gmail.com> - replace new Boolean with Boolean.valueOf - https://bugs.eclipse.org/470344
 *******************************************************************************/
package org.eclipse.compare.internal.patch;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.CompareUI;
import org.eclipse.compare.CompareViewerPane;
import org.eclipse.compare.internal.CompareUIPlugin;
import org.eclipse.compare.internal.ICompareUIConstants;
import org.eclipse.compare.internal.core.patch.DiffProject;
import org.eclipse.compare.internal.core.patch.FileDiffResult;
import org.eclipse.compare.internal.core.patch.FilePatch2;
import org.eclipse.compare.internal.core.patch.HunkResult;
import org.eclipse.compare.patch.PatchConfiguration;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.compare.structuremergeviewer.DiffTreeViewer;
import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;

public abstract class PatchCompareEditorInput extends CompareEditorInput {

	private static final String IMAGE_CACHE_KEY = "IMAGE_CACHE"; //$NON-NLS-1$

	public static ImageDescriptor createOverlay(Image baseImage, ImageDescriptor overlayImage, int quadrant) {
		return new DecoratorOverlayIcon(baseImage, createArrayFrom(overlayImage, quadrant), new Point(Math.max(baseImage.getBounds().width, 16), Math.max(baseImage.getBounds().height, 16)));
	}

	private static ImageDescriptor[] createArrayFrom(
			ImageDescriptor overlayImage, int quadrant) {
		ImageDescriptor[] descs = new ImageDescriptor[] { null, null, null, null, null };
		descs[quadrant] = overlayImage;
		return descs;
	}

	class PatcherCompareEditorLabelProvider extends LabelProvider {
		private ILabelProvider wrappedProvider;

		public PatcherCompareEditorLabelProvider(ILabelProvider labelProvider) {
			wrappedProvider = labelProvider;
		}

		@Override
		public String getText(Object element) {
			String text = wrappedProvider.getText(element);
			if (element instanceof PatchDiffNode){
				PatchDiffNode node = (PatchDiffNode) element;
				if (node instanceof PatchProjectDiffNode) {
					PatchProjectDiffNode projectNode = (PatchProjectDiffNode) node;
					if (!Utilities.getProject(projectNode.getDiffProject()).exists()) {
						text = NLS.bind(PatchMessages.Diff_2Args, new String[]{text, PatchMessages.PreviewPatchLabelDecorator_ProjectDoesNotExist});
					}
				}
				if (!node.isEnabled()) {
					return NLS.bind(PatchMessages.Diff_2Args,
							new String[]{text, PatchMessages.PatcherCompareEditorInput_NotIncluded});
				}
				if (node instanceof PatchFileDiffNode) {
					PatchFileDiffNode fileNode = (PatchFileDiffNode) node;
					if (getPatcher().hasCachedContents(fileNode.getDiffResult().getDiff())) {
						text = NLS.bind(PatchMessages.Diff_2Args, new String[] {text, PatchMessages.HunkMergePage_Merged});
					}
					if (!fileNode.fileExists()) {
						text = NLS.bind(PatchMessages.Diff_2Args, new String[] {text, PatchMessages.PatchCompareEditorInput_0});
					}
				}
				if (node instanceof HunkDiffNode) {
					HunkDiffNode hunkNode = (HunkDiffNode) node;
					if (hunkNode.isManuallyMerged()) {
						text = NLS.bind(PatchMessages.Diff_2Args, new String[] {text, PatchMessages.HunkMergePage_Merged});
					}
					if (hunkNode.isFuzzUsed()) {
						text = NLS.bind(PatchMessages.Diff_2Args,
								new String[] { text,
								NLS.bind(hunkNode.isAllContextIgnored() ? PatchMessages.PreviewPatchPage_AllContextIgnored : PatchMessages.PreviewPatchPage_FuzzUsed,
										new String[] { hunkNode.getHunkResult().getFuzz() + ""}) }); //$NON-NLS-1$
					}
				}
				if (getPatcher().isRetargeted(node.getPatchElement()))
					return NLS.bind(PatchMessages.Diff_2Args,
							new String[]{getPatcher().getOriginalPath(node.getPatchElement()).toString(),
							NLS.bind(PatchMessages.PreviewPatchPage_Target, new String[]{node.getName()})});
			}
			return text;
		}

		@Override
		public Image getImage(Object element) {
			Image image = wrappedProvider.getImage(element);
			if (element instanceof PatchDiffNode){
				PatchDiffNode node = (PatchDiffNode) element;
				if (!node.isEnabled() && image != null) {
					LocalResourceManager imageCache = PatchCompareEditorInput.getImageCache(getPatcher().getConfiguration());
					return imageCache.create(createOverlay(image, CompareUIPlugin.getImageDescriptor(ICompareUIConstants.REMOVED_OVERLAY), IDecoration.TOP_LEFT));
				}
			}
			if (element instanceof HunkDiffNode) {
				HunkDiffNode node = (HunkDiffNode) element;
				if (node.isManuallyMerged()) {
					LocalResourceManager imageCache = PatchCompareEditorInput.getImageCache(getPatcher().getConfiguration());
					return imageCache.create(PatchCompareEditorInput.createOverlay(image, CompareUIPlugin.getImageDescriptor(ICompareUIConstants.IS_MERGED_OVERLAY), IDecoration.TOP_LEFT));
				}
			}
			return image;
		}
	}

	private final DiffNode root;
	private final WorkspacePatcher patcher;
	private TreeViewer viewer;
	private boolean fShowAll;
	private boolean showMatched = false;

	/**
	 * Creates a new PatchCompareEditorInput and makes use of the passed in CompareConfiguration
	 * to configure the UI elements.
	 * @param patcher
	 * @param configuration
	 */
	public PatchCompareEditorInput(WorkspacePatcher patcher, CompareConfiguration configuration) {
		super(configuration);
		Assert.isNotNull(patcher);
		this.patcher = patcher;
		root = new DiffNode(Differencer.NO_CHANGE) {
			@Override
			public boolean hasChildren() {
				return true;
			}
		};
		initializeImageCache();
	}

	private void initializeImageCache() {
		initializeImageCache(patcher.getConfiguration());
	}

	private static LocalResourceManager initializeImageCache(PatchConfiguration patchConfiguration) {
		LocalResourceManager imageCache = new LocalResourceManager(JFaceResources.getResources());
		patchConfiguration.setProperty(IMAGE_CACHE_KEY, imageCache);
		return imageCache;
	}

	@Override
	protected void handleDispose() {
		super.handleDispose();
		getImageCache(getPatcher().getConfiguration()).dispose();
	}

	public static LocalResourceManager getImageCache(PatchConfiguration patchConfiguration) {
		LocalResourceManager cache = (LocalResourceManager)patchConfiguration.getProperty(IMAGE_CACHE_KEY);
		if (cache == null) {
			return initializeImageCache(patchConfiguration);
		}
		return cache;
	}

	@Override
	protected Object prepareInput(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		initLabels();
		return root;
	}

	private void initLabels() {
		CompareConfiguration cc = getCompareConfiguration();
		// set left editable so that unmatched hunks can be edited
		cc.setLeftEditable(true);
		cc.setRightEditable(false);
		//cc.setProperty(CompareEditor.CONFIRM_SAVE_PROPERTY, Boolean.FALSE);
		cc.setLeftLabel(getCompareConfiguration().getLeftLabel(root));
		cc.setLeftImage(getCompareConfiguration().getLeftImage(root));
		cc.setRightLabel(getCompareConfiguration().getRightLabel(root));
		cc.setRightImage(getCompareConfiguration().getRightImage(root));
	}

	/**
	 * Update the presentation of the diff tree.
	 */
	protected void updateTree() {
		if (getViewer() != null && !getViewer().getControl().isDisposed())
			getViewer().refresh(true);
	}

	/**
	 * Build the diff tree.
	 */
	protected void buildTree(){

		// Reset the input node so it is empty
		if (getRoot().hasChildren()) {
			resetRoot();
		}
		// Reset the input of the viewer so the old state is no longer used
		getViewer().setInput(getRoot());

		// Refresh the patcher state
		getPatcher().refresh();

		// Build the diff tree
		if (getPatcher().isWorkspacePatch()){
			processProjects(getPatcher().getDiffProjects());
		} else {
			processDiffs(getPatcher().getDiffs());
		}

		// Refresh the viewer
		getViewer().refresh();
	}

	private void processDiffs(FilePatch2[] diffs) {
		for (FilePatch2 diff : diffs) {
			processDiff(diff, getRoot());
		}
	}

	private void processProjects(DiffProject[] diffProjects) {
		//create diffProject nodes
		for (DiffProject diffProject : diffProjects) {
			PatchProjectDiffNode projectNode = new PatchProjectDiffNode(getRoot(), diffProject, getPatcher().getConfiguration());
			FilePatch2[] diffs = diffProject.getFileDiffs();
			for (FilePatch2 fileDiff : diffs) {
				processDiff(fileDiff, projectNode);
			}
		}
	}

	private void processDiff(FilePatch2 diff, DiffNode parent) {
		FileDiffResult diffResult = getPatcher().getDiffResult(diff);
		PatchFileDiffNode node = PatchFileDiffNode.createDiffNode(parent, diffResult);
		HunkResult[] hunkResults = diffResult.getHunkResults();
		for (HunkResult hunkResult : hunkResults) {
			if (!hunkResult.isOK()) {
				HunkDiffNode hunkNode = HunkDiffNode.createDiffNode(node, hunkResult, true);
				Object left = hunkNode.getLeft();
				if (left instanceof UnmatchedHunkTypedElement) {
					UnmatchedHunkTypedElement element = (UnmatchedHunkTypedElement) left;
					element.addContentChangeListener(source -> {
						if (getViewer() == null || getViewer().getControl().isDisposed())
							return;
						getViewer().refresh(true);
					});
				}
			} else if (showMatched) {
				HunkDiffNode.createDiffNode(node, hunkResult, false, true, false);
			}
		}
	}

	@Override
	public Viewer createDiffViewer(Composite parent) {
		viewer =  new DiffTreeViewer(parent, getCompareConfiguration()){
			@Override
			protected void fillContextMenu(IMenuManager manager) {
				PatchCompareEditorInput.this.fillContextMenu(manager);
			}
		};

		viewer.setLabelProvider(new PatcherCompareEditorLabelProvider((ILabelProvider)viewer.getLabelProvider()));
		viewer.getTree().setData(CompareUI.COMPARE_VIEWER_TITLE, PatchMessages.PatcherCompareEditorInput_PatchContents);
		viewer.addOpenListener(event -> {
			IStructuredSelection sel= (IStructuredSelection) event.getSelection();
			Object obj= sel.getFirstElement();
			if (obj instanceof HunkDiffNode) {
				if (((HunkDiffNode) obj).getHunkResult().isOK()) {
					getCompareConfiguration().setLeftLabel(PatchMessages.PatcherCompareEditorInput_LocalCopy);
					getCompareConfiguration().setRightLabel(PatchMessages.PreviewPatchPage2_MatchedHunk);
				} else {
					getCompareConfiguration().setLeftLabel(PatchMessages.PreviewPatchPage2_PatchedLocalFile);
					getCompareConfiguration().setRightLabel(PatchMessages.PreviewPatchPage2_OrphanedHunk);
				}
			} else {
				getCompareConfiguration().setLeftLabel(PatchMessages.PatcherCompareEditorInput_LocalCopy);
				getCompareConfiguration().setRightLabel(PatchMessages.PatcherCompareEditorInput_AfterPatch);
			}
		});
		viewer.setFilters(getFilters());
		viewer.setInput(root);
		return viewer;
	}

	private ViewerFilter[] getFilters() {
		return new ViewerFilter[] { new ViewerFilter() {
			@Override
			public boolean select(Viewer v, Object parentElement, Object element) {
				if (element instanceof PatchDiffNode) {
					PatchDiffNode node = (PatchDiffNode) element;
					return node.isEnabled() || isShowAll();
				}
				return false;
			}
		} };
	}

	protected boolean isShowAll() {
		return fShowAll;
	}

	protected void setShowAll(boolean show) {
		fShowAll = show;
	}

	public boolean isShowMatched() {
		return showMatched;
	}

	protected void setShowMatched(boolean show) {
		showMatched = show;
	}

	public void contributeDiffViewerToolbarItems(Action[] actions, boolean workspacePatch){
		ToolBarManager tbm= CompareViewerPane.getToolBarManager(viewer.getControl().getParent());
		if (tbm != null) {
			tbm.removeAll();

			tbm.add(new Separator("contributed")); //$NON-NLS-1$
			for (Action action : actions) {
				tbm.appendToGroup("contributed", action); //$NON-NLS-1$
			}

			tbm.update(true);
		}
	}

	public TreeViewer getViewer() {
		return viewer;
	}

	public DiffNode getRoot() {
		return root;
	}

	public void resetRoot() {
		IDiffElement[] children = root.getChildren();
		for (IDiffElement child : children) {
			root.remove(child);
		}
	}

	public WorkspacePatcher getPatcher() {
		return patcher;
	}

	public boolean confirmRebuild(String message) {
		if (getPatcher().hasCachedContents()) {
			if (promptToDiscardCachedChanges(message)) {
				getPatcher().clearCachedContents();
				return true;
			}
			return false;
		}
		return true;
	}

	private boolean promptToDiscardCachedChanges(String message) {
		return MessageDialog.openConfirm(viewer.getControl().getShell(), PatchMessages.PatcherCompareEditorInput_0, message);
	}

	/**
	 * Return whether this input has a result to apply. The input
	 * has a result to apply if at least one hunk is selected for inclusion.
	 * @return whether this input has a result to apply
	 */
	public boolean hasResultToApply() {
		boolean atLeastOneIsEnabled = false;
		if (getViewer() != null) {
			IDiffElement[] elements = getRoot().getChildren();
			for (IDiffElement element : elements) {
				if (isEnabled(element)) {
					atLeastOneIsEnabled = true;
					break;
				}
			}
		}
		return atLeastOneIsEnabled;
	}

	private boolean isEnabled(IDiffElement element) {
		if (element instanceof PatchDiffNode) {
			PatchDiffNode node = (PatchDiffNode) element;
			return node.isEnabled();
		}
		return false;
	}

	protected abstract void fillContextMenu(IMenuManager manager);

	@Override
	public Viewer findStructureViewer(Viewer oldViewer, ICompareInput input,
			Composite parent) {
		if (org.eclipse.compare.internal.Utilities.isHunk(input))
			return null;
		return super.findStructureViewer(oldViewer, input, parent);
	}
}
