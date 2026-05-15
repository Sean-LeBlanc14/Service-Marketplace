#Branch Structure main → production-ready code, protected dev →
integration branch, all PRs merge here  
feature/ → individual feature branches, cut from dev

#Branch Naming ST-XX/… fix/… chore/…

#Commit Messages feat: add tutor profile creation endpoint fix:
resolve JWT expiry bug chore: update dependencies docs: add API
readme

#The Workflow

1. Pull the latest dev
2. Cut a new feature branch from dev
3. Do your work, commit often
4. Push the branch and open a PR into dev
5. Get at least 1 teammate's approval
6. Scrum master merges the PR
7. The branch is deleted automatically after the merge

#PR Rules No direct pushes to main or dev All PRs require at
least 1 approval before merging The branch must be up to date
with dev before merging Branches are deleted automatically after
a merge Sprint Release At the end of each sprint, dev is merged
into main and tagged as a release (v1.0, v2.0, v3.0).
