<!DOCTYPE html>
<html lang="zh">
<head>
    <meta charset="UTF-8">
    <title>File Browse</title>
    <link rel="stylesheet" href="./layui/css/layui.css">
    <script src="./layui/layui.js"></script>
    <style>
        /* 隔行换色 */
        .layui-table tbody tr:nth-child(even) {
            background-color: #99cce1;
        }

        /* 鼠标悬停高亮效果 */
        .layui-table tbody tr:hover {
            background-color: #9fdf7c !important;
            transition: background-color 0.2s ease;
        }

        /* 确保链接在悬停时也保持良好的可读性 */
        .layui-table tbody tr:hover td a {
            color: #000000;
        }
    </style>
</head>
<body>
<div>
    <table class="layui-table">
        <thead>
        <tr>
            <th style="width: 50%">名称</th>
            <th style="width: 10%">类型</th>
            <th style="width: 10%">大小</th>
            <th style="width: 20%">操作</th>
        </tr>
        </thead>
        <tbody>
        <tr>
            <td></td>
            <td></td>
            <td></td>
            <td class="layui-table-link"><a href="#" onclick="returnBack()" class="layui-btn">返回</a></td>
        </tr>
        <#list files as file>
            <tr>
                <td>${file.name}</a></td>
                <td>${file.type}</td>
                <td>${file.size}</td>
                <td>
                    <#if file.dir==0>
                        <a href="${file.uri}?action=download" class="layui-btn">下载</a>
                        <#if file.canView==1>
                            <a href="${file.uri}" target="_blank" class="layui-btn">查看</a>
                        </#if>
                    <#else>
                        <a href="${file.uri}" class="layui-btn">浏览</a>
                    </#if>
                </td>
            </tr>
        </#list>
        </tbody>
    </table>
    <script>
        function returnBack() {
            window.history.back();
        }
    </script>
</div>
</body>
</html>