package jbse.algo.meta;

import static jbse.algo.Util.failExecution;
import static jbse.bc.Signatures.JAVA_HASHMAP;
import static jbse.common.Type.binaryClassName;

import java.util.function.Supplier;

import jbse.algo.Algo_INVOKEMETA_Nonbranching;
import jbse.algo.InterruptException;
import jbse.algo.StrategyUpdate;
import jbse.apps.run.DecisionProcedureGuidanceJDI;
import jbse.bc.Signature;
import jbse.common.exc.ClasspathException;
import jbse.mem.Frame;
import jbse.mem.State;
import jbse.mem.exc.FrozenStateException;
import jbse.mem.exc.InvalidSlotException;
import jbse.mem.exc.ThreadStackEmptyException;
import jbse.tree.DecisionAlternative_NONE;
import jbse.val.SymbolicMemberField;
import jbse.val.Value;

/**
 * Meta-level implementation of {@link jbse.base.JAVA_MAP#notifyMethodExecution()}.
 * 
 * @author Giovanni Denaro
 */
public final class Algo_JBSE_JAVA_MAP_NOTIFYMETHODEXECUTION extends Algo_INVOKEMETA_Nonbranching {
    @Override
    protected Supplier<Integer> numOperands() {
        return () -> 1;
    }

    @Override
    protected void cookMore(State state) 
    throws InterruptException, ClasspathException, FrozenStateException {
        if (this.ctx.stateInitial == null) {
            return;
        }
        try {
            final Signature sig = state.getCurrentMethodSignature();
            final Value v0 = state.getCurrentFrame().getLocalVariableValue(0);
            if (v0 instanceof SymbolicMemberField) {
                final SymbolicMemberField originMemberField = (SymbolicMemberField) v0;
                if (binaryClassName(originMemberField.getFieldClass()).equals(JAVA_HASHMAP) && originMemberField.getFieldName().equals("initialMap")) {
                    return; //do not notify operations on helper maps (initialMap) scoped within symbolic hash maps. The helper maps do not exist in the concrete execution.
                }
            }
            if (this.ctx.decisionProcedure instanceof DecisionProcedureGuidanceJDI) {
                final DecisionProcedureGuidanceJDI dpJDI = (DecisionProcedureGuidanceJDI) this.ctx.decisionProcedure;
                final Signature[] callCtx = new Signature[state.getStackSize()];
                int i = 0;
                for (Frame f: state.getStack()) {
                    callCtx[i++] = f.getMethodSignature();
                }
                dpJDI.notifyExecutionOfHashMapModelMethod(sig, callCtx);
            }
        } catch (ThreadStackEmptyException | InvalidSlotException | ClassCastException e) {
            //this should never happen
            failExecution(e);
        }                	
    }

    @Override
    protected StrategyUpdate<DecisionAlternative_NONE> updater() {
        return (state, alt) -> { };
    }
}
