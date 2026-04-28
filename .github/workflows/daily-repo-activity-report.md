---
description: |
  Create a daily digest issue summarizing recent repository activity
  (issues, pull requests, commits, releases, and discussions) with
  concise insights and suggested next actions.
on:
  schedule: daily on weekdays
permissions:
  contents: read
  issues: read
  pull-requests: read
tools:
  github:
    toolsets: [default]
safe-outputs:
  mentions: false
  allowed-github-references: []
  create-issue:
    max: 1
    title-prefix: "[daily-repo-activity] "
    labels: [report, daily-status]
    close-older-issues: true
---
# Daily Repository Activity Report

Create one GitHub issue that summarizes repository activity from the last 24 hours.

## Objectives

- Give maintainers a fast daily snapshot of what changed.
- Highlight important trends and potential blockers.
- Suggest a short, actionable follow-up list.

## Data to gather

1. New and updated issues
2. New and merged pull requests
3. Recent commits on the default branch
4. New releases and tags (if any)
5. High-signal discussions/comments (if available)

## Report format

Use this issue template:

### Daily Repository Activity Report ({{date}})

#### Summary
- 3-6 bullets covering the most important activity.

#### By the numbers
- Issues opened/closed
- PRs opened/merged/closed
- Commits merged to default branch
- Releases published

#### Notable items
- Top 3-7 notable changes (PRs, issues, commits, or releases) with links and one-line context.

#### Risks or blockers
- Any stalled PRs, failing checks, hot issues, or unanswered questions.

#### Recommended next actions
- 3-5 concrete maintainer actions for today.

## Constraints

- Keep the report concise and skimmable.
- Avoid speculation; prefer evidence from repository activity.
- If activity is low, explicitly say so and keep the issue short.
