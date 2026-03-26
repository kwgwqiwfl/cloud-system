@echo off
chcp 65001 >nul
cd /d "%~dp0"

:: 停止旧进程（避免端口占用，确保调试环境干净）
for /f "tokens=2 delims=," %%i in ('wmic process where "name='javaw.exe' and commandline like '%%CrawlFacade%%'" get processid^,commandline /format:csv ^| findstr /i "CrawlFacade"') do (
    taskkill /f /pid %%i >nul 2>&1
)

:: 前台启动（输出完整日志，方便调试，核心命令与后台版一致）
java -cp "jar\*" com.ring.cloud.facade.CrawlFacade ^
    --spring.config.location=config/bootstrap.properties,config/common.properties,config/crawl.properties

:: 启动失败后暂停，保留报错日志
echo.
echo 程序已退出，按任意键关闭窗口...
pause >nul