## _File Hydration using Hydrator Command_

```sh
$ du -sh /path/to/large/file
12G     /path/to/large/file
```

### _Running Hydrator on Cold EBS_

```sh
$ time /usr/lib/jvm/java-25-amazon-corretto.x86_64/bin/java -cp app.jar com.appian.hydrator.HydratorCli hydrate /path/to/large/file
```

Output:
```
[hydrate] progress: 0 file(s), 2323 block(s), 4.54 MB read, 1.14 GB hydrated, in-flight=2786
[hydrate] progress: 0 file(s), 4938 block(s), 9.64 MB read, 2.41 GB hydrated, in-flight=2791
[hydrate] progress: 0 file(s), 7554 block(s), 14.75 MB read, 3.69 GB hydrated, in-flight=2799
[hydrate] progress: 0 file(s), 10173 block(s), 19.87 MB read, 4.97 GB hydrated, in-flight=2800
[hydrate] progress: 0 file(s), 12854 block(s), 25.11 MB read, 6.28 GB hydrated, in-flight=2800
[hydrate] progress: 0 file(s), 15482 block(s), 30.24 MB read, 7.56 GB hydrated, in-flight=2806
[hydrate] progress: 0 file(s), 18108 block(s), 35.37 MB read, 8.84 GB hydrated, in-flight=2807
[hydrate] progress: 0 file(s), 20757 block(s), 40.54 MB read, 10.14 GB hydrated, in-flight=2814
[hydrate] progress: 0 file(s), 23381 block(s), 45.67 MB read, 11.42 GB hydrated, in-flight=729
Hydrated 1 file(s), 24110 block(s), 47.09 MB read, 11.77 GB hydrated, 0 error(s)

real    1m33.865s
user    0m1.370s
sys     0m1.033s
```

> Hydrator CLI on Hot (Cache) File _Ran for 1m 33s_

### _Rerunning Optimized FIO Command on Hot EBS (Cached)_


```sh
$ time /usr/lib/jvm/java-25-amazon-corretto.x86_64/bin/java -cp app.jar com.appian.hydrator.HydratorCli hydrate /path/to/large/file
```

Output:
```
Hydrated 1 file(s), 24110 block(s), 47.09 MB read, 11.77 GB hydrated, 0 error(s)

real    0m9.195s
user    0m0.672s
sys     0m0.231s
```

> Hydrator CLI on Hot (Cache) File _Ran for 9s_

### _Running Optimized FIO Command on Hot (non-cached) EBS_


```sh
$ sync; echo 3 > /proc/sys/vm/drop_caches
$ time /usr/lib/jvm/java-25-amazon-corretto.x86_64/bin/java -cp app.jar com.appian.hydrator.HydratorCli hydrate /path/to/large/file
```

Output:
```
Hydrated 1 file(s), 24110 block(s), 47.09 MB read, 11.77 GB hydrated, 0 error(s)

real    0m9.543s
user    0m0.839s
sys     0m0.913s
```

> Hydrator CLI on Hot (No Cache) File _Ran for 9.5s_