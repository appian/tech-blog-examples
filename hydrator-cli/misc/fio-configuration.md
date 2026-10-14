### _File Hydration using Optimized FIO Command_

```sh
$ du -sh /path/to/large/file
12G     /path/to/large/file
```

### _Running Optimized FIO Command on Cold EBS_

```sh
FILE="/path/to/large/file" && \
SIZE=$(wc -c < "$FILE" | tr -d ' ') && \
N=$(( (SIZE + 524288 - 1) / 524288 )) && \
D=$(( N < 3000 ? N : 3000 )) && \
time fio --name=skip-read \
    --filename="$FILE" \
    --rw=read \
    --bs=2k \
    --zonemode=strided \
    --zonesize=2k \
    --zoneskip=510k \
    --number_ios=$N \
    --direct=1 \
    --ioengine=libaio \
    --iodepth=$D \
    --iodepth_batch_submit=$D \
    --iodepth_batch_complete_min=1
```

Output:
```
skip-read: (g=0): rw=read, bs=(R) 2048B-2048B, (W) 2048B-2048B, (T) 2048B-2048B, ioengine=libaio, iodepth=3000
fio-3.32
Starting 1 process
Jobs: 1 (f=1): [R(1)][88.2%][r=490KiB/s][r=245 IOPS][eta 00m:13s]
skip-read: (groupid=0, jobs=1): err= 0: pid=10885: Wed Oct 14 09:35:00 2026
  read: IOPS=246, BW=494KiB/s (506kB/s)(47.1MiB/97663msec)
    slat (nsec): min=1475, max=11667M, avg=1455288089.41, stdev=3849724477.59
    clat (usec): min=188, max=12519k, avg=10696727.66, stdev=3119497.49
     lat (msec): min=274, max=23981, avg=12152.02, stdev=3359.64
    clat percentiles (msec):
     |  1.00th=[  625],  5.00th=[ 2601], 10.00th=[ 5000], 20.00th=[ 9866],
     | 30.00th=[12013], 40.00th=[12147], 50.00th=[12147], 60.00th=[12147],
     | 70.00th=[12147], 80.00th=[12281], 90.00th=[12281], 95.00th=[12281],
     | 99.00th=[12416], 99.50th=[12416], 99.90th=[12416], 99.95th=[12416],
     | 99.99th=[12550]
   bw (  KiB/s): min=  340, max=  536, per=99.24%, avg=490.95, stdev=22.96, samples=172
   iops        : min=  170, max=  268, avg=245.48, stdev=11.48, samples=172
  lat (usec)   : 250=0.03%
  lat (msec)   : 100=0.09%, 250=0.15%, 500=0.52%, 750=0.49%, 1000=0.49%
  lat (msec)   : 2000=2.06%, >=2000=96.17%
  cpu          : usr=0.03%, sys=0.07%, ctx=3437, majf=0, minf=1515
  IO depths    : 1=0.0%, 2=0.0%, 4=0.0%, 8=0.0%, 16=0.0%, 32=0.0%, >=64=99.5%
     submit    : 0=0.0%, 4=100.0%, 8=0.0%, 16=0.0%, 32=0.0%, 64=0.0%, >=64=0.1%
     complete  : 0=0.0%, 4=100.0%, 8=0.0%, 16=0.0%, 32=0.0%, 64=0.0%, >=64=0.1%
     issued rwts: total=24110,0,0,0 short=0,0,0,0 dropped=0,0,0,0
     latency   : target=0, window=0, percentile=100.00%, depth=3000

Run status group 0 (all jobs):
   READ: bw=494KiB/s (506kB/s), 494KiB/s-494KiB/s (506kB/s-506kB/s), io=47.1MiB (49.4MB), run=97663-97663msec

Disk stats (read/write):
    dm-6: ios=24108/0, merge=0/0, ticks=5939200/0, in_queue=5939200, util=99.91%, aggrios=24111/0, aggrmerge=0/0, aggrticks=5843389/0, aggrin_queue=5843389, aggrutil=99.76%
  nvme2n1: ios=24111/0, merge=0/0, ticks=5843389/0, in_queue=5843389, util=99.76%

real    1m38.081s
user    0m0.428s
sys     0m0.088s
```

> Optimized FIO on Cold File _Ran for 1m 38s_

### _Rerunning Optimized FIO Command on Hot EBS (Cached)_

```sh
FILE="/path/to/large/file" && \
SIZE=$(wc -c < "$FILE" | tr -d ' ') && \
N=$(( (SIZE + 524288 - 1) / 524288 )) && \
D=$(( N < 3000 ? N : 3000 )) && \
time fio --name=skip-read \
    --filename="$FILE" \
    --rw=read \
    --bs=2k \
    --zonemode=strided \
    --zonesize=2k \
    --zoneskip=510k \
    --number_ios=$N \
    --direct=1 \
    --ioengine=libaio \
    --iodepth=$D \
    --iodepth_batch_submit=$D \
    --iodepth_batch_complete_min=1
```

Output:
```
skip-read: (g=0): rw=read, bs=(R) 2048B-2048B, (W) 2048B-2048B, (T) 2048B-2048B, ioengine=libaio, iodepth=3000
fio-3.32
Starting 1 process
Jobs: 1 (f=1): [R(1)][87.5%][r=5992KiB/s][r=2996 IOPS][eta 00m:01s]
skip-read: (groupid=0, jobs=1): err= 0: pid=10941: Wed Oct 14 09:36:19 2026
  read: IOPS=3425, BW=6850KiB/s (7015kB/s)(47.1MiB/7039msec)
    slat (nsec): min=1379, max=47278k, avg=6147184.57, stdev=15451027.30
    clat (usec): min=127, max=1020.0k, avg=869526.59, stdev=267889.03
     lat (msec): min=21, max=1020, avg=875.67, stdev=258.74
    clat percentiles (msec):
     |  1.00th=[   14],  5.00th=[  178], 10.00th=[  380], 20.00th=[  776],
     | 30.00th=[ 1003], 40.00th=[ 1003], 50.00th=[ 1003], 60.00th=[ 1003],
     | 70.00th=[ 1003], 80.00th=[ 1003], 90.00th=[ 1003], 95.00th=[ 1003],
     | 99.00th=[ 1003], 99.50th=[ 1003], 99.90th=[ 1011], 99.95th=[ 1020],
     | 99.99th=[ 1020]
   bw (  KiB/s): min= 5992, max= 6244, per=87.83%, avg=6017.43, stdev=66.17, samples=14
   iops        : min= 2996, max= 3122, avg=3008.71, stdev=33.08, samples=14
  lat (usec)   : 250=0.10%, 500=0.06%, 750=0.08%, 1000=0.06%
  lat (msec)   : 2=0.27%, 4=0.29%, 10=0.09%, 20=0.12%, 50=0.75%
  lat (msec)   : 100=1.22%, 250=3.75%, 500=6.24%, 750=6.21%, 1000=57.56%
  lat (msec)   : 2000=23.19%
  cpu          : usr=0.43%, sys=0.55%, ctx=3401, majf=0, minf=1512
  IO depths    : 1=0.0%, 2=0.0%, 4=0.0%, 8=0.0%, 16=0.0%, 32=0.0%, >=64=99.5%
     submit    : 0=0.0%, 4=100.0%, 8=0.0%, 16=0.0%, 32=0.0%, 64=0.0%, >=64=0.1%
     complete  : 0=0.0%, 4=100.0%, 8=0.0%, 16=0.0%, 32=0.0%, 64=0.0%, >=64=0.1%
     issued rwts: total=24110,0,0,0 short=0,0,0,0 dropped=0,0,0,0
     latency   : target=0, window=0, percentile=100.00%, depth=3000

Run status group 0 (all jobs):
   READ: bw=6850KiB/s (7015kB/s), 6850KiB/s-6850KiB/s (7015kB/s-7015kB/s), io=47.1MiB (49.4MB), run=7039-7039msec

Disk stats (read/write):
    dm-6: ios=23686/0, merge=0/0, ticks=419380/0, in_queue=419380, util=98.48%, aggrios=24110/0, aggrmerge=0/0, aggrticks=421456/0, aggrin_queue=421456, aggrutil=97.22%
  nvme2n1: ios=24110/0, merge=0/0, ticks=421456/0, in_queue=421456, util=97.22%

real    0m7.380s
user    0m0.331s
sys     0m0.048s
```

> Optimized FIO on Hot (Cache) File _Ran for 7.4s_

### _Running Optimized FIO Command on Hot (non-cached) EBS_

```sh
sync; echo 3 > /proc/sys/vm/drop_caches

FILE="/path/to/large/file" && \
SIZE=$(wc -c < "$FILE" | tr -d ' ') && \
N=$(( (SIZE + 524288 - 1) / 524288 )) && \
D=$(( N < 3000 ? N : 3000 )) && \
time fio --name=skip-read \
    --filename="$FILE" \
    --rw=read \
    --bs=2k \
    --zonemode=strided \
    --zonesize=2k \
    --zoneskip=510k \
    --number_ios=$N \
    --direct=1 \
    --ioengine=libaio \
    --iodepth=$D \
    --iodepth_batch_submit=$D \
    --iodepth_batch_complete_min=1
```

Output:
```
skip-read: (g=0): rw=read, bs=(R) 2048B-2048B, (W) 2048B-2048B, (T) 2048B-2048B, ioengine=libaio, iodepth=3000
fio-3.32
Starting 1 process
Jobs: 1 (f=1): [R(1)][87.5%][r=5992KiB/s][r=2996 IOPS][eta 00m:01s]
skip-read: (groupid=0, jobs=1): err= 0: pid=10989: Wed Oct 14 09:37:46 2026
  read: IOPS=3425, BW=6850KiB/s (7015kB/s)(47.1MiB/7039msec)
    slat (nsec): min=1443, max=45198k, avg=5889828.29, stdev=14768848.55
    clat (usec): min=123, max=1020.0k, avg=869833.94, stdev=267466.09
     lat (msec): min=21, max=1020, avg=875.72, stdev=258.74
    clat percentiles (msec):
     |  1.00th=[   17],  5.00th=[  178], 10.00th=[  380], 20.00th=[  785],
     | 30.00th=[ 1003], 40.00th=[ 1003], 50.00th=[ 1003], 60.00th=[ 1003],
     | 70.00th=[ 1003], 80.00th=[ 1003], 90.00th=[ 1003], 95.00th=[ 1003],
     | 99.00th=[ 1003], 99.50th=[ 1003], 99.90th=[ 1011], 99.95th=[ 1020],
     | 99.99th=[ 1020]
   bw (  KiB/s): min= 5992, max= 6220, per=87.83%, avg=6017.43, stdev=59.86, samples=14
   iops        : min= 2996, max= 3110, avg=3008.71, stdev=29.93, samples=14
  lat (usec)   : 250=0.07%, 500=0.06%, 750=0.08%, 1000=0.08%
  lat (msec)   : 2=0.33%, 4=0.22%, 10=0.09%, 20=0.12%, 50=0.75%
  lat (msec)   : 100=1.22%, 250=3.72%, 500=6.24%, 750=6.24%, 1000=55.80%
  lat (msec)   : 2000=24.97%
  cpu          : usr=0.24%, sys=0.78%, ctx=3372, majf=0, minf=1514
  IO depths    : 1=0.0%, 2=0.0%, 4=0.0%, 8=0.0%, 16=0.0%, 32=0.0%, >=64=99.5%
     submit    : 0=0.0%, 4=100.0%, 8=0.0%, 16=0.0%, 32=0.0%, 64=0.0%, >=64=0.1%
     complete  : 0=0.0%, 4=100.0%, 8=0.0%, 16=0.0%, 32=0.0%, 64=0.0%, >=64=0.1%
     issued rwts: total=24110,0,0,0 short=0,0,0,0 dropped=0,0,0,0
     latency   : target=0, window=0, percentile=100.00%, depth=3000

Run status group 0 (all jobs):
   READ: bw=6850KiB/s (7015kB/s), 6850KiB/s-6850KiB/s (7015kB/s-7015kB/s), io=47.1MiB (49.4MB), run=7039-7039msec

Disk stats (read/write):
    dm-6: ios=23681/0, merge=0/0, ticks=418940/0, in_queue=418940, util=98.64%, aggrios=24111/0, aggrmerge=0/0, aggrticks=421344/0, aggrin_queue=421344, aggrutil=97.09%
  nvme2n1: ios=24111/0, merge=0/0, ticks=421344/0, in_queue=421344, util=97.09%

real    0m7.424s
user    0m0.365s
sys     0m0.078s
```

> Optimized FIO on Hot (No Cache) File _Ran for 7.4s_