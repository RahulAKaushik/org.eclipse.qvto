package org.eclipse.m2m.internal.qvt.oml.bbox.ui.settings;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.Messages;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.QVTBBoxUIPlugin;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxVisibilityScope;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.dialogs.PropertyPage;

public class BlackboxVisibilityPropertyPage extends PropertyPage {

	private IProject project;
	private Button projectVisibleButton;
	private Button projectDependenciesButton;
	private Button projectOnlyButton;

	@Override
	protected Control createContents(Composite parent) {
		project = getSelectedProject();
		if (project == null) {
			setValid(false);
			Label label = new Label(parent, SWT.NONE);
			label.setText(Messages.BlackboxSettings_noProject);
			return label;
		}

		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout());
		container.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

		Group group = new Group(container, SWT.NONE);
		group.setText(Messages.BlackboxSettings_visibilityGroup);
		group.setLayout(new GridLayout());
		group.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

		projectVisibleButton = createRadioButton(group, Messages.BlackboxNavigator_scopeProjectVisible);
		projectDependenciesButton = createRadioButton(group,
				Messages.BlackboxNavigator_scopeProjectDependencies);
		projectOnlyButton = createRadioButton(group, Messages.BlackboxNavigator_scopeProjectOnly);
		select(BlackboxVisibilitySettings.getScope(project));
		return container;
	}

	@Override
	public boolean performOk() {
		return saveScope();
	}

	@Override
	protected void performApply() {
		saveScope();
	}

	@Override
	protected void performDefaults() {
		select(BlackboxVisibilitySettings.DEFAULT_SCOPE);
		super.performDefaults();
	}

	private Button createRadioButton(Composite parent, String text) {
		Button button = new Button(parent, SWT.RADIO);
		button.setText(text);
		button.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
		return button;
	}

	private boolean saveScope() {
		if (project == null) {
			return false;
		}
		try {
			BlackboxVisibilitySettings.setScope(project, selectedScope());
			setErrorMessage(null);
			return true;
		} catch (CoreException e) {
			QVTBBoxUIPlugin.log(e.getStatus());
			setErrorMessage(e.getStatus().getMessage());
			return false;
		}
	}

	private BlackboxVisibilityScope selectedScope() {
		if (projectOnlyButton.getSelection()) {
			return BlackboxVisibilityScope.PROJECT_ONLY;
		}
		if (projectDependenciesButton.getSelection()) {
			return BlackboxVisibilityScope.PROJECT_DEPENDENCIES;
		}
		return BlackboxVisibilityScope.PROJECT_VISIBLE;
	}

	private void select(BlackboxVisibilityScope scope) {
		projectVisibleButton.setSelection(scope == BlackboxVisibilityScope.PROJECT_VISIBLE);
		projectDependenciesButton.setSelection(scope == BlackboxVisibilityScope.PROJECT_DEPENDENCIES);
		projectOnlyButton.setSelection(scope == BlackboxVisibilityScope.PROJECT_ONLY);
	}

	private IProject getSelectedProject() {
		IAdaptable element = getElement();
		if (element instanceof IProject) {
			return (IProject) element;
		}
		return element != null ? element.getAdapter(IProject.class) : null;
	}
}
