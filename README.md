

# 基于WebSocket的五子棋对战平台

一个基于Spring Boot和WebSocket实现的五子棋在线对战平台，支持用户匹配、对战、积分排行等功能。

## 项目简介

本项目是一个完整的五子棋在线对战系统，采用前后端分离架构。前端提供友好的Web界面，后端基于Spring Boot框架构建，使用WebSocket实现实时双向通信。系统支持用户注册登录、三级匹配机制（青铜/白银/黄金）、实时对战、胜负判定及积分统计。

## 技术栈

后端技术：Spring Boot、Spring WebSocket、MyBatis、Lombok
前端技术：HTML5、CSS3、JavaScript、jQuery
数据库：MySQL
构建工具：Maven

## 功能特性

用户模块支持账号注册、登录以及基本信息管理，登录后可进入游戏大厅查看个人积分数据。对战模块采用三级匹配池设计，根据用户段位自动匹配实力相当的对手，匹配成功后进入房间进行实时对战，落子后自动判断胜负并更新积分。所有通信均通过WebSocket长连接实现，确保游戏过程流畅无延迟。

## 项目结构

核心代码位于src/main/java/org/gobang_battle目录下。config目录包含WebSocket配置，controller目录处理WebSocket请求和HTTP接口，game目录封装游戏相关的请求响应对象，mapper目录实现数据访问，model目录定义Room、RoomManager等核心业务模型，service目录提供用户业务逻辑。静态资源（HTML、CSS、图片）存放在src/main/resources/static目录。

## 环境要求

JDK 8或更高版本
Maven 3.x
MySQL 5.7或更高版本

## 快速开始

克隆项目后创建数据库并导入SQL脚本，修改application.yml中的数据库连接配置。执行mvn clean package命令打包，运行java -jar target/*.jar启动服务。访问http://localhost:8080即可进入游戏界面。

## WebSocket消息格式

匹配请求：{"message":"start_match"}
匹配响应：{"ok":true,"reason":"匹配成功","message":"matched"}
游戏准备：{"message":"game_ready","roomId":"xxx","thisUserId":1,"thatUserId":2,"whiteUser":1}
落子请求：{"message":"move","userId":1,"row":7,"col":8}
落子响应：{"message":"move","userId":1,"row":7,"col":8,"win":0}

## 许可证

本项目仅供学习交流使用。