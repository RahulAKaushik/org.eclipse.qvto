package org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery;

import java.util.Comparator;

import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.impl.EPackageRegistryImpl;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.Messages;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.QVTBBoxUIPlugin;
import org.eclipse.m2m.internal.qvt.oml.blackbox.BlackboxRegistry;
import org.eclipse.m2m.internal.qvt.oml.blackbox.BlackboxUnitDescriptor;
import org.eclipse.m2m.internal.qvt.oml.blackbox.ResolutionContext;
import org.eclipse.m2m.internal.qvt.oml.blackbox.ResolutionContextImpl;
import org.eclipse.m2m.internal.qvt.oml.emf.util.URIUtils;
import org.eclipse.m2m.internal.qvt.oml.emf.util.urimap.MetamodelURIMappingHelper;
import org.eclipse.osgi.util.NLS;

/**
 * Discovers blackboxes visible from one project according to a selected
 * {@link BlackboxVisibilityScope}. The service has no SWT dependency and is
 * intended to run in a background job.
 * <p>
 * Marker synchronization is optional. A canceled run never publishes a
 * partial marker set.
 */
public class ProjectBlackboxDiscoveryService {

	private final BlackboxDescriptorLoader descriptorLoader = new BlackboxDescriptorLoader();
	private final ProjectBlackboxJavaSearch javaSearch = new ProjectBlackboxJavaSearch();
	private final BlackboxProblemMarkerSynchronizer markerSynchronizer = new BlackboxProblemMarkerSynchronizer();

	public static boolean hasBlackboxProblemMarkers(IProject project) {
		return BlackboxProblemMarkerSynchronizer.hasProblemMarkers(project);
	}

	public static boolean isBlackboxProblemMarker(IMarkerDelta markerDelta) {
		return BlackboxProblemMarkerSynchronizer.isProblemMarker(markerDelta);
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
		boolean canceled = false;

		try {
			discoverProject(result, project, scope, monitor);
			sort(result);
		} catch (OperationCanceledException e) {
			canceled = true;
			throw e;
		} catch (RuntimeException e) {
			addProjectFailure(result, project, e);
		} catch (LinkageError e) {
			addProjectFailure(result, project, e);
		} finally {
			if (!canceled && updateMarkers && scope.includesRegistryDescriptors()) {
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
		BlackboxDescriptorCandidates candidates = new BlackboxDescriptorCandidates();
		for (String qualifiedName : javaSearch.findVisibleModuleNames(project, scope, monitor)) {
			checkCanceled(monitor);
			collectJavaDescriptor(result, candidates, qualifiedName, context, packageRegistry);
		}

		if (scope.includesRegistryDescriptors()) {
			checkCanceled(monitor);
			collectRegistryDescriptors(result, project, candidates, context, packageRegistry, monitor);
		}

		for (BlackboxDescriptorCandidates.Candidate candidate : candidates.values()) {
			checkCanceled(monitor);
			result.addUnit(descriptorLoader.load(result, candidate.getDescriptor(), candidate.getQualifiedName(),
					candidate.getPackageRegistry()));
		}
	}

	private void collectJavaDescriptor(BlackboxDiscoveryResult result, BlackboxDescriptorCandidates candidates,
			String qualifiedName, ResolutionContext context, EPackage.Registry packageRegistry) {
		try {
			// Some providers populate their cache only after a qualified lookup.
			BlackboxUnitDescriptor descriptor = BlackboxRegistry.INSTANCE.getCompilationUnitDescriptor(qualifiedName,
					context);
			candidates.add(qualifiedName, descriptor, packageRegistry);
		} catch (OperationCanceledException e) {
			throw e;
		} catch (RuntimeException e) {
			addUnitFailure(result, qualifiedName, e);
		} catch (LinkageError e) {
			addUnitFailure(result, qualifiedName, e);
		}
	}

	private void collectRegistryDescriptors(BlackboxDiscoveryResult result, IProject project,
			BlackboxDescriptorCandidates candidates, ResolutionContext context,
			EPackage.Registry packageRegistry, IProgressMonitor monitor) {
		try {
			for (BlackboxUnitDescriptor descriptor : BlackboxRegistry.INSTANCE.getCompilationUnitDescriptors(context)) {
				checkCanceled(monitor);
				if (descriptor != null) {
					candidates.add(descriptor.getQualifiedName(), descriptor, packageRegistry);
				}
			}
		} catch (OperationCanceledException e) {
			throw e;
		} catch (RuntimeException e) {
			addProjectFailure(result, project, e);
		} catch (LinkageError e) {
			addProjectFailure(result, project, e);
		}
	}

	private static void addProjectFailure(BlackboxDiscoveryResult result, IProject project, Throwable throwable) {
		String message = NLS.bind(Messages.BlackboxDiscovery_projectFailed,
				new Object[] { project.getName(), BlackboxDiagnosticUtil.getMessage(throwable) });
		QVTBBoxUIPlugin.log(QVTBBoxUIPlugin.createStatus(IStatus.ERROR, message, throwable));
		result.addDiagnostic(new BlackboxDiagnosticInfo(result, Diagnostic.ERROR, message));
	}

	private static void addUnitFailure(BlackboxDiscoveryResult result, String qualifiedName, Throwable throwable) {
		String message = NLS.bind(Messages.BlackboxDiscovery_unitFailed,
				new Object[] { qualifiedName, BlackboxDiagnosticUtil.getMessage(throwable) });
		QVTBBoxUIPlugin.log(QVTBBoxUIPlugin.createStatus(IStatus.ERROR, message, throwable));
		result.addDiagnostic(new BlackboxDiagnosticInfo(result, Diagnostic.ERROR, message));
	}

	private void checkCanceled(IProgressMonitor monitor) {
		if (monitor != null && monitor.isCanceled()) {
			throw new OperationCanceledException();
		}
	}

	public static EPackage.Registry createPackageRegistry(IProject project) {
		ResourceSet resourceSet = new ResourceSetImpl();
		EPackage.Registry registry = MetamodelURIMappingHelper.mappingsToEPackageRegistry(project, resourceSet);
		if (registry != null) {
			return registry;
		}
		return new EPackageRegistryImpl(EPackage.Registry.INSTANCE);
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
			markerSynchronizer.synchronize(project, result.getDiagnostics(), result.getUnits());
		} catch (CoreException e) {
			QVTBBoxUIPlugin.log(e);
		}
	}

}
