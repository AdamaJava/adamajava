package org.qcmg.qsv.tiledaligner;

public enum TAClassifier {
	
	PM_SE(1),
	PM_LTOHE(2),
	PM_MTOHE(3),
	
	FIRST_Q_SE(4),
	FIRST_Q_LTOHE(5),
	FIRST_Q_MTOHE(6),
	
	SECOND_Q_SE(7),
	SECOND_Q_LTOHE(8),
	SECOND_Q_MTOHE(9),
	
	THIRD_Q_SE(10),
	THIRD_Q_LTOHE(11),
	THIRD_Q_MTOHE(12),
	
	FOURTH_Q_SE(13),
	FOURTH_Q_LTOHE(14),
	FOURTH_Q_MTOHE(15),
	
	UNKNOWN(33);
	
	
	
	private final int position;
	private TAClassifier(int position) {this.position = position;}
	
	public int getPosition() {
		return this.position;
	}
	
	public static int getPosition(TAClassifier tac) {
		return tac.position;
	}
	
	public static TAClassifier getTAClassifier(int pos) {
		for (TAClassifier tac : TAClassifier.values()) {
			if (tac.position == pos) {
				return tac;
			}
		}
		return null;
	}
	
	public static int getMaxPosition() {
		int maxPos = 0;
		for (TAClassifier tac : TAClassifier.values()) {
			if (tac.position > maxPos) {
				maxPos = tac.position;
			}
		}
		return maxPos;
	}

}
