@echo off
echo ========================================
echo SyncBridge 配置化数据同步平台
echo ========================================
echo.
echo 正在启动项目（SQLite 开发模式）...
echo.
echo 访问地址: http://localhost:8080
echo 前端地址: http://localhost:3000 (需另外启动)
echo.
cd /d %~dp0
mvn spring-boot:run -Dspring-boot.run.arguments=--spring.profiles.active=dev
pause
