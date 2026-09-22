package org.eclipse.m2m.internal.qvt.oml.bbox.ui.discovery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.emf.common.util.URI;

public class BlackboxUnitInfo {

	private final Object parent;
	private final String qualifiedName;
	private final URI uri;
	private boolean loaded;
	private final List<BlackboxModuleInfo> modules = new ArrayList<BlackboxModuleInfo>();
	private final List<BlackboxDiagnosticInfo> diagnostics = new ArrayList<BlackboxDiagnosticInfo>();

	public BlackboxUnitInfo(Object parent, String qualifiedName, URI uri) {
		this.parent = parent;
		this.qualifiedName = qualifiedName;
		this.uri = uri;
	}

	public Object getParent() {
		return parent;
	}

	public String getQualifiedName() {
		return qualifiedName;
	}

	public URI getURI() {
		return uri;
	}

	public boolean isLoaded() {
		return loaded;
	}

	public void setLoaded(boolean loaded) {
		this.loaded = loaded;
	}

	public List<BlackboxModuleInfo> getModules() {
		return Collections.unmodifiableList(modules);
	}

	public void addModule(BlackboxModuleInfo module) {
		modules.add(module);
	}

	public List<BlackboxDiagnosticInfo> getDiagnostics() {
		return Collections.unmodifiableList(diagnostics);
	}

	public void addDiagnostic(BlackboxDiagnosticInfo diagnostic) {
		diagnostics.add(diagnostic);
	}

	public boolean hasErrors() {
		for (BlackboxDiagnosticInfo diagnostic : diagnostics) {
			if (diagnostic.hasErrors()) {
				return true;
			}
		}
		return false;
	}
}
