@echo off
DEL /F page-ADMINBulkReplayExt653.zip
"C:\Program Files\7-Zip\7z.exe" x page-ADMINBulkReplayExt.zip
mkdir temp
move page.properties temp
move resources temp/resources
move temp\resources\index.html temp

cd temp
"C:\Program Files\7-Zip\7z.exe" a ../page-ADMINBulkReplayExt653.zip 
cd ..
RD /S /Q temp
rem copy page-ADMINBulkReplayExt653.zip C:\BonitaBPM\BonitaBPMSubscription-6.5.3\workspace\tomcat\bonita\client\tenants\1\tmp\tmp-page-ADMINBulkReplay653.zip

echo File page-ADMINBulkReplay653.zip generated

