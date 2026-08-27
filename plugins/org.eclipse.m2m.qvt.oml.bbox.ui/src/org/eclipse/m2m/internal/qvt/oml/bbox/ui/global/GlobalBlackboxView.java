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
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxDiagnosticUtil;
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
	private final GlobalBlackboxDiscoveryGeneration discoveryGeneration = new GlobalBlackboxDiscoveryGeneration();
	private final BlackboxOpenAction openAction = new BlackboxOpenAction();
	private TreeViewer viewer;
	private volatile Display display;
	private Job discoveryJob;

	@Override
	public void createPartControl(Composite parent) {
		display = parent.getDisplay();
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

		final int generation = discoveryGeneration.start();
		final boolean showCanceledResult = showLoading;
		discoveryJob = new Job(Messages.GlobalBlackboxView_discoveryJobName) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					final GlobalBlackboxDiscoveryResult result = discoveryService.discover(monitor);
					if (monitor.isCanceled()) {
						completeDiscovery(generation, showCanceledResult ? canceledResult() : null);
						return Status.CANCEL_STATUS;
					}
					completeDiscovery(generation, result);
					return Status.OK_STATUS;
				} catch (OperationCanceledException e) {
					completeDiscovery(generation, showCanceledResult ? canceledResult() : null);
					return Status.CANCEL_STATUS;
				} catch (RuntimeException e) {
					return discoveryFailed(generation, e);
				} catch (LinkageError e) {
					return discoveryFailed(generation, e);
				}
			}
		};
		discoveryJob.setUser(false);
		discoveryJob.schedule();
	}

	private IStatus discoveryFailed(int generation, Throwable throwable) {
		IStatus status = QVTBBoxUIPlugin.createStatus(IStatus.ERROR, Messages.GlobalBlackboxView_discoveryFailed,
				throwable);
		QVTBBoxUIPlugin.log(status);
		completeDiscovery(generation, errorResult(throwable));
		return status;
	}

	private void completeDiscovery(final int generation, final GlobalBlackboxDiscoveryResult result) {
		Display currentDisplay = display;
		if (currentDisplay == null || currentDisplay.isDisposed()) {
			return;
		}
		currentDisplay.asyncExec(new Runnable() {
			public void run() {
				if (viewer == null || viewer.getControl().isDisposed()) {
					return;
				}
				synchronized (GlobalBlackboxView.this) {
					if (!discoveryGeneration.isCurrent(generation)) {
						return;
					}
					discoveryJob = null;
				}
				if (result != null) {
					viewer.setInput(result);
				}
			}
		});
	}

	private static GlobalBlackboxDiscoveryResult errorResult(Throwable throwable) {
		GlobalBlackboxDiscoveryResult result = new GlobalBlackboxDiscoveryResult();
		GlobalBlackboxGroup group = result.getRuntimeRegistrations();
		group.addChild(new BlackboxDiagnosticInfo(group, Diagnostic.ERROR,
				BlackboxDiagnosticUtil.getMessage(throwable)));
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
		discoveryGeneration.invalidate();
		if (discoveryJob != null) {
			discoveryJob.cancel();
			discoveryJob = null;
		}
		display = null;
		super.dispose();
	}
}
