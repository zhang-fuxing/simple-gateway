## 编译

> 需要Java 17环境

```shell
cd $project_dir

mvn clean package

# 生成资源
$project_dir/target/simple_gateway-0.0.0.jar

# 使用

# 列出帮助信息
java -jar simple_gateway-0.0.0.jar --help

# 指定配置文件启动
java -jar simple_gateway-0.0.0.jar -f=/conf
```
