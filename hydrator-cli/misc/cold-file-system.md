### _Slow File Scanning on cold file system_

```sh
# cold ebs volume
$ time ls -al &> /dev/null
```

Output:
```
real    6m38.984s
user    0m0.029s
sys     0m0.287s
```

### _Slow File Scanning on hot (cache) file system_

```sh
$ time ls -al &> /dev/null
```

Output:
```
real    0m0.064s
user    0m0.032s
sys     0m0.032s
```

### _Slow File Scanning on hot (no cache) file system_

```sh
$ sync; echo 3 > /proc/sys/vm/drop_caches
$ time ls -al &> /dev/null
```

Output:
```
real    0m11.408s
user    0m0.029s
sys     0m0.210s
```