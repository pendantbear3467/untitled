# tests

Python-oriented validation suite for tooling and platform behaviors.

Current scope:

- Asset Studio convergence/stabilization checks
- framework validation checks
- skill tree platform tests

How to run:

```powershell
python -m unittest discover -s tests -p "test_*.py"
```

Notes:

- these tests primarily validate Python tooling behavior, not Forge runtime gameplay logic
- Java/Forge compile and runtime checks still run through Gradle (`gradlew.bat compileJava`, `gradlew.bat check`)
