#!/usr/bin/env python3
"""Run Espresso with local fixture credentials, without putting the password in shell arguments.

Usage: python3 scripts/run_espresso_tests.py
       python3 scripts/run_espresso_tests.py --classes com.splitsmart.app.RegisterTest
Credentials: .gradle/espresso-test-credentials.json, or --credentials PATH.
"""
import argparse
import json
import os
from pathlib import Path
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--classes', help='Comma-separated fully qualified test classes (default: all)')
    parser.add_argument('--credentials', type=Path, default=ROOT / '.gradle/espresso-test-credentials.json')
    args = parser.parse_args()
    credentials = args.credentials.resolve()
    if not credentials.is_file():
        parser.error('Local test credentials are missing. See docs/testing-max.md.')
    primary = json.loads(credentials.read_text())['accounts']['primary']
    if primary.get('email') != 'espresso.test@usc.edu' or not primary.get('password'):
        parser.error('Use the dedicated espresso.test@usc.edu fixture account.')
    env = os.environ.copy()
    env.setdefault('JAVA_HOME', '/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home')
    env.setdefault('ANDROID_HOME', str(Path.home() / 'Library/Android/sdk'))
    env['SPLITSMART_TEST_CREDENTIALS'] = str(credentials)
    # The init script reads the password inside Gradle. No secret is embedded in the script,
    # command line, tracked project properties, or generated APK.
    init = '''
import groovy.json.JsonSlurper
allprojects {
    afterEvaluate { project ->
        if (project.plugins.hasPlugin('com.android.application')) {
            def data = new JsonSlurper().parse(new File(System.getenv('SPLITSMART_TEST_CREDENTIALS')))
            project.android.defaultConfig.testInstrumentationRunnerArguments.putAll([
                testEmail: data.accounts.primary.email,
                testPassword: data.accounts.primary.password
            ])
        }
    }
}
'''
    with tempfile.TemporaryDirectory(prefix='splitsmart-espresso-') as tmp:
        init_file = Path(tmp) / 'credentials.init.gradle'
        init_file.write_text(init)
        command = [str(ROOT / 'gradlew'), '--init-script', str(init_file), 'connectedDebugAndroidTest']
        if args.classes:
            command.append('-Pandroid.testInstrumentationRunnerArguments.class=' + args.classes)
        process = subprocess.Popen(command, cwd=ROOT, env=env, stdout=subprocess.PIPE,
                                   stderr=subprocess.STDOUT, text=True)
        for line in process.stdout:
            print(line.replace(primary['password'], '[REDACTED]'), end='', flush=True)
        return process.wait()


if __name__ == '__main__':
    raise SystemExit(main())
