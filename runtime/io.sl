# IO库 - Slang Runtime Library
# @author Junru Shen

# 输出字符
func void write(char ch) {
    __svm__ LOAD_NAME &ch;
    __svm__ PUTCH;
}

# 输出字符串
func void write(char ch[]) {
    var int i;
    for (i = 0; ch[i] != '\0'; i = i + 1) write(ch[i]);
}

var char __NUMBER_DIGITS[] = ['0', '1', '2', '3', '4', '5', '6', '7', '8', '9'];
# 输出整型数字
func void write(int n) {
    if (n < 0) {
        write('-');
        n = -n;
    }
    var char s[20];
    var int t, i;
    for (1; n > 0; n = n / 10) {
        s[t] = __NUMBER_DIGITS[n % 10];
        t = t + 1;
    }
    for (i = t - 1; i >= 0; i = i - 1) write(s[i]);
}

# 输出一个空行
func void writeln() {
    write('\n');
}

# 输出一行，仅有一个字符
func void writeln(char ch) {
    write(ch);
    write('\n');
}

# 输出一行字符串
func void writeln(char ch[]) {
    write(ch);
    write('\n');
}

# 输出一行数字
func void writeln(int n) {
    write(n);
    write('\n');
}
