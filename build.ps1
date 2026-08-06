$cacheDir = "C:\Users\tinysx\.gradle\caches\modules-2\files-2.1"
$jars = Get-ChildItem $cacheDir -Recurse -Filter "*.jar" | Where-Object { 
    $_.Name -notlike "*-sources.jar" -and $_.Name -notlike "*-javadoc.jar" 
} | Select-Object -ExpandProperty FullName

$cp = $jars -join ";"
$cp += ";C:\Users\tinysx\Desktop\work\26.1\plugins\PlaceholderAPI-2.12.3.jar"

if (Test-Path "build\classes") {
    Remove-Item -Recurse -Force "build\classes"
}
New-Item -ItemType Directory -Force -Path "build\classes" | Out-Null

$javaFiles = Get-ChildItem "src\main\java" -Recurse -Filter "*.java" | Select-Object -ExpandProperty FullName
Write-Host "Compiling $($javaFiles.Count) Java files with full classpath..."
javac -cp $cp -d build\classes $javaFiles

if ($LASTEXITCODE -eq 0) {
    Write-Host "Creating PersonaChat-1.0.jar..."
    Copy-Item "src\main\resources\*" -Destination "build\classes\" -Recurse -Force
    jar cvf "PersonaChat-1.0.jar" -C build\classes .
    Copy-Item "PersonaChat-1.0.jar" -Destination "PersonaChat-1.1.0.jar" -Force
    Copy-Item "PersonaChat-1.0.jar" -Destination "CustomEmotePlugin-1.0.0.jar" -Force
    Write-Host "SUCCESS: PersonaChat-1.0.jar built successfully!"
} else {
    Write-Host "Compilation failed."
}
