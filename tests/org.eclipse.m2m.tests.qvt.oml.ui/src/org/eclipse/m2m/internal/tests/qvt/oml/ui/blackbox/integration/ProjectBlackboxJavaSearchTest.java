package org.eclipse.m2m.internal.tests.qvt.oml.ui.blackbox.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

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
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaModelStatus;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstallType;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.VMStandin;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.BlackboxVisibilityScope;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery.ProjectBlackboxJavaSearch;
import org.eclipse.m2m.qvt.oml.blackbox.java.Module;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class ProjectBlackboxJavaSearchTest {

	private static final String LOCAL = "visibility.local.LocalLibrary"; //$NON-NLS-1$
	private static final String DIRECT = "visibility.direct.DirectLibrary"; //$NON-NLS-1$
	private static final String TRANSITIVE = "visibility.transitive.TransitiveLibrary"; //$NON-NLS-1$
	private static final String UNRELATED = "visibility.unrelated.UnrelatedLibrary"; //$NON-NLS-1$
	private static final String EXTERNAL = "visibility.external.ExternalLibrary"; //$NON-NLS-1$
	private static final String TEST_VM_ID = "org.eclipse.qvto.blackbox.tests.vm"; //$NON-NLS-1$
	private static IVMInstall previousDefaultVM;
	private static IVMInstall testVM;
	private static int projectSequence;

	private final NullProgressMonitor monitor = new NullProgressMonitor();
	private final List<IProject> projects = new ArrayList<IProject>();
	private final ProjectBlackboxJavaSearch search = new ProjectBlackboxJavaSearch();
	private IJavaProject app;
	private IJavaProject direct;
	private IJavaProject transitive;
	private String projectPrefix;

	@BeforeClass
	public static void setUpTestVM() throws Exception {
		previousDefaultVM = JavaRuntime.getDefaultVMInstall();
		if (isValidVM(previousDefaultVM)) {
			return;
		}

		String javaHome = System.getProperty("java.home"); //$NON-NLS-1$
		File installLocation = javaHome == null ? null : new File(javaHome);
		for (IVMInstallType vmType : JavaRuntime.getVMInstallTypes()) {
			if (installLocation == null || !vmType.validateInstallLocation(installLocation).isOK()) {
				continue;
			}
			VMStandin standin = new VMStandin(vmType, TEST_VM_ID);
			standin.setName("QVTo blackbox test VM"); //$NON-NLS-1$
			standin.setInstallLocation(installLocation);
			testVM = standin.convertToRealVM();
			JavaRuntime.setDefaultVMInstall(testVM, new NullProgressMonitor(), false);
			return;
		}
		throw new IllegalStateException("Unable to use the PDE test application's java.home as a JDT VM: " //$NON-NLS-1$
				+ javaHome);
	}

	private static boolean isValidVM(IVMInstall vm) {
		return vm != null && vm.getInstallLocation() != null
				&& vm.getVMInstallType().validateInstallLocation(vm.getInstallLocation()).isOK();
	}

	@AfterClass
	public static void restoreTestVM() throws Exception {
		if (testVM == null) {
			return;
		}
		JavaRuntime.setDefaultVMInstall(previousDefaultVM, new NullProgressMonitor(), false);
		testVM.getVMInstallType().disposeVMInstall(testVM.getId());
		testVM = null;
	}

	@Before
	public void setUp() throws Exception {
		projectPrefix = "BlackboxVisibility" + (++projectSequence); //$NON-NLS-1$
		app = createJavaProject(projectPrefix + "App", LOCAL); //$NON-NLS-1$
		direct = createJavaProject(projectPrefix + "Direct", DIRECT); //$NON-NLS-1$
		transitive = createJavaProject(projectPrefix + "Transitive", TRANSITIVE); //$NON-NLS-1$
		createJavaProject(projectPrefix + "Unrelated", UNRELATED); //$NON-NLS-1$
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

	@Test
	public void exportedJarDependencyIsVisibleWithoutSourceProject() throws Exception {
		addProjectDependency(app, direct, false);
		createExternalLibraryJar(true);
		buildWorkspace();

		assertEquals(names(LOCAL, DIRECT, EXTERNAL), search.findVisibleModuleNames(app.getProject(),
				BlackboxVisibilityScope.PROJECT_DEPENDENCIES, monitor));
	}

	@Test
	public void nonExportedJarDependencyIsNotVisible() throws Exception {
		addProjectDependency(app, direct, false);
		createExternalLibraryJar(false);
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
		project.create(description, monitor);
		project.open(monitor);
		projects.add(project);
		description = project.getDescription();
		description.setNatureIds(new String[] { JavaCore.NATURE_ID });
		project.setDescription(description, monitor);

		IFolder sourceFolder = project.getFolder("src"); //$NON-NLS-1$
		sourceFolder.create(true, true, monitor);
		IFolder outputFolder = project.getFolder("bin"); //$NON-NLS-1$
		outputFolder.create(true, true, monitor);
		IJavaProject javaProject = JavaCore.create(project);
		javaProject.setOutputLocation(outputFolder.getFullPath(), monitor);
		IClasspathEntry[] classpath = baseClasspath(sourceFolder);
		IJavaModelStatus classpathStatus = JavaConventions.validateClasspath(javaProject, classpath,
				outputFolder.getFullPath());
		assertTrue(classpathStatus.getMessage(), classpathStatus.isOK());
		javaProject.setRawClasspath(classpath, monitor);
		createModuleSource(sourceFolder, qualifiedClassName);
		return javaProject;
	}

	private IClasspathEntry[] baseClasspath(IFolder sourceFolder) throws Exception {
		URL moduleResource = Module.class.getResource("Module.class"); //$NON-NLS-1$
		if (moduleResource == null) {
			throw new IllegalStateException("QVTo Module annotation is not available"); //$NON-NLS-1$
		}
		URL moduleFile = FileLocator.toFileURL(moduleResource);
		IPath modulePath = new Path(new File(moduleFile.toURI()).getAbsolutePath());
		IPath moduleTypePath = new Path(Module.class.getName().replace('.', '/'));
		IPath qvtoClasses = modulePath.removeLastSegments(moduleTypePath.segmentCount());
		return new IClasspathEntry[] {
			JavaCore.newSourceEntry(sourceFolder.getFullPath()),
			JavaRuntime.getDefaultJREContainerEntry(),
			JavaCore.newLibraryEntry(qvtoClasses, null, null)
		};
	}

	private void addProjectDependency(IJavaProject project, IJavaProject dependency, boolean exported)
			throws Exception {
		List<IClasspathEntry> entries = new ArrayList<IClasspathEntry>(Arrays.asList(project.getRawClasspath()));
		entries.add(JavaCore.newProjectEntry(dependency.getPath(), null, false, new IClasspathAttribute[0], exported));
		project.setRawClasspath(entries.toArray(new IClasspathEntry[entries.size()]), monitor);
	}

	private void addLibraryDependency(IJavaProject project, IPath libraryPath, boolean exported) throws Exception {
		List<IClasspathEntry> entries = new ArrayList<IClasspathEntry>(Arrays.asList(project.getRawClasspath()));
		entries.add(JavaCore.newLibraryEntry(libraryPath, null, null, null, new IClasspathAttribute[0], exported));
		project.setRawClasspath(entries.toArray(new IClasspathEntry[entries.size()]), monitor);
	}

	private void createExternalLibraryJar(boolean exported) throws Exception {
		IJavaProject sourceProject = createJavaProject(projectPrefix + "ExternalSource", EXTERNAL); //$NON-NLS-1$
		buildWorkspace();

		IPath classPath = new Path("bin").append( //$NON-NLS-1$
				new Path(EXTERNAL.replace('.', '/')).addFileExtension("class")); //$NON-NLS-1$
		IFile compiledClass = sourceProject.getProject().getFile(classPath);
		assertTrue("Expected the external blackbox class to be compiled", compiledClass.exists()); //$NON-NLS-1$

		IFolder libraryFolder = direct.getProject().getFolder("lib"); //$NON-NLS-1$
		libraryFolder.create(true, true, monitor);
		IFile library = libraryFolder.getFile("external-blackboxes.jar"); //$NON-NLS-1$
		ByteArrayOutputStream contents = new ByteArrayOutputStream();
		try (InputStream input = compiledClass.getContents();
				JarOutputStream output = new JarOutputStream(contents)) {
			output.putNextEntry(new JarEntry(EXTERNAL.replace('.', '/') + ".class")); //$NON-NLS-1$
			byte[] buffer = new byte[4096];
			int length;
			while ((length = input.read(buffer)) != -1) {
				output.write(buffer, 0, length);
			}
			output.closeEntry();
		}
		library.create(new ByteArrayInputStream(contents.toByteArray()), true, monitor);
		sourceProject.getProject().delete(true, true, monitor);
		addLibraryDependency(direct, library.getFullPath(), exported);
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
		List<String> errors = new ArrayList<String>();
		for (IProject project : projects) {
			if (!project.exists()) {
				continue;
			}
			for (IMarker marker : project.findMarkers(IMarker.PROBLEM, true,
					IResource.DEPTH_INFINITE)) {
				if (marker.getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO) == IMarker.SEVERITY_ERROR) {
					errors.add(project.getName() + "/" + marker.getResource().getProjectRelativePath() //$NON-NLS-1$
							+ ": " + marker.getAttribute(IMarker.MESSAGE, "Java build error")); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
		}
		assertTrue("Java build errors: " + errors, errors.isEmpty()); //$NON-NLS-1$
	}

	private static Set<String> names(String... names) {
		return new LinkedHashSet<String>(Arrays.asList(names));
	}
}
