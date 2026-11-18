"""Validate a PR title passed through the environment, never interpolated as shell."""

import os
import re

TITLE = re.compile(
    r"(?:feat|fix|perf|refactor|test|docs|build|ci|chore|style|revert)"
    r"(?:\([a-z0-9][a-z0-9._/-]*\))?!?: [^\s].*"
)


def valid_title(title):
    return (
        bool(TITLE.fullmatch(title))
        and not any(character in title for character in "\r\n")
        and title == title.strip()
    )


def main():
    if not valid_title(os.environ.get("PR_TITLE", "")):
        raise SystemExit(
            "PR title must follow Conventional Commits, e.g. "
            "feat(downloads): add episode queue controls"
        )
    print("PR title follows Conventional Commits.")


if __name__ == "__main__":
    main()
