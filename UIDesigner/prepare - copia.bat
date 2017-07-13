@echo off
DEL /F page-ADMINBulkReplay653.zip
C:\Software\7-Zip\7z.exe x page-ADMINBulkReplay.zip
mkdir temp
move page.properties temp
move resources temp/resources
move temp\resources\index.html temp
rem copy C:\Proyectos\AG2R\Development\RestDataAccesor\src\main\java\index.groovy temp
cd temp
C:\Software\7-Zip\7z.exe a ../page-ADMINBulkReplay653.zip 
cd ..
RD /S /Q temp
copy page-ADMINBulkReplay653.zip C:\BonitaBPM\BonitaBPMSubscription-6.5.3\workspace\tomcat\bonita\client\tenants\1\tmp\tmp-page-ADMINBulkReplay653.zip

echo File page-ADMINBulkReplay653.zip generated

