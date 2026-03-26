@echo off
chcp 65001 >nul
cd /d "%~dp0"

for /f "skip=1 tokens=*" %%a in ('wmic process where "name='javaw.exe' and commandline like '%%CrawlFacade%%'" get processid /value 2^>nul') do (
    for /f "tokens=2 delims==" %%b in ("%%a") do (
        taskkill /f /pid %%b >nul 2>&1
    )
)

echo ==============================
echo 程序停止完成！
echo ==============================

timeout /t 5 /nobreak >nul
exit