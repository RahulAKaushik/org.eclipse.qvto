package org.eclipse.m2m.internal.qvt.oml.bbox.ui.global;

public final class GlobalBlackboxOriginIdentity {

	private final String originKey;
	private final String qualifiedName;

	public GlobalBlackboxOriginIdentity(String originKey, String qualifiedName) {
		this.originKey = originKey;
		this.qualifiedName = qualifiedName;
	}

	@Override
	public int hashCode() {
		int result = originKey != null ? originKey.hashCode() : 0;
		return 31 * result + (qualifiedName != null ? qualifiedName.hashCode() : 0);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof GlobalBlackboxOriginIdentity == false) {
			return false;
		}
		GlobalBlackboxOriginIdentity other = (GlobalBlackboxOriginIdentity) obj;
		return equals(originKey, other.originKey) && equals(qualifiedName, other.qualifiedName);
	}

	private static boolean equals(Object left, Object right) {
		return left == null ? right == null : left.equals(right);
	}
}
