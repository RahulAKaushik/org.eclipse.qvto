package org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EOperation;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EParameter;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.Messages;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.QVTBBoxUIPlugin;
import org.eclipse.m2m.internal.qvt.oml.blackbox.BlackboxException;
import org.eclipse.m2m.internal.qvt.oml.blackbox.BlackboxUnit;
import org.eclipse.m2m.internal.qvt.oml.blackbox.BlackboxUnitDescriptor;
import org.eclipse.m2m.internal.qvt.oml.blackbox.LoadContext;
import org.eclipse.m2m.internal.qvt.oml.expressions.ImperativeOperation;
import org.eclipse.m2m.internal.qvt.oml.expressions.ModelType;
import org.eclipse.m2m.internal.qvt.oml.expressions.Module;
import org.eclipse.osgi.util.NLS;

public class BlackboxDescriptorLoader {

	public BlackboxUnitInfo load(Object parent, BlackboxUnitDescriptor descriptor, String qualifiedName,
			EPackage.Registry packageRegistry) {
		URI descriptorURI = descriptor != null ? descriptor.getURI() : null;
		BlackboxUnitInfo unitInfo = new BlackboxUnitInfo(parent, qualifiedName, descriptorURI);

		if (descriptor == null) {
			unitInfo.addDiagnostic(new BlackboxDiagnosticInfo(unitInfo, Diagnostic.ERROR,
					NLS.bind(Messages.BlackboxDiscovery_descriptorNotResolved, qualifiedName)));
			return unitInfo;
		}

		try {
			BlackboxUnit unit = descriptor.load(new LoadContext(packageRegistry));
			unitInfo.setLoaded(true);
			addDiagnostic(unitInfo, unit.getDiagnostic());
			for (org.eclipse.m2m.internal.qvt.oml.ast.env.QvtOperationalModuleEnv moduleEnv : unit.getElements()) {
				addModule(unitInfo, moduleEnv.getModuleContextType());
			}
		} catch (BlackboxException e) {
			addDiagnostic(unitInfo, e.getDiagnostic());
			if (e.getDiagnostic() == null) {
				unitInfo.addDiagnostic(new BlackboxDiagnosticInfo(unitInfo, Diagnostic.ERROR, safeMessage(e)));
			}
		} catch (RuntimeException e) {
			QVTBBoxUIPlugin.log(e);
			unitInfo.addDiagnostic(new BlackboxDiagnosticInfo(unitInfo, Diagnostic.ERROR, safeMessage(e)));
		} catch (LinkageError e) {
			QVTBBoxUIPlugin.log(e);
			unitInfo.addDiagnostic(new BlackboxDiagnosticInfo(unitInfo, Diagnostic.ERROR, safeMessage(e)));
		}
		return unitInfo;
	}

	private void addModule(BlackboxUnitInfo unitInfo, Module module) {
		if (module == null) {
			return;
		}

		BlackboxModuleInfo moduleInfo = new BlackboxModuleInfo(unitInfo, module.getName());
		unitInfo.addModule(moduleInfo);
		collectPackageURIs(moduleInfo, module);

		List<EOperation> operations = new ArrayList<EOperation>(module.getEOperations());
		Collections.sort(operations, new Comparator<EOperation>() {
			public int compare(EOperation left, EOperation right) {
				return signature(left).compareTo(signature(right));
			}
		});

		for (EOperation operation : operations) {
			moduleInfo.addOperation(new BlackboxOperationInfo(moduleInfo, operation.getName(), signature(operation)));
		}
	}

	private void collectPackageURIs(BlackboxModuleInfo moduleInfo, Module module) {
		for (ModelType modelType : module.getUsedModelType()) {
			for (EPackage ePackage : modelType.getMetamodel()) {
				moduleInfo.addPackageURI(ePackage.getNsURI());
			}
		}

		for (EOperation operation : module.getEOperations()) {
			addPackageURI(moduleInfo, operation.getEType());
			if (operation instanceof ImperativeOperation) {
				ImperativeOperation imperative = (ImperativeOperation) operation;
				if (imperative.getContext() != null) {
					addPackageURI(moduleInfo, imperative.getContext().getEType());
				}
			}
			for (EParameter parameter : operation.getEParameters()) {
				addPackageURI(moduleInfo, parameter.getEType());
			}
		}
	}

	private void addPackageURI(BlackboxModuleInfo moduleInfo, EClassifier classifier) {
		if (classifier != null && classifier.getEPackage() != null) {
			moduleInfo.addPackageURI(classifier.getEPackage().getNsURI());
		}
	}

	private static String signature(EOperation operation) {
		StringBuilder result = new StringBuilder();
		if (operation instanceof ImperativeOperation) {
			ImperativeOperation imperative = (ImperativeOperation) operation;
			if (imperative.getContext() != null && imperative.getContext().getEType() != null) {
				result.append(typeName(imperative.getContext().getEType()));
				result.append("::"); //$NON-NLS-1$
			}
		}

		result.append(operation.getName());
		result.append('(');

		boolean first = true;
		for (EParameter parameter : operation.getEParameters()) {
			if (!first) {
				result.append(", "); //$NON-NLS-1$
			}
			first = false;
			result.append(parameter.getName() != null ? parameter.getName() : "arg"); //$NON-NLS-1$
			result.append(" : "); //$NON-NLS-1$
			result.append(typeName(parameter.getEType()));
		}

		result.append(')');
		if (operation.getEType() != null) {
			result.append(" : "); //$NON-NLS-1$
			result.append(typeName(operation.getEType()));
		}

		return result.toString();
	}

	private static String typeName(EClassifier classifier) {
		return classifier != null && classifier.getName() != null ? classifier.getName() : "OclVoid"; //$NON-NLS-1$
	}

	private void addDiagnostic(BlackboxUnitInfo unitInfo, Diagnostic diagnostic) {
		BlackboxDiagnosticInfo diagnosticInfo = createDiagnostic(unitInfo, diagnostic);
		if (diagnosticInfo != null) {
			unitInfo.addDiagnostic(diagnosticInfo);
		}
	}

	private BlackboxDiagnosticInfo createDiagnostic(Object parent, Diagnostic diagnostic) {
		if (diagnostic == null || diagnostic.getSeverity() == Diagnostic.OK) {
			return null;
		}

		BlackboxDiagnosticInfo diagnosticInfo = new BlackboxDiagnosticInfo(parent, diagnostic.getSeverity(),
				diagnostic.getMessage());
		for (Diagnostic child : diagnostic.getChildren()) {
			BlackboxDiagnosticInfo childInfo = createDiagnostic(diagnosticInfo, child);
			if (childInfo != null) {
				diagnosticInfo.addChild(childInfo);
			}
		}
		if (diagnosticInfo.getChildren().isEmpty() && diagnosticInfo.getMessage() == null) {
			return null;
		}
		return diagnosticInfo;
	}

	private static String safeMessage(Throwable throwable) {
		String message = null;
		try {
			message = throwable.getMessage();
		} catch (RuntimeException e) {
			// Keep diagnostics robust even for exceptions with broken message implementations.
		}
		return message != null ? message : throwable.getClass().getName();
	}
}
