package com.slang.codegen;

import java.util.ArrayList;
import java.util.HashMap;

public class Instruction {
    public int addr;
    public InstructionCode code;
    public String otherCode;
    public ArrayList<Integer> params;
    public HashMap<InstructionCode, String> instructionCodeStringMapping = new HashMap<>() {{
        put(InstructionCode.CONSTANT, "CONSTANT");
        put(InstructionCode.LOAD_NULL, "LOAD_NULL");
        put(InstructionCode.LOAD_CONSTANT, "LOAD_CONSTANT");
        put(InstructionCode.LOAD_NAME, "LOAD_NAME");
        put(InstructionCode.LOAD_NAME_GLOBAL, "LOAD_NAME_GLOBAL");
        put(InstructionCode.LOAD_INT, "LOAD_INT");
        put(InstructionCode.LOAD_FLOAT, "LOAD_FLOAT");
        put(InstructionCode.LOAD_CHAR, "LOAD_CHAR");
        put(InstructionCode.LOAD_GLOBAL, "LOAD_GLOBAL");
        put(InstructionCode.UNARY_OP, "UNARY_OP");
        put(InstructionCode.BINARY_OP, "BINARY_OP");
        put(InstructionCode.BINARY_SUBSCR, "BINARY_SUBSCR");
        put(InstructionCode.BUILD_ARR, "BUILD_ARR");
        put(InstructionCode.CALL, "CALL");
        put(InstructionCode.HALT, "HALT");
        put(InstructionCode.JMP, "JMP");
        put(InstructionCode.JMP_TRUE, "JMP_TRUE");
        put(InstructionCode.JMP_FALSE, "JMP_FALSE");
        put(InstructionCode.STORE_GLOBAL, "STORE_GLOBAL");
        put(InstructionCode.STORE_NAME, "STORE_NAME");
        put(InstructionCode.STORE_NAME_NOPOP, "STORE_NAME_NOPOP");
        put(InstructionCode.STORE_NAME_GLOBAL, "STORE_NAME_GLOBAL");
        put(InstructionCode.STORE_NAME_GLOBAL_NOPOP, "STORE_NAME_GLOBAL_NOPOP");
        put(InstructionCode.STORE_SUBSCR, "STORE_SUBSCR");
        put(InstructionCode.STORE_SUBSCR_INPLACE, "STORE_SUBSCR_INPLACE");
        put(InstructionCode.STORE_SUBSCR_NOPOP, "STORE_SUBSCR_NOPOP");
        put(InstructionCode.PUSH, "PUSH");
        put(InstructionCode.RET, "RET");
        put(InstructionCode.PRINTK, "PRINTK");
        put(InstructionCode.NOOP, "NOOP");
        put(InstructionCode.POP_OP, "POP_OP");
        put(InstructionCode.VMALLOC, "VMALLOC");
        put(InstructionCode.CMALLOC, "CMALLOC");
        put(InstructionCode.TYPE_CVT, "TYPE_CVT");
        put(InstructionCode.SIZE_OF, "SIZE_OF");
    }};

    public Instruction(int addr, InstructionCode code, int... params) {
        this.addr = addr;
        this.code = code;
        this.setParams(params);
    }

    public Instruction(int addr, String code, int... params) {
        this.code = InstructionCode.OTHER;
        this.addr = addr;
        this.otherCode = code;
        this.setParams(params);
    }

    public void setParams(int... params) {
        this.params = new ArrayList<>();
        for (int param : params) {
            this.params.add(param);
        }
    }

    public String dump() {
        ArrayList<String> symbols = new ArrayList<>();
        symbols.add(String.valueOf(this.addr));

        String instructionString;
        if (this.code == InstructionCode.OTHER) {
            instructionString = this.otherCode;
        } else {
            instructionString = this.instructionCodeStringMapping.get(this.code);
        }
        symbols.add(instructionString);
        for (int param : params) {
            symbols.add(String.valueOf(param));
        }
        return String.join(" ", symbols);
    }
}
