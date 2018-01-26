package jbse.tree;

import jbse.bc.ClassFile;
import jbse.val.Expression;
import jbse.val.Reference;
import jbse.val.ReferenceSymbolic;

/**
 * {@link DecisionAlternative_XALOAD} for the case a read access to an array
 * returned a {@link ReferenceSymbolic} to an object in the heap that has
 * not been yet discovered during execution.
 * 
 * @author Pietro Braione
 */
public final class DecisionAlternative_XALOAD_Expands extends DecisionAlternative_XALOAD_Unresolved implements DecisionAlternative_XYLOAD_GETX_Expands {
    private final ClassFile classFileOfTargetObject;
    private final int hashCode;

    /**
     * Constructor.
     * 
     * @param arrayAccessExpression the array access {@link Expression}.
     * @param referenceToResolve the {@link ReferenceSymbolic} loaded from the array.
     * @param fresh {@code true} iff {@code valToLoad} is fresh, i.e., 
     *        is not stored in the array and, therefore, must be written
     *        back to the array.
     * @param arrayReference when {@code fresh == true} is a {@link Reference} to the array 
     *        where {@code valueToLoad} originates from.
     * @param classFileOfTargetObject the {@link ClassFile} for the class of the
     *        object {@code referenceToResolve} expands to.
     * @param branchNumber an {@code int}, the branch number.
     */
    public DecisionAlternative_XALOAD_Expands(Expression arrayAccessExpression, ReferenceSymbolic referenceToResolve, boolean fresh, Reference arrayReference, ClassFile classFileOfTargetObject, int branchNumber) {
        super(ALT_CODE + "_Expands:" + arrayAccessExpression + ":" + classFileOfTargetObject.getClassName(), arrayAccessExpression, referenceToResolve, fresh, arrayReference, branchNumber);
        this.classFileOfTargetObject = classFileOfTargetObject;
        final int prime = 829;
        int result = super.hashCode();
        result = prime * result +
            ((this.classFileOfTargetObject == null) ? 0 : this.classFileOfTargetObject.hashCode());
        this.hashCode = result;
    }

    @Override
    public ClassFile getClassFileOfTargetObject() {
        return this.classFileOfTargetObject;
    }

    @Override
    public void accept(VisitorDecisionAlternative_XALOAD v) throws Exception {
        v.visitDecisionAlternative_XALOAD_Expands(this);
    }

    @Override
    public int hashCode() {
        return this.hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        final DecisionAlternative_XALOAD_Expands other = (DecisionAlternative_XALOAD_Expands) obj;
        if (this.classFileOfTargetObject == null) {
            if (other.classFileOfTargetObject != null) {
                return false;
            }
        } else if (this.classFileOfTargetObject != other.classFileOfTargetObject) {
            return false;
        }
        return true;
    }
}
