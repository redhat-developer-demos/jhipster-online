# Contributing to JHipster Online

Are you ready to contribute to JHipster Online? We'd love to have you on board, and we will help you as much as we can. Here are the guidelines we'd like you to follow so that we can be of more help:

- [Questions and help](#question)
- [Issues and Bugs](#issue)
- [Feature Requests](#feature)
- [Submission Guidelines](#submit)
- [Generator development setup](#setup)
- [Coding Rules](#rules)
- [Git Commit Guidelines](#commit)

And don't forget we also accept [financial contributions to the project](https://www.jhipster.tech/sponsors/) using OpenCollective.

## <a name="question"></a> Questions and help

This is the JHipster Online bug tracker, and it is used for [Issues and Bugs](#issue) and for [Feature Requests](#feature). It is **not** a help desk or a support forum.

If you have a question on using JHipster Online, or if you need help with your JHipster project, please [read our help page](https://www.jhipster.tech/help/) and use the [JHipster tag on StackOverflow](http://stackoverflow.com/tags/jhipster) or join our [Gitter.im chat room](https://gitter.im/jhipster/generator-jhipster).

## <a name="issue"></a> Issues and Bugs

If you find a bug in the source code or a mistake in the documentation, you can help us by [submitting a ticket](https://opensource.guide/how-to-contribute/#opening-an-issue) to our [GitHub issues](https://github.com/jhipster/jhipster-online/issues). Even better, you can submit a Pull Request to the [JHipster Online project](https://github.com/jhipster/jhipster-online).

**Please see the Submission Guidelines below**.

## <a name="feature"></a> Feature Requests

You can request a new feature by submitting a ticket to our [GitHub issues](https://github.com/jhipster/jhipster-online/issues). If you
would like to implement a new feature then consider what kind of change it is:

- **Major Changes** that you wish to contribute to the project should be discussed first. Please open a ticket which clearly states that it is a feature request in the title and explain clearly what you want to achieve in the description, and the JHipster team will discuss with you what should be done in that ticket. You can then start working on a Pull Request.
- **Small Changes** can be proposed without any discussion. Open up a ticket which clearly states that it is a feature request in the title. Explain your change in the description, and you can propose a Pull Request straight away.

## <a name="submit"></a> Submission Guidelines

### [Submitting an Issue](https://opensource.guide/how-to-contribute/#opening-an-issue)

Before you submit your issue search the [archive](https://github.com/jhipster/jhipster-online/issues?utf8=%E2%9C%93&q=is%3Aissue), maybe your question was already answered.

If your issue appears to be a bug, and has not been reported, open a new issue.
Help us to maximize the effort we can spend fixing issues and adding new
features, by not reporting duplicate issues. Providing the following information will increase the
chances of your issue being dealt with quickly:

- **Overview of the issue** - if an error is being thrown a stack trace helps
- **Motivation for or Use Case** - explain why this is a bug for you
- **Reproduce the error** - an unambiguous set of steps to reproduce the error. If you have a JavaScript error, maybe you can provide a live example with
  [JSFiddle](http://jsfiddle.net/)?
- **Related issues** - has a similar issue been reported before?
- **Suggest a Fix** - if you can't fix the bug yourself, perhaps you can point to what might be
  causing the problem (line of code or commit)
- **JHipster Online Version(s)** - is it a regression?
- **Browsers and Operating System** - is this a problem with all browsers or only IE8?

Click [here](https://github.com/jhipster/jhipster-online/issues/new) to open a ticket.

### [Submitting a Pull Request](https://opensource.guide/how-to-contribute/#opening-a-pull-request)

Before you submit your pull request consider the following guidelines:

- Search [GitHub](https://github.com/jhipster/jhipster-online/pulls?utf8=%E2%9C%93&q=is%3Apr) for an open or closed Pull Request
  that relates to your submission.
- Make your changes in a new git branch

  ```shell
  git checkout -b my-fix-branch main
  ```

- Create your patch, **including appropriate test cases**.
- Follow our [Coding Rules](#rules).
- Ensure that all tests pass

  ```shell
  ./mvnw verify -Pprod
  ```

- Test that the new project runs correctly:

  ```shell
  ./mvnw
  ```

- Commit your changes using a descriptive commit message that follows our
  [commit message conventions](#commit-message-format).

  ```shell
  git commit -a
  ```

  Note: the optional commit `-a` command line option will automatically "add" and "rm" edited files.

- Push your branch to GitHub:

  ```shell
  git push origin my-fix-branch
  ```

- In GitHub, send a pull request to `jhipster/jhipster-online:main`.
- If we suggest changes then

  - Make the required updates.
  - Re-run the JHipster Online tests on your sample generated project to ensure tests are still passing.
  - Rebase your branch and force push to your GitHub repository (this will update your Pull Request):

    ```shell
    git rebase main -i
    git push -f
    ```

That's it! Thank you for your contribution!

#### Resolving merge conflicts ("This branch has conflicts that must be resolved")

Sometimes your PR will have merge conflicts with the upstream repository's main branch. There are several ways to solve this but if not done correctly this can end up as a true nightmare. So here is one method that works quite well.

- First, fetch the latest information from the main

  ```shell
  git fetch upstream
  ```

- Rebase your branch against the upstream/main

  ```shell
  git rebase upstream/main
  ```

- Git will stop rebasing at the first merge conflict and indicate which file is in conflict. Edit the file, resolve the conflict then

  ```shell
  git add <the file that was in conflict>
  git rebase --continue
  ```

- The rebase will continue up to the next conflict. Repeat the previous step until all files are merged and the rebase ends successfully.
- Re-run the JHipster Online tests on your sample generated project to ensure tests are still passing.
- Force push to your GitHub repository (this will update your Pull Request)

  ```shell
  git push -f
  ```

#### After your pull request is merged

After your pull request is merged, you can safely delete your branch and pull the changes
from the main (upstream) repository:

- Delete the remote branch on GitHub either through the GitHub web UI or your local shell as follows:

  ```shell
  git push origin --delete my-fix-branch
  ```

- Check out the main branch:

  ```shell
  git checkout main -f
  ```

- Delete the local branch:

  ```shell
  git branch -D my-fix-branch
  ```

- Update your main with the latest upstream version:

  ```shell
  git pull --ff upstream main
  ```

## <a name="setup"></a> Generator development setup

### Prerequisites

- **Java 21** (JDK)
- **Node.js** ≥ 22.9 and **npm** ≥ 10 (see root `package.json` `engines`)
- **Docker** or **Podman** (for MySQL / MailHog / optional full stack)
- **Git**

### Option A — Classic split (fast UI reload)

1. Start MySQL (and optionally MailHog for email capture):

   ```shell
   docker compose -f src/main/docker/mysql.yml up -d
   docker compose -f src/main/docker/mailserver.yml up -d
   ```

2. Point `src/main/resources/config/application-dev.yml` at **localhost** MySQL if you are not in Dev Spaces (uncomment the `jdbc:mysql://localhost:3306/jhipster-online` block and comment the `mariadb` URL), or keep the `mariadb` host when using a compose network named `mariadb`.

3. Backend:

   ```shell
   ./mvnw
   ```

   Default dev server: [http://localhost:8080](http://localhost:8080).

4. Frontend (Webpack + BrowserSync):

   ```shell
   npm install
   npm start
   ```

   UI proxied at [http://localhost:9000](http://localhost:9000) (API calls go to `:8080`).

### Option B — Full stack in containers (single command)

From the repository root:

```shell
podman compose -f podman-compose.yml up --build
```

See [README.md — Full stack with Podman Compose](README.md#full-stack-with-podman-compose). No Quay login required; images are built locally.

### Optional environment variables

| Variable                                                                       | Purpose                                                                |
| ------------------------------------------------------------------------------ | ---------------------------------------------------------------------- |
| `APPLICATION_JHIPSTER8WORKER_ENABLED` / `APPLICATION_JHIPSTER8WORKER_BASE_URL` | Delegate .NET / NestJS / Azure ACA generation to the JHipster 8 worker |
| `APPLICATION_PYHIPSTERWORKER_ENABLED` / `APPLICATION_PYHIPSTERWORKER_BASE_URL` | Delegate Python/Flask to the PyHipster worker                          |
| `APPLICATION_JDL_AI_*`                                                         | JDL AI assistant (see root README)                                     |

Prefer `application-dev.yml` or your shell profile for local secrets; do not commit tokens.

## <a name="rules"></a> Coding Rules

To ensure consistency throughout the source code, keep these rules in mind as you are working:

- All features or bug fixes **must be tested** by one or more tests.
- All files must follow the [.editorconfig file](http://editorconfig.org/) located at the root of the JHipster generator project. Please note that generated projects use the same `.editorconfig` file, so that both the generator and the generated projects share the same configuration.
- Java files **must be** formatted using [Intellij IDEA's code style](http://confluence.jetbrains.com/display/IntelliJIDEA/Code+Style+and+Formatting). Please note that JHipster committers have a free Intellij IDEA Ultimate Edition for developing the project.
- Generators JavaScript files **must follow** the eslint configuration defined at the project root, which is based on [Airbnb JavaScript Style Guide](https://github.com/airbnb/javascript).
- Web apps JavaScript files **must follow** [Google's JavaScript Style Guide](https://google-styleguide.googlecode.com/svn/trunk/javascriptguide.xml).
- Angular Typescript files **must follow** [Official Angular style guide](https://angular.io/styleguide).

Please ensure to run `npm run lint` and `npm test` on the project root before submitting a pull request.

## <a name="commit"></a> Git Commit Guidelines

We have rules over how our git commit messages must be formatted. Please ensure to [squash](https://help.github.com/articles/about-git-rebase/#commands-available-while-rebasing) unnecessary commits so that your commit history is clean.

### <a name="commit-message-format"></a> Commit Message Format

Each commit message consists of a **header**, a **body** and a **footer**.

```
<header>
<BLANK LINE>
<body>
<BLANK LINE>
<footer>
```

Any line of the commit message cannot be longer 100 characters! This allows the message to be easier
to read on GitHub as well as in various git tools.

### Header

The Header contains a succinct description of the change:

- use the imperative, present tense: "change" not "changed" nor "changes"
- don't capitalize first letter
- no dot (.) at the end

### Body

If your change is simple, the Body is optional.

Just as in the Header, use the imperative, present tense: "change" not "changed" nor "changes".
The Body should include the motivation for the change and contrast this with previous behavior.

### Footer

The footer is the place to reference GitHub issues that this commit **Closes**.

You **must** use the [GitHub keywords](https://help.github.com/articles/closing-issues-via-commit-messages) for
automatically closing the issues referenced in your commit.

### Example

For example, here is a good commit message:

```
upgrade to Spring Boot 1.1.7

upgrade the Maven builds to use the new Spring Boot 1.1.7,
see http://spring.io/blog/2014/09/26/spring-boot-1-1-7-released

Fix #1234
```
