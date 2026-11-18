import re
import unittest
from pathlib import Path

from check_pr_title import valid_title
from ci_config import build_outputs, validate_wrapper


class TitleTests(unittest.TestCase):
    def test_valid_titles(self):
        for title in (
            "feat(downloads): add offline queue",
            "fix(player)!: change episode identity",
            "chore: update dependencies",
            "ci(deps): bump gradle/actions from 5 to 6",
            "build!: change the minimum Android version",
            'docs: explain $(shell text) without executing it',
        ):
            with self.subTest(title=title):
                self.assertTrue(valid_title(title))

    def test_invalid_titles(self):
        for title in ("", "Update app", "feat: ", "Feat: add home", "fix:\nrun code", "fix: ok\n", "fix: ok "):
            with self.subTest(title=title):
                self.assertFalse(valid_title(title))


class ConfigTests(unittest.TestCase):
    def setUp(self):
        self.versions = {
            "projectJavaVersion": "17",
            "projectBuildJavaVersion": "21",
            "projectCompileSdkVersion": "37",
            "projectCompileSdkMinorVersion": "0",
            "projectMinSdkVersion": "26",
            "projectTargetSdkVersion": "36",
        }

    def test_current_sdk_and_jdks(self):
        self.assertEqual(
            build_outputs(self.versions),
            {
                "toolchain_java": "17",
                "build_java": "21",
                "sdk_package": "platforms;android-37.0",
                "java_environments": "JAVA_HOME_17_X64,JAVA_HOME_21_X64",
            },
        )

    def test_old_sdk_package(self):
        self.versions["projectCompileSdkVersion"] = "36"
        self.assertEqual(build_outputs(self.versions)["sdk_package"], "platforms;android-36")

    def test_duplicate_jdk_is_deduplicated(self):
        self.versions["projectBuildJavaVersion"] = "17"
        self.assertEqual(build_outputs(self.versions)["java_environments"], "JAVA_HOME_17_X64")

    def test_rejects_bad_catalog_values(self):
        cases = (
            ("projectJavaVersion", "17\nmalicious=true"),
            ("projectBuildJavaVersion", "16"),
            ("projectCompileSdkMinorVersion", "-1"),
            ("projectTargetSdkVersion", "38"),
            ("projectMinSdkVersion", "40"),
        )
        for key, value in cases:
            with self.subTest(key=key):
                values = dict(self.versions, **{key: value})
                with self.assertRaises(ValueError):
                    build_outputs(values)

    def test_missing_setting_fails(self):
        del self.versions["projectBuildJavaVersion"]
        with self.assertRaises(ValueError):
            build_outputs(self.versions)


class WorkflowTests(unittest.TestCase):
    def test_android_setup_overrides_obsolete_default_packages(self):
        workflow = (Path(__file__).resolve().parents[1] / "workflows/ci.yml").read_text()
        setup = re.search(
            r"uses: android-actions/setup-android@[^\n]+\n(?P<inputs>(?: {8,}[^\n]*\n)+)",
            workflow,
        )
        self.assertIsNotNone(setup, "Android SDK setup step must exist")
        packages = re.search(r"(?m)^\s+packages:\s+([^\n#]+)", setup.group("inputs"))
        self.assertIsNotNone(packages, "Override v3's obsolete default tools package explicitly")
        self.assertEqual(packages.group(1).strip(), "platform-tools")


class WrapperTests(unittest.TestCase):
    def setUp(self):
        self.wrapper = (
            "distributionUrl=https\\://services.gradle.org/distributions/gradle-9.9.1-bin.zip\n"
            f"distributionSha256Sum={'a' * 64}\n"
            "validateDistributionUrl=true\n"
        )

    def test_valid_wrapper(self):
        validate_wrapper(self.wrapper)

    def test_rejects_missing_hash(self):
        with self.assertRaises(ValueError):
            validate_wrapper(self.wrapper.replace(f"distributionSha256Sum={'a' * 64}\n", ""))

    def test_rejects_untrusted_url(self):
        with self.assertRaises(ValueError):
            validate_wrapper(self.wrapper.replace("services.gradle.org", "example.com"))

    def test_rejects_disabled_validation(self):
        with self.assertRaises(ValueError):
            validate_wrapper(self.wrapper.replace("validateDistributionUrl=true", "validateDistributionUrl=false"))

    def test_rejects_duplicate_properties(self):
        with self.assertRaises(ValueError):
            validate_wrapper(self.wrapper + "validateDistributionUrl=false\n")


if __name__ == "__main__":
    unittest.main()
