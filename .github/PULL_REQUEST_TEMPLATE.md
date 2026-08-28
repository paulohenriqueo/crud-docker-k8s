<!-- PR guidelines

- How should I write the title?
  Use the following pattern:
    [commit type][context] Short description of what this PR does
  Valid types:
  - feature: adds new functionality
  - bugfix: fixes a bug in the current scope of work
  - hotfix: urgent fix on the stable branch
  - docs: documentation changes (docstrings, README, guides)
  - style: formatting only (linters and similar tooling)
  - refactor: code refactoring (new layers, renaming variables, classes and methods)
  - test: adds, adjusts or refactors tests
  - chore: updates files with no direct impact on the code (Makefile, .gitignore, .editorconfig)

- Title and body are written in ENGLISH. See CLAUDE.md, section 6.
-->

# Description

Add a short description of what this PR does.
Reference (issue): 

## Type of PR

Please delete the lines that are not relevant.

- [ ] Bug fix (does not change established behaviour, restores something that stopped working)
- [ ] New feature (does not change established behaviour, only adds to it)
- [ ] Refactor (breaks nothing, keeps established behaviour)
- [ ] Breaking change (changes behaviour that other parts of the system rely on)
- [ ] Tests (does not change behaviour, only adds tests)
- [ ] Infrastructure (Docker, Kubernetes, CI — no application code change)

If this is a bugfix, please answer the questions below.

## Root cause

The problem was caused by ...

## What changed

Changed the xpto function to take an extra input.

## Why it fixes it for everyone

It fixes every similar case because ...

# How did you test this?

Describe how to test the changes you made. Add any setup needed to reproduce the scenario.

- [ ] Test A
- [ ] Test B

# Checklist

- [ ] I left the code better than I found it
- [ ] I wrote tests
- [ ] I wrote small, pure, easy-to-test functions
- [ ] I tested it on my machine before opening this PR
- [ ] I did not commit any secret, `.env` file or hardcoded password

## Infrastructure checklist

Only if this PR touches Dockerfiles, Compose or Kubernetes manifests.

- [ ] Multi-stage Dockerfile — the final image carries only the runtime
- [ ] Base image pinned to a specific tag (no `:latest`)
- [ ] `.dockerignore` present and up to date
- [ ] Container does not run as root
- [ ] No secret in `ConfigMap` or hardcoded in a manifest
- [ ] Deployments declare `readinessProbe` and `livenessProbe`
- [ ] Containers declare `resources.requests` and `resources.limits`
- [ ] Persistent data lives in a volume / PVC, never inside the container
