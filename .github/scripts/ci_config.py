"""Validate catalog-driven CI inputs without configuring Gradle or requiring an SDK."""

import os
import re
from pathlib import Path


def integer(versions, name, minimum=1):
    value = versions.get(name, "")
    if not isinstance(value, str) or not re.fullmatch(r"[0-9]+", value):
        raise ValueError(f"{name} must be a decimal integer in libs.versions.toml")
    number = int(value)
    if number < minimum:
        raise ValueError(f"{name} must be at least {minimum}")
    return number


def build_outputs(versions):
    toolchain = integer(versions, "projectJavaVersion", 17)
    runtime = integer(versions, "projectBuildJavaVersion", 17)
    major = integer(versions, "projectCompileSdkVersion")
    minor = integer(versions, "projectCompileSdkMinorVersion", 0)
    minimum = integer(versions, "projectMinSdkVersion")
    target = integer(versions, "projectTargetSdkVersion")
    if not minimum <= target <= major:
        raise ValueError("SDK versions must satisfy minSdk <= targetSdk <= compileSdk")
    if runtime < toolchain:
        raise ValueError("Build JDK must not be older than the compilation toolchain")
    if major < 37 and minor != 0:
        raise ValueError("Minor SDK packages are only configured for API 37+")
    suffix = f"{major}.{minor}" if major >= 37 else str(major)
    return {
        "toolchain_java": str(toolchain),
        "build_java": str(runtime),
        "sdk_package": f"platforms;android-{suffix}",
        "java_environments": ",".join(
            f"JAVA_HOME_{version}_X64" for version in sorted({toolchain, runtime})
        ),
    }


def validate_wrapper(text):
    properties = {}
    for line in text.splitlines():
        stripped = line.strip()
        if stripped and not stripped.startswith(("#", "!")) and "=" in stripped:
            key, value = stripped.split("=", 1)
            if key in properties:
                raise ValueError(f"Duplicate wrapper property: {key}")
            properties[key] = value
    url = properties.get("distributionUrl", "").replace(r"\:", ":")
    if not re.fullmatch(
        r"https://services\.gradle\.org/distributions/gradle-[0-9]+(?:\.[0-9]+){1,2}-bin\.zip",
        url,
    ):
        raise ValueError("Wrapper must use an official stable HTTPS Gradle binary distribution")
    if not re.fullmatch(r"[a-fA-F0-9]{64}", properties.get("distributionSha256Sum", "")):
        raise ValueError("Wrapper must pin distributionSha256Sum to a SHA-256 digest")
    if properties.get("validateDistributionUrl") != "true":
        raise ValueError("Wrapper URL validation must be enabled")


def main():
    # GitHub's Ubuntu runner supplies Python 3.12. Local users need Python 3.11+.
    import tomllib

    root = Path(__file__).resolve().parents[2]
    with (root / "gradle/libs.versions.toml").open("rb") as source:
        outputs = build_outputs(tomllib.load(source)["versions"])
    validate_wrapper((root / "gradle/wrapper/gradle-wrapper.properties").read_text())
    for key, value in outputs.items():
        print(f"{key}={value}")
    if "GITHUB_OUTPUT" in os.environ:
        with open(os.environ["GITHUB_OUTPUT"], "a", encoding="utf-8") as output:
            for key, value in outputs.items():
                print(f"{key}={value}", file=output)


if __name__ == "__main__":
    main()
