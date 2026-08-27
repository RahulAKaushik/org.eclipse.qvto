package org.eclipse.m2m.internal.qvt.oml.bbox.ui.global;

/**
 * Tracks which replaceable global discovery request may publish its result.
 */
public final class GlobalBlackboxDiscoveryGeneration {

	private int current;

	public synchronized int start() {
		return ++current;
	}

	public synchronized boolean isCurrent(int generation) {
		return generation == current;
	}

	public synchronized void invalidate() {
		current++;
	}
}
