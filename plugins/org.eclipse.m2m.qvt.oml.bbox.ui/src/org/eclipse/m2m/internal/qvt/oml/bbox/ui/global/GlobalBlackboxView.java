package org.eclipse.m2m.internal.qvt.oml.bbox.ui.global;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.Messages;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.QVTBBoxUIPlugin;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxDiagnosticInfo;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.navigator.BlackboxOpenAction;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.navigator.ICommonActionConstants;
import org.eclipse.ui.part.ViewPart;

public class GlobalBlackboxView extends ViewPart {

	public static final String ID = "org.eclipse.m2m.qvt.oml.bbox.ui.views.globalBlackboxes"; //$NON-NLS-1$

	private final GlobalBlackboxDiscoveryService discoveryService = new GlobalBlackboxDiscoveryService();
	private final BlackboxOpenAction openAction = new BlackboxOpenAction();
	private TreeViewer viewer;
	private Job discoveryJob;
	private int discoveryGeneration;

	@Override
	public void createPartControl(Composite parent) {
		viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		viewer.setContentProvider(new GlobalBlackboxContentProvider());
		viewer.setLabelProvider(new GlobalBlackboxLabelProvider());
		viewer.setInput(GlobalBlackboxLoadingNode.INSTANCE);
		getSite().setSelectionProvider(viewer);

		createActions();
		createContextMenu();
		scheduleDiscovery(true);
	}

	private void createActions() {
		final Action refreshAction = new Action(Messages.GlobalBlackboxView_refresh) {
			@Override
			public void run() {
				scheduleDiscovery(false);
			}
		};
		refreshAction.setToolTipText(Messages.GlobalBlackboxView_refresh);
		refreshAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages()
				.getImageDescriptor(ISharedImages.IMG_ELCL_SYNCED));

		Action collapseAllAction = new Action(Messages.GlobalBlackboxView_collapseAll) {
			@Override
			public void run() {
				viewer.collapseAll();
			}
		};
		collapseAllAction.setToolTipText(Messages.GlobalBlackboxView_collapseAll);
		collapseAllAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages()
				.getImageDescriptor(ISharedImages.IMG_ELCL_COLLAPSEALL));

		getViewSite().getActionBars().getToolBarManager().add(refreshAction);
		getViewSite().getActionBars().getToolBarManager().add(collapseAllAction);
		getViewSite().getActionBars().setGlobalActionHandler(ActionFactory.REFRESH.getId(), refreshAction);
		getViewSite().getActionBars().setGlobalActionHandler(ICommonActionConstants.OPEN, openAction);

		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				if (event.getSelection() instanceof IStructuredSelection) {
					openAction.selectionChanged((IStructuredSelection) event.getSelection());
				}
			}
		});
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				if (event.getSelection() instanceof IStructuredSelection) {
					openAction.selectionChanged((IStructuredSelection) event.getSelection());
					if (openAction.isEnabled()) {
						openAction.run();
					}
				}
			}
		});
	}

	private void createContextMenu() {
		MenuManager menuManager = new MenuManager();
		menuManager.setRemoveAllWhenShown(true);
		menuManager.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				if (viewer.getSelection() instanceof IStructuredSelection) {
					openAction.selectionChanged((IStructuredSelection) viewer.getSelection());
					if (openAction.isEnabled()) {
						manager.add(openAction);
					}
				}
				manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
			}
		});
		Menu menu = menuManager.createContextMenu(viewer.getControl());
		viewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuManager, viewer);
	}

	private synchronized void scheduleDiscovery(boolean showLoading) {
		if (discoveryJob != null) {
			discoveryJob.cancel();
		}
		if (showLoading) {
			viewer.setInput(GlobalBlackboxLoadingNode.INSTANCE);
		}

		final int generation = ++discoveryGeneration;
		discoveryJob = new Job(Messages.GlobalBlackboxView_discoveryJobName) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					final GlobalBlackboxDiscoveryResult result = discoveryService.discover(monitor);
					if (monitor.isCanceled()) {
						updateInput(generation, canceledResult());
						return Status.CANCEL_STATUS;
					}
					updateInput(generation, result);
					return Status.OK_STATUS;
				} catch (OperationCanceledException e) {
					updateInput(generation, canceledResult());
					return Status.CANCEL_STATUS;
				} catch (RuntimeException e) {
					QVTBBoxUIPlugin.log(e);
					updateInput(generation, errorResult(e));
					return Status.CANCEL_STATUS;
				} catch (LinkageError e) {
					QVTBBoxUIPlugin.log(e);
					updateInput(generation, errorResult(e));
					return Status.CANCEL_STATUS;
				}
			}
		};
		discoveryJob.setUser(false);
		discoveryJob.schedule();
	}

	private void updateInput(final int generation, final GlobalBlackboxDiscoveryResult result) {
		Display display = viewer != null ? viewer.getControl().getDisplay() : null;
		if (display == null || display.isDisposed()) {
			return;
		}
		display.asyncExec(new Runnable() {
			public void run() {
				if (viewer == null || viewer.getControl().isDisposed()) {
					return;
				}
				synchronized (GlobalBlackboxView.this) {
					if (generation != discoveryGeneration) {
						return;
					}
					discoveryJob = null;
				}
				viewer.setInput(result);
			}
		});
	}

	private static GlobalBlackboxDiscoveryResult errorResult(Throwable throwable) {
		GlobalBlackboxDiscoveryResult result = new GlobalBlackboxDiscoveryResult();
		String message = throwable.getMessage();
		if (message == null) {
			message = throwable.getClass().getName();
		}
		GlobalBlackboxGroup group = result.getRuntimeRegistrations();
		group.addChild(new BlackboxDiagnosticInfo(group, Diagnostic.ERROR, message));
		return result;
	}

	private static GlobalBlackboxDiscoveryResult canceledResult() {
		GlobalBlackboxDiscoveryResult result = new GlobalBlackboxDiscoveryResult();
		GlobalBlackboxGroup group = result.getRuntimeRegistrations();
		group.addChild(new GlobalBlackboxStatusNode(group, Messages.GlobalBlackboxView_canceled));
		return result;
	}

	@Override
	public void setFocus() {
		if (viewer != null) {
			viewer.getControl().setFocus();
		}
	}

	@Override
	public synchronized void dispose() {
		discoveryGeneration++;
		if (discoveryJob != null) {
			discoveryJob.cancel();
			discoveryJob = null;
		}
		super.dispose();
	}
}
