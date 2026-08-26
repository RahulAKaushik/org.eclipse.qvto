package org.eclipse.m2m.internal.tests.qvt.oml.ui.blackbox.integration;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.m2m.internal.qvt.oml.QvtPlugin;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxVisibilityScope;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.ProjectBlackboxJavaSearch;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;

public class ProjectBlackboxJavaSearchTest {

	private static final String LOCAL = "visibility.local.LocalLibrary"; //$NON-NLS-1$
	private static final String DIRECT = "visibility.direct.DirectLibrary"; //$NON-NLS-1$
	private static final String TRANSITIVE = "visibility.transitive.TransitiveLibrary"; //$NON-NLS-1$
	private static final String UNRELATED = "visibility.unrelated.UnrelatedLibrary"; //$NON-NLS-1$
	private static int projectSequence;

	private final NullProgressMonitor monitor = new NullProgressMonitor();
	private final List<IProject> projects = new ArrayList<IProject>();
	private final ProjectBlackboxJavaSearch search = new ProjectBlackboxJavaSearch();
	private IJavaProject app;
	private IJavaProject direct;
	private IJavaProject transitive;

	@Before
	public void setUp() throws Exception {
		String prefix = "BlackboxVisibility" + (++projectSequence); //$NON-NLS-1$
		app = createJavaProject(prefix + "App", LOCAL); //$NON-NLS-1$
		direct = createJavaProject(prefix + "Direct", DIRECT); //$NON-NLS-1$
		transitive = createJavaProject(prefix + "Transitive", TRANSITIVE); //$NON-NLS-1$
		createJavaProject(prefix + "Unrelated", UNRELATED); //$NON-NLS-1$
	}

	@After
	public void tearDown() throws Exception {
		for (int i = projects.size() - 1; i >= 0; i--) {
			IProject project = projects.get(i);
			if (project.exists()) {
				project.delete(true, true, monitor);
			}
		}
	}

	@Test
	public void exportedTransitiveDependencyIsVisibleAndUnrelatedProjectIsExcluded() throws Exception {
		addProjectDependency(direct, transitive, true);
		addProjectDependency(app, direct, false);
		buildWorkspace();

		assertEquals(names(LOCAL), search.findVisibleModuleNames(app.getProject(),
				BlackboxVisibilityScope.PROJECT_ONLY, monitor));
		Set<String> expectedDependencies = names(LOCAL, DIRECT, TRANSITIVE);
		assertEquals(expectedDependencies, search.findVisibleModuleNames(app.getProject(),
				BlackboxVisibilityScope.PROJECT_DEPENDENCIES, monitor));
		assertEquals(expectedDependencies, search.findVisibleModuleNames(app.getProject(),
				BlackboxVisibilityScope.PROJECT_VISIBLE, monitor));
	}

	@Test
	public void nonExportedTransitiveDependencyIsNotVisible() throws Exception {
		addProjectDependency(direct, transitive, false);
		addProjectDependency(app, direct, false);
		buildWorkspace();

		assertEquals(names(LOCAL, DIRECT), search.findVisibleModuleNames(app.getProject(),
				BlackboxVisibilityScope.PROJECT_DEPENDENCIES, monitor));
	}

	private IJavaProject createJavaProject(String projectName, String qualifiedClassName) throws Exception {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IProject project = workspace.getRoot().getProject(projectName);
		if (project.exists()) {
			project.delete(true, true, monitor);
		}
		IProjectDescription description = workspace.newProjectDescription(projectName);
		description.setNatureIds(new String[] { JavaCore.NATURE_ID });
		project.create(description, monitor);
		project.open(monitor);
		projects.add(project);

		IFolder sourceFolder = project.getFolder("src"); //$NON-NLS-1$
		sourceFolder.create(true, true, monitor);
		IFolder outputFolder = project.getFolder("bin"); //$NON-NLS-1$
		outputFolder.create(true, true, monitor);
		IJavaProject javaProject = JavaCore.create(project);
		javaProject.setOutputLocation(outputFolder.getFullPath(), monitor);
		javaProject.setRawClasspath(baseClasspath(sourceFolder), monitor);
		createModuleSource(sourceFolder, qualifiedClassName);
		return javaProject;
	}

	private IClasspathEntry[] baseClasspath(IFolder sourceFolder) throws Exception {
		Bundle qvtoBundle = Platform.getBundle(QvtPlugin.ID);
		if (qvtoBundle == null) {
			throw new IllegalStateException("QVTo core bundle is not available"); //$NON-NLS-1$
		}
		URL bundleRoot = FileLocator.toFileURL(qvtoBundle.getEntry("/")); //$NON-NLS-1$
		IPath qvtoPath = new Path(new File(bundleRoot.toURI()).getAbsolutePath());
		return new IClasspathEntry[] {
			JavaCore.newSourceEntry(sourceFolder.getFullPath()),
			JavaRuntime.getDefaultJREContainerEntry(),
			JavaCore.newLibraryEntry(qvtoPath, null, null)
		};
	}

	private void addProjectDependency(IJavaProject project, IJavaProject dependency, boolean exported)
			throws Exception {
		List<IClasspathEntry> entries = new ArrayList<IClasspathEntry>(Arrays.asList(project.getRawClasspath()));
		entries.add(JavaCore.newProjectEntry(dependency.getPath(), null, false, new IClasspathAttribute[0], exported));
		project.setRawClasspath(entries.toArray(new IClasspathEntry[entries.size()]), monitor);
	}

	private void createModuleSource(IFolder sourceFolder, String qualifiedClassName) throws Exception {
		int separator = qualifiedClassName.lastIndexOf('.');
		String packageName = qualifiedClassName.substring(0, separator);
		String className = qualifiedClassName.substring(separator + 1);
		IFolder packageFolder = sourceFolder.getFolder(new Path(packageName.replace('.', '/')));
		createFolder(packageFolder);
		String contents = "package " + packageName + ";\n\n" //$NON-NLS-1$ //$NON-NLS-2$
				+ "import org.eclipse.m2m.qvt.oml.blackbox.java.Module;\n\n" //$NON-NLS-1$
				+ "@Module\n" //$NON-NLS-1$
				+ "public class " + className + " {\n" //$NON-NLS-1$ //$NON-NLS-2$
				+ "\tpublic String value(String input) {\n" //$NON-NLS-1$
				+ "\t\treturn input;\n" //$NON-NLS-1$
				+ "\t}\n" //$NON-NLS-1$
				+ "}\n"; //$NON-NLS-1$
		IFile source = packageFolder.getFile(className + ".java"); //$NON-NLS-1$
		source.create(new ByteArrayInputStream(contents.getBytes(StandardCharsets.UTF_8)), true, monitor);
	}

	private void createFolder(IFolder folder) throws Exception {
		IContainer parent = folder.getParent();
		if (parent instanceof IFolder && !parent.exists()) {
			createFolder((IFolder) parent);
		}
		if (!folder.exists()) {
			folder.create(true, true, monitor);
		}
	}

	private void buildWorkspace() throws Exception {
		ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, monitor);
		JavaCore.rebuildIndex(monitor);
		for (IProject project : projects) {
			for (IMarker marker : project.findMarkers(IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER, true,
					IResource.DEPTH_INFINITE)) {
				if (marker.getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO) == IMarker.SEVERITY_ERROR) {
					throw new AssertionError(marker.getAttribute(IMarker.MESSAGE, "Java build error")); //$NON-NLS-1$
				}
			}
		}
	}

	private static Set<String> names(String... names) {
		return new LinkedHashSet<String>(Arrays.asList(names));
	}
}
