package org.eclipse.m2m.internal.qvt.oml.bbox.ui.global;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.QVTBBoxUIPlugin;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.ProjectBlackboxDiscoveryService;
import org.eclipse.m2m.internal.qvt.oml.blackbox.ResolutionContext;
import org.eclipse.m2m.internal.qvt.oml.blackbox.ResolutionContextImpl;
import org.eclipse.m2m.internal.qvt.oml.emf.util.URIUtils;
import org.eclipse.m2m.qvt.oml.blackbox.java.Module;

final class WorkspaceBlackboxDiscovery {

	private final GlobalBlackboxUnitResolver unitResolver;

	WorkspaceBlackboxDiscovery(GlobalBlackboxUnitResolver unitResolver) {
		this.unitResolver = unitResolver;
	}

	void discover(GlobalBlackboxDiscoveryResult result, Set<BlackboxDescriptorIdentity> attributedDescriptors,
			IProgressMonitor monitor) {
		SubMonitor progress = SubMonitor.convert(monitor);
		List<IJavaProject> javaProjects = new ArrayList<IJavaProject>();
		Map<IProject, IJavaProject> accessibleProjects = new LinkedHashMap<IProject, IJavaProject>();
		Map<String, GlobalBlackboxGroup> projectGroups = new LinkedHashMap<String, GlobalBlackboxGroup>();
		Map<String, GlobalBlackboxGroup> libraryGroups = new LinkedHashMap<String, GlobalBlackboxGroup>();
		Map<IProject, JavaProjectContext> projectContexts = new LinkedHashMap<IProject, JavaProjectContext>();
		Map<String, JavaProjectContext> libraryContexts = new LinkedHashMap<String, JavaProjectContext>();
		Set<GlobalBlackboxOriginIdentity> workspaceKeys = new LinkedHashSet<GlobalBlackboxOriginIdentity>();
		Set<GlobalBlackboxOriginIdentity> libraryKeys = new LinkedHashSet<GlobalBlackboxOriginIdentity>();

		for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
			checkCanceled(progress);
			if (isAccessibleJavaProject(project)) {
				IJavaProject javaProject = JavaCore.create(project);
				javaProjects.add(javaProject);
				accessibleProjects.put(project, javaProject);
			}
		}
		if (javaProjects.isEmpty()) {
			return;
		}

		int includeMask = IJavaSearchScope.SOURCES | IJavaSearchScope.APPLICATION_LIBRARIES;
		for (IType type : findAnnotatedTypes(javaProjects, includeMask, progress)) {
			checkCanceled(progress);
			IPackageFragmentRoot root = (IPackageFragmentRoot) type.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
			if (root == null) {
				continue;
			}

			int rootKind;
			try {
				rootKind = root.getKind();
			} catch (JavaModelException e) {
				QVTBBoxUIPlugin.log(e);
				continue;
			}

			if (rootKind == IPackageFragmentRoot.K_SOURCE) {
				addWorkspaceType(result, attributedDescriptors, accessibleProjects, projectGroups, projectContexts,
						workspaceKeys, type);
			} else if (rootKind == IPackageFragmentRoot.K_BINARY) {
				addLibraryType(result, attributedDescriptors, libraryGroups, projectContexts, libraryContexts,
						libraryKeys, type, root);
			}
		}
	}

	private void addWorkspaceType(GlobalBlackboxDiscoveryResult result,
			Set<BlackboxDescriptorIdentity> attributedDescriptors, Map<IProject, IJavaProject> accessibleProjects,
			Map<String, GlobalBlackboxGroup> projectGroups, Map<IProject, JavaProjectContext> projectContexts,
			Set<GlobalBlackboxOriginIdentity> workspaceKeys, IType type) {
		IJavaProject javaProject = type.getJavaProject();
		IProject project = javaProject.getProject();
		if (!accessibleProjects.containsKey(project)) {
			return;
		}
		GlobalBlackboxOriginIdentity key = new GlobalBlackboxOriginIdentity(project.getName(),
				type.getFullyQualifiedName());
		if (!workspaceKeys.add(key)) {
			return;
		}
		GlobalBlackboxGroup group = projectGroups.get(project.getName());
		if (group == null) {
			group = new GlobalBlackboxGroup(result.getWorkspaceProjects(), GlobalBlackboxGroupKind.PROJECT,
					project.getName(), project.getName(), javaProject);
			projectGroups.put(project.getName(), group);
			result.getWorkspaceProjects().addChild(group);
		}
		JavaProjectContext context = getProjectContext(projectContexts, javaProject);
		unitResolver.addResolvedUnit(group, type.getFullyQualifiedName(), context.resolutionContext,
				context.packageRegistry, attributedDescriptors);
	}

	private void addLibraryType(GlobalBlackboxDiscoveryResult result,
			Set<BlackboxDescriptorIdentity> attributedDescriptors, Map<String, GlobalBlackboxGroup> libraryGroups,
			Map<IProject, JavaProjectContext> projectContexts, Map<String, JavaProjectContext> libraryContexts,
			Set<GlobalBlackboxOriginIdentity> libraryKeys, IType type, IPackageFragmentRoot root) {
		String rootKey = root.getPath().toString();
		GlobalBlackboxOriginIdentity key = new GlobalBlackboxOriginIdentity(rootKey, type.getFullyQualifiedName());
		if (!libraryKeys.add(key)) {
			return;
		}
		GlobalBlackboxGroup group = libraryGroups.get(rootKey);
		if (group == null) {
			IJavaProject javaProject = type.getJavaProject();
			JavaProjectContext context = getProjectContext(projectContexts, javaProject);
			group = new GlobalBlackboxGroup(result.getJavaLibraries(), GlobalBlackboxGroupKind.LIBRARY, rootKey,
					libraryLabel(root, rootKey), javaProject);
			libraryGroups.put(rootKey, group);
			libraryContexts.put(rootKey, context);
			result.getJavaLibraries().addChild(group);
		}
		JavaProjectContext context = libraryContexts.get(rootKey);
		unitResolver.addResolvedUnit(group, type.getFullyQualifiedName(), context.resolutionContext,
				context.packageRegistry, attributedDescriptors);
	}

	private static Collection<IType> findAnnotatedTypes(List<IJavaProject> javaProjects, int includeMask,
			IProgressMonitor monitor) {
		final Map<String, IType> types = new LinkedHashMap<String, IType>();
		try {
			SearchPattern pattern = SearchPattern.createPattern(Module.class.getCanonicalName(),
					IJavaSearchConstants.ANNOTATION_TYPE, IJavaSearchConstants.ANNOTATION_TYPE_REFERENCE,
					SearchPattern.R_EXACT_MATCH);
			SearchParticipant[] participants = { SearchEngine.getDefaultSearchParticipant() };
			IJavaElement[] elements = javaProjects.toArray(new IJavaElement[javaProjects.size()]);
			IJavaSearchScope scope = SearchEngine.createJavaSearchScope(elements, includeMask);
			SearchRequestor requestor = new SearchRequestor() {
				@Override
				public void acceptSearchMatch(SearchMatch match) {
					if (match.getElement() instanceof IType) {
						IType type = (IType) match.getElement();
						types.put(type.getHandleIdentifier(), type);
					}
				}
			};
			new SearchEngine().search(pattern, participants, scope, requestor, monitor);
		} catch (CoreException e) {
			QVTBBoxUIPlugin.log(e);
		}
		return types.values();
	}

	private static JavaProjectContext getProjectContext(Map<IProject, JavaProjectContext> contexts,
			IJavaProject javaProject) {
		IProject project = javaProject.getProject();
		JavaProjectContext context = contexts.get(project);
		if (context == null) {
			context = new JavaProjectContext(project);
			contexts.put(project, context);
		}
		return context;
	}

	private static boolean isAccessibleJavaProject(IProject project) {
		try {
			return project != null && project.isAccessible() && project.hasNature(JavaCore.NATURE_ID);
		} catch (CoreException e) {
			return false;
		}
	}

	private static String libraryLabel(IPackageFragmentRoot root, String fallback) {
		if (root.getElementName() != null && root.getElementName().length() > 0) {
			return root.getElementName();
		}
		return fallback;
	}

	private static void checkCanceled(IProgressMonitor monitor) {
		if (monitor != null && monitor.isCanceled()) {
			throw new OperationCanceledException();
		}
	}

	private static final class JavaProjectContext {

		final ResolutionContext resolutionContext;
		final EPackage.Registry packageRegistry;

		JavaProjectContext(IProject project) {
			resolutionContext = new ResolutionContextImpl(URIUtils.getResourceURI(project));
			packageRegistry = ProjectBlackboxDiscoveryService.createPackageRegistry(project);
		}
	}
}
