package org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery;

import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
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
import org.eclipse.m2m.internal.qvt.oml.bbox.ui.QVTBBoxUIPlugin;

/**
 * Finds annotated Java blackbox modules visible from one workspace project.
 */
public final class ProjectBlackboxJavaSearch {

	public Set<String> findVisibleModuleNames(final IProject project, final BlackboxVisibilityScope scope,
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
			int includeMask = scope.includesJavaDependencies()
					? IJavaSearchScope.SOURCES | IJavaSearchScope.REFERENCED_PROJECTS
							| IJavaSearchScope.APPLICATION_LIBRARIES
					: IJavaSearchScope.SOURCES;
			IJavaSearchScope searchScope = SearchEngine.createJavaSearchScope(new IJavaElement[] { javaProject },
					includeMask);
			SearchRequestor requestor = new SearchRequestor() {
				@Override
				public void acceptSearchMatch(SearchMatch match) {
					Object element = match.getElement();
					if (element instanceof IType) {
						IType type = (IType) element;
						if (scope.includesJavaDependencies()
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
}
