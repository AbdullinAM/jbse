package jbse.apps;

import jbse.bc.Signature;
import jbse.common.exc.UnexpectedInternalException;
import jbse.mem.*;
import jbse.mem.exc.FrozenStateException;
import jbse.mem.exc.ThreadStackEmptyException;
import jbse.val.*;
import org.jetbrains.research.kex.ReanimatorRunner;
import org.jetbrains.research.kex.descriptor.*;
import org.jetbrains.research.kex.reanimator.callstack.CallStack;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static jbse.common.Type.*;

public class StateFormatterDescriptor implements Formatter {
    private static ReanimatorRunner runner = null;
    private final Set<Symbolic> topLevelSymbols = new HashSet<>();
    private final Map<Symbolic, Desc> descriptors = new HashMap<>();
    private final HashMap<String, String> symbolsToVariables = new HashMap<>();
    private final Supplier<Map<PrimitiveSymbolic, Simplex>> modelSupplier;
    private final Supplier<State> initialStateSupplier;

    private static void initReanimator(Path kexCong, Path sourcePaths) {
        runner = new ReanimatorRunner(kexCong, sourcePaths);
    }

    public StateFormatterDescriptor(Path kexCong, Path sourcePaths, Supplier<State> initialStateSupplier, Supplier<Map<PrimitiveSymbolic, Simplex>> modelSupplier) {
        this.initialStateSupplier = initialStateSupplier;
        this.modelSupplier = modelSupplier;
        if (runner == null)
            initReanimator(kexCong, sourcePaths);
    }

    @Override
    public void formatState(State s) {
        try {
            initDescriptors(s, modelSupplier.get());
        } catch (FrozenStateException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String emit() {
        Map<Desc, CallStack> stacks = runner.convert(topLevelSymbols.stream().map(descriptors::get).collect(Collectors.toSet()));
        CallStack stack = null;
        try {
            stack = getInvocationOfMethodUnderTest(initialStateSupplier.get(), stacks);
        } catch (ThreadStackEmptyException | FrozenStateException e) {
            e.printStackTrace();
        }
        return (stack != null ? runner.printCallStack(stack) : "null") + "\n\n";
    }

    @Override
    public void cleanup() {
        topLevelSymbols.clear();
        descriptors.clear();
        symbolsToVariables.clear();
    }

    private void initDescriptors(State state, Map<PrimitiveSymbolic, Simplex> model) throws FrozenStateException {
        final Collection<Clause> pathCondition = state.getPathCondition();
        for (final Clause clause : pathCondition) {
            if (clause instanceof ClauseAssumeExpands) {
                final ClauseAssumeExpands clauseExpands = (ClauseAssumeExpands) clause;
                final Symbolic symbol = clauseExpands.getReference();
                final long heapPosition = clauseExpands.getHeapPosition();
                final String klass = getTypeOfObjectInHeap(state, heapPosition);
                makeVariableFor(symbol);
                addDescriptor(symbol, new ObjectDesc(parseType(klass), getVariableFor(symbol)), model);
            } else if (clause instanceof ClauseAssumeNull) {
                final ClauseAssumeNull clauseNull = (ClauseAssumeNull) clause;
                final ReferenceSymbolic symbol = clauseNull.getReference();
                makeVariableFor(symbol);
                addDescriptor(symbol, new NullDesc(), model);
            } else if (clause instanceof ClauseAssumeAliases) {
                final ClauseAssumeAliases clauseAliases = (ClauseAssumeAliases) clause;
                final Symbolic symbol = clauseAliases.getReference();
                final long heapPosition = clauseAliases.getHeapPosition();
                makeVariableFor(symbol);
                final ReferenceSymbolic aliasObject = getObjectInHeap(state, heapPosition);
                addDescriptor(symbol, descriptors.get(aliasObject), model);
            } else if (clause instanceof ClauseAssume) {
                final ClauseAssume clauseAssume = (ClauseAssume) clause;
                final Primitive p = clauseAssume.getCondition();

                final Set<PrimitiveSymbolic> symbols = primitiveSymbolsIn(p);
                for (PrimitiveSymbolic symbol : symbols) {
                    if (getVariableFor(symbol) == null) { //not yet done
                        final Simplex value = model.get(symbol);
                        if (value == null) {
                            //this should never happen
                            throw new UnexpectedInternalException("No value found in model for symbol " + symbol.toString() + ".");
                        }
                        addDescriptor(symbol, new ConstantDesc(value.toString(), parseType(Character.toString(value.getType()))), model);
                    }
                }
            }
        }
    }

    private String parseType(String desc) {
        char first = desc.charAt(0);
        switch (first) {
            case 'V':
                return "void";
            case 'Z':
                return "bool";
            case 'B':
                return "byte";
            case 'C':
                return "char";
            case 'S':
                return "short";
            case 'I':
                return "int";
            case 'J':
                return "long";
            case 'F':
                return "float";
            case 'D':
                return "double";
            case '[':
                return parseType(desc.substring(1)) + "[]";
            default:
                return desc.replaceAll("/", ".");
        }
    }

    private void addDescriptor(Symbolic object, Desc value, Map<PrimitiveSymbolic, Simplex> model) {
        descriptors.put(object, value);
        if (object instanceof ReferenceSymbolicMemberArray) {
            ReferenceSymbolicMemberArray memberArray = (ReferenceSymbolicMemberArray) object;
            ReferenceSymbolic array = memberArray.getContainer();
            ArrayDesc arrayDesc = (ArrayDesc) descriptors.get(array);
            PrimitiveSymbolic index = (PrimitiveSymbolic) memberArray.getIndex();
            Simplex resolvedValue = model.get(index);
            arrayDesc.addElement(Integer.parseInt(resolvedValue.toString()), value);
        } else if (object instanceof ReferenceSymbolicMemberField) {
            ReferenceSymbolicMemberField memberField = (ReferenceSymbolicMemberField) object;
            ReferenceSymbolic objectSymbolic = memberField.getContainer();
            DescField field = new DescField(memberField.getFieldName(), parseType(memberField.getStaticType()));
            ObjectDesc objectDesc = (ObjectDesc) descriptors.get(objectSymbolic);
            objectDesc.addField(field, value);
        } else if (object instanceof PrimitiveSymbolicMemberArray) {
            PrimitiveSymbolicMemberArray memberArray = (PrimitiveSymbolicMemberArray) object;
            ReferenceSymbolic array = memberArray.getContainer();
            ArrayDesc arrayDesc = (ArrayDesc) descriptors.get(array);
            PrimitiveSymbolic index = (PrimitiveSymbolic) memberArray.getIndex();
            Simplex resolvedValue = model.get(index);
            arrayDesc.addElement(Integer.parseInt(resolvedValue.toString()), value);
        } else if (object instanceof PrimitiveSymbolicMemberField) {
            PrimitiveSymbolicMemberField memberField = (PrimitiveSymbolicMemberField) object;
            ReferenceSymbolic objectSymbolic = memberField.getContainer();
            DescField field = new DescField(memberField.getFieldName(), parseType(Character.toString(memberField.getType())));
            ObjectDesc objectDesc = (ObjectDesc) descriptors.get(objectSymbolic);
            objectDesc.addField(field, value);
        } else {
            topLevelSymbols.add(object);
        }
    }

    private ReferenceSymbolic getObjectInHeap(State finalState, long heapPos) {
        final Collection<Clause> path = finalState.getPathCondition();
        for (Clause clause : path) {
            if (clause instanceof ClauseAssumeExpands) {
                final ClauseAssumeExpands clauseExpands = (ClauseAssumeExpands) clause;
                final long heapPosCurrent = clauseExpands.getHeapPosition();
                if (heapPosCurrent == heapPos) {
                    return clauseExpands.getReference();
                }
            }
        }
        return null;
    }

    private String generateName(String name) {
        return name.replace("{ROOT}:", "__ROOT_");
    }

    private void makeVariableFor(Symbolic symbol) {
        final String value = symbol.getValue();
        final String origin = symbol.asOriginString();
        if (!this.symbolsToVariables.containsKey(value)) {
            this.symbolsToVariables.put(value, generateName(origin));
        }
    }

    private String getVariableFor(Symbolic symbol) {
        final String value = symbol.getValue();
        return this.symbolsToVariables.get(value);
    }

    private static String getTypeOfObjectInHeap(State finalState, long num) throws FrozenStateException {
        final Map<Long, Objekt> heap = finalState.getHeap();
        final Objekt o = heap.get(num);
        return o.getType().getClassName();
    }

    private Set<PrimitiveSymbolic> primitiveSymbolsIn(Primitive e) {
        final HashSet<PrimitiveSymbolic> symbols = new HashSet<>();
        PrimitiveVisitor v = new PrimitiveVisitor() {

            @Override
            public void visitWideningConversion(WideningConversion x) throws Exception {
                x.getArg().accept(this);
            }

            @Override
            public void visitTerm(Term x) throws Exception {
            }

            @Override
            public void visitSimplex(Simplex x) throws Exception {
            }

            @Override
            public void visitPrimitiveSymbolicAtomic(PrimitiveSymbolicAtomic s) {
                symbols.add(s);
            }

            @Override
            public void visitNarrowingConversion(NarrowingConversion x) throws Exception {
                x.getArg().accept(this);
            }

            @Override
            public void visitPrimitiveSymbolicApply(PrimitiveSymbolicApply x) throws Exception {
                for (Value v : x.getArgs()) {
                    if (v instanceof Primitive) {
                        ((Primitive) v).accept(this);
                    }
                }
            }

            @Override
            public void visitExpression(Expression e) throws Exception {
                if (e.isUnary()) {
                    e.getOperand().accept(this);
                } else {
                    e.getFirstOperand().accept(this);
                    e.getSecondOperand().accept(this);
                }
            }

            @Override
            public void visitAny(Any x) {
            }
        };

        try {
            e.accept(v);
        } catch (Exception exc) {
            //this should never happen
            throw new AssertionError(exc);
        }
        return symbols;
    }


    private CallStack getInvocationOfMethodUnderTest(State initialState, Map<Desc, CallStack> stacks) throws ThreadStackEmptyException, FrozenStateException {
        if (initialState == null) return null;

        Map<String, CallStack> namedStacks = stacks.entrySet().stream().collect(Collectors.toMap(
                descCallStackEntry -> descCallStackEntry.getKey().getName(),
                Map.Entry::getValue
        ));

        CallStack thisStack = runner.convert(new NullDesc());
        final String methodName = initialState.getRootMethodSignature().getName();
        if ("this".equals(initialState.getRootFrame().getLocalVariableDeclaredName(0))) {
            CallStack cs = namedStacks.get("__ROOT_this");
            if (cs != null) thisStack = cs;
        }

        final Map<Integer, Variable> lva = initialState.getRootFrame().localVariables();
        final TreeSet<Integer> slots = new TreeSet<>(lva.keySet());
        final int numParamsExcludedThis = splitParametersDescriptors(initialState.getRootMethodSignature().getDescriptor()).length;
        int currentParam = 1;
        List<CallStack> args = new ArrayList<>();
        for (int slot : slots) {
            final Variable lv = lva.get(slot);
            if ("this".equals(lv.getName())) {
                continue;
            }
            if (currentParam > numParamsExcludedThis) {
                break;
            }
            final String variable = "__ROOT_" + lv.getName();
            if (this.symbolsToVariables.containsValue(variable)) {
                args.add(namedStacks.get(variable));
            } else if (isPrimitiveIntegral(lv.getType().charAt(0))) {
                args.add(runner.convert(new ConstantDesc("0", parseType(lv.getType()))));
            } else if (isPrimitiveFloating(lv.getType().charAt(0))) {
                args.add(runner.convert(new ConstantDesc("0.0", parseType(lv.getType()))));
            } else {
                args.add(runner.convert(new NullDesc()));
            }
            ++currentParam;
        }

        Signature methodSign = initialState.getRootMethodSignature();
        return runner.getMethodInvocation(thisStack, args, methodSign.getClassName(), methodName, methodSign.getDescriptor());
    }
}
