package org.qcmg.common.model;


public enum SubsitutionEnum {
	
	// the ratio of transitions vs. transversions in SNPs.  
 
	AG(0), GA(1), CT(2), TC(3),			
	AC(4), CA(5),AT(6), TA(7),CG(8), GC(9), GT(10), TG(11), 
	NA(12),AN(13), NG(14), GN(15),NT(16),TN(17),NC(18),CN(19), Others(20); 
	
	final int order;
	SubsitutionEnum(int od) {
		this.order = od;
	}
	public boolean isTransition() {
		return (order <= 3);
	}
	public boolean isTransversion() {
		return (order >= 4 && order <= 11);
	}
	@Override
	public String toString() {
		return this.name().charAt(0)  + ">" + this.name().substring(1);
	}
	
	public static SubsitutionEnum getTrans(char ref, char alt){
		int ascRef =  Character.toUpperCase(ref) ;
		int ascii = ascRef * 2 + Character.toUpperCase(alt);
        return switch (ascii) {
            case 201 -> AG;
            case 207 -> GA;
            case 218 -> CT;
            case 235 -> TC;
            case 197 -> AC;
            case 199 -> CA;
            case 214 -> AT;
            case 233 -> TA;
            case 205 -> CG;
            case 209 -> GC;
            case 226 -> GT;
            case 239 -> TG;
            case 221 -> NA;
            case 208 -> AN;
            case 227 -> NG;
            case 220 -> GN;
            case 240 -> NT;
            case 246 -> TN;
            case 223 -> NC;
            case 212 -> CN;
            default -> Others;
        };
    }
	
}
