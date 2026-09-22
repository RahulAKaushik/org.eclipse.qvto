package org.eclipse.m2m.internal.qvt.oml.bbox.ui.global;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.impl.EPackageRegistryImpl;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.QVTBBoxUIPlugin;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxDescriptorLoader;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxDiagnosticInfo;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxUnitInfo;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.ProjectBlackboxDiscoveryService;
import org.eclipse.m2m.internal.qvt.oml.blackbox.BlackboxRegistry;
import org.eclipse.m2m.internal.qvt.oml.blackbox.BlackboxUnitDescriptor;
import org.eclipse.m2m.internal.qvt.oml.blackbox.ResolutionContext;
import org.eclipse.m2m.internal.qvt.oml.blackbox.ResolutionContextImpl;
import org.eclipse.m2m.internal.qvt.oml.emf.util.URIUtils;
import org.eclipse.m2m.qvt.oml.blackbox.java.Module;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

public class GlobalBlackboxDiscoveryService {

	private static final String EXTENSION_POINT = "javaBlackboxUnits"; //$NON-NLS-1$
	private static final String QVT_PLUGIN_ID = "org.eclipse.m2m.qvt.oml"; //$NON-NLS-1$
	private static final String UNIT_ELEMENT = "unit"; //$NON-NLS-1$
	private static final String LIBRARY_ELEMENT = "library"; //$NON-NLS-1$
	private static final String NAME_ATTRIBUTE = "name"; //$NON-NLS-1$
	private static final String NAMESPACE_ATTRIBUTE = "namespace"; //$NON-NLS-1$
	private static final String CLASS_ATTRIBUTE = "class"; //$NON-NLS-1$
	private static final String OSGI_QUERY_PREFIX = "osgi="; //$NON-NLS-1$

	private final BlackboxDescriptorLoader descriptorLoader = new BlackboxDescriptorLoader();

	public GlobalBlackboxDiscoveryResult discover(IProgressMonitor monitor) {
		SubMonitor progress = SubMonitor.convert(monitor, 100);
		GlobalBlackboxDiscoveryResult result = new GlobalBlackboxDiscoveryResult();
		Set<String> attributedDescriptors = new HashSet<String>();

		discoverWorkspace(result, attributedDescriptors, progress.split(35));
		discoverExtensionContributions(result, attributedDescriptors, progress.split(10));
		discoverActiveBundles(result, attributedDescriptors, progress.split(45));
		discoverOtherRegistrations(result, attributedDescriptors, progress.split(10));
		result.sort();
		return result;
	}

	private void discoverWorkspace(GlobalBlackboxDiscoveryResult result, Set<String> attributedDescriptors,
			IProgressMonitor monitor) {
		SubMonitor progress = SubMonitor.convert(monitor);
		Map<String, GlobalBlackboxGroup> projectGroups = new LinkedHashMap<String, GlobalBlackboxGroup>();
		Map<String, GlobalBlackboxGroup> libraryGroups = new LinkedHashMap<String, GlobalBlackboxGroup>();
		Set<String> workspaceKeys = new LinkedHashSet<String>();
		Set<String> libraryKeys = new LinkedHashSet<String>();

		for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
			checkCanceled(progress);
			if (!isAccessibleJavaProject(project)) {
				continue;
			}

			IJavaProject javaProject = JavaCore.create(project);
			ResolutionContext context = new ResolutionContextImpl(URIUtils.getResourceURI(project));
			EPackage.Registry packageRegistry = ProjectBlackboxDiscoveryService.createPackageRegistry(project);
			for (IType type : findAnnotatedTypes(javaProject, IJavaSearchScope.SOURCES, progress)) {
				if (!project.equals(type.getJavaProject().getProject())) {
					continue;
				}
				String key = project.getName() + "|" + type.getFullyQualifiedName(); //$NON-NLS-1$
				if (!workspaceKeys.add(key)) {
					continue;
				}
				GlobalBlackboxGroup group = projectGroups.get(project.getName());
				if (group == null) {
					group = new GlobalBlackboxGroup(result.getWorkspaceProjects(), GlobalBlackboxGroupKind.PROJECT,
							project.getName(), project.getName(), javaProject);
					projectGroups.put(project.getName(), group);
					result.getWorkspaceProjects().addChild(group);
				}
				addResolvedUnit(group, type.getFullyQualifiedName(), context, packageRegistry, attributedDescriptors);
			}

			for (IType type : findAnnotatedTypes(javaProject, IJavaSearchScope.APPLICATION_LIBRARIES, progress)) {
				IPackageFragmentRoot root = (IPackageFragmentRoot) type.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
				String rootKey = root != null ? root.getPath().toString() : type.getJavaProject().getElementName();
				String key = rootKey + "|" + type.getFullyQualifiedName(); //$NON-NLS-1$
				if (!libraryKeys.add(key)) {
					continue;
				}
				GlobalBlackboxGroup group = libraryGroups.get(rootKey);
				if (group == null) {
					group = new GlobalBlackboxGroup(result.getJavaLibraries(), GlobalBlackboxGroupKind.LIBRARY,
							rootKey, libraryLabel(root, rootKey), javaProject);
					libraryGroups.put(rootKey, group);
					result.getJavaLibraries().addChild(group);
				}
				addResolvedUnit(group, type.getFullyQualifiedName(), context, packageRegistry, attributedDescriptors);
			}
		}
	}

	private void discoverExtensionContributions(GlobalBlackboxDiscoveryResult result, Set<String> attributedDescriptors,
			IProgressMonitor monitor) {
		Map<String, GlobalBlackboxGroup> bundleGroups = new LinkedHashMap<String, GlobalBlackboxGroup>();
		Set<String> contributionKeys = new HashSet<String>();
		IConfigurationElement[] elements = Platform.getExtensionRegistry()
				.getConfigurationElementsFor(QVT_PLUGIN_ID, EXTENSION_POINT);
		for (IConfigurationElement element : elements) {
			checkCanceled(monitor);
			String qualifiedName = extensionQualifiedName(element);
			if (qualifiedName == null) {
				continue;
			}
			String contributor = element.getContributor().getName();
			String key = contributor + "|" + qualifiedName; //$NON-NLS-1$
			if (!contributionKeys.add(key)) {
				continue;
			}
			GlobalBlackboxGroup group = bundleGroups.get(contributor);
			if (group == null) {
				group = new GlobalBlackboxGroup(result.getExtensionContributions(), GlobalBlackboxGroupKind.BUNDLE,
						contributor, contributor);
				bundleGroups.put(contributor, group);
				result.getExtensionContributions().addChild(group);
			}
			ResolutionContext context = bundleContext(contributor);
			addResolvedUnit(group, qualifiedName, context, globalPackageRegistry(), attributedDescriptors);
		}
	}

	private void discoverActiveBundles(GlobalBlackboxDiscoveryResult result, Set<String> attributedDescriptors,
			IProgressMonitor monitor) {
		Bundle owner = FrameworkUtil.getBundle(GlobalBlackboxDiscoveryService.class);
		BundleContext bundleContext = owner != null ? owner.getBundleContext() : null;
		if (bundleContext == null) {
			return;
		}

		for (Bundle bundle : bundleContext.getBundles()) {
			checkCanceled(monitor);
			if (bundle.getState() != Bundle.ACTIVE || bundle.getSymbolicName() == null) {
				continue;
			}
			String bundleId = bundle.getSymbolicName();
			GlobalBlackboxGroup group = null;
			try {
				ResolutionContext context = bundleContext(bundleId);
				Collection<BlackboxUnitDescriptor> descriptors = BlackboxRegistry.INSTANCE
						.getCompilationUnitDescriptors(context);
				Set<String> bundleKeys = new HashSet<String>();
				for (BlackboxUnitDescriptor descriptor : descriptors) {
					checkCanceled(monitor);
					if (descriptor == null || !isOsgiDescriptorFor(descriptor, bundleId)
							|| !isDefinedByBundle(descriptor, bundle)) {
						continue;
					}
					String key = descriptorKey(descriptor);
					if (!bundleKeys.add(key)) {
						continue;
					}
					if (group == null) {
						group = new GlobalBlackboxGroup(result.getActivePlugins(), GlobalBlackboxGroupKind.BUNDLE,
								bundleId, bundleId);
						result.getActivePlugins().addChild(group);
					}
					group.addChild(descriptorLoader.load(group, descriptor, descriptor.getQualifiedName(),
							globalPackageRegistry()));
					attributedDescriptors.add(key);
				}
			} catch (RuntimeException e) {
				QVTBBoxUIPlugin.log(e);
				if (group == null) {
					group = new GlobalBlackboxGroup(result.getActivePlugins(), GlobalBlackboxGroupKind.BUNDLE,
							bundleId, bundleId);
					result.getActivePlugins().addChild(group);
				}
				group.addChild(new BlackboxDiagnosticInfo(group, Diagnostic.ERROR, safeMessage(e)));
			} catch (LinkageError e) {
				QVTBBoxUIPlugin.log(e);
				if (group == null) {
					group = new GlobalBlackboxGroup(result.getActivePlugins(), GlobalBlackboxGroupKind.BUNDLE,
							bundleId, bundleId);
					result.getActivePlugins().addChild(group);
				}
				group.addChild(new BlackboxDiagnosticInfo(group, Diagnostic.ERROR, safeMessage(e)));
			}
		}
	}

	private void discoverOtherRegistrations(GlobalBlackboxDiscoveryResult result, Set<String> attributedDescriptors,
			IProgressMonitor monitor) {
		try {
			ResolutionContext context = new ResolutionContextImpl(URI.createURI("/")); //$NON-NLS-1$
			Set<String> keys = new HashSet<String>();
			for (BlackboxUnitDescriptor descriptor : BlackboxRegistry.INSTANCE.getCompilationUnitDescriptors(context)) {
				checkCanceled(monitor);
				if (descriptor == null) {
					continue;
				}
				String key = descriptorKey(descriptor);
				if (!keys.add(key) || attributedDescriptors.contains(key)) {
					continue;
				}
				GlobalBlackboxGroup group = result.getRuntimeRegistrations();
				group.addChild(descriptorLoader.load(group, descriptor, descriptor.getQualifiedName(),
						globalPackageRegistry()));
			}
		} catch (RuntimeException e) {
			QVTBBoxUIPlugin.log(e);
			GlobalBlackboxGroup group = result.getRuntimeRegistrations();
			group.addChild(new BlackboxDiagnosticInfo(group, Diagnostic.ERROR, safeMessage(e)));
		} catch (LinkageError e) {
			QVTBBoxUIPlugin.log(e);
			GlobalBlackboxGroup group = result.getRuntimeRegistrations();
			group.addChild(new BlackboxDiagnosticInfo(group, Diagnostic.ERROR, safeMessage(e)));
		}
	}

	private void addResolvedUnit(GlobalBlackboxGroup group, String qualifiedName, ResolutionContext context,
			EPackage.Registry packageRegistry, Set<String> attributedDescriptors) {
		BlackboxUnitDescriptor descriptor = BlackboxRegistry.INSTANCE.getCompilationUnitDescriptor(qualifiedName, context);
		BlackboxUnitInfo unit = descriptorLoader.load(group, descriptor, qualifiedName, packageRegistry);
		group.addChild(unit);
		if (descriptor != null) {
			attributedDescriptors.add(descriptorKey(descriptor));
		}
	}

	private Collection<IType> findAnnotatedTypes(final IJavaProject javaProject, int includeMask,
			IProgressMonitor monitor) {
		final Map<String, IType> types = new LinkedHashMap<String, IType>();
		try {
			SearchPattern pattern = SearchPattern.createPattern(Module.class.getCanonicalName(),
					IJavaSearchConstants.ANNOTATION_TYPE, IJavaSearchConstants.ANNOTATION_TYPE_REFERENCE,
					SearchPattern.R_EXACT_MATCH);
			SearchParticipant[] participants = { SearchEngine.getDefaultSearchParticipant() };
			IJavaSearchScope scope = SearchEngine.createJavaSearchScope(new IJavaElement[] { javaProject }, includeMask);
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

	private static boolean isAccessibleJavaProject(IProject project) {
		try {
			return project != null && project.isAccessible() && project.hasNature(JavaCore.NATURE_ID);
		} catch (CoreException e) {
			return false;
		}
	}

	private static ResolutionContext bundleContext(String bundleId) {
		URI uri = URI.createPlatformPluginURI(bundleId + "/", true); //$NON-NLS-1$
		return new ResolutionContextImpl(uri);
	}

	private static EPackage.Registry globalPackageRegistry() {
		return new EPackageRegistryImpl(EPackage.Registry.INSTANCE);
	}

	private static String extensionQualifiedName(IConfigurationElement element) {
		if (UNIT_ELEMENT.equals(element.getName())) {
			String name = element.getAttribute(NAME_ATTRIBUTE);
			if (name == null) {
				return null;
			}
			String namespace = element.getAttribute(NAMESPACE_ATTRIBUTE);
			if (namespace == null) {
				namespace = element.getContributor().getName();
			}
			return namespace.length() == 0 ? name : namespace + "." + name; //$NON-NLS-1$
		}
		if (LIBRARY_ELEMENT.equals(element.getName())) {
			String className = element.getAttribute(CLASS_ATTRIBUTE);
			if (className == null) {
				return null;
			}
			String name = element.getAttribute(NAME_ATTRIBUTE);
			if (name == null) {
				return className;
			}
			int separator = className.lastIndexOf('.');
			return separator < 0 ? name : className.substring(0, separator + 1) + name;
		}
		return null;
	}

	private static String libraryLabel(IPackageFragmentRoot root, String fallback) {
		if (root != null && root.getElementName() != null && root.getElementName().length() > 0) {
			return root.getElementName();
		}
		return fallback;
	}

	private static boolean isOsgiDescriptorFor(BlackboxUnitDescriptor descriptor, String bundleId) {
		URI uri = descriptor.getURI();
		return uri != null && (OSGI_QUERY_PREFIX + bundleId).equals(uri.query());
	}

	private static boolean isDefinedByBundle(BlackboxUnitDescriptor descriptor, Bundle bundle) {
		try {
			Class<?> moduleClass = bundle.loadClass(descriptor.getQualifiedName());
			return bundle.equals(FrameworkUtil.getBundle(moduleClass));
		} catch (ClassNotFoundException e) {
			return false;
		} catch (RuntimeException e) {
			return false;
		} catch (LinkageError e) {
			return false;
		}
	}

	private static String descriptorKey(BlackboxUnitDescriptor descriptor) {
		return descriptor.getQualifiedName() + "|" + String.valueOf(descriptor.getURI()); //$NON-NLS-1$
	}

	private static void checkCanceled(IProgressMonitor monitor) {
		if (monitor != null && monitor.isCanceled()) {
			throw new OperationCanceledException();
		}
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
