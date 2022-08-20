# Fundraiser Tracker

An AWS Lambda application written in Quarkus uses DynamoDB as storage, exposes REST interface to get data and Slack command REST interface to manipulate data. Applications supports currency conversion with rates obtained from Monobank API.

[![Codacy Badge](https://app.codacy.com/project/badge/Grade/224eb30e7e5c4f38934ee9601e15237e)](https://www.codacy.com/gh/yuriytkach/fundraiser-tracker/dashboard?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=yuriytkach/fundraiser-tracker&amp;utm_campaign=Badge_Grade)
[![Codacy Badge](https://app.codacy.com/project/badge/Coverage/224eb30e7e5c4f38934ee9601e15237e)](https://www.codacy.com/gh/yuriytkach/fundraiser-tracker/dashboard?utm_source=github.com&utm_medium=referral&utm_content=yuriytkach/fundraiser-tracker&utm_campaign=Badge_Coverage)
![Build](https://github.com/yuriytkach/fundraiser-tracker/actions/workflows/maven-no-native.yml/badge.svg)

## Supported Slack commands

If you add a [slash command](https://api.slack.com/interactivity/slash-commands) to Slack (for example `/fund`) then the following commands are supported by the application.

**Create new fund:**
```text
/fund create car eur 5000 /Banderomobil/ blue
```
Creates new fund with short name `car` (will be used to track donations) with goal of €5000, with full description `Banderomobil` and color `blue` (used in UI tracker)

```text
/fund create dron usd 7000
```
Creates a new fund with short name and description `dron` with goal of $7000 and default color `green` on UI

**Supported currencies:**
`UAH, USD, EUR, PLN, GBP, CHF`

**List all funds:**
```text
/fund list
```
Displays all created funds with status (how much raised)

**Track donation:**
```text
/fund track car eur 500 Vasya 2022-05-12 14:15
/fund track car uah 500 Vasya 14:15             - track for today's date
/fund track car usd 500 Vasya                   - track for today's date and time
/fund track car usd 500                         - track for noname person
```
This will track donation for fund with short name `car`. 
- Any supported currency is allowed. Currency conversion takes place.
- Date and time is optional.
- Person name is optional. If not supplied, then `noname` is used.

**Delete fund:**
`Warning! This action cannot be undone! Use with caution!`
```text
/fund delete car
```
This will delete fund `car` with all recorded donations. _**Warning! - Cannot be undone!**_

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:
```shell script
./mvnw compile quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at http://localhost:8080/q/dev/.

## Packaging and running the application

The application can be packaged using:
```shell script
./mvnw package
```
It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:
```shell script
./mvnw package -Dquarkus.package.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Creating a native executable

You can create a native executable using: 
```shell script
./mvnw package -Pnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using: 
```shell script
./mvnw package -Pnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/fundraiser-tracker-1.0.0-SNAPSHOT-runner`

## Related Guides

- AWS Lambda HTTP ([guide](https://quarkus.io/guides/amazon-lambda-http)): Allow applications written for a servlet container to run in AWS Lambda
- YAML Configuration ([guide](https://quarkus.io/guides/config#yaml)): Use YAML to configure your Quarkus application
- Amazon DynamoDB ([guide](https://quarkiverse.github.io/quarkiverse-docs/quarkus-amazon-services/dev/amazon-dynamodb.html)): Connect to Amazon DynamoDB datastore
