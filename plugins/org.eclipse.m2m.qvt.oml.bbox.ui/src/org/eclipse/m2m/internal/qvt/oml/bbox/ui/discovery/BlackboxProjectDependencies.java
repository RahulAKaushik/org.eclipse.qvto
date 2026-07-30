package org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery;

import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.QVTBBoxUIPlugin;
import org.eclipse.m2m.internal.qvt.oml.project.QVTOProjectPlugin;

public final class BlackboxProjectDependencies {

	private BlackboxProjectDependencies() {
	}

	public static Set<IProject> includeDependentQVTProjects(Set<IProject> changedProjects) {
		Set<IProject> affectedProjects = new LinkedHashSet<IProject>();
		for (IProject candidate : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
			if (isQVTProject(candidate)
					&& (changedProjects.contains(candidate)
							|| dependsOnAny(candidate, changedProjects, new LinkedHashSet<IProject>()))) {
				affectedProjects.add(candidate);
			}
		}
		return affectedProjects;
	}

	private static boolean dependsOnAny(IProject project, Set<IProject> targets, Set<IProject> visited) {
		if (!visited.add(project)) {
			return false;
		}

		try {
			if (!project.hasNature(JavaCore.NATURE_ID)) {
				return false;
			}

			IJavaProject javaProject = JavaCore.create(project);
			for (IClasspathEntry entry : javaProject.getResolvedClasspath(true)) {
				if (entry.getEntryKind() != IClasspathEntry.CPE_PROJECT || entry.getPath().segmentCount() == 0) {
					continue;
				}
				IProject dependency = ResourcesPlugin.getWorkspace().getRoot().getProject(entry.getPath().segment(0));
				if (targets.contains(dependency) || dependsOnAny(dependency, targets, visited)) {
					return true;
				}
			}
		} catch (CoreException e) {
			QVTBBoxUIPlugin.log(e);
		}
		return false;
	}

	private static boolean isQVTProject(IProject project) {
		try {
			return project != null && project.isAccessible() && project.hasNature(QVTOProjectPlugin.NATURE_ID);
		} catch (CoreException e) {
			return false;
		}
	}
}
