package jbse.meta.algo;

import static jbse.algo.Util.throwVerifyError;
import static jbse.bc.Offsets.INVOKESTATIC_OFFSET;

import jbse.algo.Algorithm;
import jbse.algo.ExecutionContext;
import jbse.mem.State;
import jbse.mem.exc.ContradictionException;
import jbse.mem.exc.InvalidProgramCounterException;
import jbse.mem.exc.ThreadStackEmptyException;

public class SEInvokeIgnore implements Algorithm {
	@Override
	public void exec(State state, ExecutionContext ctx) 
	throws ContradictionException, ThreadStackEmptyException {
		if (state.mayViolateAssumption()) {
			throw new ContradictionException();
		} else {
	        try {
				state.incPC(INVOKESTATIC_OFFSET);
			} catch (InvalidProgramCounterException e) {
	            throwVerifyError(state);
			}
		}
	}
}
