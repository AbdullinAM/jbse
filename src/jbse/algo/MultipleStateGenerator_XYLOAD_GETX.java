package jbse.algo;

import static jbse.algo.Util.throwVerifyError;

import jbse.common.Type;
import jbse.common.exc.UnexpectedInternalException;
import jbse.dec.exc.DecisionException;
import jbse.mem.Array;
import jbse.mem.Objekt;
import jbse.mem.State;
import jbse.mem.exc.ContradictionException;
import jbse.mem.exc.InvalidProgramCounterException;
import jbse.mem.exc.ThreadStackEmptyException;
import jbse.tree.DecisionAlternative;
import jbse.tree.DecisionAlternative_XYLOAD_GETX_Aliases;
import jbse.tree.DecisionAlternative_XYLOAD_GETX_Null;
import jbse.tree.DecisionAlternative_XYLOAD_GETX_Expands;
import jbse.tree.DecisionAlternative_XYLOAD_GETX_Loads;
import jbse.val.Primitive;
import jbse.val.ReferenceConcrete;
import jbse.val.ReferenceSymbolic;
import jbse.val.Value;
import jbse.val.exc.InvalidOperandException;
import jbse.val.exc.InvalidTypeException;

/**
 * A {@link MultipleStateGenerator} for *load, *aload, get* bytecodes 
 * ([a/d/f/i/l]load[_0/1/2/3], [a/b/c/d/f/i/l/s]aload,  get[field/static]).
 * It defines method which manage refinement in the case the {@link Value} 
 * is a {@link ReferenceSymbolic} ("lazy initialization"), and/or it comes 
 * from an {@link Array}.
 * 
 * @author Pietro Braione
 *
 * @param <R> the {@link DecisionAlternative}s used to encode the results 
 *            of splitting decisions. It is <em>not</em> assumed to be a {@link DecisionAlternative_XYLOAD_GETX_Loads}
 *            so it can be used as a superclass also for array bytecodes {@link Algorithm}s, 
 *            which do not load any {@link Value} to the operand stack in the 
 *            out-of-range case. 
 */
abstract class MultipleStateGenerator_XYLOAD_GETX<R extends DecisionAlternative> extends MultipleStateGenerator<R> {
	/** Must be set by subclasses to provide the pc offset for the bytecode. */
	protected int pcOffset;
	
	protected MultipleStateGenerator_XYLOAD_GETX(Class<R> superclassDecisionAlternatives) {
		super(superclassDecisionAlternatives);
	}
	
	protected final void refineRefExpands(State s, DecisionAlternative_XYLOAD_GETX_Expands drc) 
	throws ContradictionException, InvalidTypeException {
		final ReferenceSymbolic referenceToExpand = drc.getValueToLoad();
		final String classNameOfTargetObject = drc.getClassNameOfTargetObject();
		s.assumeExpands(referenceToExpand, classNameOfTargetObject);
		//in the case the expansion object is an array, we assume it 
		//to have nonnegative length
		if (Type.isArray(classNameOfTargetObject)) {
			final Primitive lengthPositive;
			try {
				final Array targetObject = (Array) s.getObject(referenceToExpand);
				lengthPositive = targetObject.getLength().ge(this.state.getCalculator().valInt(0));
			} catch (InvalidOperandException | InvalidTypeException e) {
				//this should never happen
				throw new UnexpectedInternalException(e);
			}
			s.assume(this.ctx.decisionProcedure.simplify(lengthPositive));
		}
	}

	protected final void refineRefAliases(State s, DecisionAlternative_XYLOAD_GETX_Aliases dro)
	throws ContradictionException {
		final ReferenceSymbolic referenceToResolve = dro.getValueToLoad();
		final long aliasPosition = dro.getAliasPosition();
		final Objekt object = s.getObjectInitial(new ReferenceConcrete(aliasPosition));
		s.assumeAliases(referenceToResolve, aliasPosition, object);
	}
	
	protected final void refineRefNull(State s, DecisionAlternative_XYLOAD_GETX_Null drn)
	throws ContradictionException {
		final ReferenceSymbolic referenceToResolve = drn.getValueToLoad();
		s.assumeNull(referenceToResolve);
	}
	
	protected final void update(State s, DecisionAlternative_XYLOAD_GETX_Loads r) 
	throws DecisionException, ThreadStackEmptyException {
		final Value val = r.getValueToLoad();
		final Value valMaterialized = possiblyMaterialize(s, val);
		final char valType = valMaterialized.getType();
		final Value valToPush;
		if (Type.isPrimitive(valType) && !Type.isPrimitiveOpStack(valType)) {
		    try {
                valToPush = ((Primitive) valMaterialized).to(Type.INT);
            } catch (InvalidTypeException e) {
                //this should not happen
                throw new UnexpectedInternalException(e);
            }
		} else {
		    valToPush = valMaterialized;
		}
		
		s.pushOperand(valToPush);
		
		//manages triggers and increments the program counter
		final boolean goOn;
		try {
			goOn = this.ctx.triggerManager.runTriggers(s, r, this.pcOffset);
		} catch (InvalidProgramCounterException e) {
		    throwVerifyError(s);
			return;
		}
		if (goOn) {
			//updates the program counter
			try {
				s.incPC(this.pcOffset);
			} catch (InvalidProgramCounterException e) {
			    throwVerifyError(s);
			}
		}
	}
	
	protected abstract Value possiblyMaterialize(State s, Value val) 
	throws DecisionException;
}