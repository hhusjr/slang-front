func write(char ch) {
    print ch;
}

func write(char ch[]) {
    var int i;
    for (i = 0; ch[i] != '\0'; i = i + 1) print(ch[i]);
}

func writeln(char ch[]) {
    write(ch);
    write('\n');
}
