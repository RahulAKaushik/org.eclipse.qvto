package org.eclipse.m2m.internal.qvt.oml.bbox.ui.navigator;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.Messages;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxVisibilityScope;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.eclipse.ui.navigator.ICommonActionConstants;
import org.eclipse.ui.navigator.ICommonMenuConstants;

public class BlackboxNavigatorActionProvider extends CommonActionProvider {

	private final BlackboxOpenAction openAction = new BlackboxOpenAction();

	@Override
	public void fillContextMenu(IMenuManager menu) {
		IStructuredSelection selection = getSelection();
		openAction.selectionChanged(selection);
		if (openAction.isEnabled()) {
			menu.appendToGroup(ICommonMenuConstants.GROUP_OPEN, openAction);
		}

		if (selection == null || selection.size() != 1
				|| !(selection.getFirstElement() instanceof BlackboxRootNode)) {
			return;
		}

		MenuManager scopeMenu = new MenuManager(Messages.BlackboxNavigator_scopeMenu);
		scopeMenu.add(createScopeAction(Messages.BlackboxNavigator_scopeProjectVisible,
				BlackboxVisibilityScope.PROJECT_VISIBLE));
		scopeMenu.add(createScopeAction(Messages.BlackboxNavigator_scopeProjectDependencies,
				BlackboxVisibilityScope.PROJECT_DEPENDENCIES));
		scopeMenu.add(createScopeAction(Messages.BlackboxNavigator_scopeProjectOnly,
				BlackboxVisibilityScope.PROJECT_ONLY));
		menu.appendToGroup(ICommonMenuConstants.GROUP_ADDITIONS, scopeMenu);
	}

	@Override
	public void fillActionBars(IActionBars actionBars) {
		openAction.selectionChanged(getSelection());
		if (openAction.isEnabled()) {
			actionBars.setGlobalActionHandler(ICommonActionConstants.OPEN, openAction);
		}
	}

	private IStructuredSelection getSelection() {
		if (getContext() == null) {
			return null;
		}
		ISelection selection = getContext().getSelection();
		return selection instanceof IStructuredSelection ? (IStructuredSelection) selection : null;
	}

	private Action createScopeAction(String label, final BlackboxVisibilityScope scope) {
		Action action = new Action(label, IAction.AS_RADIO_BUTTON) {
			@Override
			public void run() {
				if (isChecked()) {
					BlackboxVisibilitySettings.setScope(scope);
				}
			}
		};
		action.setChecked(BlackboxVisibilitySettings.getScope() == scope);
		return action;
	}
}
