package org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EOperation;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EParameter;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.impl.EPackageRegistryImpl;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.Messages;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.QVTBBoxUIPlugin;
import org.eclipse.m2m.internal.qvt.oml.blackbox.BlackboxException;
import org.eclipse.m2m.internal.qvt.oml.blackbox.BlackboxRegistry;
import org.eclipse.m2m.internal.qvt.oml.blackbox.BlackboxUnit;
import org.eclipse.m2m.internal.qvt.oml.blackbox.BlackboxUnitDescriptor;
import org.eclipse.m2m.internal.qvt.oml.blackbox.LoadContext;
import org.eclipse.m2m.internal.qvt.oml.blackbox.ResolutionContext;
import org.eclipse.m2m.internal.qvt.oml.blackbox.ResolutionContextImpl;
import org.eclipse.m2m.internal.qvt.oml.emf.util.URIUtils;
import org.eclipse.m2m.internal.qvt.oml.emf.util.urimap.MetamodelURIMappingHelper;
import org.eclipse.m2m.internal.qvt.oml.expressions.ImperativeOperation;
import org.eclipse.m2m.internal.qvt.oml.expressions.ModelType;
import org.eclipse.m2m.internal.qvt.oml.expressions.Module;
import org.eclipse.m2m.internal.qvt.oml.project.QVTOProjectPlugin;
import org.eclipse.osgi.util.NLS;

public class ProjectBlackboxDiscoveryService {

	private static final String MARKER_ATTRIBUTE = QVTBBoxUIPlugin.PLUGIN_ID + ".blackboxMarker"; //$NON-NLS-1$

	public static boolean hasBlackboxProblemMarkers(IProject project) {
		try {
			if (project == null || !project.isAccessible()) {
				return false;
			}
			for (IMarker marker : project.findMarkers(QVTOProjectPlugin.PROBLEM_MARKER, false, IResource.DEPTH_ZERO)) {
				if (marker.getAttribute(MARKER_ATTRIBUTE, false)
						&& marker.getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO) == IMarker.SEVERITY_ERROR) {
					return true;
				}
			}
		} catch (CoreException e) {
			QVTBBoxUIPlugin.log(e);
		}
		return false;
	}

	public static boolean isBlackboxProblemMarker(IMarkerDelta markerDelta) {
		return markerDelta != null
				&& markerDelta.isSubtypeOf(QVTOProjectPlugin.PROBLEM_MARKER)
				&& markerDelta.getAttribute(MARKER_ATTRIBUTE, false);
	}

	public BlackboxDiscoveryResult discover(IProject project) {
		return discover(project, BlackboxVisibilityScope.PROJECT_VISIBLE, true, null);
	}

	public BlackboxDiscoveryResult discover(IProject project, boolean updateMarkers) {
		return discover(project, BlackboxVisibilityScope.PROJECT_VISIBLE, updateMarkers, null);
	}

	public BlackboxDiscoveryResult discover(IProject project, boolean updateMarkers, IProgressMonitor monitor) {
		return discover(project, BlackboxVisibilityScope.PROJECT_VISIBLE, updateMarkers, monitor);
	}

	public BlackboxDiscoveryResult discover(IProject project, BlackboxVisibilityScope scope, boolean updateMarkers,
			IProgressMonitor monitor) {
		BlackboxDiscoveryResult result = new BlackboxDiscoveryResult(project);

		try {
			discoverProject(result, project, scope, monitor);
			sort(result);
		} catch (OperationCanceledException e) {
			throw e;
		} catch (RuntimeException e) {
			QVTBBoxUIPlugin.log(e);
			result.addDiagnostic(new BlackboxDiagnosticInfo(result, Diagnostic.ERROR, safeMessage(e)));
		} catch (LinkageError e) {
			QVTBBoxUIPlugin.log(e);
			result.addDiagnostic(new BlackboxDiagnosticInfo(result, Diagnostic.ERROR, safeMessage(e)));
		} finally {
			if (updateMarkers && scope == BlackboxVisibilityScope.PROJECT_VISIBLE) {
				updateMarkers(project, result);
			}
		}

		return result;
	}

	private void discoverProject(BlackboxDiscoveryResult result, IProject project, BlackboxVisibilityScope scope,
			IProgressMonitor monitor) {
		URI projectURI = URIUtils.getResourceURI(project);
		EPackage.Registry packageRegistry = createPackageRegistry(project);
		ResolutionContext context = new ResolutionContextImpl(projectURI);
		Map<String, DescriptorCandidate> candidates = new LinkedHashMap<String, DescriptorCandidate>();
		for (String qualifiedName : findVisibleModuleNames(project, scope, monitor)) {
			checkCanceled(monitor);
			BlackboxUnitDescriptor descriptor = BlackboxRegistry.INSTANCE.getCompilationUnitDescriptor(qualifiedName, context);
			addCandidate(candidates, qualifiedName, descriptor, packageRegistry);
		}

		if (scope == BlackboxVisibilityScope.PROJECT_VISIBLE) {
			checkCanceled(monitor);
			collectRegistryDescriptors(result, candidates, context, packageRegistry);
		}

		for (DescriptorCandidate candidate : candidates.values()) {
			checkCanceled(monitor);
			loadDescriptor(result, candidate.descriptor, candidate.qualifiedName, candidate.packageRegistry);
		}
	}

	private void collectRegistryDescriptors(BlackboxDiscoveryResult result,
			Map<String, DescriptorCandidate> candidates, ResolutionContext context,
			EPackage.Registry packageRegistry) {
		try {
			for (BlackboxUnitDescriptor descriptor : BlackboxRegistry.INSTANCE.getCompilationUnitDescriptors(context)) {
				if (descriptor != null) {
					addCandidate(candidates, descriptor.getQualifiedName(), descriptor, packageRegistry);
				}
			}
		} catch (RuntimeException e) {
			QVTBBoxUIPlugin.log(e);
			result.addDiagnostic(new BlackboxDiagnosticInfo(result, Diagnostic.ERROR, safeMessage(e)));
		} catch (LinkageError e) {
			QVTBBoxUIPlugin.log(e);
			result.addDiagnostic(new BlackboxDiagnosticInfo(result, Diagnostic.ERROR, safeMessage(e)));
		}
	}

	private void addCandidate(Map<String, DescriptorCandidate> candidates, String qualifiedName,
			BlackboxUnitDescriptor descriptor, EPackage.Registry packageRegistry) {
		DescriptorCandidate existing = candidates.get(qualifiedName);
		if (existing == null || existing.descriptor == null && descriptor != null) {
			candidates.put(qualifiedName,
					new DescriptorCandidate(qualifiedName, descriptor, packageRegistry));
		}
	}

	private Set<String> findVisibleModuleNames(final IProject project, final BlackboxVisibilityScope scope,
			IProgressMonitor monitor) {
		final Set<String> qualifiedNames = new LinkedHashSet<String>();
		try {
			if (!project.hasNature(JavaCore.NATURE_ID)) {
				return qualifiedNames;
			}

			IJavaProject javaProject = JavaCore.create(project);
			SearchPattern pattern = SearchPattern.createPattern(
					org.eclipse.m2m.qvt.oml.blackbox.java.Module.class.getCanonicalName(),
					IJavaSearchConstants.ANNOTATION_TYPE, IJavaSearchConstants.ANNOTATION_TYPE_REFERENCE,
					SearchPattern.R_EXACT_MATCH);
			SearchParticipant[] participants = { SearchEngine.getDefaultSearchParticipant() };
			int includeMask = scope == BlackboxVisibilityScope.PROJECT_ONLY
					? IJavaSearchScope.SOURCES
					: IJavaSearchScope.SOURCES | IJavaSearchScope.REFERENCED_PROJECTS
							| IJavaSearchScope.APPLICATION_LIBRARIES;
			IJavaSearchScope searchScope = SearchEngine.createJavaSearchScope(new IJavaElement[] { javaProject },
					includeMask);
			SearchRequestor requestor = new SearchRequestor() {
				@Override
				public void acceptSearchMatch(SearchMatch match) {
					Object element = match.getElement();
					if (element instanceof IType) {
						IType type = (IType) element;
						if (scope != BlackboxVisibilityScope.PROJECT_ONLY
								|| project.equals(type.getJavaProject().getProject())) {
							qualifiedNames.add(type.getFullyQualifiedName());
						}
					}
				}
			};
			new SearchEngine().search(pattern, participants, searchScope, requestor, monitor);
		} catch (CoreException e) {
			QVTBBoxUIPlugin.log(e);
		}
		return qualifiedNames;
	}

	private void checkCanceled(IProgressMonitor monitor) {
		if (monitor != null && monitor.isCanceled()) {
			throw new OperationCanceledException();
		}
	}

	private void loadDescriptor(BlackboxDiscoveryResult result, BlackboxUnitDescriptor descriptor, String qualifiedName,
			EPackage.Registry packageRegistry) {
		URI descriptorURI = descriptor != null ? descriptor.getURI() : null;
		BlackboxUnitInfo unitInfo = new BlackboxUnitInfo(result, qualifiedName, descriptorURI);
		result.addUnit(unitInfo);

		if (descriptor == null) {
			unitInfo.addDiagnostic(new BlackboxDiagnosticInfo(unitInfo, Diagnostic.ERROR,
					NLS.bind(Messages.BlackboxDiscovery_descriptorNotResolved, qualifiedName)));
			return;
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
	}

	private EPackage.Registry createPackageRegistry(IProject project) {
		ResourceSet resourceSet = new ResourceSetImpl();
		EPackage.Registry registry = MetamodelURIMappingHelper.mappingsToEPackageRegistry(project, resourceSet);
		if (registry != null) {
			return registry;
		}
		return new EPackageRegistryImpl(EPackage.Registry.INSTANCE);
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

	private static String safeMessage(Throwable throwable) {
		String message = null;
		try {
			message = throwable.getMessage();
		} catch (RuntimeException e) {
			// Keep diagnostics robust even for exceptions with broken message implementations.
		}
		return message != null ? message : throwable.getClass().getName();
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

	private void sort(BlackboxDiscoveryResult result) {
		result.sortUnits(new Comparator<BlackboxUnitInfo>() {
			public int compare(BlackboxUnitInfo left, BlackboxUnitInfo right) {
				return left.getQualifiedName().compareTo(right.getQualifiedName());
			}
		});
	}

	private void updateMarkers(IProject project, BlackboxDiscoveryResult result) {
		try {
			deleteMarkers(project);
			for (BlackboxDiagnosticInfo diagnostic : result.getDiagnostics()) {
				createMarkers(project, diagnostic);
			}
			for (BlackboxUnitInfo unit : result.getUnits()) {
				for (BlackboxDiagnosticInfo diagnostic : unit.getDiagnostics()) {
					createMarkers(project, unit, diagnostic);
				}
			}
		} catch (CoreException e) {
			QVTBBoxUIPlugin.log(e);
		}
	}

	private void deleteMarkers(IProject project) throws CoreException {
		for (IMarker marker : project.findMarkers(QVTOProjectPlugin.PROBLEM_MARKER, false, IResource.DEPTH_ZERO)) {
			if (marker.getAttribute(MARKER_ATTRIBUTE, false)) {
				marker.delete();
			}
		}
	}

	private void createMarkers(IProject project, BlackboxUnitInfo unit, BlackboxDiagnosticInfo diagnostic) throws CoreException {
		boolean childMarkerCreated = false;
		for (BlackboxDiagnosticInfo child : diagnostic.getChildren()) {
			if (child.hasErrors()) {
				createMarkers(project, unit, child);
				childMarkerCreated = true;
			}
		}
		if (!childMarkerCreated && diagnostic.isError()) {
			createMarker(project, unit, diagnostic);
		}
	}

	private void createMarkers(IProject project, BlackboxDiagnosticInfo diagnostic) throws CoreException {
		boolean childMarkerCreated = false;
		for (BlackboxDiagnosticInfo child : diagnostic.getChildren()) {
			if (child.hasErrors()) {
				createMarkers(project, child);
				childMarkerCreated = true;
			}
		}
		if (!childMarkerCreated && diagnostic.isError()) {
			createMarker(project, diagnostic);
		}
	}

	private void createMarker(IProject project, BlackboxUnitInfo unit, BlackboxDiagnosticInfo diagnostic) throws CoreException {
		IMarker marker = project.createMarker(QVTOProjectPlugin.PROBLEM_MARKER);
		marker.setAttribute(MARKER_ATTRIBUTE, true);
		marker.setAttribute(IMarker.MESSAGE, NLS.bind(Messages.BlackboxDiscovery_markerMessage,
				unit.getQualifiedName(), diagnostic.getMessage()));
		marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
		marker.setAttribute(IMarker.LOCATION, unit.getURI() != null ? unit.getURI().toString() : unit.getQualifiedName());
	}

	private void createMarker(IProject project, BlackboxDiagnosticInfo diagnostic) throws CoreException {
		IMarker marker = project.createMarker(QVTOProjectPlugin.PROBLEM_MARKER);
		marker.setAttribute(MARKER_ATTRIBUTE, true);
		marker.setAttribute(IMarker.MESSAGE, NLS.bind(Messages.BlackboxDiscovery_projectMarkerMessage,
				diagnostic.getMessage()));
		marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
		marker.setAttribute(IMarker.LOCATION, project.getFullPath().toString());
	}

	private static class DescriptorCandidate {

		final String qualifiedName;
		final BlackboxUnitDescriptor descriptor;
		final EPackage.Registry packageRegistry;

		DescriptorCandidate(String qualifiedName, BlackboxUnitDescriptor descriptor,
				EPackage.Registry packageRegistry) {
			this.qualifiedName = qualifiedName;
			this.descriptor = descriptor;
			this.packageRegistry = packageRegistry;
		}
	}

}
