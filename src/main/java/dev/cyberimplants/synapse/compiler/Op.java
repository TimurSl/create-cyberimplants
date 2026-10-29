package dev.cyberimplants.synapse.compiler;

/** Bytecode operations. The operand meaning is documented per constant ({@code a} = int operand, {@code k} = object operand). */
public enum Op {
    /** Push constant {@code k}. */
    PUSH(1),
    POP(1),
    /** Push local slot {@code a}. */
    LOAD_LOCAL(1),
    /** Pop into local slot {@code a}. */
    STORE_LOCAL(1),
    /** Resolve name {@code k}: event payload, then program variable, then host global. */
    LOAD_NAME(1),
    /** Pop into program variable {@code k}. */
    STORE_GLOBAL(1),
    /** Pop an object, push its member {@code k}. */
    MEMBER(2),
    /** Push the device handle for {@code k} ({@link DeviceInfo}). */
    DEVICE(4),
    /** Pop value then object, assign member {@code k} (writable implant property). */
    SET_MEMBER(10),
    NEG(1),
    NOT(1),
    ADD(1),
    SUB(1),
    MUL(1),
    DIV(1),
    MOD(1),
    EQ(1),
    NE(1),
    LT(1),
    LE(1),
    GT(1),
    GE(1),
    JUMP(1),
    /** Pop; jump to {@code a} when falsey. */
    JUMP_IF_FALSE(1),
    /** Short-circuit AND: if top is falsey jump to {@code a} keeping it, else pop. */
    AND_JUMP(1),
    /** Short-circuit OR: if top is truthy jump to {@code a} keeping it, else pop. */
    OR_JUMP(1),
    /** Call builtin {@code k} with {@code a} arguments. */
    CALL(2),
    /** {@code k} = {@link ActivateInfo}; pops the WITH values (in order) then the optional AT target. */
    ACTIVATE(10),
    DEACTIVATE(10),
    TOGGLE(10),
    SCAN(10),
    TAG(10),
    UNTAG(10),
    LOG(2),
    ALERT(2),
    STOP(1),
    /** End of the code unit. */
    END(0);

    private final int cycles;

    Op(int cycles) {
        this.cycles = cycles;
    }

    /** Base cycle cost (docs/design/04-synapse-language.md, section 8.1). */
    public int cycles() {
        return cycles;
    }
}
