package jbse.base;

import static jbse.common.Type.binaryClassName;
import static jbse.bc.Signatures.JAVA_MAP_CONTAINSKEY;
import static jbse.bc.Signatures.JAVA_MAP_GET;

import jbse.common.exc.UnexpectedInternalException;
import jbse.mem.Clause;
import jbse.mem.ClauseAssume;
import jbse.mem.ClauseAssumeAliases;
import jbse.mem.ClauseAssumeNull;
import jbse.mem.ClauseAssumeReferenceSymbolic;
import jbse.val.Expression;
import jbse.val.Operator;
import jbse.val.Primitive;
import jbse.val.PrimitiveSymbolicMemberField;
import jbse.val.ReferenceSymbolic;
import jbse.val.SymbolicApply;
import jbse.val.SymbolicMemberField;
import jbse.val.Value;

public final class JAVA_MAP_Utils {
	public final static String INITIAL_MAP_FIELD_NAME = "initialMap";
	public final static String INITIAL_MAP_FIELD_FULL = ".java/util/HashMap:" + INITIAL_MAP_FIELD_NAME;
	public final static String GET_SIGIL = "::GET(";

	public static boolean isInitialMapField(Value value) {
		if (!(value instanceof SymbolicMemberField)) {
			return false;
		}
		final SymbolicMemberField originMemberField = (SymbolicMemberField) value;
		if (binaryClassName(originMemberField.getFieldClass()).equals("java.util.HashMap") 
		&& originMemberField.getFieldName().equals(INITIAL_MAP_FIELD_NAME)) {
			return true;
		} else {
			return false;
		}
	}


	public static boolean isSymbolicApplyOnInitialMap(Value value) {
		if (!(value instanceof SymbolicApply)) {
			return false;
		}
		final SymbolicApply symbolicApply = (SymbolicApply) value;
		final Value[] args = symbolicApply.getArgs();
		if (args.length > 0 && JAVA_MAP_Utils.isInitialMapField(args[0])) {
			if (!symbolicApply.getOperator().equals(JAVA_MAP_CONTAINSKEY.toString())) {
				throw new UnexpectedInternalException("Path condition refers to unexpected symbolicApply on a symbolic map: " + symbolicApply.getOperator());
			}
			return true;
		} else {
			return false;
		}				
	}

	public static boolean assumptionViolated(Clause clause) {
		if (clause instanceof ClauseAssumeReferenceSymbolic) {
			final ReferenceSymbolic ref = ((ClauseAssumeReferenceSymbolic) clause).getReference(); 
			if (isInitialMapField(ref) && (clause instanceof ClauseAssumeAliases || clause instanceof ClauseAssumeNull)) {
				return true;
			} 
		} else if (clause instanceof ClauseAssume) {
			final Expression cond = (Expression) ((ClauseAssume) clause).getCondition(); 
			final Primitive firstOp = cond.getFirstOperand();
			if (cond.getOperator().equals(Operator.LT) && firstOp instanceof PrimitiveSymbolicMemberField) {
				final PrimitiveSymbolicMemberField field = (PrimitiveSymbolicMemberField) firstOp;
				if (isInitialMapField(field.getContainer()) && "size".equals(field.getFieldName())) {
					return true;
				}
			}
		}
		return false;
	}

	public static String possiblyAdaptMapModelSymbols(String origin) {
		final String originNoInitialMap;
		if (origin.contains(INITIAL_MAP_FIELD_FULL)) {
			originNoInitialMap = origin.replace(INITIAL_MAP_FIELD_FULL, "");
		} else {
			originNoInitialMap = origin;
		}
		final String retVal;
		if (originNoInitialMap.contains(GET_SIGIL)) {
			final String mapRef = originNoInitialMap.substring(0, originNoInitialMap.indexOf(GET_SIGIL));
			String keyRef = originNoInitialMap.substring(originNoInitialMap.indexOf(GET_SIGIL) + GET_SIGIL.length());
			keyRef = keyRef.substring(0, keyRef.indexOf(')'));
			retVal = "<" + JAVA_MAP_GET.toString() + "@" + mapRef + "," + keyRef + ">";
		} else {
			retVal = originNoInitialMap;
		}
		return retVal;
	}
	
    /**
     * Do not instantiate it! 
     */
	private JAVA_MAP_Utils() {
        //intentionally empty
	}
}
