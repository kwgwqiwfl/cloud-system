@echo off
chcp 65001 >nul
cd /d "%~dp0%"

echo 正在启动服务...
start "" /b javaw -Xms1024m -Xmx4096m -cp "jar\*" com.ring.cloud.facade.CrawlFacade --spring.config.location=config/bootstrap.properties,config/common.properties,config/crawl.properties

echo 启动成功
timeout /t 3 /nobreak >nul
exit