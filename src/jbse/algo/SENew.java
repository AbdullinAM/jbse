package jbse.algo;

import static jbse.algo.Util.createAndThrow;
import static jbse.algo.Util.ensureKlass;
import static jbse.algo.Util.ILLEGAL_ACCESS_ERROR;
import static jbse.algo.Util.NO_CLASS_DEFINITION_FOUND_ERROR;
import static jbse.algo.Util.throwVerifyError;

import jbse.bc.ClassHierarchy;
import jbse.bc.exc.ClassFileNotAccessibleException;
import jbse.bc.exc.ClassFileNotFoundException;
import jbse.bc.exc.InvalidIndexException;
import jbse.common.Util;
import jbse.common.exc.UnexpectedInternalException;
import jbse.dec.exc.DecisionException;
import jbse.mem.State;
import jbse.mem.exc.InvalidProgramCounterException;
import jbse.mem.exc.ThreadStackEmptyException;

final class SENew implements Algorithm {
	
	@Override
	public void exec(State state, ExecutionContext ctx) 
	throws ThreadStackEmptyException, DecisionException {
		final int index;
		try {
			final byte tmp1 = state.getInstruction(1);
			final byte tmp2 = state.getInstruction(2);
			index = Util.byteCat(tmp1,tmp2);
		} catch (InvalidProgramCounterException e) {
            throwVerifyError(state);
			return;
		}
		final String currentClassName = state.getCurrentMethodSignature().getClassName();
        
        //performs resolution
        final ClassHierarchy hier = state.getClassHierarchy();
		final String classSignature;
		try {
			classSignature = hier.getClassFile(currentClassName).getClassSignature(index);
		} catch (InvalidIndexException e) {
            throwVerifyError(state);
			return;
		} catch (ClassFileNotFoundException e) {
			//this should never happen
			throw new UnexpectedInternalException(e);
		}
        final String classSignatureResolved;
        try {
			classSignatureResolved = hier.resolveClass(currentClassName, classSignature);
		} catch (ClassFileNotFoundException e) {
            createAndThrow(state, NO_CLASS_DEFINITION_FOUND_ERROR);
			return;
		} catch (ClassFileNotAccessibleException e) {
            createAndThrow(state, ILLEGAL_ACCESS_ERROR);
			return;
		}

		//initializes the class in case it is not
		final boolean mustExit;
		try {
			mustExit = ensureKlass(state, currentClassName, ctx.decisionProcedure);
		} catch (ClassFileNotFoundException e) {
			//this should never happen
			throw new UnexpectedInternalException(e);
		}
		if (mustExit) {
			//TODO this should be impossible since the current class should by definition be already initialized! Perhaps should just throw UnexpectedInternalException instead of returning.
			return;
		}

        //creates the new object in the heap
        state.push(state.createInstance(classSignatureResolved));
        
		try {
			state.incPC(3);
		} catch (InvalidProgramCounterException e) {
            throwVerifyError(state);
			return;
		}
	} 
}