copy binary
```
cp -v ../builder_centos7_glibc_2.17/output/secOpsDispatcher_glibc_2.17 ./secOpsDispatcher
```

then start with
```
docker build -t centos_7_testbox . && docker run --rm -it centos_7_testbox /bin/bash
```
