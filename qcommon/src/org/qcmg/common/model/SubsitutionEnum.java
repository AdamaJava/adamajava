package org.qcmg.common.model;


public enum SubsitutionEnum {
	
	// the ratio of transitions vs. transversions in SNPs.  
 
	AG(0), GA(1), CT(2), TC(3),			
	AC(4), CA(5),AT(6), TA(7),CG(8), GC(9), GT(10), TG(11), 
	NA(12),AN(13), NG(14), GN(15),NT(16),TN(17),NC(18),CN(19), Others(20); 
	
	final int order;
	SubsitutionEnum(int od){  this.order = od;  }		
	public boolean isTranstion(){ return (order <= 3); } 
	public boolean isTransversion(){ return (order >=4 && order <= 11); } 
	@Override
	public String toString(){ return this.name().substring(0,1)  + ">" + this.name().substring(1); }
	
	public static SubsitutionEnum getTrans(char ref, char alt){
		int ascRef =  Character.toUpperCase(ref) ;
		int ascii = ascRef * 2 + Character.toUpperCase(alt);	
		switch (ascii) {
			case 201 : return AG;
			case 207 : return GA;
			case 218 : return CT;
			case 235 : return TC;
			case 197 : return AC;
			case 199 : return CA;
			case 214 : return AT;
			case 233 : return TA;
			case 205 : return CG;
			case 209 : return GC;
			case 226 : return GT;
			case 239 : return TG;
			case 221 : return NA;
			case 208 : return AN;
			case 227 : return NG;
			case 220 : return GN;
			case 240 : return NT;
			case 246 : return TN;
			case 223 : return NC;
			case 212 : return CN;
		}			
		return Others;
	}		
	
}
