package ca.mcgill.mcb.pcingola.snpSift.lang.expression;

import ca.mcgill.mcb.pcingola.vcf.VcfEntry;
import ca.mcgill.mcb.pcingola.vcf.VcfInfoType;

/**
 * A generic expresion
 * Expressions have values (VcfInfoType)
 * 
 * @author pcingola
 */
public abstract class Expression {

	protected static boolean debug = false;

	protected VcfInfoType returnType = VcfInfoType.UNKNOWN; // Default type is INTEGER

	@SuppressWarnings("rawtypes")
	public boolean canCompareTo(Expression expr, VcfEntry vcfEntry) {
		Comparable o1 = get(vcfEntry);
		Comparable o2 = expr.get(vcfEntry);
		return (o1 != null) && (o2 != null);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public int compareTo(Expression expr, VcfEntry vcfEntry) {
		VcfInfoType exprType = expr.getReturnType();

		// Same data type? Just compare them
		Comparable o1 = get(vcfEntry);
		Comparable o2 = expr.get(vcfEntry);
		if ((o1 == null) && (o2 == null)) return 0;
		if ((o1 == null) && (o2 != null)) return -1;
		if ((o1 != null) && (o2 == null)) return 1;
		if (returnType == exprType) return o1.compareTo(o2);

		// One Integer one Float?
		if (((returnType == VcfInfoType.Integer) || (returnType == VcfInfoType.Float)) //
				&& ((exprType == VcfInfoType.Integer) || (exprType == VcfInfoType.Float)) //
		) {
			// Convert to Float and compare
			double d1 = getFloat(vcfEntry);
			double d2 = expr.getFloat(vcfEntry);

			if (d1 == d2) return 0;
			if (d1 < d2) return -1;
			return 1;
		} else if (((returnType == VcfInfoType.String) || (returnType == VcfInfoType.Character)) //
				&& ((exprType == VcfInfoType.Character) || (exprType == VcfInfoType.String)) //
		) {
			String s1 = getString(vcfEntry);
			String s2 = expr.getString(vcfEntry);

			return s1.compareTo(s2);
		}

		// Not comparable types
		throw new RuntimeException("Cannot compare '" + returnType + "' to '" + exprType + "'");
	}

	/**
	 * Get expresion value
	 * @param vcfEntry
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public abstract Comparable get(VcfEntry vcfEntry);

	/**
	 * Get expression value as a 'Float'
	 * @param vcfEntry
	 * @return
	 */
	public double getFloat(VcfEntry vcfEntry) {
		Object o = get(vcfEntry);

		switch (returnType) {
		case Float:
			return (Double) o;
		case Integer:
			return (((Long) o));
		default:
			throw new RuntimeException("Cannot cast '" + returnType + "' to FLOAT");
		}
	}

	public VcfInfoType getReturnType() {
		return returnType;
	}

	/**
	 * Get expression value as a String
	 * @param vcfEntry
	 * @return
	 */
	public String getString(VcfEntry vcfEntry) {
		return get(vcfEntry).toString();
	}

}
