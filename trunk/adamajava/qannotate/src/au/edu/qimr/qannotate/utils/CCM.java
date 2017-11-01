package au.edu.qimr.qannotate.utils;

public enum CCM {
	
	ONE(1, ".","."),
	TWO(3, ".","."),
	THREE(3, ".","."),
	FOUR(4, ".","."),
	FIVE(5, ".","."),
	ELEVEN(11, "Reference","HomozygousLoss"),
	TWELVE(12, "Reference","Reference"),
	THIRTEEN(13, "Reference","Somatic"),
	FOURTEEN(14, "Reference","SomaticNoReference"),
	FIFTEEN(15, "Reference","DoubleSomatic"),
	TWENTY_ONE(21, "Germline","HomozygousLoss"),
	TWENTY_TWO(22, "Germline","ReferenceNoVariant"),
	TWENTY_THREE(23, "Germline","Germline"),
	TWENTY_FOUR(24, "Germline","GermlineNoReference"),
	TWENTY_FIVE(25, "Germline","Somatic"),
	TWENTY_SIX(26, "Germline","Somatic"),
	TWENTY_SEVEN(27, "Germline","DoubleSomatic"),
	TWENTY_EIGHT(28, "Germline","DoubleSomatic"),
	THIRTY_ONE(31, "Germline","HomozygousLoss"),
	THIRTY_TWO(32, "Germline","ReferenceNoVariant"),
	THIRTY_THREE(33, "Germline","GermlineReversionToReference"),
	THIRTY_FOUR(34, "Germline","Germline"),
	THIRTY_FIVE(35, "Germline","Somatic"),
	THIRTY_SIX(36, "Germline","DoubleSomatic"),
	THIRTY_SEVEN(37, "Germline","DoubleSomatic"),
	THIRTY_EIGHT(38, "Germline","DoubleSomatic"),
	FORTY_ONE(41, "DoubleGermline","HomozygousLoss"),
	FORTY_TWO(42, "DoubleGermline","ReferenceNoVariant"),
	FORTY_THREE(43, "DoubleGermline","GermlineReversionToReference"),
	FORTY_FOUR(44, "DoubleGermline","GermlineReversionToReference"),
	FORTY_FIVE(45, "DoubleGermline","SomaticLostVariant"),
	FORTY_SIX(46, "DoubleGermline","SomaticLostVariant"),
	FORTY_SEVEN(47, "DoubleGermline","Germline"),
	FORTY_EIGHT(48, "DoubleGermline","DoubleSomatic"),
	FORTY_NINE(49, "DoubleGermline","DoubleSomatic"),
	FIFTY(50, "DoubleGermline","DoubleSomatic"),
	FIFTY_ONE(51, "DoubleGermline","Somatic"),
	NINETY_NINE(99, "???","???"),
	
;
	private final int id;
	private final String control;
	private final String test;
	private CCM(int id, String control, String test) {
		this.id = id;
		this.control = control;
		this.test = test;
	}
	
	
	public static int getId(CCM e) {
		return e.id;
	}
	public static String getControl(CCM e) {
		return e.control;
	}
	public static String getTest(CCM e) {
		return e.test;
	}
	public static CCM getCCM(int id) {
		for (CCM ccm : CCM.values()) {
			if (ccm.id == id) return ccm;
		}
		return null;
	}
}
