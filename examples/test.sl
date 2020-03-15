var int i = 0;
var int a[2000000] = [1, 1];
for (i = 2; i < 1000000;i = i + 1) {
    a[i] = (a[i - 2] + a[i - 1]) % 500;
}
printk a[99999];