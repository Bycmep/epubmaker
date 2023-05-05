@echo off
cd %1
del temp.zip
set zip="c:\Program Files\7-Zip\7z.exe"
%zip% a -mx=0 -mm=Copy temp.zip mimetype
%zip% a -mx=9 temp.zip META-INF OEBPS
ren temp.zip %1.epub
move %1.epub ..
cd ..
pause
