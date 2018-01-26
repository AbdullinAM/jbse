package jbse.algo.meta;

import static jbse.algo.Util.exitFromAlgorithm;
import static jbse.algo.Util.failExecution;
import static jbse.algo.Util.throwVerifyError;
import static jbse.algo.Util.valueString;
import static jbse.bc.Signatures.JAVA_FILE_PATH;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Supplier;

import jbse.algo.Algo_INVOKEMETA_Nonbranching;
import jbse.algo.InterruptException;
import jbse.algo.StrategyUpdate;
import jbse.algo.exc.SymbolicValueNotAllowedException;
import jbse.common.exc.ClasspathException;
import jbse.mem.Instance;
import jbse.mem.State;
import jbse.tree.DecisionAlternative_NONE;
import jbse.val.Reference;
import jbse.val.Simplex;

/**
 * Meta-level implementation of {@link java.io.WinNTFileSystem#getBooleanAttributes(File)}.
 * 
 * @author Pietro Braione
 */
//TODO unify with Algo_JAVA_UNIXFILESYSTEM_GETBOOLEANATTRIBUTES0
public final class Algo_JAVA_WINNTFILESYSTEM_GETBOOLEANATTRIBUTES extends Algo_INVOKEMETA_Nonbranching {
    private Simplex toPush; //set by cookMore

    @Override
    protected Supplier<Integer> numOperands() {
        return () -> 2;
    }

    @Override
    protected void cookMore(State state) 
    throws InterruptException, ClasspathException, SymbolicValueNotAllowedException {
        try {
            //gets the File parameter: if null, the attributes are 0
            final Reference fileReference = (Reference) this.data.operand(1);
            if (state.isNull(fileReference)) {
                this.toPush = state.getCalculator().valInt(0);
                return;
            }
            final Instance fileObject = (Instance) state.getObject(fileReference);
            if (fileObject == null) {
                //this should never happen
                failExecution("The File f parameter to invocation of method java.io.WinNTFileSystem.getBooleanAttributes was an unresolved symbolic reference.");
            }
            
            //gets the path field as a String
            final Reference filePathReference = (Reference) fileObject.getFieldValue(JAVA_FILE_PATH);
            if (filePathReference == null) {
                throwVerifyError(state);
                exitFromAlgorithm();
            }
            final String filePath = valueString(state, filePathReference);
            if (filePath == null) {
                throw new SymbolicValueNotAllowedException("The File f parameter to invocation of method java.io.WinNTFileSystem.getBooleanAttributes has a symbolic String in its path field.");
            }
            
            //creates a File object with same path and
            //invokes metacircularly the java.io.WinNTFileSystem.getBooleanAttributes
            //method to obtain its attributes
            final Field fileSystemField = File.class.getDeclaredField("fs");
            fileSystemField.setAccessible(true);
            final Object fileSystem = fileSystemField.get(null);
            final Class<?> fileSystemClass = fileSystem.getClass(); 
            final Method getBooleanAttributesMethod = fileSystemClass.getDeclaredMethod("getBooleanAttributes", File.class);
            getBooleanAttributesMethod.setAccessible(true);
            final File f = new File(filePath);
            final int attributes = ((Integer) getBooleanAttributesMethod.invoke(fileSystem, f)).intValue();
            
            //converts the attributes to Simplex
            this.toPush = state.getCalculator().valInt(attributes);
        } catch (ClassCastException e) {
            throwVerifyError(state);
            exitFromAlgorithm();
        } catch (NoSuchFieldException | SecurityException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            //this should not happen
            failExecution(e);
        }
    }

    @Override
    protected StrategyUpdate<DecisionAlternative_NONE> updater() {
        return (state, alt) -> {
            state.pushOperand(this.toPush);
        };
    }
}
