@echo off
DEL /F page-ADMINBonitaDSN653.zip
rem "C:\Program Files\7-Zip\7z.exe" x page-ADMINBonitaDSN.zip
C:\Software\7-Zip\7z.exe x page-ADMINBonitaDSN.zip
mkdir temp
move page.properties temp
move resources temp/resources
move temp\resources\index.html temp

cd temp
rem "C:\Program Files\7-Zip\7z.exe" a ../page-ADMINBonitaDSN653.zip 
C:\Software\7-Zip\7z.exe a ../page-ADMINBonitaDSN653.zip 
cd ..
RD /S /Q temp
rem copy page-ADMINBulkReplayExt653.zip C:\BonitaBPM\BonitaBPMSubscription-6.5.3\workspace\tomcat\bonita\client\tenants\1\tmp\tmp-page-ADMINBulkReplay653.zip

echo File page-ADMINBonitaDSN653.zip generated

