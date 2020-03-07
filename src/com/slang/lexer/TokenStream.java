package com.slang.lexer;

import com.slang.utils.Panic;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * 词法分析的相关定义和辅助工具
 */
class LexerUtil {
    /**
     * 定义保留字集合
     */
    public static HashSet<String> reservedWords = new HashSet<>() {{
        add("print");
        add("to");
        add("from");
        add("loop");
        add("ref");
        add("func");
        add("ret");
        add("var");
        add("while");
        add("if");
        add("else");
        add("true");
        add("false");
    }};

    /**
     * 定义转义字符和对应的字符集合
     */
    public static HashMap<Character, Character> escapeCharacterMapping = new HashMap<>() {{
        put('a', (char) 7);
        put('b', (char) 8);
        put('f', (char) 12);
        put('n', (char) 10);
        put('r', (char) 13);
        put('t', (char) 9);
        put('v', (char) 11);
        put('\'', (char) 39);
        put('"', (char) 34);
        put('\\', (char) 92);
    }};

    /**
     * 定义基本的运算符的Token名
     */
    public static HashMap<Character, String> basicOperatorName = new HashMap<>() {{
        put('+', "PLUS");
        put('-', "SUB");
        put('*', "PROD");
        put('/', "DIV");
        put('&', "AND");
        put('|', "OR");
        put('^', "XOR");
        put('!', "NOT");
        put('=', "EQ");
        put('<', "LT");
        put('>', "GT");
    }};

    /**
     * 判断是否为空字符
     * @param ch 待判断的字符
     * @return 字符是否为空，为空则返回true，否则返回false
     */
    public static boolean isEmptyChar(char ch) {
        return ch == ' ' || ch == '\n' || ch == '\r' || ch == '\t';
    }

    /**
     * 判断是否为字母
     * @param ch 带判断的字符
     * @return 是否为字母，如果是则返回true，否则返回false
     */
    public static boolean isAlpha(char ch) {
        return (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'X');
    }

    /**
     * 判断是否为数码
     * @param ch 带判断的字符
     * @return 是否为数码，如果是则返回true，否则返回false
     */
    public static boolean isDigit(char ch) {
        return ch >= '0' && ch <= '9';
    }

    /**
     * 获取假设标识符的真正Token名
     * 程序中会把标识符和保留字一律当作标识符识别，识别完毕后通过检查该"假设标识符"是否在
     * 保留字列表中，来获得其真实的Token名称
     * @param hypotheticalIdentifier 假设标识符
     * @return 真实的Token名称。ID或者某个保留字对应的Token名称。
     */
    public static String getHypotheticalIdentifierName(String hypotheticalIdentifier) {
        if (LexerUtil.reservedWords.contains(hypotheticalIdentifier)) {
            return hypotheticalIdentifier.toUpperCase();
        }
        return "ID";
    }
}

/**
 * Token流
 */
public class TokenStream {
    private int MAX_CODE_BUFFER_SIZE = 104857600;
    private char[] code = new char[MAX_CODE_BUFFER_SIZE];
    private ArrayList<Token> tokens = new ArrayList<>();
    private String path;
    private int codeBufferSize;
    private int tokenPointer = 0;

    /**
     * 初始化Token流
     * 遇到文件不存在或者IO故障则会引发错误
     * @param path 源代码文件
     */
    public TokenStream(String path) {
        File file = new File(path);
        this.path = path;
        try {
            InputStream inputStream = new FileInputStream(file);
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            while (true) {
                int ch = inputStreamReader.read();
                if (ch == -1) {
                    break;
                }
                code[codeBufferSize++] = (char) ch;
            }
            inputStream.close();
        } catch (FileNotFoundException e) {
            Panic panic = new Panic("File not found", new CodeAxis(path));
            panic.show();
        } catch (IOException e) {
            Panic panic = new Panic(String.format("File I/O Error: %s", e.getMessage()), new CodeAxis(path));
            panic.show();
        }
        this.tokenize();
    }

    /**
     * 产生Token的代码坐标
     * @param line 行号
     * @param charPos 位置
     * @return 代码坐标类
     */
    private CodeAxis getCodeAxis(int line, int charPos) {
        return new CodeAxis(this.path, line, charPos);
    }

    /**
     * 对源代码进行Token分解
     * 将分解后的结果放入到tokens列表中
     */
    private void tokenize() {
        int currentPosition = 0;
        int currentLine = 1;
        int currentCharPos;
        int lastCharInLinePosition = -1;

        while (currentPosition < codeBufferSize) {
            char ch = code[currentPosition];

            // 行和字符位置的统计
            if (ch == '\n') {
                currentLine++;
                lastCharInLinePosition = currentPosition;
            }
            currentCharPos = currentPosition - lastCharInLinePosition;
            CodeAxis codeAxis = this.getCodeAxis(currentLine, currentCharPos);

            // 如果当前字符是空字符
            if (LexerUtil.isEmptyChar(ch)) {
                currentPosition++;
                continue;
            }

            // 如果当前字符是字母a-z A-Z，考虑标识符
            if (LexerUtil.isAlpha((ch))) {
                StringBuilder identifier = new StringBuilder();
                while (LexerUtil.isAlpha(ch)
                        || LexerUtil.isDigit(ch)
                        || ch == '_') {
                    identifier.append(ch);
                    // 如果已经到达最后了，不能继续往后扫描
                    if (currentPosition >= codeBufferSize - 1) {
                        currentPosition++;
                        break;
                    }
                    ch = code[++currentPosition];
                }
                String identifierString = identifier.toString();
                tokens.add(new Token(LexerUtil.getHypotheticalIdentifierName(identifierString), identifierString, codeAxis));
                continue;
            }

            // 如果当前字符是引号
            if (ch == '\'' || ch == '"') {
                char quote = ch;
                StringBuilder literal = new StringBuilder();
                // 如果已经到达最后了，不能继续往后扫描
                if (currentPosition >= codeBufferSize - 1) {
                    Panic panic = new Panic("Unmatched quote", codeAxis);
                    panic.show();
                }
                ch = code[++currentPosition];
                while (ch != quote) {
                    literal.append(ch != '\\' ? ch : LexerUtil.escapeCharacterMapping.get(code[++currentPosition]));
                    // 如果已经到达最后了，不能继续往后扫描
                    if (currentPosition >= codeBufferSize - 1) {
                        Panic panic = new Panic("Unmatched quote", codeAxis);
                        panic.show();
                    }
                    ch = code[++currentPosition];
                }
                currentPosition++;
                tokens.add(new Token(quote == '"' ? "STRING_LITERAL" : "CHAR_LITERAL", literal.toString(), codeAxis));
                continue;
            }

            // 递归处理模块导入展开
            if (ch == '`') {
                StringBuilder path = new StringBuilder();
                // 如果已经到达最后了，不能继续往后扫描
                if (currentPosition >= codeBufferSize - 1) {
                    Panic panic = new Panic("Unmatched quote", codeAxis);
                    panic.show();
                }
                ch = code[++currentPosition];
                while (ch != '`') {
                    path.append(ch);
                    // 如果已经到达最后了，不能继续往后扫描
                    if (currentPosition >= codeBufferSize - 1) {
                        Panic panic = new Panic("Unmatched quote", codeAxis);
                        panic.show();
                    }
                    ch = code[++currentPosition];
                }
                currentPosition++;
                TokenStream includedFileTokenStream = new TokenStream(path.toString());
                tokens.addAll(includedFileTokenStream.getTokens());
                continue;
            }

            // 如果当前字符是数字
            if (LexerUtil.isDigit(ch)) {
                StringBuilder literal = new StringBuilder();
                while (LexerUtil.isDigit(ch) || ch == '.') {
                    literal.append(ch);
                    // 如果已经到达最后了，不能继续往后扫描
                    if (currentPosition >= codeBufferSize - 1) {
                        currentPosition++;
                        break;
                    }
                    ch = code[++currentPosition];
                }
                tokens.add(new Token("NUMBER_LITERAL", literal.toString(), codeAxis));
                continue;
            }

            // 如果当前字符是单个字符
            HashSet<Character> singleTokens = new HashSet<>() {{
                add('~');
                add('%');
                add('(');
                add(')');
                add('[');
                add(']');
                add('{');
                add('}');
                add(':');
                add(';');
                add('?');
                add(',');
            }};
            if (singleTokens.contains(ch)) {
                tokens.add(new Token(Character.toString(ch), codeAxis));
                currentPosition++;
                continue;
            }

            // 基本运算符
            if (LexerUtil.basicOperatorName.containsKey(ch)) {
                String name = LexerUtil.basicOperatorName.get(ch);
                String nameFinal = name;
                if (currentPosition < codeBufferSize - 1) {
                    char nextChar = code[currentPosition + 1];
                    if (nextChar == '=') {
                        if (ch == '=') {
                            currentPosition++;
                            nameFinal = "IS_EQ";
                        } else if (ch == '>') {
                            currentPosition++;
                            nameFinal = "GTE";
                        } else if (ch == '<') {
                            currentPosition++;
                            nameFinal = "LTE";
                        } else if (ch == '!') {
                            currentPosition++;
                            nameFinal = "IS_NEQ";
                        }
                    } else {
                        if ((ch == '&' || ch == '|') && nextChar == ch) {
                            currentPosition++;
                            nameFinal = name + "L";
                        }
                    }
                }
                tokens.add(new Token(nameFinal, codeAxis));
                currentPosition++;
                continue;
            }

            // 非法字符
            Panic panic = new Panic(String.format("Invalid character %s", ch), codeAxis);
            panic.show();
        }

        tokens.add(new Token("$$", new CodeAxis()));
    }

    /**
     * 获取所有Token
     * @return 所有的Token
     */
    public ArrayList<Token> getTokens() {
        return this.tokens;
    }

    /**
     * 用于调试，把tokens转换为字符串，用于输出
     * @return 所有Token的字符串形式
     */
    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        for (Token token : this.tokens) {
            str.append(String.format("(%s, %s)\n", token.name, token.value));
        }
        return str.toString();
    }

    /**
     * 获取是否已经扫描完成
     * @return 扫描完成返回true，否则false
     */
    public boolean isFinished() {
        // 注意结尾的文末符号$$不纳入计算范围
        return this.tokenPointer >= this.tokens.size() - 1;
    }

    /**
     * 获取下一个token
     * @return 下一个Token
     */
    public Token next() {
        return this.tokens.get(this.tokenPointer++);
    }

    /**
     * 获取向前看Token的值
     * @return 向前看token的值
     */
    public Token lookAhead() {
        return this.tokens.get(this.tokenPointer);
    }

    /**
     * 获取当前指针位置
     * @return 指针位置
     */
    public int getTokenPointer() {
        return this.tokenPointer;
    }

    /**
     * 设定当前指针位置
     * @param tokenPointer 设定的指针位置
     */
    public void setTokenPointer(int tokenPointer) {
        this.tokenPointer = tokenPointer;
    }
}
