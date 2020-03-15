package com.slang.codegen;

public enum InstructionCode {
    CONSTANT,
    LOAD_CONSTANT,
    LOAD_NULL,
    LOAD_NAME,
    LOAD_NAME_GLOBAL,
    LOAD_INT,
    LOAD_FLOAT,
    LOAD_CHAR,
    STORE_NAME,
    STORE_NAME_NOPOP,
    STORE_NAME_GLOBAL,
    STORE_NAME_GLOBAL_NOPOP,
    JMP,
    JMP_TRUE,
    JMP_FALSE,
    BINARY_OP,
    UNARY_OP,
    HALT,
    RET,
    PUSH,
    CALL,
    LOAD_GLOBAL,
    STORE_GLOBAL,
    BUILD_ARR,
    BINARY_SUBSCR,
    STORE_SUBSCR,
    STORE_SUBSCR_INPLACE,
    STORE_SUBSCR_NOPOP,
    PRINTK,
    NOOP,
    POP_OP,
    VMALLOC,
    CMALLOC
}