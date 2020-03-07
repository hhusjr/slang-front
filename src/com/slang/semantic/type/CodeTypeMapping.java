package com.slang.semantic.type;

import java.util.HashMap;

public class CodeTypeMapping {
    public static HashMap<String, BasicType> codeTypeMapping = new HashMap<>() {{
        put("int", BasicType.INT);
        put("char", BasicType.CHAR);
        put("bool", BasicType.BOOLEAN);
        put("float", BasicType.FLOAT);
        put("void", BasicType.VOID);
    }};
}
