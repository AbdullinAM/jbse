package jbse.mem;

import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import jbse.bc.LocalVariableTable;
import jbse.bc.LocalVariableTable.Row;
import jbse.common.Type;
import jbse.mem.exc.InvalidSlotException;
import jbse.val.DefaultValue;
import jbse.val.Value;

/**
 * Class representing a local variable memory area.
 */
class LocalVariablesArea implements Cloneable {
	/** The local variable table for the method. */
	private final LocalVariableTable lvt;
	
	/** Values in the memory area, accessible by slot. */
	private SortedMap<Integer, Value> values;

    /**
     * Constructor.
     * 
     * @param lvt a {@link LocalVariableTable}.
     */
    LocalVariablesArea(LocalVariableTable lvt) {
    	this.values = new TreeMap<Integer, Value>();
    	this.lvt = lvt;
		//initializes all the local variables by using args
        //until exhaustion, then DefaultValue
    }
   
    /**
     * Initializes the local variables by an array 
     * of {@link Value}s.
     * 
     * @param args a {@link Value}{@code []}; The 
     * local variables are initialized in sequence 
     * with these values. 
     * If there are less values in {@code args} than 
     * local variables in this object, 
     * the remaining variables are initialized to 
     * {@link DefaultValue}.
     * @throws InvalidSlotException  when there are 
     * too many {@code arg}s or some of their types are 
     * incompatible with their respective slots types.
     */
    void setArgs(Value[] args) throws InvalidSlotException {
		int nargs = (args == null ? 0 : args.length);
		int j = 0;
		int slot = 0;
		while (slot < this.lvt.getSlots()) {
			final Value val;
            if (j < nargs) {
            	val = args[j];
            	++j;
            } else {
            	val = DefaultValue.getInstance();
            }
            this.set(slot, 0, val);
            
            //next slot
            ++slot;
            if (!Type.isCat_1(val.getType())) {
            	++slot;
            }
		}
    }

    /**
     * Stores a value into a specific slot of the local variable area.
     * 
     * @param slot an {@code int}, the slot of the local variable.
     * @param currentPC the current program counter; if the local variable
     *        table contains type information about the local variable, this
     *        is used to check type conformance of the access.
     * @param val the {@link Value} to be stored.  
     * @throws InvalidSlotException when {@code slot} is out of range or
     *         {@code val}'s type is incompatible with the slot's type.
     */
    void set(int slot, int currentPC, Value val) throws InvalidSlotException {
    	final int nslots = (Type.isCat_1(val.getType()) ? 1 : 2);
    	if (slot < 0 || slot > this.lvt.getSlots() - nslots) {
    		throw new InvalidSlotException("slot number " + slot + " is out of range");
    	}
    	
    	final Row r = this.lvt.row(slot, currentPC);
    	if (!slotMayReceive(r, val)) {
    		throw new InvalidSlotException("slot number " + slot + " has wrong type");
    	}
    	
    	if (nslots == 2) {
    		this.values.remove(slot + 1);
    	}

   		//stores val at slot
        this.values.put(slot, val);
    }
    
    private boolean slotMayReceive(Row r, Value val) {
    	if (r == null) {
    		return true;
    	} else {
	    	char slotType = r.descriptor.charAt(0);
	    	char valueType = val.getType();
	    	return (slotType == valueType || 
	    			(slotType == Type.INT && valueType == Type.BOOLEAN) ||
	    			(slotType == Type.BOOLEAN && valueType == Type.INT) ||
	    			((slotType == Type.REFERENCE || slotType == Type.ARRAYOF) && 
	    				(valueType == Type.REFERENCE || valueType == Type.ARRAYOF || valueType == Type.NULLREF)));
    	}
    }
    
    /**
     * Returns the value of a local variable.
     * 
     * @param slot an {@code int}, the slot of the local variable.
     * @return a {@link Value}, the one stored in the local variable.
     * @throws InvalidSlotException 
     */
    Value get(int slot) throws InvalidSlotException {
        Value retVal = this.values.get(slot);
        
        //the next case denotes, e.g., we wrote a cat2 value at slot x
        //and we try to read at slot x+1. 
        // TODO investigate the JVM spec and decide what to do.
        if (retVal == null) {
        	throw new InvalidSlotException("slot " + slot + " was not written");
        }

        //the next case should never happen in verified code, however 
        //the JVM specification does not explicitly exclude it. 
        if (DefaultValue.getInstance().equals(retVal))
        	;
        /* TODO Should we throw InvalidSlotException, or rather return 
         * the true default value instead, which can be inferred from the 
         * bytecode??? Note that the latter case would require deep changes 
         * and perhaps is best done inside the load bytecode algorithm classes. 
         * Here we chose to return the DefaultValue, in order to better support 
         * prettyprinting. Externally no support to DefaultValue handling 
         * is implemented, but it shouldn't permeate with well-behaving 
         * code so this can be added to the todo list without much concern.
         */
        
        return retVal;
    }
    
    /**
     * Returns all the slots of the local variable area.
     * 
     * @return a {@link Set}<code>&lt;</code>{@link Integer}<code>&gt;</code> 
     *         containing all the valid slot numbers of this local variable
     *         area.
     */
    Set<Integer> slots() {
    	return this.values.keySet();
    }
    
    /**
     * Returns the name of a local variable.
     *  
     * @param slot the number of the slot of a local variable.
     * @param curPC the current program counter.
     * @return a {@link String} containing the name of the local
     *         variable at {@code slot} as from the available debug 
     *         information, or {@code null} if no debug information is 
     *         available for the {@code (slot, curPC)} combination.
     */
    String getLocalVariableName(int slot, int curPC) {
        final LocalVariableTable.Row r = this.lvt.row(slot, curPC);
        return (r == null ? null : r.name);
    }
    
    /**
     * Builds a (read-only) local variable.
     * 
     * @param slot the number of the slot.
     * @param curPC the current program counter.
     * @return a {@link Variable} at position.
     * @throws InvalidSlotException if {@code slot}
     * is invalid.
     */
    Variable buildLocalVariable(int slot, int curPC) throws InvalidSlotException {
    	//gets the value
        final Value val = get(slot);
                
        //finds static information
        final LocalVariableTable.Row r = this.lvt.row(slot, curPC);
        final Variable retVal;
        if (r == null) {
        	retVal = new Variable("" + Type.UNKNOWN, "__LOCAL[" + slot + "]", val);
        } else {
    		retVal = new Variable(r.descriptor, r.name, val);
        }
        return retVal;
    }
    
    @Override
    public LocalVariablesArea clone() {
        final LocalVariablesArea o;
        try {
            o = (LocalVariablesArea) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
        
        final SortedMap<Integer, Value> valuesCopy = new TreeMap<>(this.values);        
        o.values = valuesCopy;
        return o;
    }
    
    /**
     * Returns a {@code String} representation for the local variable area
     */
    @Override
    public String toString() {
        String tmp = "[";
        int j = 0;
       	for (Map.Entry<Integer, Value> e : values.entrySet()) {
            tmp += e.getKey() + ":"+ e.getValue();
            if (j < values.size() - 1) {
            	tmp += ", ";
            }
            j++;
        }
       	tmp += "]";
        return tmp;
    }
}