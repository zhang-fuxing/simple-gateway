## 编译

> 注意：需要Java 17环境

```shell
cd simple_gateway

mvn clean package

# 生成资源路径如下
# simple_gateway/target/simple_gateway-0.0.0.jar

# 使用反射
# 列出帮助信息
java -jar simple_gateway-0.0.0.jar --help

# 指定配置文件启动
java -jar simple_gateway-0.0.0.jar -f=/conf
# 生成配置文件
java -jar simple_gateway-0.0.0.jar --config-create
```
example：

> 监听80端口，将api开头的请求转发到8080端口，配置root和index后符合规则的视为静态资源请求
> order: 路由匹配顺序，默认值为int最大值，值越小越优先匹配，如果值相同，则按顺序匹配
> source: 路由匹配规则，正则表达式
> target: 路由转发目标
> root: 静态资源目录
> index: 静态资源目录下的默认文件

```json
{
    "http": [
        {
            "port": 80,
            "routes": [
                {
                    "source": "^/api/(.*)",
                    "target": "http://localhost:8080/api/$1",
                    "order": 1
                },
                {
                    "source": "^/",
                    "root": "/opt/www/web/resource/80",
                    "index": "index.html"
                }
            ]
        }
    ]
}
```