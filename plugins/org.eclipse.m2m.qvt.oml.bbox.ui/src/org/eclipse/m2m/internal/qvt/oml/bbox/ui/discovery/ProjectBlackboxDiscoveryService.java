package org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EOperation;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EParameter;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.impl.EPackageRegistryImpl;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.Messages;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.QVTBBoxUIPlugin;
import org.eclipse.m2m.internal.qvt.oml.blackbox.BlackboxException;
import org.eclipse.m2m.internal.qvt.oml.blackbox.BlackboxRegistry;
import org.eclipse.m2m.internal.qvt.oml.blackbox.BlackboxUnit;
import org.eclipse.m2m.internal.qvt.oml.blackbox.BlackboxUnitDescriptor;
import org.eclipse.m2m.internal.qvt.oml.blackbox.LoadContext;
import org.eclipse.m2m.internal.qvt.oml.blackbox.ResolutionContext;
import org.eclipse.m2m.internal.qvt.oml.blackbox.ResolutionContextImpl;
import org.eclipse.m2m.internal.qvt.oml.ast.parser.QvtOperationalParserUtil;
import org.eclipse.m2m.internal.qvt.oml.compiler.BlackboxUnitResolver;
import org.eclipse.m2m.internal.qvt.oml.compiler.CompiledUnit;
import org.eclipse.m2m.internal.qvt.oml.compiler.QVTOCompiler;
import org.eclipse.m2m.internal.qvt.oml.compiler.QvtCompilerOptions;
import org.eclipse.m2m.internal.qvt.oml.compiler.ResolverUtils;
import org.eclipse.m2m.internal.qvt.oml.compiler.UnitProxy;
import org.eclipse.m2m.internal.qvt.oml.compiler.UnitResolverFactory;
import org.eclipse.m2m.internal.qvt.oml.cst.ImportCS;
import org.eclipse.m2m.internal.qvt.oml.emf.util.URIUtils;
import org.eclipse.m2m.internal.qvt.oml.emf.util.urimap.MetamodelURIMappingHelper;
import org.eclipse.m2m.internal.qvt.oml.expressions.ImperativeOperation;
import org.eclipse.m2m.internal.qvt.oml.expressions.ModelType;
import org.eclipse.m2m.internal.qvt.oml.expressions.Module;
import org.eclipse.m2m.internal.qvt.oml.project.QVTOProjectPlugin;
import org.eclipse.osgi.util.NLS;

public class ProjectBlackboxDiscoveryService {

	private static final String MARKER_ATTRIBUTE = QVTBBoxUIPlugin.PLUGIN_ID + ".blackboxMarker"; //$NON-NLS-1$
	private static final String JDT_QUERY = "jdt"; //$NON-NLS-1$
	private static final String OSGI_QUERY = "osgi"; //$NON-NLS-1$
	private static final String QVTO_NAMESPACE_SEPARATOR = "."; //$NON-NLS-1$

	public BlackboxDiscoveryResult discover(IProject project) {
		return discover(project, true);
	}

	public BlackboxDiscoveryResult discover(IProject project, boolean updateMarkers) {
		BlackboxDiscoveryResult result = new BlackboxDiscoveryResult(project);
		Map<String, Candidate> candidates = new LinkedHashMap<String, Candidate>();

		try {
			URI projectURI = URIUtils.getResourceURI(project);
			EPackage.Registry packageRegistry = createPackageRegistry(project);
			collectImportedDescriptors(project, projectURI, packageRegistry, candidates, result);
			collectAvailableDescriptors(project, projectURI, candidates);
			loadCandidates(result, candidates.values(), packageRegistry);
			sort(result);
		} catch (RuntimeException e) {
			QVTBBoxUIPlugin.log(e);
			result.addDiagnostic(new BlackboxDiagnosticInfo(result, Diagnostic.ERROR, safeMessage(e)));
		} catch (LinkageError e) {
			QVTBBoxUIPlugin.log(e);
			result.addDiagnostic(new BlackboxDiagnosticInfo(result, Diagnostic.ERROR, safeMessage(e)));
		} finally {
			if (updateMarkers) {
				updateMarkers(project, result);
			}
		}

		return result;
	}

	private void collectImportedDescriptors(IProject project, URI projectURI, EPackage.Registry packageRegistry, Map<String, Candidate> candidates, BlackboxDiscoveryResult result) {
		List<UnitProxy> units = UnitResolverFactory.Registry.INSTANCE.findAllUnits(projectURI);

		ImportParsingCompiler compiler = new ImportParsingCompiler(packageRegistry);
		QvtCompilerOptions options = new QvtCompilerOptions();
		options.setGenerateCompletionData(false);

		try {
			Set<ImportedBlackbox> blackboxImports = new LinkedHashSet<ImportedBlackbox>();
			if (!units.isEmpty()) {
				try {
					CompiledUnit[] compiledUnits = compiler.compile(units.toArray(new UnitProxy[units.size()]), options,
							(org.eclipse.core.runtime.IProgressMonitor) null);
					for (CompiledUnit compiledUnit : compiledUnits) {
						collectBlackboxImports(compiledUnit, blackboxImports, new LinkedHashSet<URI>());
					}
				} catch (Exception e) {
					QVTBBoxUIPlugin.log(e);
					result.addDiagnostic(new BlackboxDiagnosticInfo(result, Diagnostic.WARNING, safeMessage(e)));
				}
			}

			collectSourceBlackboxImports(units, compiler, options, blackboxImports, result);
			for (ImportedBlackbox blackboxImport : blackboxImports) {
				URI blackboxURI = blackboxImport.uri;
				String qualifiedName = getQualifiedName(blackboxURI);
				if (qualifiedName == null) {
					continue;
				}

				ResolutionContext context = new ResolutionContextImpl(blackboxImport.contextURI);
				BlackboxUnitDescriptor descriptor = BlackboxRegistry.INSTANCE.getCompilationUnitDescriptor(qualifiedName, context);
				addCandidate(candidates, descriptor, qualifiedName, blackboxURI, true);
			}
		} catch (Exception e) {
			QVTBBoxUIPlugin.log(e);
			result.addDiagnostic(new BlackboxDiagnosticInfo(result, Diagnostic.WARNING, safeMessage(e)));
		} finally {
			compiler.cleanup();
		}
	}

	private void collectSourceBlackboxImports(List<UnitProxy> units, ImportParsingCompiler compiler, QvtCompilerOptions options,
			Set<ImportedBlackbox> blackboxImports, BlackboxDiscoveryResult result) {
		for (UnitProxy unit : units) {
			URI contextURI = unit.getURI();
			try {
				for (String qualifiedName : compiler.parseImportNames(unit, options)) {
					ResolutionContext context = new ResolutionContextImpl(contextURI);
					BlackboxUnitDescriptor descriptor = BlackboxRegistry.INSTANCE.getCompilationUnitDescriptor(qualifiedName, context);
					if (descriptor != null) {
						blackboxImports.add(new ImportedBlackbox(descriptor.getURI(), contextURI));
					}
				}
			} catch (Exception e) {
				QVTBBoxUIPlugin.log(e);
				result.addDiagnostic(new BlackboxDiagnosticInfo(result, Diagnostic.WARNING, safeMessage(e)));
			}
		}
	}

	private void collectBlackboxImports(CompiledUnit unit, Set<ImportedBlackbox> blackboxImports, Set<URI> visited) {
		if (unit == null || !visited.add(unit.getURI())) {
			return;
		}

		for (CompiledUnit importedUnit : unit.getCompiledImports()) {
			if (BlackboxUnitResolver.isBlackboxUnitURI(importedUnit.getURI())) {
				blackboxImports.add(new ImportedBlackbox(importedUnit.getURI(), unit.getURI()));
			}
			collectBlackboxImports(importedUnit, blackboxImports, visited);
		}
	}

	private void collectAvailableDescriptors(IProject project, URI projectURI, Map<String, Candidate> candidates) {
		ResolutionContext context = new ResolutionContextImpl(projectURI);
		for (BlackboxUnitDescriptor descriptor : BlackboxRegistry.INSTANCE.getCompilationUnitDescriptors(context)) {
			if (isProjectDescriptor(project, descriptor)) {
				addCandidate(candidates, descriptor, descriptor.getQualifiedName(), descriptor.getURI(), false);
			}
		}
	}

	private boolean isProjectDescriptor(IProject project, BlackboxUnitDescriptor descriptor) {
		if (project == null || descriptor == null || descriptor.getURI() == null) {
			return false;
		}

		URI descriptorURI = descriptor.getURI();
		String projectName = project.getName();
		return projectName.equals(ResolverUtils.getQueryValue(descriptorURI, JDT_QUERY))
				|| projectName.equals(ResolverUtils.getQueryValue(descriptorURI, OSGI_QUERY))
				|| isProjectPlatformURI(projectName, descriptor.reconvertURI());
	}

	private boolean isProjectPlatformURI(String projectName, URI uri) {
		if (uri == null || uri.segmentCount() < 2) {
			return false;
		}
		return (uri.isPlatformResource() || uri.isPlatformPlugin()) && projectName.equals(uri.segment(1));
	}

	private void addCandidate(Map<String, Candidate> candidates, BlackboxUnitDescriptor descriptor, String qualifiedName, URI uri, boolean used) {
		String key = descriptor != null ? descriptor.getURI().toString() : String.valueOf(uri);
		Candidate candidate = candidates.get(key);
		if (candidate == null) {
			candidate = new Candidate(descriptor, qualifiedName, uri, used);
			candidates.put(key, candidate);
		} else {
			candidate.used = candidate.used || used;
			if (candidate.descriptor == null && descriptor != null) {
				candidate.descriptor = descriptor;
			}
		}
	}

	private void loadCandidates(BlackboxDiscoveryResult result, Collection<Candidate> candidates, EPackage.Registry packageRegistry) {
		for (Candidate candidate : candidates) {
			BlackboxUnitInfo unitInfo = new BlackboxUnitInfo(result, candidate.qualifiedName, candidate.uri, candidate.used);
			result.addUnit(unitInfo);

			if (candidate.descriptor == null) {
				unitInfo.addDiagnostic(new BlackboxDiagnosticInfo(unitInfo, Diagnostic.ERROR,
						NLS.bind(Messages.BlackboxDiscovery_descriptorNotResolved, candidate.qualifiedName)));
				continue;
			}

			try {
				BlackboxUnit unit = candidate.descriptor.load(new LoadContext(packageRegistry));
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
		if (diagnostic == null || diagnostic.getSeverity() == Diagnostic.OK) {
			return;
		}

		if (diagnostic.getChildren().isEmpty()) {
			unitInfo.addDiagnostic(new BlackboxDiagnosticInfo(unitInfo, diagnostic.getSeverity(), diagnostic.getMessage()));
			return;
		}

		for (Diagnostic child : diagnostic.getChildren()) {
			addDiagnostic(unitInfo, child);
		}
	}

	private static String getQualifiedName(URI blackboxURI) {
		return blackboxURI != null && blackboxURI.segmentCount() > 0 ? blackboxURI.lastSegment() : null;
	}

	private void sort(BlackboxDiscoveryResult result) {
		result.sortUnits(new Comparator<BlackboxUnitInfo>() {
			public int compare(BlackboxUnitInfo left, BlackboxUnitInfo right) {
				if (left.isUsed() != right.isUsed()) {
					return left.isUsed() ? -1 : 1;
				}
				return left.getQualifiedName().compareTo(right.getQualifiedName());
			}
		});
	}

	private void updateMarkers(IProject project, BlackboxDiscoveryResult result) {
		try {
			deleteMarkers(project);
			for (BlackboxDiagnosticInfo diagnostic : result.getDiagnostics()) {
				if (diagnostic.isError()) {
					createMarker(project, diagnostic);
				}
			}
			for (BlackboxUnitInfo unit : result.getUnits()) {
				for (BlackboxDiagnosticInfo diagnostic : unit.getDiagnostics()) {
					if (diagnostic.isError()) {
						createMarker(project, unit, diagnostic);
					}
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

	private static class Candidate {
		BlackboxUnitDescriptor descriptor;
		final String qualifiedName;
		final URI uri;
		boolean used;

		Candidate(BlackboxUnitDescriptor descriptor, String qualifiedName, URI uri, boolean used) {
			this.descriptor = descriptor;
			this.qualifiedName = qualifiedName;
			this.uri = uri;
			this.used = used;
		}
	}

	private static class ImportedBlackbox {
		final URI uri;
		final URI contextURI;

		ImportedBlackbox(URI uri, URI contextURI) {
			this.uri = uri;
			this.contextURI = contextURI;
		}

		@Override
		public int hashCode() {
			return 31 * uri.hashCode() + contextURI.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof ImportedBlackbox == false) {
				return false;
			}
			ImportedBlackbox other = (ImportedBlackbox) obj;
			return uri.equals(other.uri) && contextURI.equals(other.contextURI);
		}
	}

	private static class ImportParsingCompiler extends QVTOCompiler {

		ImportParsingCompiler(EPackage.Registry packageRegistry) {
			super(packageRegistry);
		}

		List<String> parseImportNames(UnitProxy unit, QvtCompilerOptions options) throws Exception {
			List<String> result = new ArrayList<String>();
			CSTParseResult parseResult = parse(unit, options);
			if (parseResult.unitCS == null) {
				return result;
			}

			for (ImportCS importCS : QvtOperationalParserUtil.getImports(parseResult.unitCS)) {
				if (importCS.getPathNameCS() != null) {
					result.add(QvtOperationalParserUtil.getStringRepresentation(importCS.getPathNameCS(),
							QVTO_NAMESPACE_SEPARATOR));
				}
			}
			return result;
		}
	}
}
