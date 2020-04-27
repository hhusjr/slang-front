# slang-front
## 介绍
slang是个人学了一点点浅薄的编译原理知识顺便写着玩的“编译器”（可能只能算个玩具）。有很多设计不合理的地方，后面会逐渐重构。
该部分是slang的编译器前端（词法分析、语法分析、生成AST）部分源码，是用java写的。

完整”编译器“请见：https://github.com/hhusjr/slang

## SLang的编译器前端
因为主要是为了学习，且为了便于开发调试，编译后直接产生的代码是人类可以看懂的文本形式（非二进制形式），这里称为“中间代码”（和编译原理中的中间代码不是一个意思）。比如下列代码：

```
var int a = 2;
var int b = 3;
var int c;
c = a + b;
```

会被编译成（#号及后面内容是人为添加的）：
```
0 VMALLOC 3 # 申请三个全局变量的内存空间
2 LOAD_CONSTANT 0 # 0号常量“2(int)”进入操作数栈（0号常量定义在下面）
4 STORE_NAME_GLOBAL 0 # 取操作数栈栈顶，存在0号全局变量里
6 LOAD_CONSTANT 1
8 STORE_NAME_GLOBAL 1
10 LOAD_INT 0
12 STORE_NAME_GLOBAL 2 # 未手动初始化的int变量默认初始化为0
14 LOAD_NAME_GLOBAL 0 # 0号全局变量（a）入操作数栈
16 LOAD_NAME_GLOBAL 1 # 1号（b）入操作数栈
18 BINARY_OP 0 # 栈顶两元素弹栈，将和压栈
20 STORE_NAME_GLOBAL 2 # 栈顶元素弹栈，存到2号变量里
22 HALT # 程序结束
0 CMALLOC 2 # 申请两个常量的内存空间
0 CONSTANT 0 2 1 # 申请一个int（0）型常量2，被引用了1次
1 CONSTANT 0 3 1 # 申请一个int（0）型常量3，被引用了1次
```

编译部分是java写的。通过运行：
```
java —jar slang.jar -c 源文件 -i 输出的中间代码文件
```
如
```
java -jar slang.jar -c hello.sl -i hello.sli
```

编译成功会提示Done，否则会有相关的错误提示。

其他的编译指令：

```
1、java -jar slang.jar -a 源文件（输出该文件进行词法分析、语法分析后构建的抽象语法树AST）
2、java -jar slang.jar -t 语法分析是自顶向下的预测表法，该指令可以输出预测表。
```

如果想要顺利运行产生的”中间代码“，可以见https://github.com/hhusjr/slang ，用C++写了一个设计同样不太合理的栈虚拟机，直接执行这些中间代码。（还存在一些内存溢出的情况，后面修复。。。）
