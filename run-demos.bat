@echo off
echo ========================================
echo SyncBridge 配置化数据同步平台 - 运行Demo
echo ========================================
echo.
echo 正在编译并运行测试Demo...
echo.
cd /d %~dp0

echo [1/8] 编译项目...
mvn clean compile -DskipTests

echo.
echo [2/8] 运行采购申请单同步Demo (DB→API)...
mvn exec:java -Dexec.mainClass="com.sync.demo.DemoPurchaseOrderSync" -Dexec.classpathScope=test

echo.
echo [3/8] 运行销售订单DB同步Demo (DB→DB)...
mvn exec:java -Dexec.mainClass="com.sync.demo.DemoSalesOrderDbToDbSync" -Dexec.classpathScope=test

echo.
echo [4/8] 运行供应商API同步Demo (API→DB)...
mvn exec:java -Dexec.mainClass="com.sync.demo.DemoSupplierApiToDbSync" -Dexec.classpathScope=test

echo.
echo [5/8] 运行客户API同步Demo (API→API)...
mvn exec:java -Dexec.mainClass="com.sync.demo.DemoCustomerApiToApiSync" -Dexec.classpathScope=test

echo.
echo [6/8] 运行Cloud数据同步Job Demo (热更新配置)...
mvn exec:java -Dexec.mainClass="com.sync.demo.DemoCloudDataSyncJob" -Dexec.classpathScope=test

echo.
echo [7/8] 运行金蝶↔MES双向同步Demo...
mvn exec:java -Dexec.mainClass="com.sync.demo.DemoKingdeeMesBidirectionalSync" -Dexec.classpathScope=test

echo.
echo [8/8] 运行Excel/SQL文件数据同步Demo...
mvn exec:java -Dexec.mainClass="com.sync.demo.DemoFileDataSync" -Dexec.classpathScope=test

echo.
echo ========================================
echo 所有Demo运行完成！
echo ========================================
pause
